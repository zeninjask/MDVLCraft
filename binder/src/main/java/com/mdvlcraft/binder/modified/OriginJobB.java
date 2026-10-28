package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.specs.JobSpecs;
import com.mdvlcraft.binder.specs.Specs;
import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

/**
 * The Origin tab's Modified abilities of Miner, Lumberjack, Explorer, Alchemist, Sailor, Fisher (tools/origins/groups.json, group JobB):
 * self tonics, a pickaxe blow, a rolling log, thrown flasks, a rope, a hook and a net, a following lamp, a boat's tailwind,
 * a weather forecast and a patch of chummed water.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginJobB {
    private static final String HARD_HAT = "hard_hat";
    private static final UUID PICKAXE_ID = UUID.fromString("b4d1e8a2-3c7f-4e95-8a06-1f2d9c7b5e31");
    private static final UUID FOREST_SPEED_ID = UUID.fromString("b4d1e8a2-3c7f-4e95-8a06-1f2d9c7b5e32");
    private static final UUID PATHFINDER_STEP_ID = UUID.fromString("b4d1e8a2-3c7f-4e95-8a06-1f2d9c7b5e33");
    private static final UUID ACID_ID = UUID.fromString("b4d1e8a2-3c7f-4e95-8a06-1f2d9c7b5e34");
    private static final UUID TAILWIND_ID = UUID.fromString("b4d1e8a2-3c7f-4e95-8a06-1f2d9c7b5e35");
    private static final double TAILWIND_BONUS = 0.4;
    private static final int FORECAST_TICKS = 12000;
    private static final String LANG = "ability.mdvlcraft.modified.";
    private static final Field TIME_UNTIL_LURED = ObfuscationReflectionHelper.findField(FishingHook.class, "f_37090_");
    private static final Field TIME_UNTIL_HOOKED = ObfuscationReflectionHelper.findField(FishingHook.class, "f_37091_");
    private static final Field NIBBLE = ObfuscationReflectionHelper.findField(FishingHook.class, "f_37089_");

    private OriginJobB() {
    }

    // ---- shared helpers ----

    /** Changes `target`'s `attribute` by `amount` (`operation`) for `ticks` (a transient modifier, taken off when the last one runs out). */
    public static void modify(LivingEntity target, Attribute attribute, UUID id, String key, double amount, AttributeModifier.Operation operation, int ticks) {
        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) {
            throw new IllegalStateException(target + " has no " + attribute.getDescriptionId());
        }
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, "mdvlcraft " + key, amount, operation));
        Buffs.give(target, key, ticks);
        Zones.start(level, target.position(), target, 0.0F, ticks, ticks + 1, zone -> {
        }, () -> {
            if (!Buffs.has(target, key)) {
                instance.removeModifier(id);
            }
        }, null);
    }

    /** Throws `target` toward the caster by up to `blocks` blocks (horizontally, with a little lift), stopping short of it. */
    static void pull(LivingEntity caster, LivingEntity target, double blocks) {
        Vec3 in = caster.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        double distance = Math.min(blocks, in.length() - 1.5);
        if (distance <= 0.0) {
            return;
        }
        target.setDeltaMovement(in.normalize().scale(distance * 0.3).add(0.0, 0.25, 0.0));
        target.hurtMarked = true;
    }

    /** A boat, or a ship of Aleki's Nifty Ships. */
    static boolean vessel(Entity entity) {
        return entity instanceof Boat || ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getNamespace().equals("alekiships");
    }

    static int field(Field field, FishingHook hook) {
        try {
            return field.getInt(hook);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read FishingHook." + field.getName(), e);
        }
    }

    static void field(Field field, FishingHook hook, int value) {
        try {
            field.setInt(hook, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot write FishingHook." + field.getName(), e);
        }
    }

    /** Vanilla effects on the caster, with a puff of `particle` and a sound. */
    public static class Tonic extends ModifiedSpell {
        private final List<MobEffect> effects;
        private final List<Integer> amplifiers;
        private final int ticks;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        Tonic(String key, int ticks, List<MobEffect> effects, List<Integer> amplifiers, ParticleOptions particle, SoundEvent sound) {
            super(key, 0);
            if (effects.size() != amplifiers.size()) {
                throw new IllegalArgumentException(key + ": one amplifier per effect");
            }
            this.effects = effects;
            this.amplifiers = amplifiers;
            this.ticks = ticks;
            this.particle = particle;
            this.sound = sound;
        }

        /** How long the effects last on `caster`. */
        int ticks(LivingEntity caster) {
            return this.ticks;
        }

        /** Anything more the tonic does, after the effects. */
        void more(ServerLevel level, LivingEntity caster) {
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (int i = 0; i < this.effects.size(); i++) {
                caster.addEffect(new MobEffectInstance(this.effects.get(i), this.ticks(caster), this.amplifiers.get(i)));
            }
            if (level instanceof ServerLevel server) {
                this.more(server, caster);
                server.sendParticles(this.particle, caster.getX(), caster.getY(0.6), caster.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
            }
            level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Miner ----

    /** Hard Hat: for 30 s, Resistance I and no damage from falling blocks (anvils, dripstone) or suffocation. */
    static Tonic hardHat() {
        return new Tonic(HARD_HAT, 600, List.of(MobEffects.DAMAGE_RESISTANCE), List.of(0), ParticleTypes.CRIT, SoundEvents.ARMOR_EQUIP_IRON) {
            @Override
            void more(ServerLevel level, LivingEntity caster) {
                Buffs.give(caster, HARD_HAT, 600);
            }
        };
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !Buffs.has(victim, HARD_HAT)) {
            return;
        }
        if (event.getSource().is(DamageTypes.FALLING_BLOCK) || event.getSource().is(DamageTypes.FALLING_ANVIL)
            || event.getSource().is(DamageTypes.FALLING_STALACTITE) || event.getSource().is(DamageTypes.IN_WALL)) {
            event.setCanceled(true);
        }
    }

    /**
     * Miner's Lamp: Night Vision for 3 minutes and a light (an invisible light block of level 10, so light 5 or more within 5
     * blocks) at the caster's head that follows it, moved every 2 ticks into the air block its eyes are in. Nothing is lit
     * while the eyes are in water or a solid block; the light is taken away when the lamp ends or the caster changes level.
     */
    public static class MinersLamp extends ModifiedSpell {
        private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 10);

        MinersLamp() {
            super("miner_s_lamp", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0));
            BlockState light = Specs.has(caster, "bright_lamp") ? LIGHT.setValue(LightBlock.LEVEL, 15) : LIGHT;
            if (level instanceof ServerLevel server) {
                List<BlockPos> lit = new ArrayList<>();
                Zones.start(server, caster.position(), caster, 0.0F, 3600, 2, zone -> {
                    if (caster.level() != server || !caster.isAlive()) {
                        zone.stop();
                        return;
                    }
                    BlockPos head = BlockPos.containing(caster.getEyePosition());
                    if (lit.size() == 1 && lit.get(0).equals(head)) {
                        return;
                    }
                    WallSpells.clear(server, lit, light);
                    lit.clear();
                    lit.addAll(WallSpells.place(server, List.of(head), light));
                }, () -> WallSpells.clear(server, lit, light), null);
                server.sendParticles(ParticleTypes.FLAME, caster.getX(), caster.getEyeY() + 0.3, caster.getZ(), 8, 0.2, 0.1, 0.2, 0.01);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Pickaxe Strike: needs a pickaxe in the main hand (players and mobs); a blow on the creature looked at (4 blocks) for
     * 160% of the caster's attack damage, taking 4 of its armour for 8 s.
     */
    public static class PickaxeStrike extends SmiteSpells.Smite {
        PickaxeStrike() {
            super("pickaxe_strike", 0, 4.0F);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (MartialSpells.category(caster.getMainHandItem()) != WeaponCategories.PICKAXE) {
                MartialSpells.refuse(caster, "need_pickaxe");
                return false;
            }
            return super.checkPreCastConditions(level, spellLevel, caster, data);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, MartialSpells.attack(caster) * 1.6F);
            Wave2Blaze.armour(target, PICKAXE_ID, "pickaxe_strike_armour", -4.0, AttributeModifier.Operation.ADDITION, 160);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), target.getX(), target.getY(0.6),
                target.getZ(), 30, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.5F, 0.7F);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    // ---- Lumberjack ----

    /** Log Throw: a log rolling 10 blocks along the ground ahead (half a block a tick; blocks stop it), hurting each enemy it meets once and knocking it 3 blocks on. */
    public static class LogThrow extends ModifiedSpell {
        LogThrow() {
            super("log_throw", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 look = Wave2Assassin.flatLook(caster);
                Vec3 from = caster.position().add(0.0, 0.5, 0.0);
                double reach = Wave2Assassin.reach(caster, look, 10.0).distanceTo(from);
                Set<LivingEntity> struck = new HashSet<>();
                BlockParticleOption log = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState());
                Zones.start(server, from, caster, this.getSpellPower(spellLevel, caster), 20, 1, zone -> {
                    double d = (zone.age() + 1) * 0.5;
                    if (d > reach) {
                        zone.stop();
                        return;
                    }
                    Vec3 at = from.add(look.scale(d));
                    server.sendParticles(log, at.x, at.y, at.z, 10, 0.6, 0.2, 0.6, 0.05);
                    for (LivingEntity enemy : Targets.enemies(caster, at, 1.4)) {
                        if (enemy != caster && struck.add(enemy)) {
                            DamageSources.ignoreNextKnockback(enemy);
                            DamageSources.applyDamage(enemy, zone.power, this.getDamageSource(caster));
                            Wave2Guardian.knock(enemy.position().subtract(look), enemy, 3.0);
                        }
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Woodsman's Grit: Resistance I and Haste II for 15 s. */
    static Tonic woodsmansGrit() {
        return new Tonic("woodsman_s_grit", 300, List.of(MobEffects.DAMAGE_RESISTANCE, MobEffects.DIG_SPEED), List.of(0, 1),
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), SoundEvents.WOOD_PLACE);
    }

    /** Forest Friend: only in a forest, taiga or jungle biome (refused elsewhere); Regeneration I and 10% more speed for 30 s. */
    public static class ForestFriend extends Tonic {
        ForestFriend() {
            super("forest_friend", 600, List.of(MobEffects.REGENERATION), List.of(0), ParticleTypes.HAPPY_VILLAGER, SoundEvents.AZALEA_LEAVES_PLACE);
        }

        static boolean forest(LivingEntity caster) {
            var biome = caster.level().getBiome(caster.blockPosition());
            return biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!forest(caster)) {
                MartialSpells.refuse(caster, "need_forest");
                return false;
            }
            return true;
        }

        @Override
        int ticks(LivingEntity caster) {
            return Specs.has(caster, "forest_friend_s_call") ? 900 : 600;
        }

        @Override
        void more(ServerLevel level, LivingEntity caster) {
            modify(caster, Attributes.MOVEMENT_SPEED, FOREST_SPEED_ID, "forest_friend_speed", 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL, this.ticks(caster));
        }
    }

    // ---- Explorer ----

    /** Second Wind: a player's Epic Fight stamina, hunger and saturation are filled; a mob caster gets nothing (it has neither). */
    public static class SecondWind extends ModifiedSpell {
        SecondWind() {
            super("second_wind", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player) {
                BuffSpells.restoreStamina(player, 1.0F);
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0F);
                if (Specs.has(player, "deep_reserves")) {
                    SpecKit.addMana(player, SpecKit.maxMana(player) * 0.2F);
                }
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY(0.5), caster.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Pathfinder: for 30 s, Speed I (20%) and half a block more step height, so full blocks are walked up without jumping. */
    public static class Pathfinder extends Tonic {
        Pathfinder() {
            super("pathfinder", 600, List.of(MobEffects.MOVEMENT_SPEED), List.of(0), ParticleTypes.CLOUD, SoundEvents.ARMOR_EQUIP_LEATHER);
        }

        @Override
        void more(ServerLevel level, LivingEntity caster) {
            modify(caster, ForgeMod.STEP_HEIGHT_ADDITION.get(), PATHFINDER_STEP_ID, "pathfinder_step", 0.5, AttributeModifier.Operation.ADDITION, 600);
        }
    }

    /** Grappling Rope: a thrown rope; the creature it hits is pulled all the way to the caster (the caster never moves). */
    static BoltSpells.Bolt grapplingRope() {
        return new BoltSpells.Bolt("grappling_rope", 0, Items.LEAD, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            pull(caster, target, Double.MAX_VALUE);
            if (caster.level() instanceof ServerLevel level) {
                SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.CRIT);
            }
        }, null) {
            @Override
            void release(Level level, LivingEntity caster, float power) {
                BoltProjectile rope = this.launch(level, caster, power, caster.getEyePosition(), caster.getLookAngle());
                if (Specs.has(caster, "long_rope")) {
                    rope.setDeltaMovement(rope.getDeltaMovement().scale(1.5));
                }
            }
        }.speed(2.0F).gravity(0.02).sound(SoundEvents.FISHING_BOBBER_THROW);
    }

    // ---- Alchemist ----

    /** Acid Flask: a thrown flask bursting where it lands; every enemy within 3 blocks takes 4 damage and loses 4 armour for 8 s. */
    static BoltSpells.Bolt acidFlask() {
        return new BoltSpells.Bolt("acid_flask", 4, Items.LINGERING_POTION, ParticleTypes.ITEM_SLIME, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                bolt.hurt(caster, enemy, power);
                Wave2Blaze.armour(enemy, ACID_ID, "acid_flask_armour", -4.0, AttributeModifier.Operation.ADDITION, 160);
            }
            level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.3, at.z, 60, 1.5, 0.3, 1.5, 0.1);
            level.sendParticles(ParticleTypes.SNEEZE, at.x, at.y + 0.3, at.z, 30, 1.5, 0.3, 1.5, 0.02);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        }).speed(1.0F).gravity(0.05).sound(SoundEvents.SPLASH_POTION_THROW);
    }

    /** Elixir of Vigor: drunk; Strength I, Speed I and Haste I for 30 s. */
    static Tonic elixirOfVigor() {
        return new Tonic("elixir_of_vigor", 600, List.of(MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED), List.of(0, 0, 0),
            ParticleTypes.EFFECT, SoundEvents.GENERIC_DRINK);
    }

    /**
     * Volatile Mix: a thrown flask bursting where it lands into a 3-block cloud of one random harmful effect (Poison I,
     * Slowness I, Weakness I or Blindness) that lasts 6 s; enemies in it get the effect for 2 s, renewed every half second.
     */
    static BoltSpells.Bolt volatileMix() {
        List<MobEffect> harms = List.of(MobEffects.POISON, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.BLINDNESS);
        return new BoltSpells.Bolt("volatile_mix", 0, Items.SPLASH_POTION, ParticleTypes.WITCH, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            MobEffect harm = harms.get(caster.getRandom().nextInt(harms.size()));
            int color = harm.getColor();
            DustParticleOptions cloud = new DustParticleOptions(new Vector3f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                (color & 255) / 255.0F), 1.5F);
            Zones.start(level, at, caster, power, 120, 10, zone -> {
                GroundSpells.ring(level, at, 3.0, cloud);
                level.sendParticles(cloud, at.x, at.y + 1.0, at.z, 20, 1.5, 0.6, 1.5, 0.0);
                GroundSpells.effect(Targets.enemies(caster, at, 3.0), caster, harm, 40, 0);
            }, () -> {
            }, null);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }).speed(1.0F).gravity(0.05).sound(SoundEvents.SPLASH_POTION_THROW);
    }

    // ---- Sailor ----

    /**
     * Tailwind: players only, riding a boat or ship (refused otherwise, and for mobs, which never steer). For 30 s the vessel
     * the caster rides goes 40% faster: whoever steers it moves it from their own side (as with the Sailor's Helmsman), so the
     * caster carries a harmless zero Luck modifier that every client sees, and the steering side adds 40% of the vessel's
     * motion each tick while anyone aboard carries it.
     */
    public static class Tailwind extends ModifiedSpell {
        Tailwind() {
            super("tailwind", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster instanceof Player) || !vessel(caster.getRootVehicle()) || caster.getRootVehicle() == caster) {
                MartialSpells.refuse(caster, "need_boat");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            modify(caster, Attributes.LUCK, TAILWIND_ID, "tailwind", 0.0, AttributeModifier.Operation.ADDITION, 600);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 1.5, caster.getZ(), 30, 1.2, 0.4, 1.2, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }

        static boolean blowing(Entity vessel) {
            for (Entity aboard : vessel.getIndirectPassengers()) {
                if (aboard instanceof Player player && player.getAttribute(Attributes.LUCK).getModifier(TAILWIND_ID) != null) {
                    return true;
                }
            }
            return false;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != Phase.END) {
            return;
        }
        Entity vessel = player.getRootVehicle();
        if (vessel != player && vessel.isControlledByLocalInstance() && vessel.getFirstPassenger() == player && vessel(vessel)
            && Tailwind.blowing(vessel)) {
            Vec3 motion = vessel.getDeltaMovement();
            vessel.move(MoverType.SELF, new Vec3(motion.x * TAILWIND_BONUS, 0.0, motion.z * TAILWIND_BONUS));
        }
    }

    /** Deep Breath: Water Breathing and Dolphin's Grace for 60 s. */
    static Tonic deepBreath() {
        return new Tonic("deep_breath", 1200, List.of(MobEffects.WATER_BREATHING, MobEffects.DOLPHINS_GRACE), List.of(0, 0), ParticleTypes.BUBBLE,
            SoundEvents.PLAYER_BREATH);
    }

    /**
     * Weather Eye: Conduit Power for 30 s; a player also reads the next 10 minutes of the world's weather in chat, worked out
     * from the overworld's weather timers the way the game advances them: the weather now and each change (clear, rain,
     * thunderstorm) with the time until it. A change whose timing the game will only roll later is not foretold, and it says
     * nothing is coming when the weather cycle is off.
     */
    public static class WeatherEye extends Tonic {
        WeatherEye() {
            super("weather_eye", 600, List.of(MobEffects.CONDUIT_POWER), List.of(0), ParticleTypes.NAUTILUS, SoundEvents.CONDUIT_ACTIVATE);
        }

        @Override
        void more(ServerLevel level, LivingEntity caster) {
            if (caster instanceof ServerPlayer player) {
                player.sendSystemMessage(forecast(level));
            }
        }

        static String sky(boolean raining, boolean thundering) {
            return LANG + (raining ? thundering ? "weather_eye_storm" : "weather_eye_rain" : "weather_eye_clear");
        }

        static String time(int ticks) {
            int seconds = ticks / 20;
            return String.format("%d:%02d", seconds / 60, seconds % 60);
        }

        /** Steps ServerLevel.advanceWeatherCycle 10 minutes ahead; a timer the game would roll at random stops being followed. */
        static Component forecast(ServerLevel level) {
            ServerLevelData weather = level.getServer().getWorldData().overworldData();
            boolean cycle = level.getServer().overworld().getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE);
            int clear = weather.getClearWeatherTime();
            int thunder = weather.getThunderTime();
            int rain = weather.getRainTime();
            boolean thundering = weather.isThundering();
            boolean raining = weather.isRaining();
            boolean thunderKnown = true;
            boolean rainKnown = true;
            String now = sky(raining, thundering);
            MutableComponent out = Component.translatable(LANG + "weather_eye_now", Component.translatable(now));
            boolean changed = false;
            for (int t = 1; cycle && t <= FORECAST_TICKS; t++) {
                if (clear > 0) {
                    clear--;
                    thunder = thundering ? 0 : 1;
                    rain = raining ? 0 : 1;
                    thundering = false;
                    raining = false;
                } else {
                    if (thunderKnown) {
                        if (thunder > 0) {
                            if (--thunder == 0) {
                                thundering = !thundering;
                            }
                        } else {
                            thunderKnown = false;
                        }
                    }
                    if (rainKnown) {
                        if (rain > 0) {
                            if (--rain == 0) {
                                raining = !raining;
                            }
                        } else {
                            rainKnown = false;
                        }
                    }
                }
                String next = sky(raining, thundering);
                if (!next.equals(now)) {
                    out.append(" ").append(Component.translatable(LANG + "weather_eye_change", time(t), Component.translatable(next)));
                    now = next;
                    changed = true;
                }
            }
            if (!changed) {
                out.append(" ").append(Component.translatable(LANG + "weather_eye_steady"));
            }
            return out;
        }
    }

    // ---- Fisher ----

    /** Hook and Reel: a thrown hook; the creature it hits is pulled 6 blocks toward the caster (stopping short of it). */
    static BoltSpells.Bolt hookAndReel() {
        return new BoltSpells.Bolt("hook_and_reel", 0, Items.FISHING_ROD, ParticleTypes.SPLASH, (bolt, caster, target, power) -> {
            pull(caster, target, 6.0);
            if (Specs.has(caster, "barbed_hook")) {
                JobSpecs.bleed(caster, target, 3);
            }
            if (caster.level() instanceof ServerLevel level) {
                SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.SPLASH);
                level.playSound(null, caster.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.2F, 1.0F);
            }
        }, null).speed(1.8F).gravity(0.03).sound(SoundEvents.FISHING_BOBBER_THROW);
    }

    /**
     * Chum the Water: for 60 s, around the spot looked at (water counts, 24 blocks), every fishing hook within 8 blocks waits
     * half as long for a bite: its lure and approach timers count down twice a tick (each stops at 1, so the game itself
     * makes the bite).
     */
    public static class ChumTheWater extends ModifiedSpell {
        ChumTheWater() {
            super("chum_the_water", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 eye = caster.getEyePosition();
                Vec3 at = server.clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(24.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                    caster)).getLocation();
                Zones.start(server, at, caster, 0.0F, 1200, 1, zone -> {
                    if (zone.age() % 20 == 0) {
                        GroundSpells.ring(server, at, 8.0, ParticleTypes.BUBBLE_POP);
                        server.sendParticles(ParticleTypes.FISHING, at.x, at.y + 0.1, at.z, 12, 3.0, 0.0, 3.0, 0.02);
                    }
                    int steps = zone.age() < 600 && Specs.has(caster, "feeding_frenzy") ? 3 : 1;
                    for (FishingHook hook : server.getEntitiesOfClass(FishingHook.class, new AABB(at, at).inflate(8.0), h -> h.distanceToSqr(at) <= 64.0)) {
                        for (int s = 0; s < steps && field(NIBBLE, hook) <= 0; s++) {
                            int hooked = field(TIME_UNTIL_HOOKED, hook);
                            int lured = field(TIME_UNTIL_LURED, hook);
                            if (hooked > 1) {
                                field(TIME_UNTIL_HOOKED, hook, hooked - 1);
                            } else if (hooked <= 0 && lured > 1) {
                                field(TIME_UNTIL_LURED, hook, lured - 1);
                            }
                        }
                    }
                }, () -> {
                }, null);
                level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Net Throw: a thrown net; where it lands (or on the creature it hits) every enemy within 3 blocks is rooted for 3 s. */
    static BoltSpells.Bolt netThrow() {
        return new BoltSpells.Bolt("fisher_net_throw", 0, Items.COBWEB, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                MartialSpells.root(enemy, 60);
            }
            GroundSpells.ring(level, at, 3.0, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.2F, 0.8F);
        }).speed(1.1F).gravity(0.03).scale(1.2F).sound(SoundEvents.FISHING_BOBBER_THROW);
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            hardHat(), new MinersLamp(), new PickaxeStrike(),
            new LogThrow(), woodsmansGrit(), new ForestFriend(),
            new SecondWind(), new Pathfinder(), grapplingRope(),
            acidFlask(), elixirOfVigor(), volatileMix(),
            new Tailwind(), deepBreath(), new WeatherEye(),
            hookAndReel(), new ChumTheWater(), netThrow());
    }
}
