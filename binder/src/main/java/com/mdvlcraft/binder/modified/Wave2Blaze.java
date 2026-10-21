package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The second-wave Modified abilities of the blaze vocations (Frostfire to Desert Walker) and the druid vocations that share the
 * group (Kodama Sage to Wayfinder): fire rings, bombs and lances, marks paid off by events, timed states kept with Buffs, the
 * slimes and bees they summon, and their conjured food.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Blaze {
    private static final String CINDER_RIME = "cinder_rime";
    private static final String PHOENIX = "phoenix_rebirth";
    private static final String SIGIL = "infernal_sigil";
    private static final String ASHEN = "ashen_renewal";
    private static final String WISP = "wisp_brand";
    private static final String TITHE = "ember_tithe";
    private static final String PYRE = "pyre_quiver";
    private static final String SAPJADE = "sapjade_bark";
    private static final String BRAMBLE = "bramble_shot";
    private static final String BRAMBLE_ARROW = "mdvlcraft_bramble_arrow";
    private static final String LEAFEN = "leafen_hide";
    private static final String TRAILBLAZE = "trailblaze";
    private static final String FLATBREAD = "blaze_flatbread";
    private static final String STEW = "blaze_stew";
    private static final UUID VERMILION_ID = UUID.fromString("5a9e3c17-2b8d-4f60-a1c4-93e7d2b0f85c");
    private static final UUID SAPJADE_ID = UUID.fromString("c63f1e8a-7d24-4b95-8e0a-1f5b9c2d7e46");

    private Wave2Blaze() {
    }

    // ---- shared helpers ----

    /** The power of Modified ability `key` for `caster` (for event handlers, which have no spell level). */
    static float power(String key, LivingEntity caster) {
        return Wave2Guardian.spell(key).getSpellPower(1, caster);
    }

    /** Freezes `target` for `ticks`: frozen over and held in place. */
    static void freeze(LivingEntity target, int ticks) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        MartialSpells.root(target, ticks);
    }

    /** Changes `target`'s armour by `amount` (`operation`) for `ticks` (a transient modifier, taken off when the last one runs out). */
    static void armour(LivingEntity target, UUID id, String key, double amount, AttributeModifier.Operation operation, int ticks) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        armor.removeModifier(id);
        armor.addTransientModifier(new AttributeModifier(id, "mdvlcraft " + key, amount, operation));
        Buffs.give(target, key, ticks);
        Zones.start(level, target.position(), target, 0.0F, ticks, ticks + 1, zone -> {
        }, () -> {
            if (!Buffs.has(target, key)) {
                armor.removeModifier(id);
            }
        }, null);
    }

    /** One roll of `victim`'s loot table, as if `source` had killed it. */
    static List<ItemStack> loot(ServerLevel level, LivingEntity victim, DamageSource source) {
        LootTable table = level.getServer().getLootData().getLootTable(victim.getLootTable());
        LootParams.Builder params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, victim)
            .withParameter(LootContextParams.ORIGIN, victim.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, source)
            .withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity())
            .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity());
        if (source.getEntity() instanceof Player player) {
            params = params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
        }
        return table.getRandomItems(params.create(LootContextParamSets.ENTITY));
    }

    /** A spell that only puts the timed state `key` (Buffs) on the caster, with a puff of particles; the event handlers do the rest. */
    public static class Timed extends ModifiedSpell {
        private final int ticks;

        Timed(String key, int ticks) {
            super(key, 0);
            this.ticks = ticks;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, this.config.key(), this.ticks);
            this.started(level, caster);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.FLAME, caster.getX(), caster.getY(0.6), caster.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }

        /** What else starts with the state (an effect, blocks). */
        void started(Level level, LivingEntity caster) {
        }
    }

    // ---- Frostfire ----

    /** Equinox Ring: a 6-block ring; enemies within 3 blocks take the spell's power and burn 4 s, those 3-6 away take it and freeze 2 s. */
    public static class EquinoxRing extends ModifiedSpell {
        EquinoxRing() {
            super("equinox_ring", 5);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                    DamageSources.applyDamage(enemy, power, this.getDamageSource(caster));
                    if (enemy.distanceToSqr(caster.position()) <= 9.0) {
                        enemy.setSecondsOnFire(4);
                    } else {
                        freeze(enemy, 40);
                    }
                }
                GroundSpells.ring(server, caster.position(), 3.0, ParticleTypes.FLAME);
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.SNOWFLAKE);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Cinder Rime: marks the creature looked at with frost (Slowness I) for 8 s; if it is set on fire meanwhile it explodes for 8 within 2.5 blocks. */
    public static class CinderRime extends SmiteSpells.Smite {
        CinderRime() {
            super(CINDER_RIME, 8, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0), caster);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.SNOWFLAKE);
            level.playSound(null, target.blockPosition(), SoundEvents.POWDER_SNOW_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
            Zones.start(level, target.position(), caster, power, 160, 1, zone -> {
                if (!target.isAlive()) {
                    zone.stop();
                    return;
                }
                if (zone.age() % 10 == 0) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
                }
                if (target.isOnFire()) {
                    for (LivingEntity enemy : Targets.enemies(caster, target.position(), 2.5)) {
                        this.hurt(caster, enemy, zone.power);
                    }
                    level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5), target.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 40, 1.0, 0.6, 1.0, 0.1);
                    level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.2F);
                    zone.stop();
                }
            }, () -> {
            }, null);
        }
    }

    // ---- Steamcaller ----

    /** Kettle Bomb: a boiling kettle bursting in 3 blocks where it lands, dealing the spell's power, knocking 5 blocks back, burning 3 s. */
    static BoltSpells.Bolt kettleBomb() {
        return new BoltSpells.Bolt("kettle_bomb", 5, Items.CAULDRON, ParticleTypes.CLOUD, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                bolt.hurt(caster, enemy, power);
                Wave2Guardian.knock(at, enemy, 5.0);
                enemy.setSecondsOnFire(3);
            }
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.5, at.z, 60, 1.5, 0.6, 1.5, 0.08);
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 60, 1.5, 0.4, 1.5, 0.2);
            level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.5F);
            level.playSound(null, BlockPos.containing(at), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.5F, 0.8F);
        }).speed(1.0F).gravity(0.05).sound(SoundEvents.WITCH_THROW);
    }

    /** Vapour Dash: a 7-block dash on a jet of steam, setting every creature passed on fire for 4 s. */
    public static class VapourDash extends ModifiedSpell {
        VapourDash() {
            super("vapour_dash", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 from = caster.position();
            for (LivingEntity target : MartialSpells.dash(caster, 7.0)) {
                target.setSecondsOnFire(4);
            }
            if (level instanceof ServerLevel server) {
                Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                for (int i = 0; i <= 7; i++) {
                    Vec3 at = from.add(look.scale(i));
                    server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, 4, 0.2, 0.2, 0.2, 0.02);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Solar Flare ----

    /** Sunspot: marks the creature looked at (Glowing); 3 s later a solar bolt strikes it for 10. */
    public static class Sunspot extends SmiteSpells.Smite {
        Sunspot() {
            super("sunspot", 10, 24.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0), caster);
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(1.0) + 0.5, target.getZ(), 15, 0.3, 0.2, 0.3, 0.01);
            level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.5F);
            Zones.start(level, target.position(), caster, power, 61, 60, zone -> {
                if (zone.age() == 0 || !target.isAlive()) {
                    return;
                }
                SmiteSpells.flash(level, target.position());
                this.hurt(caster, target, zone.power);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            }, () -> {
            }, null);
        }
    }

    /** Corona Halo: for 10 s, every second a halo zaps the nearest enemy within 5 blocks for 3 and sets it on fire for 3 s. */
    public static class CoronaHalo extends ModifiedSpell {
        CoronaHalo() {
            super("corona_halo", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 200, 20, zone -> {
                    if (!caster.isAlive()) {
                        zone.stop();
                        return;
                    }
                    Vec3 halo = caster.getEyePosition().add(0.0, 0.6, 0.0);
                    for (int i = 0; i < 12; i++) {
                        double a = i * Math.PI / 6;
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, halo.x + Math.cos(a) * 0.5, halo.y, halo.z + Math.sin(a) * 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                    Targets.enemies(caster, caster.position(), 5.0).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(caster)))
                        .ifPresent(enemy -> {
                            SmiteSpells.beam(server, halo, enemy.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK);
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                            enemy.setSecondsOnFire(3);
                            server.playSound(null, enemy.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 1.8F);
                        });
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Phoenix ----

    /** Solar Lance: a burning lance dealing 8 (double to the undead) and setting the target on fire for 5 s. */
    static BoltSpells.Bolt solarLance() {
        return new BoltSpells.Bolt("solar_lance", 8, Items.BLAZE_ROD, ParticleTypes.FLAME, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, BoltSpells.undead(target) ? power * 2.0F : power)) {
                target.setSecondsOnFire(5);
            }
        }, null).speed(2.0F).sound(SoundEvents.TRIDENT_THROW);
    }

    // ---- Infernalist ----

    /** Brimstone Slimes: 3 magma cubes for 20 s that hunt hostile mobs and set every enemy they touch on fire for 4 s. */
    public static class BrimstoneSlimes extends SummonSpell {
        BrimstoneSlimes() {
            super("brimstone_slimes", 2);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.MAGMA_CUBE, 3, castData, cube -> cube.setSize(2, true));
        }
    }

    /** Infernal Sigil: marks the creature looked at for 8 s; every spell its marker (a player) casts deals 4 soul fire damage to it. */
    static ModifiedSpell infernalSigil() {
        return new Wave2Samurai.Mark(SIGIL, 160);
    }

    // ---- Wildfire ----

    /** Ember Seed: a seed that sprouts a fire flower where it lands for 4 s; it deals the spell's power once and keeps enemies within 3 blocks burning (4 s). */
    static BoltSpells.Bolt emberSeed() {
        return new BoltSpells.Bolt("ember_seed", 4, Items.TORCHFLOWER_SEEDS, ParticleTypes.FLAME, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Entity flower = GroundSpells.marker(level, at, Items.TORCHFLOWER);
            Zones.start(level, at, caster, power, 80, 20, zone -> {
                for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                    if (zone.age() == 0) {
                        bolt.hurt(caster, enemy, zone.power);
                    }
                    enemy.setSecondsOnFire(4);
                }
                GroundSpells.ring(level, at, 3.0, ParticleTypes.FLAME);
                level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.6, at.z, 4, 0.2, 0.2, 0.2, 0.0);
            }, () -> {
            }, flower);
            level.playSound(null, BlockPos.containing(at), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }).speed(1.0F).gravity(0.05).scale(0.6F).sound(SoundEvents.CROP_PLANTED);
    }

    // ---- Foxfire ----

    /** Vermilion Shard: a burning jade shard dealing the spell's power, setting the target on fire 4 s and taking 3 of its armour for 6 s. */
    static BoltSpells.Bolt vermilionShard() {
        return new BoltSpells.Bolt("vermilion_shard", 5, Items.EMERALD, ParticleTypes.FLAME, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, power)) {
                target.setSecondsOnFire(4);
            }
            armour(target, VERMILION_ID, "vermilion_shard_armour", -3.0, AttributeModifier.Operation.ADDITION, 120);
        }, null).speed(1.7F).scale(0.6F).sound(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    /** Ninefold Ring: a 5-block fire ring dealing the spell's power and burning enemies 3 s; Resistance for 5 s, one level per enemy hit (up to II). */
    public static class NinefoldRing extends ModifiedSpell {
        NinefoldRing() {
            super("ninefold_ring", 5);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                int hit = 0;
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 5.0)) {
                    if (DamageSources.applyDamage(enemy, power, this.getDamageSource(caster))) {
                        hit++;
                    }
                    enemy.setSecondsOnFire(3);
                }
                if (hit > 0) {
                    caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, Math.min(hit, 2) - 1));
                }
                GroundSpells.ring(server, caster.position(), 5.0, ParticleTypes.FLAME);
                GroundSpells.ring(server, caster.position(), 3.0, ParticleTypes.SOUL_FIRE_FLAME);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, caster.blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 0.6F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Onibi ----

    /** Wisp Brand: marks the creature looked at for 12 s; if it dies, 3 soulfire bursts (half a second apart) hit enemies within 4 blocks for the spell's power and soulburn 4 s. */
    static ModifiedSpell wispBrand() {
        return new Wave2Samurai.Mark(WISP, 240);
    }

    /** Oni Hearth: a 5-block area on the caster pulsing for 6 s, healing allies 1 a second and soulburning enemies in it. */
    static GroundSpells.Area oniHearth() {
        return new GroundSpells.Area("oni_hearth", 0, 5.0, 6, 20, false, ParticleTypes.SOUL_FIRE_FLAME, SoundEvents.SOUL_ESCAPE,
            (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(1.0F);
                }
                for (LivingEntity enemy : enemies) {
                    BoltSpells.soulburn(zone.caster, enemy, 40);
                }
            }).marker(Items.SOUL_CAMPFIRE);
    }

    // ---- Firebug ----

    /** Firecracker Purse: a pouch bursting into 5 small fire blasts within 3 blocks of where it lands; each deals the spell's power, knocks back and burns 3 s. */
    static BoltSpells.Bolt firecrackerPurse() {
        return new BoltSpells.Bolt("firecracker_purse", 3, Items.FIREWORK_STAR, ParticleTypes.FIREWORK, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Zones.start(level, at, caster, power, 15, 3, zone -> {
                Vec3 blast = at.add((level.random.nextDouble() - 0.5) * 4.0, 0.0, (level.random.nextDouble() - 0.5) * 4.0);
                for (LivingEntity enemy : Targets.enemies(caster, blast, 1.8)) {
                    enemy.invulnerableTime = 0;
                    bolt.hurt(caster, enemy, zone.power);
                    Wave2Guardian.knock(blast, enemy, 2.0);
                    enemy.setSecondsOnFire(3);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, blast.x, blast.y + 0.4, blast.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.FLAME, blast.x, blast.y + 0.4, blast.z, 15, 0.5, 0.3, 0.5, 0.05);
                level.playSound(null, BlockPos.containing(blast), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.2F);
            }, () -> {
            }, null);
        }).speed(1.0F).gravity(0.05).scale(0.7F).sound(SoundEvents.WITCH_THROW);
    }

    /** Ember Tithe: marks the creature looked at for 15 s; if it dies burning, its loot table is rolled once more and it erupts, dealing the spell's power and burning enemies within 3 blocks. */
    static ModifiedSpell emberTithe() {
        return new Wave2Samurai.Mark(TITHE, 300);
    }

    // ---- Fire Archer ----

    /** Signal Flare: a flare; enemies within 6 blocks of where it lands glow for 10 s and burn for 3 s. */
    static BoltSpells.Bolt signalFlare() {
        return new BoltSpells.Bolt("signal_flare", 0, Items.FIREWORK_ROCKET, ParticleTypes.FIREWORK, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            for (LivingEntity enemy : Targets.enemies(caster, at, 6.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), caster);
                enemy.setSecondsOnFire(3);
            }
            level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 1.0, at.z, 80, 2.0, 1.5, 2.0, 0.1);
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 30, 1.0, 0.5, 1.0, 0.05);
            level.playSound(null, BlockPos.containing(at), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0F, 1.0F);
        }).speed(1.6F).gravity(0.02).sound(SoundEvents.FIREWORK_ROCKET_LAUNCH);
    }

    // ---- Desert Walker ----

    /** Searing Sands: a 6-block cone of hot sand dealing the spell's power, blinding enemies 3 s and burning them 2 s. */
    static ConeSpells.Cone searingSands() {
        return new ConeSpells.Cone("searing_sands", 4, 6.0, 35.0, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
            SoundEvents.SAND_BREAK, (caster, target, dealt) -> {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), caster);
                target.setSecondsOnFire(2);
            });
    }

    /** Sunbaked Flatbread: three flatbreads; eaten, Fire Resistance for 60 s and Speed I for 30 s. */
    static BoonSpells.Food sunbakedFlatbread() {
        return new BoonSpells.Food("sunbaked_flatbread", Items.BREAD, FLATBREAD, 3, false);
    }

    // ---- Kodama Sage ----

    /** Bamboo Palisade: a bamboo wall 5 wide and 3 tall, 3 blocks ahead of the caster, for 8 s. */
    public static class BambooPalisade extends ModifiedSpell {
        BambooPalisade() {
            super("bamboo_palisade", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockState bamboo = Blocks.BAMBOO_BLOCK.defaultBlockState();
                List<BlockPos> wall = WallSpells.wall(caster);
                Wave2Guardian.raise(server, caster, wall, bamboo, 160, 160, (zone, box) -> {
                });
                for (BlockPos pos : wall) {
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, bamboo), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.05);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BAMBOO_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Forest Kami ----

    /** Kami Swarm: 3 spirit bees for 20 s whose stings soulburn for 3 s (they keep their stingers). */
    public static class KamiSwarm extends SummonSpell {
        KamiSwarm() {
            super("kami_swarm", 2);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.BEE, 3, castData, bee -> {
            });
        }
    }

    /** Mossbound Circle: a 5-block circle on the caster pulsing for 6 s, healing allies 1 a second, 2 for one standing on grass. */
    static GroundSpells.Area mossboundCircle() {
        return new GroundSpells.Area("mossbound_circle", 0, 5.0, 6, 20, false, ParticleTypes.HAPPY_VILLAGER, SoundEvents.MOSS_PLACE,
            (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(ally.getBlockStateOn().is(Blocks.GRASS_BLOCK) ? 2.0F : 1.0F);
                }
            });
    }

    // ---- Magpie ----

    /** Glitter Bait: a shiny trinket that, where it lands, pulls mobs within 6 blocks toward it for 3 s. */
    static BoltSpells.Bolt glitterBait() {
        return new BoltSpells.Bolt("glitter_bait", 0, Items.GOLD_NUGGET, ParticleTypes.WAX_OFF, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Entity trinket = GroundSpells.marker(level, at, Items.GOLD_NUGGET);
            Zones.start(level, at, caster, power, 60, 2, zone -> {
                for (LivingEntity enemy : Targets.enemies(caster, at, 6.0)) {
                    Vec3 to = at.subtract(enemy.position()).multiply(1.0, 0.0, 1.0);
                    if (enemy instanceof Mob && to.length() > 1.0) {
                        enemy.setDeltaMovement(to.normalize().scale(0.3).add(0.0, enemy.getDeltaMovement().y, 0.0));
                        enemy.hurtMarked = true;
                    }
                }
                if (zone.age() % 10 == 0) {
                    level.sendParticles(ParticleTypes.WAX_OFF, at.x, at.y + 0.5, at.z, 10, 0.4, 0.4, 0.4, 0.5);
                    level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
                }
            }, () -> {
            }, trinket);
        }).speed(1.0F).gravity(0.05).sound(SoundEvents.ITEM_PICKUP);
    }

    /** Thieving Talon: strikes the creature looked at for the spell's power with Slowness II for 4 s; 30% chance it drops one item of its loot. */
    public static class ThievingTalon extends SmiteSpells.Smite {
        ThievingTalon() {
            super("thieving_talon", 4, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.7), target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.PARROT_HURT, SoundSource.PLAYERS, 1.0F, 0.8F);
            if (target.isAlive() && !BoltSpells.summon(target) && caster.getRandom().nextFloat() < 0.3F) {
                List<ItemStack> loot = loot(level, target, this.getDamageSource(caster));
                if (!loot.isEmpty()) {
                    target.spawnAtLocation(loot.get(caster.getRandom().nextInt(loot.size())));
                    level.sendParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY(0.7), target.getZ(), 10, 0.3, 0.3, 0.3, 0.5);
                    level.playSound(null, target.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                }
            }
        }
    }

    // ---- Sylvan Archer ----

    /** Bramble Shot: within 30 s, the caster's next arrow roots what it hits (Slowness VI) for 3 s and makes it glow for 8 s. */
    static ModifiedSpell brambleShot() {
        return new Timed(BRAMBLE, 600);
    }

    /** Leafen Hide: a ring of leaves around the caster (a window ahead) for 10 s; Invisibility and 20% more ranged damage for 10 s. */
    public static class LeafenHide extends Timed {
        LeafenHide() {
            super(LEAFEN, 200);
        }

        @Override
        void started(Level level, LivingEntity caster) {
            caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false, true));
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            BlockPos at = caster.blockPosition();
            Direction ahead = caster.getDirection();
            BlockPos window = at.relative(ahead, 2).above();
            List<BlockPos> hide = new ArrayList<>();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        for (int y = 0; y < 2; y++) {
                            BlockPos pos = at.offset(dx, y, dz);
                            if (!pos.equals(window)) {
                                hide.add(pos);
                            }
                        }
                    }
                }
            }
            Wave2Guardian.raise(server, caster, hide, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 200, 200,
                (zone, box) -> {
                });
            server.sendParticles(ParticleTypes.COMPOSTER, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 1.5, 0.6, 1.5, 0.0);
            level.playSound(null, caster.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 1.5F, 1.0F);
        }
    }

    // ---- Wayfinder ----

    /** Trailblaze: Speed II for 12 s; each kill meanwhile restores 2 hunger. */
    public static class Trailblaze extends Timed {
        Trailblaze() {
            super(TRAILBLAZE, 240);
        }

        @Override
        void started(Level level, LivingEntity caster) {
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 1));
        }
    }

    /** Wildroot Stew: two bowls of stew (8 hunger each) that grant Resistance I for 60 s. */
    public static class WildrootStew extends ModifiedSpell {
        WildrootStew() {
            super("wildroot_stew", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof Player player) {
                for (int i = 0; i < 2; i++) {   // a stew doesn't stack: one bowl at a time
                    ItemStack stew = BoonSpells.boon(Items.MUSHROOM_STEW, STEW, 1);
                    if (!player.getInventory().add(stew)) {
                        player.spawnAtLocation(stew);
                    }
                }
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, caster.getX(), caster.getY() + 1.0, caster.getZ(), 15, 0.8, 0.5, 0.8, 0.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new EquinoxRing(),
            new CinderRime(),
            kettleBomb(),
            new VapourDash(),
            new Sunspot(),
            new CoronaHalo(),
            new Timed(PHOENIX, 600),        // Phoenix Rebirth: for 30 s lethal damage instead revives with 6 health and a 4-block fire ring
            solarLance(),
            new BrimstoneSlimes(),
            infernalSigil(),
            emberSeed(),
            new Timed(ASHEN, 240),          // Ashen Renewal: for 12 s killing a burning creature gives Regeneration II for 8 s
            vermilionShard(),
            new NinefoldRing(),
            wispBrand(),
            oniHearth(),
            firecrackerPurse(),
            emberTithe(),
            signalFlare(),
            new Timed(PYRE, 200),           // Pyre Quiver: for 10 s arrows ignite and deal 3 more to targets already burning
            searingSands(),
            sunbakedFlatbread(),
            new BambooPalisade(),
            new Timed(SAPJADE, 200),        // Sapjade Bark: for 10 s each hit taken heals 2 and takes 10% of the attacker's armour for 5 s
            new KamiSwarm(),
            mossboundCircle(),
            glitterBait(),
            new ThievingTalon(),
            brambleShot(),
            new LeafenHide(),
            new Trailblaze(),
            new WildrootStew());
    }

    // ---- event handlers ----

    /** Phoenix Rebirth: lethal damage is undone; the caster stands with 6 health and a 4-block ring of fire burns its enemies. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLethal(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!Buffs.has(dead, PHOENIX) || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        event.setCanceled(true);
        Buffs.clear(dead, PHOENIX);
        dead.setHealth(6.0F);
        dead.clearFire();
        float power = power(PHOENIX, dead);
        for (LivingEntity enemy : Targets.enemies(dead, dead.position(), 4.0)) {
            Wave2Samurai.hurt(PHOENIX, dead, enemy, power);
            enemy.setSecondsOnFire(5);
        }
        GroundSpells.ring(level, dead.position(), 4.0, ParticleTypes.FLAME);
        level.sendParticles(ParticleTypes.FLAME, dead.getX(), dead.getY(1.0), dead.getZ(), 60, 0.5, 1.0, 0.5, 0.1);
        level.playSound(null, dead.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.playSound(null, dead.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** Ashen Renewal, Trailblaze, Wisp Brand, Ember Tithe and Brimstone Slimes pay off when something dies. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // Brimstone Slimes: a dying cube doesn't split into wild ones
        if ("brimstone_slimes".equals(Summons.kind(victim)) && victim instanceof Slime cube) {
            cube.setSize(1, false);
        }
        if (event.getSource().getEntity() instanceof LivingEntity killer && killer != victim) {
            if (Buffs.has(killer, ASHEN) && victim.isOnFire()) {
                killer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
            }
            if (Buffs.has(killer, TRAILBLAZE) && killer instanceof Player player) {
                player.getFoodData().eat(2, 0.0F);
            }
        }
        // Wisp Brand: 3 soulfire bursts half a second apart
        LivingEntity brander = Buffs.markedBy(victim, WISP);
        if (brander != null) {
            Buffs.clear(victim, WISP);
            Vec3 at = victim.position();
            Zones.start(level, at, brander, power(WISP, brander), 30, 10, zone -> {
                for (LivingEntity enemy : Targets.enemies(brander, at, 4.0)) {
                    enemy.invulnerableTime = 0;
                    Wave2Samurai.hurt(WISP, brander, enemy, zone.power);
                    BoltSpells.soulburn(brander, enemy, 80);
                }
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.8, at.z, 50, 1.5, 0.6, 1.5, 0.05);
                level.playSound(null, BlockPos.containing(at), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 2.0F, 0.8F);
            }, () -> {
            }, null);
        }
        // Ember Tithe: a burning death erupts in 3-block flames (the extra loot is rolled in onDrops)
        LivingEntity tither = Buffs.markedBy(victim, TITHE);
        if (tither != null && victim.isOnFire()) {
            float power = power(TITHE, tither);
            for (LivingEntity enemy : Targets.enemies(tither, victim.position(), 3.0)) {
                Wave2Samurai.hurt(TITHE, tither, enemy, power);
                enemy.setSecondsOnFire(4);
            }
            level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY(0.5), victim.getZ(), 60, 1.5, 0.8, 1.5, 0.08);
            level.sendParticles(ParticleTypes.LAVA, victim.getX(), victim.getY(0.5), victim.getZ(), 10, 1.0, 0.5, 1.0, 0.0);
            level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    /** Ember Tithe: a marked creature that dies burning rolls its loot table once more. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!Buffs.has(victim, TITHE) || !victim.isOnFire() || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        Buffs.clear(victim, TITHE);
        for (ItemStack stack : loot(level, victim, event.getSource())) {
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        // Leafen Hide: 20% more ranged damage
        if (source.is(DamageTypeTags.IS_PROJECTILE) && source.getEntity() instanceof LivingEntity shooter && Buffs.has(shooter, LEAFEN)) {
            event.setAmount(event.getAmount() * 1.2F);
        }
        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity archer) {
            // Pyre Quiver: 3 more to a burning target, and the target burns
            if (Buffs.has(archer, PYRE)) {
                if (victim.isOnFire()) {
                    event.setAmount(event.getAmount() + 3.0F);
                }
                victim.setSecondsOnFire(4);
            }
            // Bramble Shot: rooted 3 s and glowing 8 s
            if (arrow.getPersistentData().getBoolean(BRAMBLE_ARROW)) {
                arrow.getPersistentData().remove(BRAMBLE_ARROW);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5), archer);
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0), archer);
                ((ServerLevel)victim.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()),
                    victim.getX(), victim.getY() + 0.3, victim.getZ(), 30, 0.4, 0.3, 0.4, 0.1);
            }
        }
        // Sapjade Bark: heal 2, the attacker loses 10% of its armour for 5 s
        if (Buffs.has(victim, SAPJADE) && source.getEntity() instanceof LivingEntity attacker && attacker != victim) {
            victim.heal(2.0F);
            armour(attacker, SAPJADE_ID, "sapjade_bark_armour", -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL, 100);
            ((ServerLevel)victim.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, victim.getX(), victim.getY(0.6), victim.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
        }
    }

    /** Kami Swarm: a bee's sting soulburns for 3 s. */
    @SubscribeEvent
    public static void onSummonHit(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Bee bee && "kami_swarm".equals(Summons.kind(bee)) && Summons.owner(bee) instanceof LivingEntity owner) {
            BoltSpells.soulburn(owner, event.getEntity(), 60);
        }
    }

    /** Infernal Sigil: each spell a player casts deals 4 soul fire damage to the creatures it has marked. */
    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        Player caster = event.getEntity();
        if (!(caster.level() instanceof ServerLevel level) || event.getSpellId().equals("mdvlcraft:" + SIGIL)) {
            return;
        }
        for (LivingEntity marked : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(32.0),
                e -> e.isAlive() && Buffs.markedBy(e, SIGIL) == caster)) {
            marked.invulnerableTime = 0;
            Wave2Samurai.hurt(SIGIL, caster, marked, 4.0F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, marked.getX(), marked.getY(0.6), marked.getZ(), 20, 0.3, 0.5, 0.3, 0.03);
            level.playSound(null, marked.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    /** Bramble Shot: the next arrow the caster looses is tagged. */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
            || !(arrow.getOwner() instanceof LivingEntity owner) || !Buffs.has(owner, BRAMBLE)) {
            return;
        }
        Buffs.clear(owner, BRAMBLE);
        arrow.getPersistentData().putBoolean(BRAMBLE_ARROW, true);
    }

    /** Brimstone Slimes hunt hostile mobs and ignite what they touch; Kami Swarm bees stay angry at their target and keep their stingers. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        switch (Summons.kind(mob)) {
            case "brimstone_slimes" -> {
                mob.targetSelector.removeAllGoals(goal -> true);
                mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Mob.class, 10, true, false,
                    target -> target instanceof Enemy && !Summons.allied(mob, target)));
                mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 10, cube -> {
                    for (LivingEntity touched : cube.level().getEntitiesOfClass(LivingEntity.class, cube.getBoundingBox().inflate(0.3),
                            e -> e != cube && e.isAlive() && !Summons.allied(cube, e))) {
                        touched.setSecondsOnFire(4);
                    }
                }));
            }
            case "kami_swarm" -> mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 10, m -> {
                if (!(m instanceof Bee bee)) {
                    return;
                }
                if (bee.hasStung()) {   // a spirit bee keeps its stinger (and so doesn't die of stinging)
                    CompoundTag tag = new CompoundTag();
                    bee.addAdditionalSaveData(tag);
                    tag.putBoolean("HasStung", false);
                    bee.readAdditionalSaveData(tag);
                }
                LivingEntity target = bee.getTarget();
                if (target != null && target.isAlive() && !bee.isAngry()) {
                    bee.setPersistentAngerTarget(target.getUUID());
                    bee.setRemainingPersistentAngerTime(200);
                }
                if (bee.level() instanceof ServerLevel level && bee.tickCount % 20 == 0) {
                    level.sendParticles(ParticleTypes.SOUL, bee.getX(), bee.getY(0.5), bee.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                }
            }));
            default -> {
            }
        }
    }

    /** Sunbaked Flatbread: Fire Resistance 60 s and Speed I 30 s. Wildroot Stew: 8 hunger in all and Resistance I 60 s. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity eater = event.getEntity();
        switch (BoonSpells.kindOf(event.getItem())) {
            case FLATBREAD -> {
                eater.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            }
            case STEW -> {
                if (eater instanceof Player player) {
                    player.getFoodData().eat(2, 0.1F);   // a mushroom stew gives 6
                }
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
            }
            default -> {
            }
        }
    }
}
