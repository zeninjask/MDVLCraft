package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.specs.JobSpecs;
import com.mdvlcraft.binder.specs.Specs;
import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import com.mdvlcraft.binder.content.WildCooking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Origin tab's Modified abilities of Blacksmith, Cleric, Cook, Farmer, Merchant, Rancher (tools/origins/groups.json, group JobA):
 * tempering and smelting, cleansing and sanctuary, feasts and spices, crops and scarecrows, coins and bribes, and herding.
 * Timed states and marks are Buffs; placed things (sanctuary, feast, scarecrow) are Zones; what they do lives in the
 * event handlers at the end.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginJobA {
    private static final String TEMPER = "temper_armour";
    private static final String SHRED = "hammer_shred";
    private static final String TURNED = "turned_undead";
    private static final String IRON_STOMACH = "iron_stomach";
    private static final String BRIBED = "bribed";
    private static final String BRAND = "branding_iron";
    private static final String SCARECROW_SEEN = "scarecrow_seen";
    private static final String SCARECROW_LURED = "scarecrow_lured";
    private static final UUID TEMPER_ARMOUR_ID = UUID.fromString("6e2f4b81-3c9d-4a57-b0e8-1d7a5c3f9b26");
    private static final UUID TEMPER_TOUGHNESS_ID = UUID.fromString("b8d31e6a-7f42-4c09-a5b3-e2c94f1d6a87");
    private static final UUID SHRED_ID = UUID.fromString("2c9a7e15-d4b8-4f63-8e01-a6f5b3c7d942");
    private static final Set<MobEffect> STOMACH_PROOF = Set.of(MobEffects.POISON, MobEffects.HUNGER, MobEffects.CONFUSION);
    /** The sanctuaries and scarecrows standing now (removed when their zone ends). */
    private static final List<Placed> SANCTUARIES = new ArrayList<>();
    private static final List<Placed> SCARECROWS = new ArrayList<>();

    private OriginJobA() {
    }

    /** Something placed on the ground for a while: where, by whom, and how far it reaches. */
    private record Placed(ServerLevel level, Vec3 center, LivingEntity caster, double radius) {
        boolean reaches(LivingEntity e) {
            return e.level() == this.level && e.distanceToSqr(this.center) <= this.radius * this.radius;
        }
    }

    /** Starts a placed zone that is in `list` until it ends. */
    private static void place(List<Placed> list, ServerLevel level, Vec3 center, LivingEntity caster, double radius, int ticks,
                              Consumer<Zones.Zone> pulse, @Nullable Entity marker) {
        Placed placed = new Placed(level, center, caster, radius);
        list.add(placed);
        Zones.start(level, center, caster, 0.0F, ticks, 20, pulse, () -> list.remove(placed), marker);
    }

    /** Adds a transient `amount` of `attribute` to `target` under `id` (replacing an earlier one). */
    private static void modifier(LivingEntity target, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) {
            throw new IllegalStateException(target + " has no " + attribute.getDescriptionId());
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    /** Takes every stack in a player's inventory matching `what` out of it and returns how many items were taken. */
    private static int take(Inventory inventory, Predicate<ItemStack> what) {
        int taken = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && what.test(stack)) {
                taken += stack.getCount();
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        return taken;
    }

    /** A hostile mob not on the caster's side. */
    private static boolean hostile(LivingEntity caster, LivingEntity e) {
        return e instanceof Mob && e instanceof Enemy && !Summons.allied(caster, e);
    }

    // ---- Blacksmith ----

    /** Temper Armour: the caster and the ally looked at (16 blocks) gain +3 armour and +1 toughness for 30 s. */
    public static class TemperArmour extends ModifiedSpell {
        TemperArmour() {
            super("temper_armour", 0);
        }

        static void temper(ServerLevel level, LivingEntity target, int ticks) {
            modifier(target, Attributes.ARMOR, TEMPER_ARMOUR_ID, "mdvlcraft temper armour", 3.0);
            modifier(target, Attributes.ARMOR_TOUGHNESS, TEMPER_TOUGHNESS_ID, "mdvlcraft temper toughness", 1.0);
            Buffs.give(target, TEMPER, ticks);
            Zones.start(level, target.position(), target, 0.0F, ticks, ticks + 1, zone -> {
            }, () -> {
                if (!Buffs.has(target, TEMPER)) {
                    target.getAttribute(Attributes.ARMOR).removeModifier(TEMPER_ARMOUR_ID);
                    target.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifier(TEMPER_TOUGHNESS_ID);
                }
            }, null);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                temper(server, caster, 600);
                LivingEntity looked = Targets.looked(caster, 16.0F);
                if (looked != null && Summons.allied(caster, looked)) {
                    temper(server, looked, 600);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hammer Strike: a blow on the creature looked at (5 blocks) for 150% of the caster's attack damage; it loses 3 armour for 8 s. */
    public static class HammerStrike extends SmiteSpells.Smite {
        HammerStrike() {
            super("hammer_strike", 0, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, MartialSpells.attack(caster) * 1.5F);
            Wave2Guardian.armour(target, SHRED_ID, SHRED, -3.0, 160);
            if (Specs.has(caster, "ringing_blow")) {
                SpecKit.stun(target, 10);
            }
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    /** Forge Heat: players only (mobs are refused): every raw ore and ore block in the inventory is smelted at once. */
    public static class ForgeHeat extends ModifiedSpell {
        ForgeHeat() {
            super("forge_heat", 0);
        }

        static Optional<ItemStack> smelted(Level level, ItemStack stack) {
            if (!stack.is(Tags.Items.RAW_MATERIALS) && !stack.is(Tags.Items.ORES)) {
                return Optional.empty();
            }
            return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level)
                .map((SmeltingRecipe r) -> r.getResultItem(level.registryAccess()));
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster instanceof ServerPlayer player) || player.getInventory().items.stream().noneMatch(s -> smelted(level, s).isPresent())) {
                MartialSpells.refuse(caster, "nothing_to_smelt");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player && level instanceof ServerLevel server) {
                Inventory inventory = player.getInventory();
                List<ItemStack> results = new ArrayList<>();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    Optional<ItemStack> result = smelted(level, stack);
                    if (result.isPresent()) {
                        ItemStack out = result.get().copy();
                        out.setCount(out.getCount() * stack.getCount());
                        results.add(out);
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
                results.forEach(inventory::placeItemBackInInventory);
                server.sendParticles(ParticleTypes.FLAME, caster.getX(), caster.getY(0.6), caster.getZ(), 25, 0.4, 0.5, 0.4, 0.03);
                level.playSound(null, caster.blockPosition(), SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 1.5F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Cleric ----

    /** Purify: the caster and allies within 6 blocks lose every harmful effect. */
    public static class Purify extends ModifiedSpell {
        Purify() {
            super("purify", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 6.0)) {
                ally.getActiveEffects().stream().map(MobEffectInstance::getEffect).filter(e -> e.getCategory() == MobEffectCategory.HARMFUL)
                    .toList().forEach(ally::removeEffect);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.END_ROD);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Sanctuary: a 5-block circle on the caster for 10 s; allies in it heal 1 health a second, and enemies standing in it can't hit anyone. */
    public static class Sanctuary extends ModifiedSpell {
        Sanctuary() {
            super("sanctuary", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = caster.position();
                place(SANCTUARIES, server, at, caster, 5.0, 200, zone -> {
                    GroundSpells.ring(server, at, 5.0, ParticleTypes.END_ROD);
                    Targets.allies(caster, at, 5.0).forEach(a -> a.heal(1.0F));
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Turn Undead: undead enemies within 10 blocks get Weakness II and flee from the caster (and won't target it) for 6 s. */
    public static class TurnUndead extends ModifiedSpell {
        TurnUndead() {
            super("turn_undead", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            List<LivingEntity> turned = Targets.enemies(caster, caster.position(), 10.0).stream().filter(BoltSpells::undead).toList();
            for (LivingEntity undead : turned) {
                Buffs.mark(undead, TURNED, caster, 120);
                undead.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), caster);
                if (Specs.has(caster, "rebuke")) {
                    undead.setSecondsOnFire(5);
                }
            }
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, 0.0F, 120, 10, zone -> {
                    for (LivingEntity undead : turned) {
                        if (!undead.isAlive() || !(undead instanceof PathfinderMob mob)) {
                            continue;
                        }
                        if (mob.getTarget() == caster) {
                            mob.setTarget(null);
                        }
                        Vec3 away = mob.position().subtract(caster.position()).multiply(1.0, 0.0, 1.0);
                        away = (away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize()).scale(8.0);
                        mob.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.3);
                    }
                }, () -> {
                }, null);
                GroundSpells.ring(server, caster.position(), 10.0, ParticleTypes.WAX_OFF);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Cook ----

    /**
     * Feast: a feast (a cake marker) for 20 s; players within 4 blocks get Regeneration, Saturation I, 1 hunger a second and
     * Strength I for 60 s. Saturation acts every tick it lasts, so each second's pulse gives one tick of it.
     */
    static GroundSpells.Area feast() {
        return new GroundSpells.Area("feast", 0, 4.0, 20, 20, false, ParticleTypes.HAPPY_VILLAGER, SoundEvents.GENERIC_EAT,
            (area, zone, allies, enemies) -> {
                for (LivingEntity ally : allies) {
                    if (ally instanceof Player player) {
                        player.getFoodData().eat(1, 0.5F);
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true), zone.caster);
                        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, true), zone.caster);
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0), zone.caster);
                    }
                }
            }).marker(Items.CAKE);
    }

    /** Iron Stomach: Poison, Hunger and Nausea are cured, and can't take hold of the caster for 60 s. */
    public static class IronStomach extends ModifiedSpell {
        IronStomach() {
            super("iron_stomach", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            STOMACH_PROOF.forEach(caster::removeEffect);
            Buffs.give(caster, IRON_STOMACH, 1200);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, caster.getX(), caster.getY(0.6), caster.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Wild Cooking (Spice Mix's spell): opens the caster's own cooking pot screen anywhere (content/WildCooking). */
    public static class SpiceMix extends ModifiedSpell {
        SpiceMix() {
            super("spice_mix", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player) {
                WildCooking.open(player);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Farmer ----

    /**
     * Bountiful Harvest: every crop (crops, stems, berry bushes, nether wart, cocoa) within 6 blocks grows one stage. A pitcher
     * crop grows through its own bone-meal step (one stage, both halves), the other crops by their age property.
     */
    public static class BountifulHarvest extends ModifiedSpell {
        BountifulHarvest() {
            super("bountiful_harvest", 0);
        }

        static boolean crop(BlockState state) {
            return state.is(BlockTags.CROPS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.NETHER_WART) || state.is(Blocks.COCOA);
        }

        static boolean grow(ServerLevel level, BlockPos pos, BlockState state) {
            if (state.getBlock() instanceof PitcherCropBlock pitcher) {
                if (state.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER || !pitcher.isValidBonemealTarget(level, pos, state, false)) {
                    return false;
                }
                pitcher.performBonemeal(level, level.random, pos, state);
                return true;
            }
            IntegerProperty age = state.getProperties().stream().filter(p -> p instanceof IntegerProperty && p.getName().equals("age"))
                .map(p -> (IntegerProperty)p).findFirst().orElse(null);
            if (age == null) {
                return false;
            }
            int now = state.getValue(age);
            if (now >= age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElseThrow()) {
                return false;
            }
            level.setBlock(pos, state.setValue(age, now + 1), Block.UPDATE_CLIENTS);
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                BlockPos center = caster.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -2, -6), center.offset(6, 2, 6))) {
                    BlockState state = server.getBlockState(pos);
                    if (pos.distSqr(center) <= 36 && crop(state) && grow(server, pos.immutable(), state)) {
                        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
                    }
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.2F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Scarecrow: a scarecrow (a carved-pumpkin marker) on the caster's spot for 60 s; hostile mobs within 8 blocks get Slowness I.
     * Mobs can't target a marker, so "target it first" is a lure: a hostile mob is drawn to it once, the first time it comes in
     * reach (it drops its target and walks to the scarecrow for 3 s, taking no other target meanwhile), then fights as usual.
     */
    public static class Scarecrow extends ModifiedSpell {
        Scarecrow() {
            super("scarecrow", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = caster.position();
                place(SCARECROWS, server, at, caster, 8.0, 1200, zone -> {
                    GroundSpells.ring(server, at, 1.0, ParticleTypes.CRIT);
                    for (LivingEntity enemy : Targets.enemies(caster, at, 8.0)) {
                        if (hostile(caster, enemy) && enemy instanceof PathfinderMob mob) {
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0), caster);
                            if (!Buffs.has(mob, SCARECROW_SEEN)) {
                                Buffs.give(mob, SCARECROW_SEEN, 1200);
                                Buffs.give(mob, SCARECROW_LURED, 60);
                            }
                            if (Buffs.has(mob, SCARECROW_LURED)) {
                                mob.setTarget(null);
                                mob.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
                            }
                        }
                    }
                }, GroundSpells.marker(server, at, Items.CARVED_PUMPKIN));
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.2F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Compost: players only (mobs are refused): every rotten flesh, seed and sapling in the inventory becomes one bone meal. */
    public static class Compost extends ModifiedSpell {
        Compost() {
            super("compost", 0);
        }

        static boolean compostable(ItemStack stack) {
            return stack.is(Items.ROTTEN_FLESH) || stack.is(Tags.Items.SEEDS) || stack.is(ItemTags.SAPLINGS);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster instanceof ServerPlayer player) || player.getInventory().items.stream().noneMatch(Compost::compostable)) {
                MartialSpells.refuse(caster, "nothing_to_compost");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player && level instanceof ServerLevel server) {
                int count = take(player.getInventory(), Compost::compostable);
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.BONE_MEAL, count));
                if (Specs.has(caster, "rich_soil")) {
                    JobSpecs.richSoil(server, caster.blockPosition());
                }
                server.sendParticles(ParticleTypes.COMPOSTER, caster.getX(), caster.getY(0.6), caster.getZ(), 20, 0.4, 0.5, 0.4, 0.0);
                level.playSound(null, caster.blockPosition(), SoundEvents.COMPOSTER_READY, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Merchant ----

    /** Coin Toss: a coin is flipped; heads gives the caster Luck II and Speed I for 20 s, tails throws it as a bolt dealing 6 damage. */
    static BoltSpells.Bolt coinToss() {
        return new BoltSpells.Bolt("coin_toss", 6, Items.GOLD_NUGGET, ParticleTypes.WAX_ON,
            (bolt, caster, target, power) -> bolt.hurt(caster, target, power), null) {
            @Override
            void release(Level level, LivingEntity caster, float power) {
                if (caster.getRandom().nextBoolean()) {
                    caster.addEffect(new MobEffectInstance(MobEffects.LUCK, 400, 1));
                    caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
                    if (level instanceof ServerLevel server) {
                        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)), caster.getX(),
                            caster.getY() + 2.2, caster.getZ(), 12, 0.1, 0.2, 0.1, 0.05);
                    }
                    level.playSound(null, caster.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                } else {
                    super.release(level, caster, power);
                }
            }
        }.sound(SoundEvents.CHAIN_STEP);
    }

    /** Bribe: players only (mobs are refused): 3 emeralds are spent and the hostile mob looked at (16 blocks) won't target or hit the caster for 15 s. */
    public static class Bribe extends ModifiedSpell {
        Bribe() {
            super("bribe", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!(caster instanceof ServerPlayer player) || player.getInventory().countItem(Items.EMERALD) < 3) {
                MartialSpells.refuse(caster, "need_emeralds");
                return false;
            }
            LivingEntity target = Targets.looked(caster, 16.0F);
            if (target == null || !hostile(caster, target)) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 16.0F);
            if (caster instanceof ServerPlayer player && level instanceof ServerLevel server && target instanceof Mob mob) {
                player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.EMERALD), 3, player.inventoryMenu.getCraftSlots());
                Buffs.mark(mob, BRIBED, caster, 300);
                if (mob.getTarget() == caster) {
                    mob.setTarget(null);
                }
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EMERALD)), mob.getX(), mob.getY(1.0),
                    mob.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, mob.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Caravan Guard: an allied iron golem for 30 s. */
    public static class CaravanGuard extends SummonSpells.SummonSpell {
        CaravanGuard() {
            super("caravan_guard", 0);
        }

        @Override
        protected int duration() {
            return 600;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.IRON_GOLEM, Specs.has(caster, "hired_swords") ? 2 : 1, castData, (IronGolem golem) -> golem.setPlayerCreated(true));
        }
    }

    // ---- Rancher ----

    /** Herd Call: every farm animal (any animal but a tamable pet) within 24 blocks walks to the caster, following it for 10 s. */
    public static class HerdCall extends ModifiedSpell {
        HerdCall() {
            super("herd_call", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                double reach = Specs.has(caster, "long_call") ? 48.0 : 24.0;
                List<Animal> herd = server.getEntitiesOfClass(Animal.class, caster.getBoundingBox().inflate(reach),
                    a -> a.isAlive() && !(a instanceof TamableAnimal) && a.distanceToSqr(caster) <= reach * reach);
                Zones.start(server, caster.position(), caster, 0.0F, 200, 20, zone -> {
                    for (Animal animal : herd) {
                        if (animal.isAlive() && animal.distanceToSqr(caster) > 9.0) {
                            animal.getNavigation().moveTo(caster, 1.2);
                        }
                    }
                }, () -> {
                }, null);
                server.sendParticles(ParticleTypes.NOTE, caster.getX(), caster.getY() + 2.2, caster.getZ(), 6, 0.4, 0.2, 0.4, 1.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.PLAYERS, 1.5F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Fatten Up: baby animals within 8 blocks grow up at once. */
    public static class FattenUp extends ModifiedSpell {
        FattenUp() {
            super("fatten_up", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (Animal baby : server.getEntitiesOfClass(Animal.class, caster.getBoundingBox().inflate(8.0),
                    a -> a.isAlive() && a.isBaby() && a.distanceToSqr(caster) <= 64.0)) {
                    baby.setAge(0);
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, baby.getX(), baby.getY(0.6), baby.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Branding Iron: brands the creature looked at (24 blocks): it glows for 60 s, and if the caster kills it, it drops double loot. */
    public static class BrandingIron extends Wave2Assassin.Mark {
        BrandingIron() {
            super("branding_iron");
        }

        @Override
        void marked(ServerLevel level, LivingEntity caster, LivingEntity target) {
            Buffs.mark(target, BRAND, caster, 1200);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0), caster);
            level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(0.8), target.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new TemperArmour(),
            new HammerStrike(),
            new ForgeHeat(),
            new Purify(),
            new Sanctuary(),
            new TurnUndead(),
            feast(),
            new IronStomach(),
            new SpiceMix(),
            new BountifulHarvest(),
            new Scarecrow(),
            new Compost(),
            coinToss(),
            new Bribe(),
            new CaravanGuard(),
            new HerdCall(),
            new FattenUp(),
            new BrandingIron());
    }

    // ---- event handlers ----

    /** Turned undead won't target their cleric, bribed mobs their briber, and mobs a scarecrow is luring anyone. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null || mob.level().isClientSide()) {
            return;
        }
        if (Buffs.markedBy(mob, TURNED) == target || Buffs.markedBy(mob, BRIBED) == target
            || Buffs.has(mob, SCARECROW_LURED)) {
            event.setCanceled(true);
        }
    }

    /** Sanctuary: enemies standing in one can't hit anyone; Bribe: a bribed mob can't hit its briber. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }
        if (Buffs.markedBy(attacker, BRIBED) == victim
            || SANCTUARIES.stream().anyMatch(s -> s.reaches(attacker) && !Summons.allied(s.caster(), attacker))) {
            event.setCanceled(true);
        }
    }

    /** Iron Stomach: Poison, Hunger and Nausea can't take hold. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (STOMACH_PROOF.contains(event.getEffectInstance().getEffect()) && Buffs.has(event.getEntity(), IRON_STOMACH)) {
            event.setResult(Result.DENY);
        }
    }

    /** Branding Iron: a branded creature killed by its brander drops everything twice. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity brander = Buffs.markedBy(victim, BRAND);
        if (brander == null || event.getSource().getEntity() != brander) {
            return;
        }
        for (ItemEntity drop : List.copyOf(event.getDrops())) {
            event.getDrops().add(new ItemEntity(victim.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
        }
    }
}
