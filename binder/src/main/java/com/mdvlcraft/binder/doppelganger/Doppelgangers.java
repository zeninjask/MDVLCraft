package com.mdvlcraft.binder.doppelganger;

import com.hm.efn.client.sound.EFNSounds;
import com.mdvlcraft.binder.ability.AbilityGrants;
import com.mdvlcraft.binder.ability.DoppelgangerAbility;
import com.mdvlcraft.binder.util.Teleports;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

@EventBusSubscriber(modid = "mdvlcraft")
public final class Doppelgangers {
    private static final UUID MIRROR_LISTENER = UUID.fromString("2f6d1c84-93b0-4c57-a7a2-58e1f0c3d9a4");
    private static final int UPKEEP_INTERVAL_TICKS = 20;
    private static final int UPKEEP_MANA = 1;
    private static final double TELEPORT_RANGE = 24.0;
    private static final double TELEPORT_BEHIND = 1.0;
    private static final int SWAP_HOLD_TICKS = 40;
    private static final Map<UUID, DoppelgangerEntity> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> HELD = new HashMap<>();

    private Doppelgangers() {
    }

    public static void press(ServerPlayer player) {
        DoppelgangerEntity clone = active(player);
        if (player.isShiftKeyDown()) {
            if (clone != null) {
                dismiss(player, clone);
            }
        } else if (clone == null) {
            summon(player);
        } else {
            HELD.put(player.getUUID(), player.server.getTickCount());
        }
    }

    public static void release(ServerPlayer player) {
        Integer start = HELD.remove(player.getUUID());
        DoppelgangerEntity clone = active(player);
        if (start != null && start >= 0 && clone != null) {
            LivingEntity target = lookedAt(player);
            if (target == null) {
                actionBar(player, Component.translatable("ability.mdvlcraft.doppelganger.no_target"));
            } else {
                sendBehind(player, clone, target);
            }
        }
    }

    @Nullable
    public static void dismissIfUngranted(ServerPlayer player) {
        DoppelgangerEntity clone = active(player);
        if (clone != null && AbilityGrants.level(player, DoppelgangerAbility.INSTANCE) == 0) {
            dismiss(player, clone);
        }
    }

    private static DoppelgangerEntity active(ServerPlayer player) {
        DoppelgangerEntity clone = ACTIVE.get(player.getUUID());
        if (clone != null && clone.isRemoved()) {
            ACTIVE.remove(player.getUUID());
            stopMirroring(player);
            return null;
        } else {
            return clone;
        }
    }

    private static void summon(ServerPlayer player) {
        if (!player.isCreative() && MagicData.getPlayerMagicData(player).getMana() < 1.0F) {
            actionBar(player, Component.translatable("ui.irons_spellbooks.cast_error_mana", Component.translatable("ability.mdvlcraft.doppelganger")));
        } else {
            DoppelgangerEntity clone = DoppelgangerSetup.DOPPELGANGER.get().create(player.level());
            if (clone != null) {
                clone.setOwner(player);
                clone.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                clone.setYHeadRot(player.getYHeadRot());
                player.level().addFreshEntity(clone);
                ACTIVE.put(player.getUUID(), clone);
                startMirroring(player, clone);
                effects(player.serverLevel(), clone.position(), (SoundEvent)EFNSounds.DOPPELGANGER_OPEN.get());
            }
        }
    }

    private static void dismiss(ServerPlayer player, DoppelgangerEntity clone) {
        ACTIVE.remove(player.getUUID());
        HELD.remove(player.getUUID());
        stopMirroring(player);
        if (!clone.isRemoved()) {
            effects(player.serverLevel(), clone.position(), (SoundEvent)EFNSounds.DOPPELGANGER_CLOSE.get());
            clone.discard();
        }
    }

    private static void startMirroring(ServerPlayer player, DoppelgangerEntity clone) {
        ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null) {
            patch.getEventListener().addEventListener(EventType.ACTION_EVENT_SERVER, MIRROR_LISTENER, event -> {
                DoppelgangerPatch doublePatch = (DoppelgangerPatch)EpicFightCapabilities.getEntityPatch(clone, DoppelgangerPatch.class);
                if (doublePatch != null && !clone.isRemoved()) {
                    LivingEntity target = doublePatch.getTarget();
                    if (target != null) {
                        clone.lookAt(Anchor.EYES, target.getEyePosition());
                    }

                    doublePatch.playAnimationSynchronized(event.getAnimation(), 0.0F);
                }
            });
        }
    }

    private static void stopMirroring(ServerPlayer player) {
        ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null) {
            patch.getEventListener().removeListener(EventType.ACTION_EVENT_SERVER, MIRROR_LISTENER);
        }
    }

    @Nullable
    private static LivingEntity lookedAt(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(24.0);
        Vec3 end = eye.add(reach);
        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
        if (blockHit.getType() != Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB area = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player,
            eye,
            end,
            area,
            entity -> entity instanceof LivingEntity livingx
                && livingx.isAlive()
                && !(entity instanceof DoppelgangerEntity)
                && !entity.isSpectator()
                && entity.isPickable(),
            eye.distanceToSqr(end)
        );
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void sendBehind(ServerPlayer player, DoppelgangerEntity clone, LivingEntity target) {
        Vec3 spot = Teleports.spotBehind(clone, target, 1.0);
        if (spot == null) {
            actionBar(player, Component.translatable("ability.mdvlcraft.doppelganger.no_room"));
        } else {
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, clone.getX(), clone.getY() + 0.5, clone.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
            clone.moveTo(spot.x, spot.y, spot.z, target.getYHeadRot(), clone.getXRot());
            clone.setDeltaMovement(Vec3.ZERO);
            clone.sendAfter(target);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, spot.x, spot.y + 0.5, spot.z, 15, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
    }

    private static void swap(ServerPlayer player, DoppelgangerEntity clone) {
        ServerLevel level = player.serverLevel();
        Vec3 mine = player.position();
        float myYaw = player.getYRot();
        float myPitch = player.getXRot();
        Vec3 theirs = clone.position();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, mine.x, mine.y + 1.0, mine.z, 20, 0.3, 0.5, 0.3, 0.1);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, theirs.x, theirs.y + 1.0, theirs.z, 20, 0.3, 0.5, 0.3, 0.1);
        player.teleportTo(level, theirs.x, theirs.y, theirs.z, clone.getYRot(), clone.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        clone.moveTo(mine.x, mine.y, mine.z, myYaw, myPitch);
        clone.setDeltaMovement(Vec3.ZERO);
        clone.holdPosition();
        level.playSound(null, theirs.x, theirs.y, theirs.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.8F);
        level.playSound(null, mine.x, mine.y, mine.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END && (!ACTIVE.isEmpty() || !HELD.isEmpty())) {
            MinecraftServer server = event.getServer();

            for (Entry<UUID, Integer> held : HELD.entrySet()) {
                if (held.getValue() >= 0 && server.getTickCount() - held.getValue() >= 40) {
                    held.setValue(-1);
                    ServerPlayer player = server.getPlayerList().getPlayer(held.getKey());
                    DoppelgangerEntity clone = player == null ? null : active(player);
                    if (clone != null) {
                        swap(player, clone);
                    }
                }
            }

            boolean upkeep = server.getTickCount() % 20 == 0;
            Iterator<Entry<UUID, DoppelgangerEntity>> entries = ACTIVE.entrySet().iterator();

            while (entries.hasNext()) {
                Entry<UUID, DoppelgangerEntity> entry = entries.next();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                DoppelgangerEntity clone = entry.getValue();
                if (player == null || clone.isRemoved()) {
                    entries.remove();
                    if (player != null) {
                        stopMirroring(player);
                    }

                    if (!clone.isRemoved()) {
                        clone.discard();
                    }
                } else if (upkeep && !player.isCreative() && !payUpkeep(player)) {
                    entries.remove();
                    stopMirroring(player);
                    effects(player.serverLevel(), clone.position(), (SoundEvent)EFNSounds.DOPPELGANGER_CLOSE.get());
                    clone.discard();
                    actionBar(player, Component.translatable("ability.mdvlcraft.doppelganger.out_of_mana"));
                }
            }
        }
    }

    private static boolean payUpkeep(ServerPlayer player) {
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < 1.0F) {
            return false;
        }

        magic.setMana(magic.getMana() - 1.0F);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DoppelgangerEntity clone = ACTIVE.get(player.getUUID());
            if (clone != null) {
                dismiss(player, clone);
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DoppelgangerEntity clone = ACTIVE.get(player.getUUID());
            if (clone != null) {
                dismiss(player, clone);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.values().forEach(Entity::discard);
        ACTIVE.clear();
        HELD.clear();
    }

    private static void effects(ServerLevel level, Vec3 at, SoundEvent sound) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 40, 0.4, 0.6, 0.4, 0.05);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 20, 0.3, 0.5, 0.3, 0.2);
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message.copy().withStyle(ChatFormatting.RED)));
    }
}
