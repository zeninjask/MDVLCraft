package com.mdvlcraft.binder.specs;

import com.mdvlcraft.binder.modified.MartialSpells;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/** Guardian, Brute, Samurai, Berserker and Monk specialisations (Specs). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class WarriorSpecs {
    private static final UUID DODGE_LISTENER = UUID.fromString("0b6e0c5e-9f53-4d21-8a2f-3c1e5d7a9b11");
    private static final UUID PARRY_LISTENER = UUID.fromString("6a2c9d14-71e8-4b3f-9e0d-5f8b2a4c6d23");
    private static final UUID TOUGHNESS = UUID.fromString("c4f1a8e2-5b7d-4e93-8a16-0d2e9f3b7c45");
    private static final Set<String> SLAMS = Set.of("irons_spellbooks:stomp", "mdvlcraft:quake_slam", "esoteric_spell:impact_magic");
    /** Highest absorption each player had since it last reached 0 (Bursting Ward). */
    private static final Map<UUID, Float> BARRIER = new HashMap<>();
    /** While a slam's hits land (Aftershock): player -> [cast x, y, z, best hit, until tick]. */
    private static final Map<UUID, double[]> SLAM = new HashMap<>();
    private static final Map<UUID, Integer> KI = new HashMap<>();
    private static final Map<UUID, Long> COUNTER_UNTIL = new HashMap<>();
    /** True while Thick Skull re-applies a halved stun. */
    private static boolean halving;
    /** True while a Guardian is taking a share of an ally's hit (Interception), so it isn't shared again. */
    private static boolean intercepting;

    private WarriorSpecs() {
    }

    // ---- Epic Fight listeners: perfect dodges (Afterimage) and parries (Counter Draw) ----

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            return;
        }
        patch.getEventListener().addEventListener(EventType.DODGE_SUCCESS_EVENT, DODGE_LISTENER, dodge -> {
            if (Specs.has(player, "afterimage") && player.level() instanceof ServerLevel level) {
                SpecKit.decoy(level, player, player.position(), 6.0, 40);
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(1.0), player.getZ(), 10, 0.3, 0.6, 0.3, 0.01);
            }
        });
        patch.getEventListener().addEventListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, PARRY_LISTENER, attack -> {
            if (attack.isParried() && Specs.has(player, "counter_draw")) {
                COUNTER_UNTIL.put(player.getUUID(), player.level().getGameTime() + 30);
            }
        });
    }

    // ---- hits taken ----

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        // Guardian, Interception: a share of a party member's hit goes to a nearby Guardian instead
        if (!intercepting && victim instanceof Player && victim.level() instanceof ServerLevel level) {
            for (Player guardian : level.getEntitiesOfClass(Player.class, victim.getBoundingBox().inflate(6.0),
                p -> p != victim && p.isAlive() && Specs.has(p, "interception") && p.isAlliedTo(victim))) {
                float share = event.getAmount() * 0.2F;
                event.setAmount(event.getAmount() - share);
                intercepting = true;
                try {
                    guardian.hurt(guardian.damageSources().generic(), share);
                } finally {
                    intercepting = false;
                }
                break;
            }
        }

        if (source.getEntity() instanceof Player attacker) {
            attackerHit(event, attacker, victim, source);
        }
    }

    private static void attackerHit(LivingHurtEvent event, Player attacker, LivingEntity victim, DamageSource source) {
        boolean melee = SpecKit.melee(source, attacker);
        // Brute, Aftershock: remember the strongest hit of a slam just cast
        double[] slam = SLAM.get(attacker.getUUID());
        if (slam != null && attacker.level().getGameTime() <= slam[4]) {
            slam[3] = Math.max(slam[3], event.getAmount());
        }
        // Berserker, Scent of Fear
        if (Specs.has(attacker, "scent_of_fear") && victim.getHealth() < victim.getMaxHealth() * 0.25F) {
            event.setAmount(event.getAmount() * 1.15F);
        }
        if (!melee) {
            return;
        }
        // Brute, Hooked: a spear hit on an enemy moving away pulls it back
        if (Specs.has(attacker, "hooked") && SpecKit.spear(attacker)) {
            Vec3 away = victim.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0).normalize();
            if (victim.getDeltaMovement().multiply(1.0, 0.0, 1.0).dot(away) > 0.03 && attacker.level() instanceof ServerLevel level) {
                // a tick later, so the hit's own knockback doesn't cancel the pull
                SpecKit.later(level, 1, () -> {
                    victim.setDeltaMovement(away.scale(-0.6).add(0.0, 0.15, 0.0));
                    victim.hurtMarked = true;
                });
            }
        }
        // Samurai, Severing Wind: a wind slash on from the katana hit, once a second
        if (Specs.has(attacker, "severing_wind") && SpecKit.katana(attacker) && attacker.level() instanceof ServerLevel level
            && attacker.level().getGameTime() - attacker.getPersistentData().getLong("mdvlcraft_wind") >= 20) {
            attacker.getPersistentData().putLong("mdvlcraft_wind", attacker.level().getGameTime());
            float damage = event.getAmount() * 0.3F;
            Vec3 look = attacker.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
            Vec3 start = attacker.getEyePosition();
            for (int step = 1; step <= 6; step++) {
                Vec3 at = start.add(look.scale(step));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y - 0.4, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                for (LivingEntity hit : SpecKit.enemies(attacker, at, 1.2)) {
                    if (hit != victim && !hit.getPersistentData().getBoolean("mdvlcraft_wind_hit")) {
                        hit.getPersistentData().putBoolean("mdvlcraft_wind_hit", true);
                        hit.hurt(attacker.damageSources().indirectMagic(attacker, attacker), damage);
                        SpecKit.later(level, 1, () -> hit.getPersistentData().remove("mdvlcraft_wind_hit"));
                    }
                }
            }
        }
        // Samurai, Counter Draw: the first hit after a parry dashes through the target
        Long counter = COUNTER_UNTIL.remove(attacker.getUUID());
        if (counter != null && attacker.level().getGameTime() <= counter && attacker.level() instanceof ServerLevel level) {
            Vec3 through = victim.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0).normalize();
            Vec3 to = victim.position().add(through.scale(1.5));
            if (level.noCollision(attacker, attacker.getBoundingBox().move(to.subtract(attacker.position())))) {
                attacker.teleportTo(to.x, to.y, to.z);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY(0.6), victim.getZ(), 3, 0.4, 0.2, 0.4, 0.0);
            }
        }
        // Monk: Silencing Palm (every fifth hit on one enemy) and Ki Wave (a shockwave after 12 hits)
        if (SpecKit.unarmed(attacker)) {
            if (Specs.has(attacker, "silencing_palm")) {
                String key = "mdvlcraft_palm_" + attacker.getUUID();
                int hits = victim.getPersistentData().getInt(key) + 1;
                victim.getPersistentData().putInt(key, hits % 5);
                if (hits >= 5) {
                    Silence.apply(victim, 40);
                }
            }
            if (Specs.has(attacker, "ki_wave") && attacker.level() instanceof ServerLevel level) {
                int ki = KI.getOrDefault(attacker.getUUID(), 0) + 1;
                if (ki > 12) {
                    KI.put(attacker.getUUID(), 0);
                    float damage = event.getAmount();
                    for (LivingEntity hit : SpecKit.enemies(attacker, victim.position(), 5.0)) {
                        if (hit != victim) {
                            hit.hurt(attacker.damageSources().playerAttack(attacker), damage);
                        }
                    }
                    level.sendParticles(ParticleTypes.SONIC_BOOM, victim.getX(), victim.getY(0.5), victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 1.4F);
                } else {
                    KI.put(attacker.getUUID(), ki);
                }
            }
        }
    }

    /** Samurai, Crimson Edge: katana critical hits (combat/Crits) make the target bleed. */
    @SubscribeEvent
    public static void onCrit(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getSource().getDirectEntity() instanceof Player player && event.getSource().getEntity() == player
            && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
            && com.mdvlcraft.binder.combat.Crits.critical(target) && Specs.has(player, "crimson_edge") && SpecKit.katana(player)
            && player.level() instanceof ServerLevel level) {
            for (int s = 1; s <= 4; s++) {
                SpecKit.later(level, s * 20, () -> {
                    if (target.isAlive()) {
                        target.invulnerableTime = 0;
                        target.hurt(player.damageSources().indirectMagic(player, player), 1.0F);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.6), target.getZ(),
                            2, 0.2, 0.2, 0.2, 0.0);
                    }
                });
            }
        }
    }

    /** Guardian, Iron Answer: a block within 0.5 s of raising the shield staggers the attacker. */
    @SubscribeEvent
    public static void onBlock(ShieldBlockEvent event) {
        if (event.getEntity() instanceof Player player && Specs.has(player, "iron_answer") && player.getTicksUsingItem() <= 10
            && event.getDamageSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
            SpecKit.stun(attacker, 20);
        }
    }

    /** Brute, Unyielding: every 8 s the next knockback is ignored and toughness rises for 5 s. */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player && Specs.has(player, "unyielding")) {
            if (unyield(player)) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean unyield(Player player) {
        long now = player.level().getGameTime();
        if (now - player.getPersistentData().getLong("mdvlcraft_unyielding") < 160) {
            return false;
        }
        player.getPersistentData().putLong("mdvlcraft_unyielding", now);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null && toughness.getModifier(TOUGHNESS) == null && player.level() instanceof ServerLevel level) {
            toughness.addTransientModifier(new AttributeModifier(TOUGHNESS, "Unyielding", 2.0, AttributeModifier.Operation.ADDITION));
            SpecKit.later(level, 100, () -> toughness.removeModifier(TOUGHNESS));
        }
        return true;
    }

    /** Stuns as effects: Brute's Unyielding ignores one, Berserker's Thick Skull halves them. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || !stun(event.getEffectInstance()) || halving) {
            return;
        }
        if (Specs.has(player, "unyielding") && unyield(player)) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        if (!halving && Specs.has(player, "thick_skull") && effect.getDuration() > 1) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            halving = true;
            try {
                player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration() / 2, effect.getAmplifier(),
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            } finally {
                halving = false;
            }
        }
    }

    private static boolean stun(MobEffectInstance effect) {
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
        return id != null && (id.getPath().contains("stun") || id.getPath().contains("paraly"));
    }

    // ---- slams (Aftershock) ----

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        Player player = event.getEntity();
        if (!SLAMS.contains(event.getSpellId()) || !Specs.has(player, "aftershock") || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        double[] slam = {player.getX(), player.getY(), player.getZ(), 0.0, level.getGameTime() + 10};
        SLAM.put(player.getUUID(), slam);
        SpecKit.later(level, 20, () -> {
            SLAM.remove(player.getUUID());
            Vec3 at = new Vec3(slam[0], slam[1], slam[2]);
            float damage = (float)(slam[3] > 0.0 ? slam[3] * 0.4 : player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4);
            for (LivingEntity hit : SpecKit.enemies(player, at, 4.0)) {
                hit.invulnerableTime = 0;
                hit.hurt(player.damageSources().indirectMagic(player, player), damage);
            }
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                level.getBlockState(net.minecraft.core.BlockPos.containing(at).below())), at.x, at.y + 0.2, at.z, 60, 2.0, 0.2, 2.0, 0.2);
            level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.7F);
        });
    }

    // ---- barriers (Bursting Ward) ----

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 2 != 0) {
            return;
        }
        if (!Specs.has(player, "bursting_ward")) {
            BARRIER.remove(player.getUUID());
            return;
        }
        float now = player.getAbsorptionAmount();
        float had = BARRIER.getOrDefault(player.getUUID(), 0.0F);
        if (now > had) {
            BARRIER.put(player.getUUID(), now);
        } else if (now <= 0.0F && had > 0.0F) {
            BARRIER.remove(player.getUUID());
            ServerLevel level = player.serverLevel();
            for (LivingEntity hit : SpecKit.enemies(player, player.position(), 4.0)) {
                hit.invulnerableTime = 0;
                hit.hurt(player.damageSources().indirectMagic(player, player), had * 0.25F);
                Vec3 push = hit.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
                hit.setDeltaMovement(push.scale(0.9).add(0.0, 0.3, 0.0));
                hit.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 30, 1.0, 0.6, 1.0, 0.15);
            level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    /** Monk, Silencing Palm: silenced creatures can't cast or shoot (mixin/irons/SpellCastingMobCalloutMixin, onJoinProjectile). */
    public static final class Silence {
        private Silence() {
        }

        public static void apply(LivingEntity target, int ticks) {
            target.getPersistentData().putLong("mdvlcraft_silenced", target.level().getGameTime() + ticks);
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY(1.0) + 0.3, target.getZ(), 8, 0.2, 0.1, 0.2, 0.01);
            }
        }

        public static boolean on(LivingEntity entity) {
            return entity.getPersistentData().getLong("mdvlcraft_silenced") > entity.level().getGameTime();
        }
    }

    /** A silenced mob's projectiles never leave it. */
    @SubscribeEvent
    public static void onJoinProjectile(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
            && projectile.getOwner() instanceof Mob owner && Silence.on(owner)) {
            event.setCanceled(true);
        }
    }

    /** Silenced mobs also stop using items (bows, crossbows, potions) while it lasts. */
    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Mob mob && Silence.on(mob)) {
            event.setCanceled(true);
        }
    }

}
