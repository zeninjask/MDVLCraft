package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The ground Modified abilities: areas that pulse for a while (Zones) where the caster stands or looks, healing allies
 * or hurting enemies in them, plus the storms that strike near the caster and the wave and parade that sweep ahead.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GroundSpells {
    private GroundSpells() {
    }

    /** Jade Ward and Spirit Ward take a quarter off damage from spells of their school. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource() instanceof SpellDamageSource spell) {
            String school = spell.spell().getSchoolType().getId().toString();
            LivingEntity victim = event.getEntity();
            if (school.equals("iss_magicfromtheeast:symmetry") && victim.hasEffect(BinderEffects.JADE_WARD.get())
                || school.equals("iss_magicfromtheeast:spirit") && victim.hasEffect(BinderEffects.SPIRIT_WARD.get())) {
                event.setAmount(event.getAmount() * 0.75F);
            }
        }
    }

    interface Pulse {
        void apply(Area area, Zones.Zone zone, List<LivingEntity> allies, List<LivingEntity> enemies);
    }

    /** An area of `radius` blocks lasting `seconds`, pulsing every `interval` ticks, on the caster or where it looks. */
    public static class Area extends ModifiedSpell {
        final double radius;
        final int seconds;
        final int interval;
        final boolean atLook;
        final ParticleOptions particle;
        final SoundEvent sound;
        final Pulse pulse;
        @Nullable
        Item marker;

        Area(String key, int power, double radius, int seconds, int interval, boolean atLook, ParticleOptions particle, SoundEvent sound, Pulse pulse) {
            super(key, power);
            this.radius = radius;
            this.seconds = seconds;
            this.interval = interval;
            this.atLook = atLook;
            this.particle = particle;
            this.sound = sound;
            this.pulse = pulse;
        }

        Area marker(Item item) {
            this.marker = item;
            return this;
        }

        boolean hurt(LivingEntity caster, LivingEntity target, float amount) {
            return DamageSources.applyDamage(target, amount, this.getDamageSource(caster));
        }

        Vec3 center(LivingEntity caster) {
            return this.atLook ? VolleySpells.lookedPoint(caster, 24.0) : caster.position();
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = this.center(caster);
                Entity marker = this.marker == null ? null : GroundSpells.marker(server, at, this.marker);
                Zones.start(server, at, caster, this.getSpellPower(spellLevel, caster), this.seconds * 20, this.interval, zone -> {
                    ring(server, zone.center, this.radius, this.particle);
                    this.pulse.apply(this, zone, Targets.allies(caster, zone.center, this.radius), Targets.enemies(caster, zone.center, this.radius));
                }, () -> {
                }, marker);
                level.playSound(null, BlockPos.containing(at), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** An invisible armour stand wearing `item` on its head, standing at `at` (removed with its zone). */
    static ArmorStand marker(ServerLevel level, Vec3 at, Item item) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        if (stand == null) {
            throw new IllegalStateException("Cannot create an armour stand");
        }
        stand.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item));
        level.addFreshEntity(stand);
        return stand;
    }

    static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        int points = (int)(radius * 8);
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.2, center.z + Math.sin(a) * radius, 1, 0.05, 0.1, 0.05, 0.0);
        }
        level.sendParticles(particle, center.x, center.y + 0.3, center.z, (int)(radius * radius), radius / 2, 0.1, radius / 2, 0.0);
    }

    static void effect(List<LivingEntity> who, LivingEntity caster, net.minecraft.world.effect.MobEffect effect, int ticks, int amplifier) {
        for (LivingEntity e : who) {
            e.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, true), caster);
        }
    }

    /** A creature counts as wet in rain, in water or with Travel Optics' Wet. */
    static boolean wet(LivingEntity target) {
        return target.isInWaterRainOrBubble() || target.hasEffect(BrandSpells.external("traveloptics:wet"));
    }

    /** Raijin's Drums: 8 beats a second apart, each striking an enemy within 10 blocks; the last soulburns them all. */
    public static class RaijinDrums extends ModifiedSpell {
        RaijinDrums() {
            super("raijin_drums", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 8 * 20, 20, zone -> {
                    List<LivingEntity> enemies = Targets.enemies(caster, caster.position(), 10.0);
                    server.playSound(null, caster.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 2.0F, 0.5F);
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 3.5, caster.getZ(), 20, 1.0, 0.3, 1.0, 0.2);
                    if (enemies.isEmpty()) {
                        return;
                    }
                    LivingEntity struck = enemies.get(caster.getRandom().nextInt(enemies.size()));
                    SmiteSpells.flash(server, struck.position());
                    DamageSources.applyDamage(struck, zone.power, this.getDamageSource(caster));
                    if (zone.age() >= 7 * 20) {
                        enemies.forEach(e -> BoltSpells.soulburn(caster, e, 100));
                    }
                }, () -> {
                }, null);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Typhoon: 10 s of storm around the caster: enemies are soaked; every second lightning strikes one, wet ones first, twice as hard. */
    public static class Typhoon extends ModifiedSpell {
        Typhoon() {
            super("storm_sea", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 10 * 20, 20, zone -> {
                    Vec3 at = caster.position();
                    server.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y + 6.0, at.z, 120, 8.0, 1.0, 8.0, 0.0);
                    List<LivingEntity> enemies = Targets.enemies(caster, at, 10.0);
                    LivingEntity struck = enemies.stream().filter(GroundSpells::wet).findAny()
                        .orElse(enemies.isEmpty() ? null : enemies.get(caster.getRandom().nextInt(enemies.size())));
                    if (struck != null) {
                        SmiteSpells.flash(server, struck.position());
                        DamageSources.applyDamage(struck, wet(struck) ? zone.power * 2.0F : zone.power, this.getDamageSource(caster));
                    }
                    enemies.forEach(e -> e.addEffect(new MobEffectInstance(BrandSpells.external("traveloptics:wet"), 60, 0), caster));
                }, () -> {
                }, null);
                level.playSound(null, caster.blockPosition(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 2.0F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hyakki Yagyo: a 4-block-wide parade advancing 14 blocks ahead over 1.4 s; each enemy it passes is hurt once, soulburned and weakened. */
    public static class NightParade extends ModifiedSpell {
        NightParade() {
            super("night_parade", 8);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 start = caster.position();
                Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                Set<LivingEntity> passed = new HashSet<>();
                Zones.start(server, start, caster, this.getSpellPower(spellLevel, caster), 28, 2, zone -> {
                    Vec3 front = start.add(look.scale(zone.age() / 2.0 + 1.0));
                    server.sendParticles(ParticleTypes.SOUL, front.x, front.y + 1.0, front.z, 12, 1.5, 0.8, 1.5, 0.02);
                    server.sendParticles(ParticleTypes.SCULK_SOUL, front.x, front.y + 0.5, front.z, 6, 1.5, 0.3, 1.5, 0.02);
                    for (LivingEntity enemy : Targets.enemies(caster, front, 2.0)) {
                        if (passed.add(enemy)) {
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                            BoltSpells.soulburn(caster, enemy, 100);
                            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), caster);
                        }
                    }
                }, () -> {
                }, null);
                level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.7F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Wildfire: a fire burst where the caster looks that also lights the grass and leaves around it (if mobs may grief). */
    public static class Wildfire extends ModifiedSpell {
        Wildfire() {
            super("wildfire", 7);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = VolleySpells.lookedPoint(caster, 24.0);
                for (LivingEntity enemy : Targets.enemies(caster, at, 4.0)) {
                    if (DamageSources.applyDamage(enemy, this.getSpellPower(spellLevel, caster), this.getDamageSource(caster))) {
                        enemy.setSecondsOnFire(6);
                    }
                }
                if (server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    BlockPos center = BlockPos.containing(at);
                    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -3, -6), center.offset(6, 4, 6))) {
                        BlockState state = server.getBlockState(pos);
                        boolean burns = state.is(BlockTags.LEAVES) || state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN);
                        if (burns && pos.distSqr(center) <= 36 && caster.getRandom().nextFloat() < 0.6F) {
                            BlockPos above = pos.above();
                            if (state.is(BlockTags.LEAVES) && server.isEmptyBlock(above)) {
                                server.setBlockAndUpdate(above, BaseFireBlock.getState(server, above));
                            } else if (!state.is(BlockTags.LEAVES)) {
                                server.setBlockAndUpdate(pos, BaseFireBlock.getState(server, pos));
                            }
                        }
                    }
                }
                server.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.0, at.z, 120, 3.0, 1.0, 3.0, 0.08);
                level.playSound(null, BlockPos.containing(at), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Taiji Seal: allies in it heal; enemies take symmetry damage, then spirit damage, by turns. */
    static void taiji(Area area, Zones.Zone zone, List<LivingEntity> allies, List<LivingEntity> enemies) {
        allies.forEach(a -> a.heal(1.5F));
        boolean yin = zone.age() / area.interval % 2 == 0;
        for (LivingEntity enemy : enemies) {
            DamageSources.applyDamage(enemy, zone.power, yin ? area.getDamageSource(zone.caster)
                : SpellRegistry.getSpell(new ResourceLocation("mdvlcraft", "palm_soul")).getDamageSource(zone.caster));
        }
        zone.level.sendParticles(yin ? ParticleTypes.WHITE_ASH : ParticleTypes.SQUID_INK, zone.center.x, zone.center.y + 0.3, zone.center.z,
            40, 2.5, 0.1, 2.5, 0.0);
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new Area("black_ice", 0, 5.0, 8, 10, false, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_PLACE, (area, zone, allies, enemies) -> {
                for (LivingEntity enemy : enemies) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), zone.caster);
                    enemy.addEffect(new MobEffectInstance(BinderEffects.SHATTERED.get(), 30, 0), zone.caster);
                    Vec3 slip = new Vec3(enemy.getRandom().nextDouble() - 0.5, 0.0, enemy.getRandom().nextDouble() - 0.5).scale(0.5);
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(slip));
                    enemy.hurtMarked = true;
                }
            }),
            new Area("bog", 0, 4.0, 8, 10, true, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), SoundEvents.MUD_PLACE,
                (area, zone, allies, enemies) -> {
                    for (LivingEntity enemy : enemies) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3), zone.caster);
                        enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), zone.caster);
                        enemy.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 128, false, false), zone.caster);
                    }
                }),
            new Area("corrupted_grove", 0, 5.0, 8, 20, false, ParticleTypes.SCULK_SOUL, SoundEvents.SCULK_BLOCK_SPREAD, (area, zone, allies, enemies) -> {
                for (LivingEntity enemy : enemies) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), zone.caster);
                    MartialSpells.root(enemy, 20);
                }
            }),
            new Area("sanctify", 0, 5.0, 10, 20, false, ParticleTypes.HAPPY_VILLAGER, SoundEvents.AMETHYST_BLOCK_CHIME,
                (area, zone, allies, enemies) -> effect(allies, zone.caster, MobEffects.REGENERATION, 50, 0)),
            new Area("hailstorm", 5, 5.0, 8, 10, true, ParticleTypes.ITEM_SNOWBALL, SoundEvents.SNOW_BREAK, (area, zone, allies, enemies) -> {
                zone.level.sendParticles(ParticleTypes.ITEM_SNOWBALL, zone.center.x, zone.center.y + 6.0, zone.center.z, 60, 4.0, 0.5, 4.0, 0.0);
                for (LivingEntity enemy : enemies) {
                    area.hurt(zone.caster, enemy, zone.power * 0.3F);
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), zone.caster);
                }
            }),
            new Area("hot_spring", 0, 4.0, 10, 20, false, ParticleTypes.CLOUD, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(2.0F);
                    ally.setTicksFrozen(0);
                    ally.removeEffect(BrandSpells.external("irons_spellbooks:chilled"));
                }
            }),
            new Area("jade_lotus", 0, 4.0, 10, 20, false, ParticleTypes.HAPPY_VILLAGER, SoundEvents.AMETHYST_BLOCK_RESONATE, (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(1.5F);
                    ally.addEffect(new MobEffectInstance(BinderEffects.JADE_WARD.get(), 50, 0, true, true), zone.caster);
                }
            }).marker(Items.LILY_PAD),
            new Area("war_banner", 0, 6.0, 15, 20, false, ParticleTypes.CRIT, SoundEvents.RAID_HORN.value(),
                (area, zone, allies, enemies) -> effect(allies, zone.caster, MobEffects.DAMAGE_BOOST, 50, 0)).marker(Items.RED_BANNER),
            new Area("kami_shrine", 0, 5.0, 15, 20, false, ParticleTypes.CHERRY_LEAVES, SoundEvents.BELL_BLOCK, (area, zone, allies, enemies) -> {
                allies.forEach(a -> a.heal(1.0F));
                enemies.stream().filter(e -> e.distanceToSqr(zone.center) <= 9.0).forEach(e -> MartialSpells.root(e, 30));
            }).marker(Items.LANTERN),
            new Area("totem_ward", 0, 5.0, 15, 20, false, ParticleTypes.TOTEM_OF_UNDYING, SoundEvents.TOTEM_USE,
                (area, zone, allies, enemies) -> allies.forEach(a -> a.heal(1.5F))).marker(Items.TOTEM_OF_UNDYING),
            new Area("taiji_seal", 3, 5.0, 10, 20, false, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE, GroundSpells::taiji),
            new Area("caltrops", 2, 2.5, 20, 10, false, ParticleTypes.CRIT, SoundEvents.CHAIN_PLACE, (area, zone, allies, enemies) -> {
                for (LivingEntity enemy : enemies) {
                    if (enemy.onGround()) {
                        area.hurt(zone.caster, enemy, zone.power);
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), zone.caster);
                    }
                }
            }) {
                @Override
                Vec3 center(LivingEntity caster) {
                    return caster.position().subtract(caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(2.5));
                }
            },
            new RaijinDrums(), new Typhoon(), new NightParade(), new Wildfire());
    }
}
