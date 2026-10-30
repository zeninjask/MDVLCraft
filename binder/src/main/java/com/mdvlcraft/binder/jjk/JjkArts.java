package com.mdvlcraft.binder.jjk;

import com.mdvlcraft.binder.jjk.JjkSkill.Kind;
import com.mdvlcraft.binder.modified.Targets;
import com.mdvlcraft.binder.specs.SpecKit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

/**
 * What the JJK weapons' innate skills (JjkSkill) do. While a skill's animation plays (its window), the player's own hits
 * count as the skill's hits; some skills also act at the animation's last hit (its contact). The skills' own extra
 * damage goes through `deal`, so it isn't taken for another of the player's hits.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class JjkArts {
    private static final ResourceKey<DamageType> TRUE_CUT = key("true_cut");
    private static final ResourceKey<DamageType> PUNCTURE = key("puncture");
    private static final ResourceKey<DamageType> BLEED = key("bleed");
    private static final ResourceLocation DRAGON_BONE = ResourceLocation.parse("cursedfate:dragon_bone");
    private static final UUID ZONE_SPEED = UUID.fromString("0f6c1d1e-6a43-4b8e-9a7e-5b1a8d2c4e01");
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55F, 0.0F, 0.0F), 1.2F);
    private static final DustParticleOptions BLACK_SPARK = new DustParticleOptions(new Vector3f(0.05F, 0.0F, 0.08F), 1.6F);

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    /** Dragon-Bone's stored impacts. */
    private static final Map<UUID, Integer> STORED = new HashMap<>();
    /** Into the Zone: until when, and the hits landed in it. */
    private static final Map<UUID, Zone> ZONES = new HashMap<>();
    private static final Map<UUID, Long> NO_FALL = new HashMap<>();
    private static final List<Bleed> BLEEDS = new ArrayList<>();
    /** Ratio 7:3: the share of the armour-blocked damage put back, for the hit being dealt. */
    private static final Map<LivingEntity, Float> PIERCE = new HashMap<>();
    /** Miwa's Simple Domain: until when each player's circle holds. */
    private static final Map<UUID, Long> DOMAINS = new HashMap<>();
    /** Inked (G-Warstaff): creatures whose moves a player can read, and until when. */
    private static final Map<LivingEntity, Ink> INKED = new java.util.WeakHashMap<>();
    private static boolean dealing;

    private JjkArts() {
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("mdvlcraft", path));
    }

    private static DamageSource source(ServerPlayer player, ResourceKey<DamageType> type, @Nullable Entity direct) {
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),
            direct, player);
    }

    /** Skill damage the skill handlers below ignore. */
    private static void deal(LivingEntity target, DamageSource source, float amount) {
        boolean was = dealing;
        dealing = true;
        try {
            target.invulnerableTime = 0;
            target.hurt(source, amount);
        } finally {
            dealing = was;
        }
    }

    // ---- starting a skill ----

    static void start(ServerPlayer player, Kind kind) {
        ServerLevel level = player.serverLevel();
        int contact = kind.contactTicks();
        Active active = new Active(kind, level.getGameTime() + contact + 8);
        ACTIVE.put(player.getUUID(), active);
        switch (kind) {
            case STORED_IMPACT -> {
                active.stacks = STORED.getOrDefault(player.getUUID(), 0);
                STORED.remove(player.getUUID());
                SpecKit.later(level, contact, () -> {
                    if (active.stacks >= 3 && player.isAlive()) {
                        for (LivingEntity e : SpecKit.enemies(player, player.position(), 4.0)) {
                            deal(e, player.damageSources().playerAttack(player), 6.0F);
                            e.knockback(0.6, player.getX() - e.getX(), player.getZ() - e.getZ());
                        }
                        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 6, 1.5, 0.1, 1.5, 0.0);
                        sound(player, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.3F);
                    }
                });
            }
            case THUNDERCLAP -> SpecKit.later(level, contact, () -> {
                Vec3 point = aimed(player, 24.0);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(point);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                DamageSource lightning = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), bolt, player);
                for (LivingEntity e : SpecKit.enemies(player, point, 3.0)) {
                    deal(e, lightning, 12.0F);
                }
            });
            case HEIAN_SKY_DANCE -> {
                int window = contact + 10;
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, window, 0, false, false));
                NO_FALL.put(player.getUUID(), level.getGameTime() + window + 60);
            }
            case FULL_MOON_SWEEP -> SpecKit.later(level, contact, () -> {
                for (LivingEntity e : SpecKit.enemies(player, player.position(), 5.0)) {
                    e.knockback(1.2, player.getX() - e.getX(), player.getZ() - e.getZ());
                    SpecKit.stun(e, 20);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY(1.0), player.getZ(), 12, 2.5, 0.2, 2.5, 0.0);
            });
            case BIRD_STRIKE -> SpecKit.later(level, contact + 4, () -> {
                LivingEntity target = target(player, active);
                if (target != null) {
                    for (int i = 0; i < 8; i++) {
                        level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 6.0 - i * 0.7, target.getZ(), 3, 0.15, 0.1, 0.15, 0.0);
                    }
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY(1.0), target.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                    sound(target, SoundEvents.PHANTOM_SWOOP, 1.0F, 1.4F);
                    deal(target, source(player, TRUE_CUT, player), 14.0F);
                }
            });
            case RIKAS_ARM -> SpecKit.later(level, contact + 3, () -> {
                LivingEntity target = target(player, active);
                if (target != null) {
                    level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 2.5, target.getZ(), 20, 0.6, 0.8, 0.6, 0.02);
                    level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 2, 0.3, 0.0, 0.3, 0.0);
                    sound(target, SoundEvents.WARDEN_ATTACK_IMPACT, 1.0F, 0.8F);
                    deal(target, player.damageSources().indirectMagic(player, player), 6.0F);
                    SpecKit.stun(target, 20);
                }
            });
            case JACOBS_LADDER -> SpecKit.later(level, contact, () -> {
                LivingEntity looked = Targets.looked(player, 20.0F);
                Vec3 point = looked != null ? looked.position() : aimed(player, 20.0);
                Set<LivingEntity> hit = new HashSet<>(SpecKit.enemies(player, point, 2.0));
                if (looked != null) {
                    hit.add(looked);
                }
                for (int i = 0; i < 24; i++) {
                    level.sendParticles(ParticleTypes.END_ROD, point.x, point.y + i * 0.5, point.z, 3, 0.25, 0.1, 0.25, 0.0);
                }
                sound(player, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
                for (LivingEntity e : hit) {
                    float amount = e.getMobType() == MobType.UNDEAD ? 20.0F : 10.0F;
                    deal(e, player.damageSources().indirectMagic(player, player), amount);
                    new ArrayList<>(e.getActiveEffects()).stream().filter(effect -> effect.getEffect().isBeneficial())
                        .forEach(effect -> e.removeEffect(effect.getEffect()));
                }
            });
            case FPS_24 -> {
                List<LivingEntity> targets = new ArrayList<>(SpecKit.enemies(player, player.position(), 8.0));
                targets.removeIf(e -> e == player);
                targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
                for (int i = 0; i < Math.min(5, targets.size()); i++) {
                    LivingEntity target = targets.get(i);
                    SpecKit.later(level, 4 + i * 3, () -> blink(player, target));
                }
            }
            case THROUGH_INFINITY -> SpecKit.later(level, contact, () -> {
                // a thrust no guard, shield or immunity frame stops (mdvlcraft:puncture)
                LivingEntity target = target(player, active);
                if (target != null) {
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.2, 0.3, 0.2, 0.3);
                    deal(target, source(player, PUNCTURE, player), (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE));
                }
            });
            case CLOUD_CRUSHER -> SpecKit.later(level, contact, () -> {
                for (LivingEntity e : SpecKit.enemies(player, player.position(), 4.0)) {
                    deal(e, source(player, PUNCTURE, player), 8.0F);
                    launch(player, e);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.1, player.getZ(), 8, 1.8, 0.1, 1.8, 0.0);
                sound(player, SoundEvents.GENERIC_EXPLODE, 0.9F, 0.8F);
            });
            case DISPEL_FIELD -> SpecKit.later(level, contact, () -> {
                net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(5.0);
                for (Entity e : level.getEntities(player, box)) {
                    boolean foreignProjectile = e instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() != player;
                    LivingEntity owner = e instanceof LivingEntity living ? SpecKit.owner(living) : null;
                    boolean foreignSummon = owner != null && owner != player && !com.mdvlcraft.binder.modified.Summons.allied(player, owner);
                    if (foreignProjectile || foreignSummon) {
                        level.sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY(0.5), e.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
                        e.discard();
                    }
                }
                for (LivingEntity e : SpecKit.enemies(player, player.position(), 5.0)) {
                    e.knockback(1.0, player.getX() - e.getX(), player.getZ() - e.getZ());
                }
                sound(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.6F);
            });
            case SIMPLE_DOMAIN -> {
                DOMAINS.put(player.getUUID(), level.getGameTime() + 60);
                sound(player, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.4F);
            }
            case EVENING_MOON -> SpecKit.later(level, contact, () -> {
                float damage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F;
                for (LivingEntity e : inArc(player, 4.0, 60.0)) {
                    deal(e, source(player, PUNCTURE, player), damage);
                }
                arcParticles(level, player, 4.0, ParticleTypes.SWEEP_ATTACK);
            });
            case PILLAR -> SpecKit.later(level, contact, () -> {
                for (LivingEntity e : SpecKit.enemies(player, player.position(), 3.0)) {
                    deal(e, player.damageSources().playerAttack(player), 6.0F);
                    e.setDeltaMovement(e.getDeltaMovement().x, 1.2, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                }
                Vec3 ahead = player.position().add(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(1.5));
                for (int i = 0; i < 16; i++) {
                    level.sendParticles(ParticleTypes.CRIT, ahead.x, ahead.y + i * 0.4, ahead.z, 2, 0.15, 0.1, 0.15, 0.0);
                }
                sound(player, SoundEvents.ANVIL_LAND, 0.6F, 1.4F);
            });
            case BLAZING_DRAW -> SpecKit.later(level, contact, () -> {
                for (LivingEntity e : inArc(player, 4.0, 60.0)) {
                    e.setSecondsOnFire(5);
                }
                arcParticles(level, player, 4.0, ParticleTypes.FLAME);
                sound(player, SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F);
            });
            case BLIGHT -> SpecKit.later(level, contact, () -> {
                for (LivingEntity e : SpecKit.enemies(player, player.position(), 4.0)) {
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), player);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), player);
                }
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, player.getX(), player.getY(0.5), player.getZ(), 40, 2.0, 0.5, 2.0, 0.0);
                level.sendParticles(ParticleTypes.MYCELIUM, player.getX(), player.getY(0.3), player.getZ(), 40, 2.0, 0.3, 2.0, 0.0);
            });
            default -> {
            }
        }
    }

    /** Creatures within `reach` blocks and `halfAngle` degrees of where the player faces. */
    private static List<LivingEntity> inArc(ServerPlayer player, double reach, double halfAngle) {
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        double cos = Math.cos(Math.toRadians(halfAngle));
        return SpecKit.enemies(player, player.position(), reach).stream().filter(e -> {
            Vec3 to = e.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            return to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) >= cos;
        }).toList();
    }

    private static void arcParticles(ServerLevel level, ServerPlayer player, double reach, ParticleOptions particle) {
        float yaw = player.getYRot() * ((float)Math.PI / 180F);
        for (int i = -6; i <= 6; i++) {
            double a = yaw + i * Math.toRadians(10.0);
            level.sendParticles(particle, player.getX() - Math.sin(a) * reach * 0.8, player.getY(0.6), player.getZ() + Math.cos(a) * reach * 0.8,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** 24 FPS: appear just behind the target and stab it. */
    private static void blink(ServerPlayer player, LivingEntity target) {
        if (!player.isAlive() || !target.isAlive() || target.level() != player.level()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(1.0), player.getZ(), 6, 0.2, 0.4, 0.2, 0.01);
        Vec3 away = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        Vec3 behind = away.lengthSqr() < 1.0E-4 ? target.position() : target.position().add(away.normalize().scale(1.2));
        if (level.noCollision(player, player.getBoundingBox().move(behind.subtract(player.position())))) {
            float yaw = (float)(Math.atan2(target.getZ() - behind.z, target.getX() - behind.x) * (180.0 / Math.PI)) - 90.0F;
            player.teleportTo(level, behind.x, behind.y, behind.z, yaw, player.getXRot());
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
        sound(target, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
        deal(target, player.damageSources().playerAttack(player), (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    /** Cloud Crusher's hits send an enemy flying away, as hard as Follow Up Kick (the Projection Sorcery add-on's kick). */
    private static void launch(ServerPlayer player, LivingEntity victim) {
        if (!victim.isAlive()) {
            return;
        }
        Vec3 away = victim.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle().multiply(1.0, 0.0, 1.0) : away;
        Vec3 push = away.normalize().scale(4.8);
        victim.setDeltaMovement(push.x, 0.3, push.z);
        victim.hurtMarked = true;
    }

    // ---- the skills' hits ----

    @Nullable
    private static Active active(Player player) {
        Active active = ACTIVE.get(player.getUUID());
        if (active != null && player.level().getGameTime() > active.until) {
            ACTIVE.remove(player.getUUID());
            return null;
        }
        return active;
    }

    /** The player's own weapon hit (an Epic Fight animation hit or a vanilla swing), not skill damage. */
    private static boolean ownHit(DamageSource source, Entity attacker) {
        return !dealing && source.getDirectEntity() == attacker && !source.is(TRUE_CUT) && !source.is(PUNCTURE) && !source.is(BLEED);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getSource().getEntity() instanceof LivingEntity attacker) || dealing) {
            return;
        }
        // Inked: half the attacks of a creature the player has read miss
        Ink ink = INKED.get(attacker);
        if (ink != null && ink.by.equals(player.getUUID()) && player.level().getGameTime() <= ink.until && player.getRandom().nextBoolean()) {
            event.setCanceled(true);
            player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(1.0), player.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            return;
        }
        // Simple Domain: an attacker within reach is cut first
        Long until = DOMAINS.get(player.getUUID());
        if (until != null && player.level().getGameTime() <= until && attacker.distanceToSqr(player) <= 36.0) {
            event.setCanceled(true);
            counter(player, attacker);
        }
    }

    /** Simple Domain's draw: double the weapon's damage on the creature that came in, and the circle is spent. */
    private static void counter(ServerPlayer player, LivingEntity target) {
        DOMAINS.remove(player.getUUID());
        yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch patch =
            yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(player, yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch.class);
        if (patch != null) {
            patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BATTOJUTSU, 0.0F);
        }
        player.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.5F);
        deal(target, player.damageSources().playerAttack(player), (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Long until = DOMAINS.get(player.getUUID());
        if (until == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() > until) {
            DOMAINS.remove(player.getUUID());
            return;
        }
        if (player.tickCount % 4 == 0) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8.0;
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX() + Math.cos(a) * 3.0, player.getY() + 0.1, player.getZ() + Math.sin(a) * 3.0,
                    1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        LivingEntity intruder = SpecKit.nearestEnemy(player, player.position(), 3.0, player);
        if (intruder != null && intruder instanceof net.minecraft.world.entity.monster.Enemy) {
            counter(player, intruder);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (victim instanceof Player holder && isDragonBone(holder) && !holder.level().isClientSide) {
            STORED.merge(holder.getUUID(), 1, (a, b) -> Math.min(5, a + b));
        }
        if (!(source.getEntity() instanceof ServerPlayer player) || !ownHit(source, player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Zone zone = ZONES.get(player.getUUID());
        if (zone != null) {
            zone.hits++;
            if (zone.hits % 4 == 0) {
                blackFlash(level, victim, event);
            }
        }
        Active active = active(player);
        if (active == null) {
            if (isDragonBone(player)) {
                STORED.merge(player.getUUID(), 1, (a, b) -> Math.min(5, a + b));
            }
            return;
        }
        if (active.first == null) {
            active.first = victim;
        }
        switch (active.kind) {
            case CLOUD_CRUSHER -> SpecKit.later(level, 1, () -> launch(player, victim));
            case SPLINTERED_SOUL -> {
                if (active.struck.add(victim.getUUID())) {
                    float amount = event.getAmount() * 0.7F;
                    SpecKit.later(level, 1, () -> {
                        if (victim.isAlive()) {
                            Vec3 behind = victim.position().add(victim.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize());
                            level.sendParticles(ParticleTypes.SQUID_INK, behind.x, behind.y + 1.0, behind.z, 25, 0.25, 0.7, 0.25, 0.01);
                            level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY(0.6), victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                            sound(victim, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
                            deal(victim, source(player, TRUE_CUT, player), amount);
                        }
                    });
                }
            }
            case STORED_IMPACT -> event.setAmount(event.getAmount() * (1.0F + 0.2F * active.stacks));
            case HEMORRHAGE -> {
                BLEEDS.removeIf(bleed -> bleed.victim == victim);
                BLEEDS.add(new Bleed(victim, player, level.getGameTime() + 120, victim.position()));
            }
            case INTO_THE_ZONE -> {
                ZONES.put(player.getUUID(), new Zone(level.getGameTime() + 160));
                AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
                if (speed != null && speed.getModifier(ZONE_SPEED) == null) {
                    speed.addTransientModifier(new AttributeModifier(ZONE_SPEED, "Into the Zone", 0.2, AttributeModifier.Operation.MULTIPLY_BASE));
                }
            }
            case POSITIVE_FLAME -> {
                if (victim.getMobType() == MobType.UNDEAD) {
                    event.setAmount(event.getAmount() * 2.0F);
                }
                player.heal(event.getAmount() * 0.2F);
                level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY(0.6), victim.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
            }
            case INKED -> {
                INKED.put(victim, new Ink(player.getUUID(), level.getGameTime() + 160));
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, false));
                level.sendParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY(0.6), victim.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
            }
            case TSUKKOMI -> {
                if (active.struck.add(victim.getUUID())) {
                    event.setAmount(Math.min(event.getAmount(), 1.0F));
                    victim.knockback(2.5, player.getX() - victim.getX(), player.getZ() - victim.getZ());
                    SpecKit.stun(victim, 40);
                    sound(victim, SoundEvents.NOTE_BLOCK_COW_BELL.value(), 1.0F, 1.6F);
                    sound(victim, SoundEvents.SLIME_SQUISH, 1.0F, 1.2F);
                }
            }
            case RATIO -> {
                event.setAmount(event.getAmount() * 1.5F);
                PIERCE.put(victim, 0.3F);
                double top = victim.getY() + victim.getBbHeight() * 0.7;
                for (int i = -4; i <= 4; i++) {
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX() + i * 0.15, top, victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            default -> {
            }
        }
    }

    private static void blackFlash(ServerLevel level, LivingEntity victim, LivingHurtEvent event) {
        event.setAmount(event.getAmount() * 2.5F);
        level.sendParticles(BLACK_SPARK, victim.getX(), victim.getY(0.6), victim.getZ(), 30, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.6), victim.getZ(), 12, 0.4, 0.4, 0.4, 0.3);
        sound(victim, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7F, 1.8F);
    }

    /** Ratio 7:3: part of what armour took off the hit is put back. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent event) {
        Float share = PIERCE.remove(event.getEntity());
        if (share != null) {
            float before = event.getEntity().getArmorValue() > 0 ? event.getAmount() / armourFactor(event.getEntity()) : event.getAmount();
            event.setAmount(event.getAmount() + (before - event.getAmount()) * share);
        }
    }

    /** About how much of a hit gets past the entity's armour (vanilla's formula at a 10-damage hit). */
    private static float armourFactor(LivingEntity entity) {
        float armour = entity.getArmorValue();
        float toughness = (float)entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        return net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(10.0F, armour, toughness) / 10.0F;
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        Long until = NO_FALL.get(event.getEntity().getUUID());
        if (until != null) {
            if (event.getEntity().level().getGameTime() <= until) {
                event.setCanceled(true);
            } else {
                NO_FALL.remove(event.getEntity().getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PIERCE.clear();
        long tick = event.getServer().getTickCount();
        if (tick % 20 == 0) {
            for (Iterator<Bleed> it = BLEEDS.iterator(); it.hasNext();) {
                Bleed bleed = it.next();
                LivingEntity victim = bleed.victim;
                if (!victim.isAlive() || !bleed.by.isAlive() || victim.level().getGameTime() > bleed.until || victim.level() != bleed.by.level()) {
                    it.remove();
                    continue;
                }
                boolean moved = victim.position().distanceToSqr(bleed.last) > 0.09;
                bleed.last = victim.position();
                ((ServerLevel)victim.level()).sendParticles(BLOOD, victim.getX(), victim.getY(0.5), victim.getZ(), moved ? 12 : 6, 0.25, 0.3, 0.25, 0.0);
                deal(victim, source(bleed.by, BLEED, null), moved ? 4.0F : 2.0F);
            }
        }
        for (Iterator<Map.Entry<UUID, Zone>> it = ZONES.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Zone> entry = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.level().getGameTime() > entry.getValue().until) {
                if (player != null) {
                    AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
                    if (speed != null) {
                        speed.removeModifier(ZONE_SPEED);
                    }
                }
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        ACTIVE.remove(id);
        STORED.remove(id);
        NO_FALL.remove(id);
        DOMAINS.remove(id);
        Zone zone = ZONES.remove(id);
        if (zone != null) {
            AttributeInstance speed = event.getEntity().getAttribute(Attributes.ATTACK_SPEED);
            if (speed != null) {
                speed.removeModifier(ZONE_SPEED);
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
        STORED.clear();
        ZONES.clear();
        NO_FALL.clear();
        BLEEDS.clear();
        PIERCE.clear();
        DOMAINS.clear();
        INKED.clear();
    }

    // ---- helpers ----

    private static boolean isDragonBone(Player player) {
        return DRAGON_BONE.equals(ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem()));
    }

    /** The first creature the skill's animation hit, else the nearest enemy just ahead. */
    @Nullable
    private static LivingEntity target(ServerPlayer player, Active active) {
        if (active.first != null && active.first.isAlive()) {
            return active.first;
        }
        Vec3 ahead = player.position().add(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(2.0));
        return SpecKit.nearestEnemy(player, ahead, 3.0, player);
    }

    /** Where the player looks, up to `reach` blocks: the creature or block hit, else the end of the reach. */
    private static Vec3 aimed(ServerPlayer player, double reach) {
        LivingEntity looked = Targets.looked(player, (float)reach);
        if (looked != null) {
            return looked.position();
        }
        HitResult hit = player.pick(reach, 1.0F, false);
        return hit.getLocation();
    }

    private static void sound(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static final class Active {
        final Kind kind;
        final long until;
        final Set<UUID> struck = new HashSet<>();
        int stacks;
        @Nullable
        LivingEntity first;

        Active(Kind kind, long until) {
            this.kind = kind;
            this.until = until;
        }
    }

    private record Ink(UUID by, long until) {
    }

    private static final class Zone {
        final long until;
        int hits;

        Zone(long until) {
            this.until = until;
        }
    }

    private static final class Bleed {
        final LivingEntity victim;
        final ServerPlayer by;
        final long until;
        Vec3 last;

        Bleed(LivingEntity victim, ServerPlayer by, long until, Vec3 last) {
            this.victim = victim;
            this.by = by;
            this.until = until;
            this.last = last;
        }
    }
}
