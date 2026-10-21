package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Origin tab's Modified abilities of Bard, Herbalist, Beekeeper, Gravedigger (tools/origins/groups.json, group JobC):
 * songs that buff, lull or refresh allies, a herbal heal and two timed plant zones, a bee swarm, a honey patch and
 * stingers, and the gravedigger's shovel swing, undead stun and veil.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginJobC {
    private static final UUID SONG_DAMAGE_ID = UUID.fromString("7b3e9c21-4d6a-4f18-a2c5-9e1d0b6f3a47");
    private static final UUID SONG_SPEED_ID = UUID.fromString("a14f6d83-2c9b-4e75-8d30-5f7b1e2c9a06");
    private static final String SONG = "song_of_courage";
    private static final String LULLABY = "lullaby";
    private static final String THORN_PRICK = "thornbush_prick";
    private static final String SWARM = "swarm";
    private static final String SWARM_TARGET = "mdvlcraft_swarm_target";
    private static final String VEIL = "mourner_s_veil";
    private static final String VEIL_PROVOKED = "mourner_s_veil_provoked";
    /** Each creature's beneficial effects with the full duration they were last given (Encore restores it). */
    private static final String ENCORE_FULL = "mdvlcraft_encore_full";

    private OriginJobC() {
    }

    // ---- Bard ----

    /** Song of Courage: for 20 s, allies within 10 blocks get +10% attack damage and +10% attack speed. */
    public static class SongOfCourage extends ModifiedSpell {
        SongOfCourage() {
            super(SONG, 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (LivingEntity ally : Targets.allies(caster, caster.position(), 10.0)) {
                    inspire(server, ally, 400);
                    server.sendParticles(ParticleTypes.NOTE, ally.getX(), ally.getY() + ally.getBbHeight() + 0.4, ally.getZ(), 4, 0.4, 0.2, 0.4, 0.5);
                }
                GroundSpells.ring(server, caster.position(), 10.0, ParticleTypes.NOTE);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.PLAYERS, 2.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** +10% attack damage and attack speed on `ally` for `ticks` (transient modifiers, taken off when the last song runs out). */
    static void inspire(ServerLevel level, LivingEntity ally, int ticks) {
        AttributeInstance damage = ally.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = ally.getAttribute(Attributes.ATTACK_SPEED);
        boost(damage, SONG_DAMAGE_ID);
        boost(speed, SONG_SPEED_ID);
        Buffs.give(ally, SONG, ticks);
        Zones.start(level, ally.position(), ally, 0.0F, ticks, ticks + 1, zone -> {
        }, () -> {
            if (!Buffs.has(ally, SONG)) {
                if (damage != null) {
                    damage.removeModifier(SONG_DAMAGE_ID);
                }
                if (speed != null) {
                    speed.removeModifier(SONG_SPEED_ID);
                }
            }
        }, null);
    }

    /** +10% on `attribute` (if the creature has it), replacing a running song's. */
    private static void boost(AttributeInstance attribute, UUID id) {
        if (attribute != null) {
            attribute.removeModifier(id);
            attribute.addTransientModifier(new AttributeModifier(id, "mdvlcraft " + SONG, 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /**
     * Lullaby: enemies within 8 blocks fall asleep for 4 s or until hit: rooted, they can't attack or pick a target, and a
     * mob drops the one it had. Any damage wakes them (the root is lifted at once).
     */
    public static class Lullaby extends ModifiedSpell {
        Lullaby() {
            super(LULLABY, 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                List<LivingEntity> sleepers = Targets.enemies(caster, caster.position(), 8.0).stream().filter(e -> e != caster).toList();
                for (LivingEntity enemy : sleepers) {
                    Buffs.mark(enemy, LULLABY, caster, 80);
                    MartialSpells.root(enemy, 80);
                    if (enemy instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.getNavigation().stop();
                    }
                }
                Zones.start(server, caster.position(), caster, 0.0F, 80, 20, zone -> {
                    for (LivingEntity sleeper : sleepers) {
                        if (sleeper.isAlive() && Buffs.has(sleeper, LULLABY)) {
                            server.sendParticles(ParticleTypes.NOTE, sleeper.getX(), sleeper.getY() + sleeper.getBbHeight() + 0.3, sleeper.getZ(), 2, 0.3, 0.1, 0.3, 0.3);
                            server.sendParticles(ParticleTypes.CLOUD, sleeper.getX(), sleeper.getY() + sleeper.getBbHeight() + 0.2, sleeper.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
                        }
                    }
                }, () -> {
                }, null);
                GroundSpells.ring(server, caster.position(), 8.0, ParticleTypes.NOTE);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 2.0F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Wakes a sleeper: the mark goes and the root (Slowness VII and the jump block MartialSpells.root gives) is lifted. */
    static void wake(LivingEntity sleeper) {
        Buffs.clear(sleeper, LULLABY);
        MobEffectInstance slow = sleeper.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() == 6) {
            sleeper.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        MobEffectInstance jump = sleeper.getEffect(MobEffects.JUMP);
        if (jump != null && jump.getAmplifier() == 128) {
            sleeper.removeEffect(MobEffects.JUMP);
        }
    }

    /**
     * Encore: every beneficial effect on allies within 10 blocks goes back to the full duration it was last given (kept by
     * onEffectAdded; an effect given before the creature joined this session, or with no end, is left as it is).
     */
    public static class Encore extends ModifiedSpell {
        Encore() {
            super("encore", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 10.0)) {
                CompoundTag full = ally.getPersistentData().getCompound(ENCORE_FULL);
                for (MobEffectInstance effect : List.copyOf(ally.getActiveEffects())) {
                    String id = effectId(effect.getEffect());
                    if (effect.getEffect().isBeneficial() && !effect.isInfiniteDuration() && full.getInt(id) > effect.getDuration()) {
                        ally.addEffect(new MobEffectInstance(effect.getEffect(), full.getInt(id), effect.getAmplifier(), effect.isAmbient(),
                            effect.isVisible(), effect.showIcon()));
                    }
                }
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.NOTE, ally.getX(), ally.getY() + ally.getBbHeight() + 0.4, ally.getZ(), 5, 0.4, 0.2, 0.4, 0.5);
                }
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 10.0, ParticleTypes.END_ROD);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 2.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    static String effectId(MobEffect effect) {
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (id == null) {
            throw new IllegalStateException("Unregistered effect " + effect.getDescriptionId());
        }
        return id.toString();
    }

    // ---- Herbalist ----

    /** Poultice: heals the ally looked at within 8 blocks (or the caster, looking at none) for 8 and cures its bleeding and poison. */
    public static class Poultice extends ModifiedSpell {
        Poultice() {
            super("poultice", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity looked = Targets.looked(caster, 8.0F);
            LivingEntity patient = looked != null && Summons.allied(caster, looked) ? looked : caster;
            patient.heal(8.0F);
            patient.removeEffect(MobEffects.POISON);
            patient.removeEffect(BrandSpells.external("attributeslib:bleeding"));
            if (level instanceof ServerLevel server) {
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FERN)), patient.getX(), patient.getY(0.6),
                    patient.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
                server.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY(1.0) + 0.3, patient.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            }
            level.playSound(null, patient.blockPosition(), SoundEvents.MOSS_PLACE, SoundSource.PLAYERS, 1.2F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Thornbush: a ring of thorny bushes (particles, no blocks) 3 blocks around where the caster stands, for 10 s; an enemy
     * touching the ring (between 2 and 4 blocks from its centre) takes 2 damage, at most once a second, and Slowness II for 2 s.
     */
    public static class Thornbush extends ModifiedSpell {
        Thornbush() {
            super("thornbush", 2);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = caster.position();
                BlockParticleOption bush = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState());
                Zones.start(server, at, caster, this.getSpellPower(spellLevel, caster), 200, 5, zone -> {
                    for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI * 2 / 24;
                        server.sendParticles(bush, at.x + Math.cos(a) * 3.0, at.y + 0.4, at.z + Math.sin(a) * 3.0, 2, 0.15, 0.3, 0.15, 0.0);
                    }
                    for (LivingEntity enemy : Targets.enemies(caster, at, 4.0)) {
                        double d = enemy.position().subtract(at).horizontalDistance();
                        if (enemy != caster && d >= 2.0 && !Buffs.has(enemy, THORN_PRICK)) {
                            Buffs.give(enemy, THORN_PRICK, 20);
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), caster);
                        }
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Healing Garden: flowers bloom in a 4-block circle on the caster for 12 s; allies in it get Regeneration I. */
    static GroundSpells.Area healingGarden() {
        List<ItemParticleOption> flowers = List.of(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.POPPY)),
            new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.DANDELION)), new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CORNFLOWER)));
        return new GroundSpells.Area("healing_garden", 0, 4.0, 12, 20, false, ParticleTypes.HAPPY_VILLAGER, SoundEvents.FLOWERING_AZALEA_PLACE,
            (area, zone, allies, enemies) -> {
                GroundSpells.effect(allies, zone.caster, MobEffects.REGENERATION, 50, 0);
                for (ItemParticleOption flower : flowers) {
                    zone.level.sendParticles(flower, zone.center.x, zone.center.y + 0.2, zone.center.z, 8, 2.0, 0.05, 2.0, 0.0);
                }
            });
    }

    // ---- Beekeeper ----

    /**
     * Swarm: 4 bees for 15 s, angry at the creature looked at (refused without one) and keeping their stingers, so they
     * sting again and don't die of it. Once that creature is dead they only follow the caster.
     */
    public static class Swarm extends SummonSpell {
        Swarm() {
            super(SWARM, 2);
        }

        @Override
        protected int duration() {
            return 300;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!data.getPlayerRecasts().hasRecastForSpell(this) && enemyLooked(caster) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            LivingEntity target = enemyLooked(caster);
            if (target == null) {
                throw new IllegalStateException("Swarm cast with no enemy looked at");
            }
            this.spawn(level, caster, power, EntityType.BEE, 4, castData, bee -> {
                bee.getPersistentData().putUUID(SWARM_TARGET, target.getUUID());
                bee.setTarget(target);
            });
        }
    }

    static LivingEntity enemyLooked(LivingEntity caster) {
        LivingEntity looked = Targets.looked(caster, 24.0F);
        return looked != null && !Summons.allied(caster, looked) ? looked : null;
    }

    /** Honey Trap: thrown honey; where it lands, enemies in a 3-block patch for 8 s have Slowness IV and can't jump. */
    static BoltSpells.Bolt honeyTrap() {
        return new BoltSpells.Bolt("honey_trap", 0, Items.HONEY_BOTTLE, ParticleTypes.FALLING_HONEY, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Vec3 patch = level.clip(new ClipContext(at.add(0.0, 0.1, 0.0), at.subtract(0.0, 4.0, 0.0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, caster)).getLocation();
            BlockParticleOption honey = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HONEY_BLOCK.defaultBlockState());
            Zones.start(level, patch, caster, power, 160, 10, zone -> {
                GroundSpells.ring(level, patch, 3.0, ParticleTypes.LANDING_HONEY);
                level.sendParticles(honey, patch.x, patch.y + 0.1, patch.z, 15, 1.5, 0.02, 1.5, 0.0);
                for (LivingEntity enemy : Targets.enemies(caster, patch, 3.0)) {
                    if (enemy != caster) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3), caster);
                        enemy.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 128, false, false), caster);
                    }
                }
            }, () -> {
            }, null);
            level.playSound(null, patch.x, patch.y, patch.z, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
        }).speed(1.0F).gravity(0.04).sound(SoundEvents.SNOWBALL_THROW);
    }

    /** Stinger Shot: 3 stingers, each dealing 3 damage and Poison I for 4 s to what it hits. */
    static BoltSpells.Bolt stingerShot() {
        return new BoltSpells.Bolt("stinger_shot", 3, Items.ARROW, ParticleTypes.WAX_ON, (bolt, caster, target, power) -> {
            if (bolt.hurt(caster, target, power)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), caster);
            }
        }, null).speed(1.8F).scale(0.5F).spread(3).sound(SoundEvents.BEE_STING);
    }

    // ---- Gravedigger ----

    /**
     * Shovel Slam: needs a shovel; a wide swing (3 blocks, 75 degrees either side) dealing the caster's attack damage +40% to
     * every enemy in it, knocking each down for 1 s (Epic Fight knockdown and a 1 s root).
     */
    public static class ShovelSlam extends ConeSpells.Cone {
        ShovelSlam() {
            super("shovel_slam", 0, 3.0, 75.0, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, (caster, target, dealt) -> {
                MartialSpells.faceTowards(target, caster);   // Epic Fight's stun throws a creature back from where it faces
                MartialSpells.stun(target, StunType.KNOCKDOWN, 1.0F);
                MartialSpells.root(target, 20);
            });
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (MartialSpells.category(caster.getMainHandItem()) != WeaponCategories.SHOVEL) {
                MartialSpells.refuse(caster, "need_shovel");
                return false;
            }
            return true;
        }

        @Override
        float damage(LivingEntity caster, int spellLevel) {
            return MartialSpells.attack(caster) * 1.4F;
        }

        @Override
        void particles(ServerLevel level, LivingEntity caster) {
            Vec3 look = Wave2Assassin.flatLook(caster);
            for (int i = -5; i <= 5; i++) {
                Vec3 at = caster.position().add(look.yRot(i * 15.0F * (float)Math.PI / 180.0F).scale(2.0)).add(0.0, 1.0, 0.0);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), caster.getX(), caster.getY() + 0.2,
                caster.getZ(), 40, 1.5, 0.1, 1.5, 0.15);
        }
    }

    /** Rest in Peace: undead within 6 blocks are stunned for 3 s (Epic Fight stun and a root) and take 6 damage. */
    public static class RestInPeace extends ModifiedSpell {
        RestInPeace() {
            super("rest_in_peace", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float power = this.getSpellPower(spellLevel, caster);
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                if (enemy == caster || !BoltSpells.undead(enemy)) {
                    continue;
                }
                MartialSpells.faceTowards(enemy, caster);   // Epic Fight's stun throws a creature back from where it faces
                MartialSpells.stun(enemy, StunType.LONG, 3.0F);
                MartialSpells.root(enemy, 60);
                DamageSources.applyDamage(enemy, power, this.getDamageSource(caster));
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SOUL, enemy.getX(), enemy.getY(0.6), enemy.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
                }
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.END_ROD);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Mourner's Veil: for 15 s undead don't target the caster (those on it drop it), except one the caster has hurt since. */
    public static class MournersVeil extends ModifiedSpell {
        MournersVeil() {
            super(VEIL, 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, VEIL, 300);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(32.0), m -> m.getTarget() == caster && BoltSpells.undead(m))) {
                mob.setTarget(null);
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ASH, caster.getX(), caster.getY(0.6), caster.getZ(), 40, 0.4, 0.6, 0.4, 0.02);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new SongOfCourage(), new Lullaby(), new Encore(),
            new Poultice(), new Thornbush(), healingGarden(),
            new Swarm(), honeyTrap(), stingerShot(),
            new ShovelSlam(), new RestInPeace(), new MournersVeil());
    }

    // ---- event handlers ----

    /** Lullaby: a sleeper can't attack. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != event.getEntity() && Buffs.has(attacker, LULLABY)) {
            event.setCanceled(true);
        }
    }

    /** Lullaby: any damage wakes a sleeper. Mourner's Veil: an undead the veiled caster hurts may target it again. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (Buffs.has(victim, LULLABY)) {
            wake(victim);
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker && Buffs.has(attacker, VEIL) && BoltSpells.undead(victim)) {
            Buffs.mark(victim, VEIL_PROVOKED, attacker, Buffs.remaining(attacker, VEIL));
        }
    }

    /** Lullaby: a sleeper picks no target. Mourner's Veil: undead don't target the veiled caster unless it hurt them. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null) {
            return;
        }
        if (Buffs.has(mob, LULLABY)
            || Buffs.has(target, VEIL) && BoltSpells.undead(mob) && Buffs.markedBy(mob, VEIL_PROVOKED) != target) {
            event.setCanceled(true);
        }
    }

    /** Encore: remembers the full duration of each beneficial effect a creature is given. */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance added = event.getEffectInstance();
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !added.getEffect().isBeneficial() || added.isInfiniteDuration()) {
            return;
        }
        MobEffectInstance old = event.getOldEffectInstance();
        int full = Math.max(added.getDuration(), old != null && !old.isInfiniteDuration() ? old.getDuration() : 0);
        CompoundTag tag = entity.getPersistentData().getCompound(ENCORE_FULL);
        tag.putInt(effectId(added.getEffect()), full);
        entity.getPersistentData().put(ENCORE_FULL, tag);
    }

    /** Swarm: the bees stay angry at the creature they were sent at and keep their stingers. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Bee bee) || !SWARM.equals(Summons.kind(bee))) {
            return;
        }
        bee.goalSelector.addGoal(0, new SummonSpells.PulseGoal(bee, 5, mob -> {
            Bee b = (Bee)mob;
            if (b.hasStung()) {
                CompoundTag tag = new CompoundTag();
                b.addAdditionalSaveData(tag);
                tag.putBoolean("HasStung", false);
                b.readAdditionalSaveData(tag);
            }
            Entity sent = b.getPersistentData().hasUUID(SWARM_TARGET) ? level.getEntity(b.getPersistentData().getUUID(SWARM_TARGET)) : null;
            if (sent instanceof LivingEntity target && target.isAlive()) {
                b.setTarget(target);
                b.setPersistentAngerTarget(target.getUUID());
                b.setRemainingPersistentAngerTime(100);
            }
        }));
    }
}
