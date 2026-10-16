package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.compat.wom.WomBlackout;
import com.mdvlcraft.binder.monk.Fists;
import cursedfate.CursedfateMod;
import cursedfate.init.CursedfateModMobEffects;
import cursedfate.init.CursedfateModParticleTypes;
import cursedfate.network.CursedfateModVariables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@EventBusSubscriber(modid = "mdvlcraft")
public final class MonkArts {
    static final int FOCUS_TICKS = 200;
    static final float BLACK_FLASH_MULTIPLIER = 2.5F;
    static final int KNOCKOUT_WINDOW_TICKS = 12;
    static final float KNOCKOUT_MULTIPLIER = 1.5F;
    private static final double REACH = 4.0;
    private static final ResourceLocation CURSED_FATE_MELEE = ResourceLocation.fromNamespaceAndPath("cursedfate", "normal_damage");
    private static final Map<UUID, Long> FOCUSED = new HashMap<>();
    private static final Map<UUID, MonkArts.Art> ARTS = new HashMap<>();
    private static final Map<UUID, MonkArts.Knockout> KNOCKOUTS = new HashMap<>();

    private MonkArts() {
    }

    static boolean cast(ServerPlayer player, Technique technique) {
        return switch (technique) {
            case FOCUS -> focus(player);
            case KNOCKOUT -> knockout(player);
            default -> false;
        };
    }

    /** Focus for another ability (Mushin): the next strike within 10 s is a Black Flash. */
    public static void focusFor(ServerPlayer player) {
        focus(player);
    }

    private static float artMultiplier(Technique technique) {
        return switch (technique) {
            case HEAVY_BLOW, LEAPING_CRUSH -> 2.0F;
            case UPPERCUT, FOLLOW_UP_PUNCH -> 1.5F;
            case BARRAGE -> 0.35F;
            default -> 0.0F;
        };
    }

    private static int artWindow(Technique technique) {
        return technique != Technique.BARRAGE && technique != Technique.LEAPING_CRUSH ? 40 : 80;
    }

    static void noteCast(ServerPlayer player, Technique technique) {
        if (artMultiplier(technique) > 0.0F) {
            ARTS.put(player.getUUID(), new MonkArts.Art(technique, player.level().getGameTime() + artWindow(technique)));
        }
    }

    public static boolean artActive(ServerPlayer player) {
        MonkArts.Art art = ARTS.get(player.getUUID());
        return art != null && player.level().getGameTime() <= art.until();
    }

    private static float punch(ServerPlayer player) {
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (Fists.unarmed(player)) {
            damage += player.getAttributeValue(BinderAttributes.UNARMED_DAMAGE.get());
        }

        if (Fists.fisted(player)) {
            damage *= 1.0 + player.getAttributeValue(BinderAttributes.FIST_MASTERY.get());
        }

        return (float)damage;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArtHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
            && event.getSource().getDirectEntity() == player
            && event.getSource().typeHolder().unwrapKey().map(key -> key.location().getNamespace().equals("cursedfate")).orElse(false)
            && artActive(player)) {
            event.setAmount(punch(player) * artMultiplier(ARTS.get(player.getUUID()).technique()));
        }
    }

    private static boolean focus(ServerPlayer player) {
        FOCUSED.put(player.getUUID(), player.level().getGameTime() + 200L);
        play(player, sound("cursedfate", "blackflash", SoundEvents.BEACON_ACTIVATE), 0.6F, 2.0F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(
                (SimpleParticleType)CursedfateModParticleTypes.BLACK_FLASH_SPARK.get(),
                player.getX(),
                player.getY() + 1.2,
                player.getZ(),
                8,
                0.4,
                0.5,
                0.4,
                0.0
            );
        }

        actionBar(player, Component.translatable("ability.mdvlcraft.focus.ready").withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    private static boolean knockout(ServerPlayer player) {
        LivingEntity target = lookTarget(player);
        if (target == null) {
            actionBar(player, Component.translatable("ability.mdvlcraft.knockout.no_target").withStyle(ChatFormatting.RED));
            return false;
        }

        KNOCKOUTS.put(player.getUUID(), new MonkArts.Knockout(player.level().getGameTime() + 12L, target.getId()));
        ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && patch.isEpicFightMode()) {
            patch.playAnimationSynchronized(Animations.FIST_AUTO3, 0.0F);
        } else {
            land(player, target);
        }

        play(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
        return true;
    }

    private static void land(ServerPlayer player, LivingEntity target) {
        float damage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), damage);
    }

    private static LivingEntity lookTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(4.0));
        AABB box = player.getBoundingBox().expandTowards(player.getViewVector(1.0F).scale(4.0)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, eye, reach, box, e -> e instanceof LivingEntity livingx && livingx.isAlive() && !e.isSpectator() && e != player, 16.0
        );
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isStrike(DamageSource source, ServerPlayer player) {
        return source.getDirectEntity() != player
            ? false
            : source.is(DamageTypes.PLAYER_ATTACK) || source.typeHolder().unwrapKey().map(key -> key.location().equals(CURSED_FATE_MELEE)).orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && isStrike(event.getSource(), player)) {
            LivingEntity target = event.getEntity();
            long now = player.level().getGameTime();
            MonkArts.Knockout knockout = KNOCKOUTS.get(player.getUUID());
            if (knockout != null && (knockout.forced || now <= knockout.until) && event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
                knockout.landed = true;
                event.setAmount(event.getAmount() * 1.5F);
                WomBlackout.strike(player, target);
            }

            Long focused = FOCUSED.get(player.getUUID());
            if (focused != null) {
                FOCUSED.remove(player.getUUID());
                if (now <= focused) {
                    blackFlash(player, target, event);
                }
            }
        }
    }

    /** The Soul Shrine charged punch landed inside its domain (SoulpunchStopProcedure), without the music. */
    private static void blackFlash(ServerPlayer player, LivingEntity target, LivingHurtEvent event) {
        event.setAmount(event.getAmount() * 2.5F);
        target.addEffect(new MobEffectInstance((MobEffect)CursedfateModMobEffects.MOVEMENT_STUN.get(), 23, 1, false, false), player);
        play(target, sound("cursedfate", "blackflash", SoundEvents.GENERIC_EXPLODE), 1.0F, 1.0F);
        play(target, SoundEvents.METAL_BREAK, 1.3F, 0.8F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(
                (SimpleParticleType)CursedfateModParticleTypes.BLACK_FLASH_SPARK.get(),
                target.getX(),
                target.getY() + target.getBbHeight() / 2.0,
                target.getZ(),
                24,
                0.5,
                0.5,
                0.5,
                0.2
            );
            for (String fx : List.of("bloodspill", "blackflash", "soulpunchhitdomain", "soulshrinecut")) {
                level.getServer()
                    .getCommands()
                    .performPrefixedCommand(
                        target.createCommandSourceStack().withSuppressedOutput().withPermission(4),
                        "photon fx photon:" + fx + " entity @s 0 1 0 0 0 0 0 false true"
                    );
            }
        }

        screenFlash(player, 1);
        actionBar(player, Component.translatable("ability.mdvlcraft.focus.black_flash").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    /** Cursed Fate's three black flash screen frames, one a tick. */
    private static void screenFlash(ServerPlayer player, int frame) {
        player.getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            variables.BlackFlashBool = frame <= 3;
            variables.BlackFlashScreenNum = frame;
            variables.syncPlayerVariables(player);
        });
        if (frame <= 3) {
            CursedfateMod.queueServerWork(1, () -> screenFlash(player, frame + 1));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            MinecraftServer server = event.getServer();

            for (Entry<UUID, MonkArts.Knockout> entry : List.copyOf(KNOCKOUTS.entrySet())) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                MonkArts.Knockout knockout = entry.getValue();
                if (player == null) {
                    KNOCKOUTS.remove(entry.getKey());
                } else if (player.level().getGameTime() > knockout.until) {
                    if (!knockout.landed
                        && player.level().getEntity(knockout.targetId) instanceof LivingEntity living
                        && living.isAlive()
                        && living.distanceTo(player) <= 5.0) {
                        knockout.forced = true;
                        land(player, living);
                    }

                    KNOCKOUTS.remove(entry.getKey());
                }
            }

            FOCUSED.entrySet().removeIf(e -> server.getPlayerList().getPlayer(e.getKey()) == null);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        FOCUSED.remove(event.getEntity().getUUID());
        KNOCKOUTS.remove(event.getEntity().getUUID());
        ARTS.remove(event.getEntity().getUUID());
    }

    private static SoundEvent sound(String namespace, String path, SoundEvent fallback) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return sound != null ? sound : fallback;
    }

    private static void play(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    private record Art(Technique technique, long until) {
    }

    private static final class Knockout {
        final long until;
        final int targetId;
        boolean landed;
        boolean forced;

        Knockout(long until, int targetId) {
            this.until = until;
            this.targetId = targetId;
        }
    }
}
