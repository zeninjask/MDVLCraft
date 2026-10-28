package com.mdvlcraft.binder.specs;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.mobs.SummonedZombie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Blaze, Frost, Water, Lightning, Holy, Warlock, Necromancer, Druid, Jade and Spirit specialisations (Specs). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MageSpecs {
    private static final Set<String> WATER = Set.of("aqua", "water");
    private static final String SOAKED = "mdvlcraft_soaked";
    private static final String SMOULDER = "mdvlcraft_smoulder";
    /** Last holy cast per player (game time), for crediting heals (Martyr's Grace, Last Light). */
    private static final Map<UUID, Long> HEALER = new HashMap<>();
    private static final Map<UUID, Integer> CASTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_WATER = new HashMap<>();
    private static final Map<UUID, Integer> TIDE = new HashMap<>();
    private static final Map<UUID, Long> LAST_LIGHTNING = new HashMap<>();
    private static final Map<UUID, List<UUID>> RISEN = new HashMap<>();
    private static final List<Wisp> WISPS = new ArrayList<>();
    /** Jade's Perfect Symmetry: player -> [last action (1 hit, 2 cast), its time, spell bonus until]. */
    private static final Map<UUID, long[]> SYMMETRY = new HashMap<>();
    private static boolean echoing;
    private static boolean crediting;

    private MageSpecs() {
    }

    // ---- casts ----

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        String school = event.getSchoolType() == null ? "" : event.getSchoolType().getId().getPath();
        long now = level.getGameTime();
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());

        if (school.equals("holy")) {
            HEALER.put(player.getUUID(), now);
            if (Specs.has(player, "sanctified_ground")) {
                SpecKit.zone(level, player.position(), 3.0, 80, ParticleTypes.END_ROD, zone -> {
                    for (LivingEntity e : zone.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().move(zone.centre().subtract(player.position())).inflate(3.0))) {
                        if (e.getMobType() == MobType.UNDEAD && zone.inside(e)) {
                            e.invulnerableTime = 0;
                            e.hurt(player.damageSources().indirectMagic(player, player), 3.0F);
                            e.setSecondsOnFire(2);
                        }
                    }
                });
            }
        }
        if (WATER.contains(school) && Specs.has(player, "rising_tide")) {
            Long last = LAST_WATER.get(player.getUUID());
            int stacks = last != null && now - last <= 100 ? Math.min(5, TIDE.getOrDefault(player.getUUID(), 0) + 1) : 0;
            TIDE.put(player.getUUID(), stacks);
            event.setManaCost(Math.round(event.getManaCost() * (1.0F - 0.05F * stacks)));
        }
        if (WATER.contains(school)) {
            LAST_WATER.put(player.getUUID(), now);
        }
        if (school.equals("lightning")) {
            LAST_LIGHTNING.put(player.getUUID(), now);
        }
        if (school.equals("ice") && Specs.has(player, "cold_snap")) {
            int n = CASTS.merge(key(player, "ice"), 1, Integer::sum);
            if (n >= 3) {
                CASTS.put(key(player, "ice"), 0);
                player.getPersistentData().putLong("mdvlcraft_cold_snap", now + 100);
            }
        }
        if (Specs.has(player, "celestial_seal")) {
            int n = CASTS.merge(key(player, "seal"), 1, Integer::sum);
            if (n >= 8) {
                CASTS.put(key(player, "seal"), 0);
                Vec3 at = player.position();
                player.getPersistentData().putLong("mdvlcraft_seal_until", now + 100);
                player.getPersistentData().putDouble("mdvlcraft_seal_x", at.x);
                player.getPersistentData().putDouble("mdvlcraft_seal_z", at.z);
                SpecKit.zone(level, at, 3.0, 100, ParticleTypes.ENCHANT, zone -> {
                    for (LivingEntity e : SpecKit.enemies(player, zone.centre(), 3.0)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), player);
                    }
                });
            }
        }
        if (Specs.has(player, "perfect_symmetry")) {
            long[] s = SYMMETRY.computeIfAbsent(player.getUUID(), u -> new long[3]);
            s[2] = s[0] == 1 && now - s[1] <= 100 ? now + 60 : 0;
            s[0] = 2;
            s[1] = now;
        }
        if (!echoing && (school.equals("eldritch") || school.equals("ender")) && Specs.has(player, "void_echo")
            && event.getCastSource() != CastSource.COMMAND && spell != null && level.random.nextFloat() < 0.10F) {
            int echoLevel = Math.max(1, event.getSpellLevel() / 2);
            SpecKit.later(level, 12, () -> {
                if (player.isAlive()) {
                    echoing = true;
                    try {
                        spell.castSpell(level, echoLevel, player, CastSource.COMMAND, false);
                    } finally {
                        echoing = false;
                    }
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
                }
            });
        }
        // Lightning, Surge Step: a step spell right after a lightning spell goes twice as far
        String path = event.getSpellId();
        if (Specs.has(player, "surge_step") && (path.contains("step") || path.contains("blink") || path.contains("teleport"))
            && LAST_LIGHTNING.getOrDefault(player.getUUID(), -1000L) >= now - 40 && !school.equals("lightning")) {
            surge(player, 3);
        }
    }

    /** Lightning, Surge Step: after the move `ticks` from now, carry the player on as far again (if there's room). */
    static void surge(ServerPlayer player, int ticks) {
        LAST_LIGHTNING.remove(player.getUUID());
        Vec3 start = player.position();
        SpecKit.later(player.serverLevel(), ticks, () -> {
            Vec3 moved = player.position().subtract(start).multiply(1.0, 0.0, 1.0);
            if (moved.lengthSqr() < 1.0) {
                return;
            }
            Vec3 to = player.position().add(moved);
            if (player.level().noCollision(player, player.getBoundingBox().move(moved))) {
                player.teleportTo(to.x, player.getY(), to.z);
                player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y + 1.0, to.z, 20, 0.3, 0.6, 0.3, 0.1);
            }
        });
    }

    /** Surge Step for Epic Fight dodges (WarriorSpecs hears the dodge cast). */
    static boolean surgeReady(ServerPlayer player) {
        return Specs.has(player, "surge_step") && LAST_LIGHTNING.getOrDefault(player.getUUID(), -1000L) >= player.level().getGameTime() - 40;
    }

    private static UUID key(Player player, String what) {
        return UUID.nameUUIDFromBytes((player.getUUID() + what).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ---- hits ----

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity src = source.getEntity();

        // spells a player lands
        if (src instanceof ServerPlayer player && SpecKit.spell(source)) {
            ServerLevel level = player.serverLevel();
            ResourceLocation school = SpecKit.school(source);
            String path = school == null ? "" : school.getPath();
            long now = level.getGameTime();
            if (victim.isOnFire() && Specs.has(player, "wildfire") && now - victim.getPersistentData().getLong("mdvlcraft_wildfire") >= 10) {
                victim.getPersistentData().putLong("mdvlcraft_wildfire", now);
                LivingEntity next = SpecKit.nearestEnemy(player, victim.position(), 3.0, victim);
                if (next != null) {
                    next.setSecondsOnFire(Math.max(4, next.getRemainingFireTicks() / 20));
                    level.sendParticles(ParticleTypes.FLAME, next.getX(), next.getY(0.5), next.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                }
            }
            if (path.equals("fire") && victim.isOnFire() && Specs.has(player, "fuel_the_flames")
                && now - player.getPersistentData().getLong("mdvlcraft_fuel") >= 10) {
                player.getPersistentData().putLong("mdvlcraft_fuel", now);
                SpecKit.addMana(player, 3.0F);
            }
            if (Specs.has(player, "smoulder")) {
                SpecKit.later(level, 1, () -> {
                    if (victim.isAlive() && victim.isOnFire()) {
                        victim.getPersistentData().putLong(SMOULDER, level.getGameTime() + Math.max(40, victim.getRemainingFireTicks()));
                    }
                });
            }
            if (WATER.contains(path)) {
                if (Specs.has(player, "undertow")) {
                    Vec3 pull = player.position().subtract(victim.position()).multiply(1.0, 0.0, 1.0).normalize();
                    victim.setDeltaMovement(victim.getDeltaMovement().add(pull.scale(0.45)));
                    victim.hurtMarked = true;
                }
                if (Specs.has(player, "drenched")) {
                    victim.getPersistentData().putLong(SOAKED, now + 100);
                    victim.clearFire();
                }
            }
            if ((path.equals("lightning") || path.equals("ice")) && victim.getPersistentData().getLong(SOAKED) > now) {
                event.setAmount(event.getAmount() * 1.15F);
            }
            if (path.equals("ice") && player.getPersistentData().getLong("mdvlcraft_cold_snap") > now) {
                player.getPersistentData().remove("mdvlcraft_cold_snap");
                victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), victim.getTicksRequiredToFreeze() + 20));
                SpecKit.stun(victim, 20);
            }
            if (Specs.has(player, "sculk_whispers")) {
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false), player);
            }
            long[] sym = SYMMETRY.get(player.getUUID());
            if (sym != null && sym[2] > now && Specs.has(player, "perfect_symmetry")) {
                event.setAmount(event.getAmount() * 1.15F);
            }
        }

        // weapon hits (Perfect Symmetry's other half)
        if (src instanceof ServerPlayer player && SpecKit.melee(source, player) && Specs.has(player, "perfect_symmetry")) {
            long now = player.level().getGameTime();
            long[] s = SYMMETRY.computeIfAbsent(player.getUUID(), u -> new long[3]);
            if (s[0] == 2 && now - s[1] <= 100) {
                event.setAmount(event.getAmount() * 1.15F);
            }
            s[0] = 1;
            s[1] = now;
            s[2] = 0;
        }

        // hits from a player's summons
        LivingEntity owner = src == null ? null : SpecKit.owner(src);
        if (owner instanceof ServerPlayer player) {
            if (player.getPersistentData().getLong("mdvlcraft_howl") > player.level().getGameTime() && Specs.has(player, "pack_howl")) {
                event.setAmount(event.getAmount() * 1.2F);
            }
            if (victim instanceof Mob mob && Specs.has(player, "possession") && player.level().random.nextFloat() < 0.05F) {
                LivingEntity other = SpecKit.nearestEnemy(player, mob.position(), 10.0, mob);
                if (other != null && other != player) {
                    SpecKit.pin(mob, other, 60);
                    player.serverLevel().sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY(1.0), mob.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                }
            }
        }

        // hits a player takes
        if (victim instanceof ServerPlayer player) {
            long now = player.level().getGameTime();
            boolean meleeHit = src instanceof LivingEntity attacker && source.getDirectEntity() == attacker && !(source.getDirectEntity() instanceof Projectile);
            if (meleeHit && Specs.has(player, "static_skin") && player.level().random.nextFloat() < 0.2F) {
                LivingEntity attacker = (LivingEntity)src;
                attacker.hurt(player.damageSources().indirectMagic(player, player), 3.0F);
                player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 15, 0.3, 0.4, 0.3, 0.1);
            }
            if (meleeHit && Specs.has(player, "venomous_thorns")) {
                ((LivingEntity)src).addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
            }
            if (Specs.has(player, "pack_howl")) {
                if (player.getPersistentData().getLong("mdvlcraft_howl") <= now) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 0.6F, 1.0F);
                }
                player.getPersistentData().putLong("mdvlcraft_howl", now + 100);
            }
            if (player.getPersistentData().getLong("mdvlcraft_seal_until") > now && Specs.has(player, "celestial_seal")) {
                double dx = player.getX() - player.getPersistentData().getDouble("mdvlcraft_seal_x");
                double dz = player.getZ() - player.getPersistentData().getDouble("mdvlcraft_seal_z");
                if (dx * dx + dz * dz <= 9.0) {
                    event.setAmount(event.getAmount() * 0.85F);
                }
            }
        }
    }

    // ---- deaths ----

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();

        // a player's summon dies
        LivingEntity owner = SpecKit.owner(dead);
        if (owner instanceof ServerPlayer player) {
            if (Specs.has(player, "corpse_blast")) {
                float damage = dead.getMaxHealth() * 0.3F;
                for (LivingEntity hit : SpecKit.enemies(player, dead.position(), 3.0)) {
                    hit.invulnerableTime = 0;
                    hit.hurt(player.damageSources().indirectMagic(dead, player), damage);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, dead.getX(), dead.getY(0.5), dead.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, dead.getX(), dead.getY(0.5), dead.getZ(), 20, 0.6, 0.6, 0.6, 0.1);
                level.playSound(null, dead.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.3F);
            }
            if (Specs.has(player, "grave_feast")) {
                player.heal(2.0F);
            }
        }

        if (!(source.getEntity() instanceof ServerPlayer player) || owner == player) {
            return;
        }
        ResourceLocation school = SpecKit.school(source);
        String path = school == null ? "" : school.getPath();
        if (path.equals("lightning") && Specs.has(player, "thunderclap")) {
            for (LivingEntity hit : SpecKit.enemies(player, dead.position(), 3.0)) {
                if (hit != dead) {
                    SpecKit.stun(hit, 10);
                }
            }
            level.playSound(null, dead.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.6F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, dead.getX(), dead.getY(0.5), dead.getZ(), 30, 1.5, 0.5, 1.5, 0.2);
        }
        if (dead.getTicksFrozen() > 0 && Specs.has(player, "shatter")) {
            float damage = dead.getMaxHealth() * 0.15F;
            for (LivingEntity hit : SpecKit.enemies(player, dead.position(), 4.0)) {
                if (hit != dead) {
                    hit.invulnerableTime = 0;
                    hit.hurt(player.damageSources().indirectMagic(player, player), damage);
                    hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), player);
                }
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, dead.getX(), dead.getY(0.5), dead.getZ(), 40, 1.0, 0.6, 1.0, 0.15);
            level.playSound(null, dead.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if (path.equals("nature") && Specs.has(player, "seed_burst")) {
            SpecKit.zone(level, dead.position(), 2.0, 100, ParticleTypes.HAPPY_VILLAGER, zone -> {
                for (LivingEntity ally : SpecKit.allies(player, zone.centre(), 2.0)) {
                    ally.heal(1.0F);
                }
            });
        }
        if (Specs.has(player, "soul_debt")) {
            SpecKit.addMana(player, Math.min(15.0F, dead.getMaxHealth() * 0.05F));
        }
        if (Specs.has(player, "soul_harvest")) {
            WISPS.add(new Wisp(level, dead.position().add(0.0, 0.5, 0.0), player.getUUID(), level.getGameTime() + 200));
        }
        if (dead instanceof Mob && !(dead instanceof Player) && Specs.has(player, "rise_again") && level.random.nextFloat() < 0.10F) {
            List<UUID> risen = RISEN.computeIfAbsent(player.getUUID(), u -> new ArrayList<>());
            risen.removeIf(id -> !(level.getEntity(id) instanceof LivingEntity e) || !e.isAlive());
            if (risen.size() < 2) {
                SummonedZombie zombie = new SummonedZombie(level, player, true);
                zombie.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), 0.0F);
                level.addFreshEntity(zombie);
                risen.add(zombie.getUUID());
                SpecKit.later(level, 400, () -> {
                    if (zombie.isAlive()) {
                        level.sendParticles(ParticleTypes.SMOKE, zombie.getX(), zombie.getY(0.5), zombie.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                        zombie.discard();
                    }
                });
            }
        }
    }

    // ---- heals (Martyr's Grace, Last Light) ----

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHeal(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        if (crediting || !(target.level() instanceof ServerLevel level) || event.getAmount() <= 0.0F) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer healer : level.getEntitiesOfClass(ServerPlayer.class, target.getBoundingBox().inflate(16.0),
            p -> HEALER.getOrDefault(p.getUUID(), -1000L) >= now - 100 && (p == target || p.isAlliedTo(target)))) {
            if (Specs.has(healer, "last_light") && target.getHealth() < target.getMaxHealth() * 0.3F) {
                event.setAmount(event.getAmount() * 1.4F);
            }
            if (target != healer && target instanceof Player && Specs.has(healer, "martyr_s_grace")) {
                crediting = true;
                try {
                    healer.heal(event.getAmount() * 0.3F);
                } finally {
                    crediting = false;
                }
            }
            return;
        }
    }

    // ---- projectiles (Jade Mirror) ----

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof ServerPlayer player
            && projectile.getOwner() != player && Specs.has(player, "jade_mirror") && player.level().random.nextFloat() < 0.15F) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.0));
            projectile.setOwner(player);
            player.serverLevel().sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
        }
    }

    // ---- per tick: wisps, Glacial Step, Ghost Lanterns, Smoulder, Drenched ----

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 5 != 0) {
            return;
        }
        for (Iterator<Wisp> it = WISPS.iterator(); it.hasNext();) {
            Wisp wisp = it.next();
            if (wisp.level.getGameTime() > wisp.until) {
                it.remove();
                continue;
            }
            wisp.level.sendParticles(ParticleTypes.SOUL, wisp.pos.x, wisp.pos.y, wisp.pos.z, 2, 0.1, 0.1, 0.1, 0.01);
            if (wisp.level.getPlayerByUUID(wisp.owner) instanceof ServerPlayer player && player.position().add(0.0, 0.5, 0.0).distanceToSqr(wisp.pos) < 2.25) {
                SpecKit.addMana(player, SpecKit.maxMana(player) * 0.05F);
                wisp.level.playSound(null, BlockPos.containing(wisp.pos), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 1.2F);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.onGround() && Specs.has(player, "glacial_step")) {
            FrostWalkerEnchantment.onEntityMoved(player, player.level(), player.blockPosition(), 1);
        }
        if (player.tickCount % 20 == 0 && Specs.has(player, "ghost_lanterns")) {
            for (Entity e : player.serverLevel().getEntities(player, player.getBoundingBox().inflate(32.0), e -> SpecKit.owner(e) == player)) {
                for (LivingEntity enemy : SpecKit.enemies(player, e.position(), 8.0)) {
                    if (enemy.isInvisible()) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (entity.getPersistentData().getLong(SOAKED) > now && entity.isOnFire()) {
            entity.clearFire();
        }
        long smoulder = entity.getPersistentData().getLong(SMOULDER);
        if (smoulder > now && entity.isInWaterRainOrBubble()) {
            entity.setRemainingFireTicks((int)(smoulder - now));
            if (now % 20 == 0) {
                entity.hurt(entity.damageSources().onFire(), 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && Specs.has(player, "glacial_step")) {
            BlockPos below = player.blockPosition().below();
            var state = player.level().getBlockState(below);
            var at = player.level().getBlockState(player.blockPosition());
            if (state.is(BlockTags.ICE) || state.is(BlockTags.SNOW) || at.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                event.setCanceled(true);
            }
        }
    }

    private record Wisp(ServerLevel level, Vec3 pos, UUID owner, long until) {
    }
}
