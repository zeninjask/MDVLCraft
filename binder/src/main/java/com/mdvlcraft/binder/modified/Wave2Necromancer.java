package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.BoltSpells.Bolt;
import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The second wave's necromancer Modified abilities: bone walls and cages, death marks that pay off when the marked creature
 * dies, kill-raising auras, the dead as summons (whose hits do something extra) and grave foods.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Necromancer {
    private Wave2Necromancer() {
    }

    static AbstractSpell spell(String key) {
        return SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("mdvlcraft", key));
    }

    /** Places `state` at `where` for `ticks`, running `pulse` every `interval` ticks and `after` once the blocks are gone. */
    static List<BlockPos> raise(ServerLevel level, LivingEntity caster, float power, List<BlockPos> where, BlockState state, int ticks, int interval,
                                java.util.function.Consumer<List<BlockPos>> pulse, Runnable after) {
        List<BlockPos> placed = WallSpells.place(level, where, state);
        Vec3 center = placed.isEmpty() ? caster.position() : Vec3.atCenterOf(placed.get(placed.size() / 2));
        Zones.start(level, center, caster, power, ticks, interval, zone -> pulse.accept(placed), () -> {
            WallSpells.clear(level, placed, state);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y, center.z, 60, 1.5, 1.0, 1.5, 0.1);
            after.run();
        }, null);
        return placed;
    }

    /** The enemies within `radius` of `center` touching any of the blocks `placed`. */
    static List<LivingEntity> touching(LivingEntity caster, Vec3 center, double radius, List<BlockPos> placed) {
        return Targets.enemies(caster, center, radius).stream()
            .filter(e -> placed.stream().anyMatch(p -> e.getBoundingBox().inflate(0.3).intersects(new AABB(p)))).toList();
    }

    /** Raises `count` allied mobs of `type` at `at` for `ticks`, as the summon kind `kind`. */
    static <T extends Mob> void raiseDead(ServerLevel level, LivingEntity owner, EntityType<T> type, int count, int ticks, String kind, Vec3 at,
                                          java.util.function.Consumer<T> setup) {
        Summons.summon(level, owner, type, count, ticks, kind, new SummonedEntitiesCastData(), mob -> {
            mob.moveTo(at.x + (mob.getRandom().nextDouble() - 0.5), at.y, at.z + (mob.getRandom().nextDouble() - 0.5), owner.getYRot(), 0.0F);
            setup.accept(mob);
        });
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 20 * count, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, BlockPos.containing(at), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    static void skeleton(Mob skeleton) {
        UndeadSummons.equip(skeleton, EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        UndeadSummons.cap(skeleton, 0xE8E0C8);
        if (skeleton instanceof net.minecraft.world.entity.monster.AbstractSkeleton archer) {
            archer.reassessWeaponGoal();
        }
    }

    /** A spell that marks the creature looked at for `ticks`; what happens when it dies is in onDeath. */
    static class DeathMark extends SmiteSpells.Smite {
        private final int ticks;

        DeathMark(String key, int power, int ticks) {
            super(key, power, 20.0F);
            this.ticks = ticks;
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Buffs.mark(target, this.config.key(), caster, this.ticks);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, this.ticks, 0, false, false), caster);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.SOUL);
            level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(1.0), target.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
            level.playSound(null, target.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
    }

    /** A spell giving the caster the state `key` (Buffs) for `ticks`, plus `extra`. */
    static class Aura extends ModifiedSpell {
        private final int ticks;

        Aura(String key, int ticks) {
            super(key, 0);
            this.ticks = ticks;
        }

        void extra(ServerLevel level, LivingEntity caster) {
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Buffs.give(caster, this.config.key(), this.ticks);
                this.extra(server, caster);
                server.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY(1.0), caster.getZ(), 30, 0.5, 0.6, 0.5, 0.03);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.6F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Ossuary Wall: a 5-wide bone block wall for 10 s; when it crumbles it releases 3 allied skeletons (30 s). */
    public static class OssuaryWall extends ModifiedSpell {
        OssuaryWall() {
            super("ossuary_wall", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                List<BlockPos> wall = WallSpells.wall(caster);
                Vec3 base = Vec3.atBottomCenterOf(wall.get(wall.size() / 2)).subtract(0.0, 1.0, 0.0);
                raise(server, caster, 0.0F, wall, Blocks.BONE_BLOCK.defaultBlockState(), 200, 1000, placed -> {
                }, () -> raiseDead(server, caster, EntityType.SKELETON, 3, 600, "ossuary_wall", base, Wave2Necromancer::skeleton));
                level.playSound(null, caster.blockPosition(), SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 1.5F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Offal Feast: one raw offal; eaten, it heals 6 hearts and grants Strength I for 20 s. */
    public static class OffalFeast extends BoonSpells.Food {
        OffalFeast() {
            super("offal_feast", Items.BEEF, "necromancer_offal", 1, false);
        }
    }

    /** Grave Fruit: three grave fruits; each restores 4 hunger, grants Regeneration II for 6 s and cures poison. */
    public static class GraveFruit extends BoonSpells.Food {
        GraveFruit() {
            super("grave_fruit", Items.GLOW_BERRIES, "necromancer_grave_fruit", 3, false);
        }
    }

    /** Ghoul's Jerky: three jerky; each restores 6 hunger, grants Strength I for 20 s and removes Hunger. */
    public static class GhoulsJerky extends BoonSpells.Food {
        GhoulsJerky() {
            super("ghouls_jerky", Items.ROTTEN_FLESH, "necromancer_jerky", 3, false);
        }
    }

    /** Tops the eater's hunger up to `hunger` points for the food (the item's own nutrition counts). */
    static void feed(LivingEntity eater, ItemStack stack, int hunger) {
        if (eater instanceof Player player) {
            FoodProperties food = stack.getFoodProperties(player);
            int own = food == null ? 0 : food.getNutrition();
            if (hunger > own) {
                player.getFoodData().eat(hunger - own, 0.3F);
            }
        }
    }

    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity eater = event.getEntity();
        switch (BoonSpells.kindOf(event.getItem())) {
            case "necromancer_offal" -> {
                eater.heal(12.0F);
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
            }
            case "necromancer_grave_fruit" -> {
                feed(eater, event.getItem(), 4);
                eater.removeEffect(MobEffects.POISON);
                eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            }
            case "necromancer_jerky" -> {
                feed(eater, event.getItem(), 6);
                eater.removeEffect(MobEffects.HUNGER);
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
            }
            default -> {
            }
        }
    }

    /** Blood Tithe: pay 4 health for Strength II and 20% extra life steal on melee hits for 10 s; refused at 4 health or less. */
    public static class BloodTithe extends Aura {
        BloodTithe() {
            super("blood_tithe", 200);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (caster.getHealth() <= 4.0F) {
                MartialSpells.refuse(caster, "too_weak");
                return false;
            }
            return true;
        }

        @Override
        void extra(ServerLevel level, LivingEntity caster) {
            caster.setHealth(caster.getHealth() - 4.0F);
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getY(1.0), caster.getZ(), 10, 0.4, 0.4, 0.4, 0.1);
        }
    }

    /** Corpse Hop: three hops a second apart; each landing deals the spell's power and Weakness (5 s) to creatures within 2 blocks. */
    public static class CorpseHop extends ModifiedSpell {
        CorpseHop() {
            super("corpse_hop", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                long[] hopped = {-1};
                Zones.start(server, caster.position(), caster, this.getSpellPower(spellLevel, caster), 75, 1, zone -> {
                    long age = zone.age();
                    if (hopped[0] >= 0 && age - hopped[0] > 3 && caster.onGround()) {
                        hopped[0] = -1;
                        for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 2.0)) {
                            if (DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster))) {
                                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), caster);
                            }
                        }
                        server.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY() + 0.1, caster.getZ(), 20, 1.0, 0.1, 1.0, 0.02);
                        server.playSound(null, caster.blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.PLAYERS, 0.6F, 1.2F);
                    }
                    if (age < 60 && age % 20 == 0) {
                        Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                        caster.setDeltaMovement(look.scale(0.35).add(0.0, 0.6, 0.0));
                        caster.hurtMarked = true;
                        hopped[0] = age;
                    }
                }, () -> {
                }, null);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Cinder Palisade: a 5-wide wall of magma blocks for 6 s; enemies touching it take the spell's power and burn. */
    public static class CinderPalisade extends ModifiedSpell {
        CinderPalisade() {
            super("cinder_palisade", 2);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                List<BlockPos> wall = WallSpells.wall(caster);
                Vec3 center = Vec3.atCenterOf(wall.get(wall.size() / 2));
                raise(server, caster, power, wall, Blocks.MAGMA_BLOCK.defaultBlockState(), 120, 10, placed -> {
                    for (LivingEntity enemy : touching(caster, center, 5.0, placed)) {
                        DamageSources.applyDamage(enemy, power, this.getDamageSource(caster));
                        enemy.setSecondsOnFire(3);
                    }
                    for (BlockPos pos : placed) {
                        server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.4, 0.4, 0.4, 0.01);
                    }
                }, () -> {
                });
                level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Bone Lattice: a ring of bone blocks (radius 3, 2 high) around the caster for 6 s; enemies touching it get Wither I for 4 s. */
    public static class BoneLattice extends ModifiedSpell {
        BoneLattice() {
            super("bone_lattice", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos center = caster.blockPosition();
                List<BlockPos> ring = new ArrayList<>();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 1, 3))) {
                    double d = Math.sqrt(Math.pow(pos.getX() - center.getX(), 2) + Math.pow(pos.getZ() - center.getZ(), 2));
                    if (d >= 2.5 && d < 3.5) {
                        ring.add(pos.immutable());
                    }
                }
                Vec3 at = Vec3.atBottomCenterOf(center);
                raise(server, caster, 0.0F, ring, Blocks.BONE_BLOCK.defaultBlockState(), 120, 10, placed -> {
                    for (LivingEntity enemy : touching(caster, at, 5.0, placed)) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), caster);
                    }
                }, () -> {
                });
                level.playSound(null, center, SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 1.5F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hoarfrost Tithe: drains the spell's power from every enemy within 5 blocks, slowing them 4 s and healing the caster as much per enemy hit. */
    public static class HoarfrostTithe extends ModifiedSpell {
        HoarfrostTithe() {
            super("hoarfrost_tithe", 2);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 5.0)) {
                    if (DamageSources.applyDamage(enemy, power, this.getDamageSource(caster))) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), caster);
                        enemy.setTicksFrozen(Math.max(enemy.getTicksFrozen(), enemy.getTicksRequiredToFreeze() + 40));
                        caster.heal(power);
                        SmiteSpells.beam(server, enemy.getEyePosition(), caster.getEyePosition().subtract(0.0, 0.3, 0.0), ParticleTypes.SNOWFLAKE);
                    }
                }
                GroundSpells.ring(server, caster.position(), 5.0, ParticleTypes.SNOWFLAKE);
                level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Shepherd's Toll: a 6-block pulse that pulls enemies 3 blocks toward the caster and soulburns them for 4 s. */
    public static class ShepherdsToll extends ModifiedSpell {
        ShepherdsToll() {
            super("shepherds_toll", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                    Vec3 in = caster.position().subtract(enemy.position()).multiply(1.0, 0.0, 1.0);
                    double pull = Math.min(3.0, Math.max(0.0, in.length() - 1.0));
                    if (pull > 0.0) {
                        enemy.setDeltaMovement(in.normalize().scale(pull * 0.35).add(0.0, 0.2, 0.0));
                        enemy.hurtMarked = true;
                    }
                    BoltSpells.soulburn(caster, enemy, 80);
                }
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.SOUL_FIRE_FLAME);
                level.playSound(null, caster.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Marrow Pilfer: strikes the creature looked at for the spell's power, healing as much, and steals one of its positive effects. */
    public static class MarrowPilfer extends SmiteSpells.Smite {
        MarrowPilfer() {
            super("marrow_pilfer", 2, 6.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            if (this.hurt(caster, target, power)) {
                caster.heal(power);
            }
            target.getActiveEffects().stream().filter(e -> e.getEffect().isBeneficial()).findFirst().ifPresent(stolen -> {
                MobEffectInstance copy = new MobEffectInstance(stolen);
                target.removeEffect(stolen.getEffect());
                caster.addEffect(copy);
            });
            SmiteSpells.beam(level, target.getEyePosition(), caster.getEyePosition().subtract(0.0, 0.3, 0.0), ParticleTypes.DAMAGE_INDICATOR);
            level.playSound(null, target.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    /** Ribcage Trap: a thrown bone that cages the creature hit in bone blocks for 4 s. */
    static void cage(ServerLevel level, LivingEntity caster, LivingEntity target) {
        BlockPos at = target.blockPosition();
        List<BlockPos> cage = new ArrayList<>();
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || z != 0) {
                        cage.add(at.offset(x, y, z));
                    }
                }
            }
        }
        cage.add(at.above(2));
        target.teleportTo(at.getX() + 0.5, target.getY(), at.getZ() + 0.5);
        MartialSpells.root(target, 80);
        raise(level, caster, 0.0F, cage, Blocks.BONE_BLOCK.defaultBlockState(), 80, 1000, placed -> {
        }, () -> {
        });
        level.playSound(null, at, SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
    }

    // ---- summons ----

    /** Cinder Husks: three fire-immune husks for 20 s that set alight whatever they hit. */
    public static class CinderHusks extends SummonSpell {
        CinderHusks() {
            super("cinder_husks", 4);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.HUSK, 3, castData, husk -> {
                husk.setBaby(false);
                husk.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
                husk.setRemainingFireTicks(400);
            });
        }
    }

    /** Barrow Thrall: a frost-armoured zombie for 30 s that explodes when it dies, freezing enemies within 4 blocks for 3 s. */
    public static class BarrowThrall extends SummonSpell {
        BarrowThrall() {
            super("barrow_thrall", 4);
        }

        @Override
        protected int duration() {
            return 600;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.ZOMBIE, 1, castData, zombie -> {
                zombie.setBaby(false);
                UndeadSummons.cap(zombie, 0xBFE6F2);
                UndeadSummons.equip(zombie, EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                UndeadSummons.equip(zombie, EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                UndeadSummons.equip(zombie, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                UndeadSummons.stats(zombie, 30.0, 0.0);
            });
        }
    }

    /** Voltaic Revenant: a charged husk for 20 s with Speed II whose hits chain lightning to one nearby enemy. */
    public static class VoltaicRevenant extends SummonSpell {
        VoltaicRevenant() {
            super("voltaic_revenant", 4);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.HUSK, 1, castData, husk -> {
                husk.setBaby(false);
                UndeadSummons.cap(husk, 0xF2E14C);
                husk.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 1, false, false));
            });
        }
    }

    /** Bound Shade: two allied vexes for 20 s whose hits make their targets glow. */
    public static class BoundShade extends SummonSpell {
        BoundShade() {
            super("bound_shade", 3);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 2, castData, vex -> {
            });
        }
    }

    /** Carrion Beetles: four silverfish for 20 s whose bites poison (I) for 4 s. */
    public static class CarrionBeetles extends SummonSpell {
        CarrionBeetles() {
            super("carrion_beetles", 2);
        }

        @Override
        protected int duration() {
            return 400;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.SILVERFISH, 4, castData, silverfish -> {
            });
        }
    }

    /** Bonehound Pack: two skeletal wolves for 25 s whose bites wither (I) for 3 s. */
    public static class BonehoundPack extends SummonSpell {
        BonehoundPack() {
            super("bonehound_pack", 4);
        }

        @Override
        protected int duration() {
            return 500;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 2, castData, wolf -> {
                SummonSpells.tame(wolf, caster);
                wolf.setCollarColor(DyeColor.WHITE);
            });
        }
    }

    // ---- states on the caster ----

    /** Crown of Brine: for 15 s each kill raises a drowned with a trident that fights for the caster for 20 s. */
    public static class CrownOfBrine extends Aura {
        CrownOfBrine() {
            super("crown_of_brine", 300);
        }
    }

    /** Grave Pilgrim: for 15 s each kill heals the caster 3 and raises a skeleton that fights for it for 10 s. */
    public static class GravePilgrim extends Aura {
        GravePilgrim() {
            super("grave_pilgrim", 300);
        }
    }

    /** Phylactery Ward: for 10 s the first lethal hit leaves the caster at 4 health instead and summons 2 vexes for 15 s. */
    public static class PhylacteryWard extends Aura {
        PhylacteryWard() {
            super("phylactery_ward", 200);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new OssuaryWall(),
            // Grave Pact: mark the creature looked at for 10 s; if it dies, the caster gains Absorption II (10 s) and a zombie bodyguard (30 s)
            new DeathMark("grave_pact", 0, 200),
            new OffalFeast(),
            // Carrion Cage: mark for 15 s; when it dies it bursts, hurting enemies within 4 blocks and healing the caster 2 hearts per enemy hit
            new DeathMark("carrion_cage", 5, 300),
            new BloodTithe(),
            // Bleeding Bone: a thrown bone that bleeds for 6 s, healing the thrower 1 health each second it bleeds
            new Bolt("bleeding_bone", 3, Items.BONE, ParticleTypes.DAMAGE_INDICATOR, (bolt, caster, target, power) -> {
                if (bolt.hurt(caster, target, power) && target.level() instanceof ServerLevel level) {
                    target.addEffect(new MobEffectInstance(BrandSpells.external("attributeslib:bleeding"), 120, 0), caster);
                    Zones.start(level, target.position(), caster, power, 121, 20, zone -> {
                        if (zone.age() > 0 && target.isAlive() && target.hasEffect(BrandSpells.external("attributeslib:bleeding"))) {
                            caster.heal(1.0F);
                            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
                        }
                    }, () -> {
                    }, null);
                }
            }, null).speed(1.6F).sound(SoundEvents.SKELETON_SHOOT),
            new CorpseHop(),
            // Grave Qi: mark the creature looked at for 12 s; if it dies, it rises as an allied husk for 20 s
            new DeathMark("grave_qi", 0, 240),
            new CinderHusks(),
            new CinderPalisade(),
            new BarrowThrall(),
            new HoarfrostTithe(),
            new CrownOfBrine(),
            // Brackish Bile: a thrown flask; enemies within 3 blocks of where it breaks get Poison II (4 s) and Nausea (6 s)
            new Bolt("brackish_bile", 0, Items.SPLASH_POTION, ParticleTypes.SPLASH, (bolt, caster, target, power) -> {
            }, (bolt, level, caster, at, power) -> {
                for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), caster);
                    enemy.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), caster);
                }
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.3, at.z, 60, 1.4, 0.3, 1.4, 0.1);
                level.sendParticles(ParticleTypes.SNEEZE, at.x, at.y + 0.5, at.z, 30, 1.4, 0.4, 1.4, 0.02);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
            }).speed(1.0F).gravity(0.05).sound(SoundEvents.SPLASH_POTION_THROW),
            new VoltaicRevenant(),
            // Necrotic Static: 5 s of pulses within 5 blocks: the spell's power to enemies, and as much healing to the caster's summons
            new GroundSpells.Area("necrotic_static", 2, 5.0, 5, 20, false, ParticleTypes.ELECTRIC_SPARK, SoundEvents.BEACON_ACTIVATE,
                (area, zone, allies, enemies) -> {
                    enemies.forEach(e -> area.hurt(zone.caster, e, zone.power));
                    allies.stream().filter(BoltSpells::summon).forEach(a -> a.heal(zone.power));
                }),
            // Salt Circle: 6 s of pulses within 4 blocks: the spell's power to undead enemies each second, and Weakness cleared from allies
            new GroundSpells.Area("salt_circle", 3, 4.0, 6, 20, false, ParticleTypes.WHITE_ASH, SoundEvents.SAND_PLACE,
                (area, zone, allies, enemies) -> {
                    enemies.stream().filter(BoltSpells::undead).forEach(e -> area.hurt(zone.caster, e, zone.power));
                    allies.forEach(a -> a.removeEffect(MobEffects.WEAKNESS));
                }),
            new BoundShade(),
            new PhylacteryWard(),
            // Grave Hex: mark for 10 s; if it dies, it explodes for 6 damage and Wither I (5 s) within 4 blocks
            new DeathMark("grave_hex", 6, 200),
            new CarrionBeetles(),
            new GraveFruit(),
            // Binding Fu: a thrown paper talisman; the creature hit is stunned 3 s and takes a quarter more damage from the caster's summons for 10 s
            new Bolt("binding_fu", 2, Items.PAPER, ParticleTypes.ENCHANT, (bolt, caster, target, power) -> {
                bolt.hurt(caster, target, power);
                MartialSpells.stun(target, StunType.LONG, 3.0F);
                MartialSpells.root(target, 60);
                Buffs.mark(target, "binding_fu", caster, 200);
            }, null).speed(1.7F).sound(SoundEvents.BOOK_PAGE_TURN),
            new BoneLattice(),
            new ShepherdsToll(),
            // Restless Flock: mark for 15 s; if it dies, it releases 3 vexes that fight for the caster for 15 s
            new DeathMark("restless_flock", 0, 300),
            new GhoulsJerky(),
            new MarrowPilfer(),
            new Bolt("ribcage_trap", 2, Items.BONE, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
                bolt.hurt(caster, target, power);
                if (target.level() instanceof ServerLevel level) {
                    cage(level, caster, target);
                }
            }, null).speed(1.6F).sound(SoundEvents.SKELETON_SHOOT),
            new BonehoundPack(),
            // Styx Fog: a 5-block fog for 6 s: enemies in it get Slowness II, the caster's summons Regeneration I
            new GroundSpells.Area("styx_fog", 0, 5.0, 6, 20, false, ParticleTypes.CAMPFIRE_COSY_SMOKE, SoundEvents.SOUL_ESCAPE,
                (area, zone, allies, enemies) -> {
                    GroundSpells.effect(enemies, zone.caster, MobEffects.MOVEMENT_SLOWDOWN, 30, 1);
                    GroundSpells.effect(allies.stream().filter(BoltSpells::summon).toList(), zone.caster, MobEffects.REGENERATION, 50, 0);
                }),
            new GravePilgrim());
    }

    // ---- what the marks, states and summons do ----

    /** Phylactery Ward: a death while warded is undone (like a totem). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onWardedDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!Buffs.has(dead, "phylactery_ward") || !(dead.level() instanceof ServerLevel level)) {
            return;
        }
        event.setCanceled(true);
        Buffs.clear(dead, "phylactery_ward");
        dead.setHealth(4.0F);
        raiseDead(level, dead, EntityType.VEX, 2, 300, "phylactery_ward", dead.position().add(0.0, 1.0, 0.0), vex -> {
        });
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, dead.getX(), dead.getY(1.0), dead.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
        level.playSound(null, dead.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    /** The death marks pay off, and kills under Crown of Brine or Grave Pilgrim raise the dead. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = dead.position();

        LivingEntity by = Buffs.markedBy(dead, "grave_pact");
        if (by != null) {
            by.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            raiseDead(level, by, EntityType.ZOMBIE, 1, 600, "grave_pact", by.position(), zombie -> {
                zombie.setBaby(false);
                UndeadSummons.cap(zombie, 0x3B3B3B);
                UndeadSummons.equip(zombie, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                UndeadSummons.equip(zombie, EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            });
        }
        by = Buffs.markedBy(dead, "carrion_cage");
        if (by != null) {
            AbstractSpell spell = spell("carrion_cage");
            for (LivingEntity enemy : Targets.enemies(by, at, 4.0)) {
                if (enemy != dead && DamageSources.applyDamage(enemy, spell.getSpellPower(1, by), spell.getDamageSource(by))) {
                    by.heal(4.0F);
                }
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState()),
                at.x, at.y + 0.5, at.z, 80, 1.5, 0.6, 1.5, 0.2);
            level.playSound(null, dead.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.5F, 0.5F);
        }
        by = Buffs.markedBy(dead, "grave_qi");
        if (by != null) {
            raiseDead(level, by, EntityType.HUSK, 1, 400, "grave_qi", at, husk -> husk.setBaby(false));
        }
        by = Buffs.markedBy(dead, "grave_hex");
        if (by != null) {
            AbstractSpell spell = spell("grave_hex");
            for (LivingEntity enemy : Targets.enemies(by, at, 4.0)) {
                if (enemy != dead && DamageSources.applyDamage(enemy, spell.getSpellPower(1, by), spell.getDamageSource(by))) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), by);
                }
            }
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, 0.8, 0.4, 0.8, 0.0);
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.5, at.z, 40, 1.5, 0.6, 1.5, 0.05);
            level.playSound(null, dead.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        by = Buffs.markedBy(dead, "restless_flock");
        if (by != null) {
            raiseDead(level, by, EntityType.VEX, 3, 300, "restless_flock", at.add(0.0, 1.0, 0.0), vex -> {
            });
        }

        if ("barrow_thrall".equals(Summons.kind(dead)) && Summons.owner(dead) instanceof LivingEntity owner) {
            for (LivingEntity enemy : Targets.enemies(owner, at, 4.0)) {
                enemy.setTicksFrozen(enemy.getTicksRequiredToFreeze() + 60);
                MartialSpells.root(enemy, 60);
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.5, at.z, 80, 2.0, 0.8, 2.0, 0.1);
            level.playSound(null, dead.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
        }

        if (event.getSource().getEntity() instanceof LivingEntity killer && killer != dead && !Summons.allied(killer, dead)) {
            if (Buffs.has(killer, "crown_of_brine")) {
                raiseDead(level, killer, EntityType.DROWNED, 1, 400, "crown_of_brine", at, drowned -> {
                    drowned.setBaby(false);
                    UndeadSummons.equip(drowned, EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
                    UndeadSummons.cap(drowned, 0x2F7F8F);
                });
            }
            if (Buffs.has(killer, "grave_pilgrim")) {
                killer.heal(3.0F);
                raiseDead(level, killer, EntityType.SKELETON, 1, 200, "grave_pilgrim", at, Wave2Necromancer::skeleton);
            }
        }
    }

    /** Blood Tithe: melee hits heal a fifth of the damage dealt. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && event.getSource().getDirectEntity() == attacker
            && Buffs.has(attacker, "blood_tithe")) {
            attacker.heal(event.getAmount() * 0.2F);
        }
    }

    /** Drowned raised by the Crown of Brine fight on land by day (a drowned's own attack waits for night or water). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && "crown_of_brine".equals(Summons.kind(mob))) {
            mob.goalSelector.addGoal(2, new MeleeAttackGoal((PathfinderMob)mob, 1.0, false));
        }
    }

    /** What the necromancer summons' hits do on top of their damage, and Binding Fu's mark. */
    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !(Summons.owner(attacker) instanceof LivingEntity owner)) {
            return;
        }
        if (Buffs.markedBy(victim, "binding_fu") == owner) {
            event.setAmount(event.getAmount() * 1.25F);
        }
        switch (Summons.kind(attacker)) {
            case "cinder_husks" -> victim.setSecondsOnFire(4);
            case "voltaic_revenant" -> Targets.enemies(owner, victim.position(), 4.0).stream().filter(e -> e != victim).findFirst().ifPresent(next -> {
                next.hurt(attacker.damageSources().lightningBolt(), SummonSpells.power(attacker) * 0.5F);
                SummonSpells.spark((ServerLevel)attacker.level(), victim.getEyePosition(), next.getEyePosition());
            });
            case "bound_shade" -> victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), owner);
            case "carrion_beetles" -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
            case "bonehound_pack" -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), owner);
            default -> {
            }
        }
    }
}
