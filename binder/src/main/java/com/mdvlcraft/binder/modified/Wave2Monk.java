package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import com.mdvlcraft.binder.monk.Fists;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * The second-wave Modified abilities of the monk vocations (Agni Kai to Pilgrim) and of the warlock's druid, jade, spirit,
 * thief, stalker and wanderer vocations: palm strikes and kicks, timed katas kept with Buffs and judged on unarmed hits,
 * areas, shelters of temporary blocks, thrown pods, coins and traps, arrow stances and the lanterns of the Yokai Lord.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Monk {
    private static final UUID EPIC_FIGHT_LISTENER = UUID.fromString("c3e1a7f4-5b2d-4c8e-9f16-7d4a2b8e0c53");
    private static final UUID MASK_ID = UUID.fromString("e6b2d9a1-4f7c-4a35-8e0d-3c9f1b7a2d64");
    private static final String FLOWING = "flowing_counter";
    private static final String STATIC = "static_kata";
    private static final String MERCY = "mantra_of_mercy";
    private static final String SPLINTER = "mind_splinter";
    private static final String ROOTED = "rooted_trunk";
    private static final String SAMSARA = "samsara_turn";
    private static final String SWAY = "drunken_sway";
    private static final String ZAZEN = "zazen_aim";
    private static final String LONG_ROAD = "long_road";
    private static final String LONG_ROAD_READY = "long_road_ready";
    private static final String MASK = "mask_of_rule";
    private static final String COIN = "cursed_coin";
    private static final String USURY = "usury_pact";
    private static final String TETHER = "rift_tether";
    private static final String TEA = "monk_tea";
    private static final String ZAZEN_HOMING = "mdvlcraft_zazen_homing";
    private static final String ZAZEN_ARROW = "mdvlcraft_zazen_arrow";
    private static final String LOTUS_ARROW = "mdvlcraft_lotus_arrow";

    private Wave2Monk() {
    }

    // ---- shared helpers ----

    /** A bare-handed (or gloved, or fist-weapon) creature: Fists.fisted for players, an empty main hand for mobs. */
    static boolean fisted(LivingEntity entity) {
        return entity instanceof Player player ? Fists.fisted(player) : entity.getMainHandItem().isEmpty();
    }

    /** The 3x3 shell of blocks around `center` (floor, walls two high and roof), leaving the two blocks a creature stands in. */
    static List<BlockPos> shell(BlockPos center) {
        List<BlockPos> shell = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (dx != 0 || dz != 0 || dy == -1 || dy == 2) {
                        shell.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }
        return shell;
    }

    /** Moves the caster to the middle of its block, so a shell around it doesn't cut into it. */
    static BlockPos centre(LivingEntity caster) {
        BlockPos at = caster.blockPosition();
        caster.teleportTo(at.getX() + 0.5, caster.getY(), at.getZ() + 0.5);
        return at;
    }

    /** A strike on the creature looked at that needs empty hands, gloves or a fist weapon (players). */
    abstract static class PalmStrike extends SmiteSpells.Smite {
        PalmStrike(String key, int power, float reach) {
            super(key, power, reach);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (caster instanceof Player player && !Fists.fisted(player)) {
                MartialSpells.refuse(caster, "need_fists");
                return false;
            }
            return super.checkPreCastConditions(level, spellLevel, caster, data);
        }
    }

    // ---- Agni Kai ----

    /** Wheel of Agni: a spin-kick hitting every enemy within 4 blocks, knocking them 3 blocks back and setting them on fire for 4 s. */
    static ConeSpells.Cone wheelOfAgni() {
        return new ConeSpells.Cone("wheel_of_agni", 5, 4.0, 180.0, ParticleTypes.FLAME, SoundEvents.PLAYER_ATTACK_SWEEP, (caster, target, dealt) -> {
            Wave2Guardian.knock(caster.position(), target, 3.0);
            target.setSecondsOnFire(4);
        });
    }

    /** Sun Salutation: 6 s of a 4-block area on the caster; allies have Fire Resistance and Strength I, enemies are set on fire. */
    static GroundSpells.Area sunSalutation() {
        return new GroundSpells.Area("sun_salutation", 0, 4.0, 6, 20, false, ParticleTypes.FLAME, SoundEvents.BLAZE_SHOOT,
            (area, zone, allies, enemies) -> {
                int left = (int)(130 - zone.age());
                for (LivingEntity ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, left, 0, true, true), zone.caster);
                    ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, left, 0, true, true), zone.caster);
                }
                for (LivingEntity enemy : enemies) {
                    enemy.setSecondsOnFire(3);
                }
                zone.level.sendParticles(ParticleTypes.END_ROD, zone.center.x, zone.center.y + 2.5, zone.center.z, 20, 0.6, 0.6, 0.6, 0.02);
            });
    }

    // ---- Frost Palm ----

    /** Ice Cocoon: the caster is encased in ice for 4 s, healing 2 health each second while inside. */
    public static class IceCocoon extends ModifiedSpell {
        IceCocoon() {
            super("ice_cocoon", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos at = centre(caster);
                Wave2Guardian.raise(server, caster, shell(at), Blocks.ICE.defaultBlockState(), 80, 20, (zone, box) -> {
                    if (box.contains(caster.position())) {
                        caster.heal(2.0F);
                        server.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX(), caster.getY(0.6), caster.getZ(), 10, 0.3, 0.4, 0.3, 0.01);
                    }
                });
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Shatter Kick: kicks the creature looked at 6 blocks back; a frozen one shatters for double damage. */
    public static class ShatterKick extends SmiteSpells.Smite {
        ShatterKick() {
            super("shatter_kick", 7, 4.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            boolean frozen = target.isFullyFrozen() || BoltSpells.chilled(target);
            DamageSources.ignoreNextKnockback(target);
            this.hurt(caster, target, frozen ? power * 2.0F : power);
            Wave2Guardian.knock(caster.position(), target, 6.0);
            if (frozen) {
                target.setTicksFrozen(0);
                target.removeEffect(BrandSpells.external("irons_spellbooks:chilled"));
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), target.getX(), target.getY(0.5),
                    target.getZ(), 50, 0.4, 0.6, 0.4, 0.2);
                level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
    }

    // ---- Tide Monk ----

    /** Rain Dance: 6 s of a 5-block area on the caster; allies have Regeneration I and Dolphin's Grace, and fires in it go out. */
    static GroundSpells.Area rainDance() {
        return new GroundSpells.Area("rain_dance", 0, 5.0, 6, 20, false, ParticleTypes.SPLASH, SoundEvents.WEATHER_RAIN,
            (area, zone, allies, enemies) -> {
                int left = (int)(130 - zone.age());
                for (LivingEntity ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, left, 0, true, true), zone.caster);
                    ally.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, left, 0, true, true), zone.caster);
                    ally.clearFire();
                }
                for (LivingEntity enemy : enemies) {
                    enemy.clearFire();
                }
                BlockPos c = BlockPos.containing(zone.center);
                for (BlockPos pos : BlockPos.betweenClosed(c.offset(-5, -2, -5), c.offset(5, 3, 5))) {
                    if (pos.distSqr(c) <= 25.0 && zone.level.getBlockState(pos).is(BlockTags.FIRE)) {
                        zone.level.removeBlock(pos, false);
                    }
                }
                zone.level.sendParticles(ParticleTypes.FALLING_WATER, zone.center.x, zone.center.y + 4.0, zone.center.z, 120, 2.5, 0.3, 2.5, 0.0);
            });
    }

    // ---- Lightning Lotus ----

    /** Arc Palm: strikes the creature looked at; lightning arcs from it to up to 3 more enemies within 4 blocks. */
    public static class ArcPalm extends PalmStrike {
        ArcPalm() {
            super("arc_palm", 6, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            List<LivingEntity> arcs = Targets.enemies(caster, target.position(), 4.0).stream().filter(e -> e != target && e != caster)
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(target))).limit(3).toList();
            for (LivingEntity next : arcs) {
                SummonSpells.spark(level, target.getEyePosition(), next.getEyePosition());
                this.hurt(caster, next, power);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 30, 0.4, 0.5, 0.4, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 1.2F);
        }
    }

    // ---- Enlightened One ----

    /** Dawn Bell: clears harmful effects from allies within 6 blocks and makes enemies there glow for 10 s. */
    public static class DawnBell extends ModifiedSpell {
        DawnBell() {
            super("dawn_bell", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 6.0)) {
                for (MobEffectInstance effect : List.copyOf(ally.getActiveEffects())) {
                    if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                        ally.removeEffect(effect.getEffect());
                    }
                }
            }
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), caster);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.END_ROD);
                server.sendParticles(ParticleTypes.NOTE, caster.getX(), caster.getY() + 2.2, caster.getZ(), 8, 0.6, 0.3, 0.6, 0.5);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Mind Flayer ----

    /** Mind Lift: every enemy within 5 blocks levitates for 2 s, then is slammed down to take the fall. */
    public static class MindLift extends ModifiedSpell {
        MindLift() {
            super("mind_lift", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                List<LivingEntity> lifted = Targets.enemies(caster, caster.position(), 5.0).stream().filter(e -> e != caster).toList();
                for (LivingEntity enemy : lifted) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 3), caster);
                }
                Zones.start(server, caster.position(), caster, 0.0F, 41, 40, zone -> {
                    if (zone.age() == 0) {
                        return;
                    }
                    for (LivingEntity enemy : lifted) {
                        if (enemy.isAlive()) {
                            enemy.removeEffect(MobEffects.LEVITATION);
                            enemy.setDeltaMovement(0.0, -2.0, 0.0);
                            enemy.hurtMarked = true;
                            server.sendParticles(ParticleTypes.REVERSE_PORTAL, enemy.getX(), enemy.getY(0.5), enemy.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                        }
                    }
                    server.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.6F);
                }, () -> {
                }, null);
                GroundSpells.ring(server, caster.position(), 5.0, ParticleTypes.REVERSE_PORTAL);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Mind Splinter: strikes the creature looked at, with Nausea and Slowness; for 6 s it takes 25% more damage. */
    public static class MindSplinter extends SmiteSpells.Smite {
        MindSplinter() {
            super("mind_splinter", 5, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), caster);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0), caster);
            Buffs.mark(target, SPLINTER, caster, 120);
            level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY(1.0), target.getZ(), 30, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
    }

    // ---- Druidic Fist ----

    /** Vine Lash: lashes the creature looked at up to 8 blocks away, pulling it to the caster with Poison I for 5 s. */
    public static class VineLash extends SmiteSpells.Smite {
        VineLash() {
            super("vine_lash", 4, 8.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            DamageSources.ignoreNextKnockback(target);
            this.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), caster);
            Vec3 toward = caster.position().add(Wave2Assassin.flatLook(caster).scale(1.5)).subtract(target.position());
            target.setDeltaMovement(toward.scale(0.35).add(0.0, 0.2, 0.0));
            target.hurtMarked = true;
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(),
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState()));
            level.playSound(null, caster.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
    }

    // ---- Wukong ----

    /** Ruyi Toss: a spinning staff flying 10 blocks (a block a tick), hurting every enemy it passes and knocking it 3 blocks on. */
    public static class RuyiToss extends ModifiedSpell {
        RuyiToss() {
            super("ruyi_toss", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 look = caster.getLookAngle();
                Vec3 from = caster.getEyePosition().subtract(0.0, 0.4, 0.0);
                double reach = level.clip(new ClipContext(from, from.add(look.scale(10.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    caster)).getLocation().distanceTo(from);
                Set<LivingEntity> struck = new HashSet<>();
                ItemParticleOption staff = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.STICK));
                Zones.start(server, from, caster, this.getSpellPower(spellLevel, caster), 10, 1, zone -> {
                    double d = zone.age() + 1.0;
                    if (d > reach) {
                        zone.stop();
                        return;
                    }
                    Vec3 at = from.add(look.scale(d));
                    server.sendParticles(staff, at.x, at.y, at.z, 8, 0.5, 0.1, 0.5, 0.05);
                    server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 4, 0.3, 0.3, 0.3, 0.1);
                    for (LivingEntity enemy : Targets.enemies(caster, at.subtract(0.0, 0.8, 0.0), 1.6)) {
                        if (enemy != caster && struck.add(enemy)) {
                            DamageSources.ignoreNextKnockback(enemy);
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                            Wave2Guardian.knock(enemy.position().subtract(look.multiply(1.0, 0.0, 1.0)), enemy, 3.0);
                        }
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Cloud Somersault: a leap about 10 blocks forward, with Slow Falling for 5 s. */
    public static class CloudSomersault extends ModifiedSpell {
        CloudSomersault() {
            super("cloud_somersault", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.setDeltaMovement(Wave2Assassin.flatLook(caster).scale(0.95).add(0.0, 0.75, 0.0));
            caster.hurtMarked = true;
            caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.2, caster.getZ(), 40, 0.6, 0.2, 0.6, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Bodhisattva ----

    /** Ancestor's Mercy: 5 s of a 5-block area on the caster; each second allies heal 1 health and enemies soulburn. */
    static GroundSpells.Area ancestorsMercy() {
        return new GroundSpells.Area("ancestors_mercy", 0, 5.0, 5, 20, false, ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE,
            (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    ally.heal(1.0F);
                }
                for (LivingEntity enemy : enemies) {
                    BoltSpells.soulburn(zone.caster, enemy, 40);
                }
            });
    }

    // ---- Drunken Master ----

    /** Sleight Palm: strikes the creature looked at and steals one of its active effects. */
    public static class SleightPalm extends PalmStrike {
        SleightPalm() {
            super("sleight_palm", 5, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            List<MobEffectInstance> effects = List.copyOf(target.getActiveEffects());
            if (!effects.isEmpty()) {
                MobEffectInstance stolen = effects.get(caster.getRandom().nextInt(effects.size()));
                target.removeEffect(stolen.getEffect());
                caster.addEffect(new MobEffectInstance(stolen));
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, caster.getX(), caster.getY(0.6), caster.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
                level.playSound(null, caster.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.1F);
        }
    }

    // ---- Zen Archer ----

    /** Lotus Arrows: 6 arrows fired in a ring around the caster, each giving what it hits Slowness II for 4 s. */
    public static class LotusArrows extends ModifiedSpell {
        LotusArrows() {
            super("lotus_arrows", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float power = this.getSpellPower(spellLevel, caster);
            Vec3 look = Wave2Assassin.flatLook(caster);
            for (int i = 0; i < 6; i++) {
                Vec3 dir = look.yRot(i * 60.0F * ((float)Math.PI / 180.0F));
                Arrow arrow = new Arrow(level, caster);
                Vec3 from = caster.getEyePosition().subtract(0.0, 0.3, 0.0).add(dir.scale(0.8));
                arrow.setPos(from.x, from.y, from.z);
                arrow.setBaseDamage(power / 2.5);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                arrow.getPersistentData().putBoolean(LOTUS_ARROW, true);
                arrow.shoot(dir.x, 0.05, dir.z, 2.5F, 0.0F);
                level.addFreshEntity(arrow);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 1.5, ParticleTypes.CHERRY_LEAVES);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Pilgrim ----

    /** Mountain Tea: a flask of tea that, drunk, gives Resistance I and Jump Boost II for 60 s. */
    static BoonSpells.Food mountainTea() {
        return new BoonSpells.Food("mountain_tea", Items.HONEY_BOTTLE, TEA, 1, false);
    }

    // ---- Blight Druid ----

    /** Withering Spores: a thrown spore pod; where it lands a 4-block cloud for 5 s gives enemies Wither I and Poison I. */
    static BoltSpells.Bolt witheringSpores() {
        return new BoltSpells.Bolt("withering_spores", 0, Items.SPORE_BLOSSOM, ParticleTypes.SPORE_BLOSSOM_AIR, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Zones.start(level, at, caster, power, 100, 20, zone -> {
                GroundSpells.ring(level, at, 4.0, ParticleTypes.SPORE_BLOSSOM_AIR);
                level.sendParticles(ParticleTypes.MYCELIUM, at.x, at.y + 0.8, at.z, 60, 2.0, 0.6, 2.0, 0.0);
                for (LivingEntity enemy : Targets.enemies(caster, at, 4.0)) {
                    if (enemy != caster) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), caster);
                        enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), caster);
                    }
                }
            }, () -> {
            }, null);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.MOSS_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
        }).speed(1.0F).gravity(0.04).sound(SoundEvents.SNOWBALL_THROW);
    }

    /** Rotroot Grasp: roots in a 6-block cone give enemies Slowness V for 2 s and Weakness I for 6 s. */
    public static class RotrootGrasp extends ModifiedSpell {
        RotrootGrasp() {
            super("rotroot_grasp", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Wave2Guardian.cone(caster, 6.0, 40.0)) {
                enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4), caster);
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), caster);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), enemy.getX(),
                        enemy.getY() + 0.2, enemy.getZ(), 25, 0.3, 0.3, 0.3, 0.1);
                }
            }
            if (level instanceof ServerLevel server) {
                Wave2Brute.coneParticles(server, caster, 6.0, ParticleTypes.MYCELIUM);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Yokai Lord ----

    /** Mask of Rule: strikes the creature looked at, shattering 30% of its armour for 8 s; the caster's summons deal 30% more to it. */
    public static class MaskOfRule extends SmiteSpells.Smite {
        MaskOfRule() {
            super("mask_of_rule", 6, 8.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            Buffs.mark(target, MASK, caster, 160);
            AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.removeModifier(MASK_ID);
                armor.addTransientModifier(new AttributeModifier(MASK_ID, "mdvlcraft mask of rule", -0.3, AttributeModifier.Operation.MULTIPLY_TOTAL));
                Zones.start(level, target.position(), target, 0.0F, 160, 161, zone -> {
                }, () -> {
                    if (!Buffs.has(target, MASK)) {
                        armor.removeModifier(MASK_ID);
                    }
                }, null);
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.4, 0.3, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    /** Chochin Swarm: 3 paper-lantern vexes for 20 s; each bursts for 3 damage within 2 blocks when it dies. */
    public static class ChochinSwarm extends SummonSpell {
        ChochinSwarm() {
            super("chochin_swarm", 3);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 3, castData, vex -> UndeadSummons.equip(vex, EquipmentSlot.MAINHAND,
                new ItemStack(Items.LANTERN)));
        }
    }

    // ---- Netherseer ----

    /** Soulflame Rift: a 4-block rift where the caster looks for 5 s, pulling enemies toward its centre and soulburning them. */
    static GroundSpells.Area soulflameRift() {
        return new GroundSpells.Area("soulflame_rift", 0, 4.0, 5, 10, true, ParticleTypes.SOUL_FIRE_FLAME, SoundEvents.PORTAL_TRIGGER,
            (area, zone, allies, enemies) -> {
                for (LivingEntity enemy : enemies) {
                    if (enemy == zone.caster) {
                        continue;
                    }
                    Vec3 in = zone.center.subtract(enemy.position()).multiply(1.0, 0.0, 1.0);
                    if (in.lengthSqr() > 0.25) {
                        enemy.setDeltaMovement(in.normalize().scale(0.3).add(0.0, 0.05, 0.0));
                        enemy.hurtMarked = true;
                    }
                    BoltSpells.soulburn(zone.caster, enemy, 60);
                }
                zone.level.sendParticles(ParticleTypes.REVERSE_PORTAL, zone.center.x, zone.center.y + 1.0, zone.center.z, 30, 0.4, 0.8, 0.4, 0.2);
            });
    }

    /** Abyssal Gaze: strikes the creature looked at with Wither II for 6 s and Blindness and Darkness for 4 s. */
    public static class AbyssalGaze extends SmiteSpells.Smite {
        AbyssalGaze() {
            super("abyssal_gaze", 5, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1), caster);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), caster);
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), caster);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.SCULK_SOUL);
            level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(1.0), target.getZ(), 30, 0.3, 0.5, 0.3, 0.05);
            level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_NEARBY_CLOSER, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    // ---- Trickster ----

    /** Cursed Coin: a thrown coin; the creature hit gets Slowness II and takes 20% more damage for 8 s. */
    static BoltSpells.Bolt cursedCoin() {
        return new BoltSpells.Bolt("cursed_coin", 3, Items.GOLD_NUGGET, ParticleTypes.WITCH, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1), caster);
            Buffs.mark(target, COIN, caster, 160);
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY(1.0), target.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
            }
        }, null).speed(1.8F).gravity(0.02).sound(SoundEvents.ARMOR_EQUIP_GOLD);
    }

    // ---- Void Archer ----

    /** Gravity Snare: a thrown void trap; the first enemy to step on it (within 60 s) levitates for 3 s and takes 4 damage. */
    static BoltSpells.Bolt gravitySnare() {
        return new BoltSpells.Bolt("gravity_snare", 4, Items.ENDER_EYE, ParticleTypes.REVERSE_PORTAL, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Vec3 trap = level.clip(new ClipContext(at.add(0.0, 0.1, 0.0), at.subtract(0.0, 4.0, 0.0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster)).getLocation();
            Zones.start(level, trap, caster, power, 1200, 4, zone -> {
                if (zone.age() % 20 == 0) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, trap.x, trap.y + 0.2, trap.z, 10, 0.4, 0.05, 0.4, 0.0);
                }
                Targets.enemies(caster, trap, 1.5).stream().filter(e -> e != caster).findFirst().ifPresent(victim -> {
                    bolt.hurt(caster, victim, zone.power);
                    victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1), caster);
                    level.sendParticles(ParticleTypes.PORTAL, victim.getX(), victim.getY(0.5), victim.getZ(), 60, 0.4, 0.8, 0.4, 0.5);
                    level.playSound(null, victim.blockPosition(), SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 1.2F, 0.6F);
                    zone.stop();
                });
            }, () -> {
            }, null);
            level.playSound(null, trap.x, trap.y, trap.z, SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0F, 0.6F);
        }).speed(1.0F).gravity(0.04).sound(SoundEvents.ENDER_EYE_LAUNCH);
    }

    // ---- Riftwalker ----

    /** Void Burrow: a 3x3 obsidian shelter around the caster that vanishes after 8 s. */
    public static class VoidBurrow extends ModifiedSpell {
        VoidBurrow() {
            super("void_burrow", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos at = centre(caster);
                Wave2Guardian.raise(server, caster, shell(at), Blocks.OBSIDIAN.defaultBlockState(), 160, 160, (zone, box) -> {
                });
                server.sendParticles(ParticleTypes.PORTAL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 60, 1.0, 1.0, 1.0, 0.3);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Rift Hop: an 8-block dash through creatures; each one passed takes 4 damage and is blinded for 2 s. */
    public static class RiftHop extends ModifiedSpell {
        RiftHop() {
            super("rift_hop", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float power = this.getSpellPower(spellLevel, caster);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.PORTAL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
            }
            for (LivingEntity target : MartialSpells.dash(caster, 8.0)) {
                DamageSources.applyDamage(target, power, this.getDamageSource(caster));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), caster);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY(0.6), target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.3F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            wheelOfAgni(), sunSalutation(), new IceCocoon(), new ShatterKick(),
            // Flowing Counter: 8 s; after each dodge the next unarmed hit deals 6 more and knocks back with a water burst
            new Wave2Guardian.Stance(FLOWING, 160, ParticleTypes.SPLASH, SoundEvents.PLAYER_SPLASH),
            rainDance(), new ArcPalm(),
            // Static Kata: 10 s; every fifth unarmed hit stuns for 2 s
            new Wave2Guardian.Stance(STATIC, 200, ParticleTypes.ELECTRIC_SPARK, SoundEvents.BEEHIVE_WORK),
            new DawnBell(),
            // Mantra of Mercy: 12 s; each unarmed hit heals the lowest-health ally within 8 blocks 2 health
            new Wave2Guardian.Stance(MERCY, 240, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE),
            new MindLift(), new MindSplinter(),
            // Rooted Trunk: 6 s of no knockback; attackers are poisoned for 3 s
            new Wave2Guardian.Stance(ROOTED, 120, ParticleTypes.HAPPY_VILLAGER, SoundEvents.ROOTED_DIRT_PLACE),
            new VineLash(), new RuyiToss(), new CloudSomersault(), ancestorsMercy(),
            // Samsara Turn: 10 s; each kill refills dodge stamina and gives Resistance I for 5 s
            new Wave2Guardian.Stance(SAMSARA, 200, ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE),
            // Drunken Sway: 8 s; each hit has a 30% chance to miss as the caster staggers 2 blocks aside
            new Wave2Guardian.Stance(SWAY, 160, ParticleTypes.CLOUD, SoundEvents.GENERIC_DRINK),
            new SleightPalm(),
            // Zazen Aim: 10 s; arrows loosed standing still deal 50% more and home in
            new Wave2Guardian.Stance(ZAZEN, 200, ParticleTypes.ENCHANT, SoundEvents.AMETHYST_BLOCK_CHIME),
            new LotusArrows(), mountainTea(),
            // Long Road: 20 s; sprinting builds Speed up to III, and the first hit after sprinting stuns for 1 s
            new Wave2Guardian.Stance(LONG_ROAD, 400, ParticleTypes.CLOUD, SoundEvents.ARMOR_EQUIP_LEATHER),
            witheringSpores(), new RotrootGrasp(), new MaskOfRule(), new ChochinSwarm(), soulflameRift(), new AbyssalGaze(), cursedCoin(),
            // Usury Pact: 10 s; each kill drops an extra emerald and refunds 20% of the caster's mana
            new Wave2Guardian.Stance(USURY, 200, ParticleTypes.HAPPY_VILLAGER, SoundEvents.VILLAGER_YES),
            gravitySnare(),
            // Rift Tether: 8 s; arrows teleport the creature hit 4 blocks toward the archer
            new Wave2Guardian.Stance(TETHER, 160, ParticleTypes.REVERSE_PORTAL, SoundEvents.ENDERMAN_AMBIENT),
            new VoidBurrow(), new RiftHop());
    }

    // ---- event handlers ----

    /** Flowing Counter readies its strike on each dodge; Samsara Turn refills stamina on a kill (see onDeath). */
    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
        }
        patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, EPIC_FIGHT_LISTENER, consume -> {
            if (consume.getSkill() instanceof DodgeSkill && Buffs.has(player, FLOWING) && Buffs.count(player, FLOWING) == 0) {
                Buffs.add(player, FLOWING, 1);
            }
        });
    }

    /** Zazen Aim: an arrow loosed standing still is marked to deal 50% more and steers toward the nearest enemy ahead. */
    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof AbstractArrow arrow)
            || !(arrow.getOwner() instanceof LivingEntity owner) || !Buffs.has(owner, ZAZEN) || arrow.getPersistentData().getBoolean(ZAZEN_ARROW)
            || new Vec3(owner.getX() - owner.xo, 0.0, owner.getZ() - owner.zo).lengthSqr() > 1.0E-4) {
            return;
        }
        arrow.getPersistentData().putBoolean(ZAZEN_ARROW, true);
        arrow.getPersistentData().putBoolean(ZAZEN_HOMING, true);
        Zones.start(level, arrow.position(), owner, 0.0F, 100, 1, zone -> {
            if (arrow.isRemoved() || !arrow.getPersistentData().getBoolean(ZAZEN_HOMING)) {
                zone.stop();
                return;
            }
            Vec3 motion = arrow.getDeltaMovement();
            double speed = motion.length();
            if (speed < 0.1) {
                return;
            }
            Vec3 heading = motion.scale(1.0 / speed);
            Targets.enemies(owner, arrow.position(), 12.0).stream()
                .filter(e -> e != owner && e.getBoundingBox().getCenter().subtract(arrow.position()).normalize().dot(heading) > 0.5)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(arrow))).ifPresent(target -> {
                    Vec3 to = target.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
                    arrow.setDeltaMovement(heading.scale(0.6).add(to.scale(0.4)).normalize().scale(speed));
                    arrow.hurtMarked = true;
                    level.sendParticles(ParticleTypes.ENCHANT, arrow.getX(), arrow.getY(), arrow.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
                });
        }, () -> {
        }, null);
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            arrow.getPersistentData().remove(ZAZEN_HOMING);
        }
    }

    /** Drunken Sway: 30% of the hits on the swaying caster miss as it staggers 2 blocks aside. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !Buffs.has(victim, SWAY) || !(event.getSource().getEntity() instanceof LivingEntity attacker)
            || attacker == victim || victim.getRandom().nextFloat() >= 0.3F) {
            return;
        }
        event.setCanceled(true);
        Vec3 away = victim.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            away = Wave2Assassin.flatLook(victim);
        }
        Vec3 side = new Vec3(-away.z, 0.0, away.x).normalize().scale(victim.getRandom().nextBoolean() ? 2.0 : -2.0);
        for (Vec3 step : List.of(side, side.scale(-1.0))) {
            if (level.noCollision(victim, victim.getBoundingBox().move(step))) {
                level.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY(0.5), victim.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                victim.teleportTo(victim.getX() + step.x, victim.getY(), victim.getZ() + step.z);
                break;
            }
        }
        level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Rooted Trunk: no knockback. */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (Buffs.has(event.getEntity(), ROOTED)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        Entity source = event.getSource().getEntity();

        // Mind Splinter and Cursed Coin: the marked creature takes 25% / 20% more
        if (Buffs.has(victim, SPLINTER)) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        if (Buffs.has(victim, COIN)) {
            event.setAmount(event.getAmount() * 1.2F);
        }
        // Mask of Rule: the marker's summons deal 30% more to it
        LivingEntity ruler = Buffs.markedBy(victim, MASK);
        if (ruler != null && source instanceof Mob summon && Summons.owner(summon) == ruler) {
            event.setAmount(event.getAmount() * 1.3F);
        }
        // Rooted Trunk: attackers are poisoned for 3 s
        if (source instanceof LivingEntity attacker && attacker != victim && Buffs.has(victim, ROOTED)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), victim);
        }

        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity archer) {
            arrowHit(event, arrow, archer, victim);
        }

        LivingEntity attacker = Wave2Samurai.meleeAttacker(event.getSource());
        if (attacker == null || attacker == victim) {
            return;
        }
        // Long Road: the first hit after sprinting stuns for 1 s
        if (Buffs.has(attacker, LONG_ROAD_READY)) {
            Buffs.clear(attacker, LONG_ROAD_READY);
            MartialSpells.stun(victim, StunType.LONG, 1.0F);
            MartialSpells.root(victim, 20);
        }
        if (!fisted(attacker)) {
            return;
        }
        // Flowing Counter: the readied strike deals 6 more and knocks back with a water burst
        if (Buffs.count(attacker, FLOWING) > 0) {
            Buffs.add(attacker, FLOWING, -1);
            event.setAmount(event.getAmount() + 6.0F);
            Wave2Guardian.knock(attacker.position(), victim, 3.0);
            level.sendParticles(ParticleTypes.SPLASH, victim.getX(), victim.getY(0.6), victim.getZ(), 60, 0.5, 0.5, 0.5, 0.3);
            level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        // Static Kata: every fifth unarmed hit stuns for 2 s
        if (Buffs.has(attacker, STATIC) && Buffs.add(attacker, STATIC, 1) % 5 == 0) {
            MartialSpells.stun(victim, StunType.LONG, 2.0F);
            MartialSpells.root(victim, 40);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.6), victim.getZ(), 40, 0.4, 0.6, 0.4, 0.3);
            level.playSound(null, victim.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
        }
        // Mantra of Mercy: each unarmed hit heals the lowest-health ally within 8 blocks 2 health
        if (Buffs.has(attacker, MERCY)) {
            Targets.allies(attacker, attacker.position(), 8.0).stream()
                .min(Comparator.comparingDouble(e -> e.getHealth() / e.getMaxHealth())).ifPresent(ally -> {
                    ally.heal(2.0F);
                    level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0), ally.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
                });
        }
    }

    /** Zazen Aim, Lotus Arrows and Rift Tether: what an arrow does to the creature it hits. */
    private static void arrowHit(LivingHurtEvent event, AbstractArrow arrow, LivingEntity archer, LivingEntity victim) {
        if (arrow.getPersistentData().getBoolean(ZAZEN_ARROW)) {
            event.setAmount(event.getAmount() * 1.5F);
        }
        if (arrow.getPersistentData().getBoolean(LOTUS_ARROW)) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), archer);
        }
        if (Buffs.has(archer, TETHER) && victim != archer && victim.level() instanceof ServerLevel level) {
            Vec3 toward = archer.position().subtract(victim.position()).multiply(1.0, 0.0, 1.0);
            double step = Math.min(4.0, toward.length() - 1.5);
            if (step > 0.0) {
                Vec3 move = toward.normalize().scale(step);
                if (level.noCollision(victim, victim.getBoundingBox().move(move))) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, victim.getX(), victim.getY(0.5), victim.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                    victim.teleportTo(victim.getX() + move.x, victim.getY(), victim.getZ() + move.z);
                    level.playSound(null, victim.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.4F);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // Chochin Swarm: a lantern bursts for 3 within 2 blocks
        if (victim instanceof Mob lantern && "chochin_swarm".equals(Summons.kind(lantern)) && Summons.owner(lantern) instanceof LivingEntity owner) {
            for (LivingEntity near : Targets.enemies(owner, victim.position(), 2.0)) {
                if (near != victim && near != owner) {
                    Wave2Samurai.hurt("chochin_swarm", owner, near, 3.0F);
                }
            }
            level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY(0.5), victim.getZ(), 40, 0.8, 0.8, 0.8, 0.05);
            level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY(0.5), victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, victim.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity killer) || killer == victim) {
            return;
        }
        // Samsara Turn: a kill refills dodge stamina and gives Resistance I for 5 s
        if (Buffs.has(killer, SAMSARA)) {
            killer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            if (killer instanceof ServerPlayer player) {
                ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
                if (patch != null) {
                    patch.setStamina(patch.getMaxStamina());
                }
            }
            level.sendParticles(ParticleTypes.SOUL, killer.getX(), killer.getY(1.0), killer.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
        }
        // Usury Pact: a kill drops an extra emerald and refunds 20% of the killer's mana
        if (Buffs.has(killer, USURY)) {
            victim.spawnAtLocation(new ItemStack(Items.EMERALD));
            if (killer instanceof ServerPlayer player) {
                MagicData magic = MagicData.getPlayerMagicData(player);
                float max = (float)player.getAttributeValue((Attribute)AttributeRegistry.MAX_MANA.get());
                magic.setMana(Math.min(max, magic.getMana() + max * 0.2F));
            }
        }
    }

    /** Long Road: sprinting builds Speed (a level every 3 s, up to III) and readies a stunning hit for 2 s after. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != Phase.END || player.level().isClientSide() || !Buffs.has(player, LONG_ROAD)) {
            return;
        }
        if (player.isSprinting()) {
            int ticks = Buffs.add(player, LONG_ROAD, 1);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, Math.min(2, ticks / 60), false, true));
            Buffs.give(player, LONG_ROAD_READY, 40);
        } else if (Buffs.count(player, LONG_ROAD) > 0) {
            Buffs.add(player, LONG_ROAD, -Buffs.count(player, LONG_ROAD));
        }
    }

    /** Mountain Tea: drunk, Resistance I and Jump Boost II for 60 s. */
    @SubscribeEvent
    public static void onDrunk(LivingEntityUseItemEvent.Finish event) {
        if (TEA.equals(BoonSpells.kindOf(event.getItem()))) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.JUMP, 1200, 1));
        }
    }
}
