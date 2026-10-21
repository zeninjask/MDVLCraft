package com.mdvlcraft.binder.content;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** The behaviour behind the pack's own races' and classes' marker powers (data/mdvlcraft/powers). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class OriginTraits {
    private static final ResourceLocation INFAMY = power("infernal/infamy");
    private static final ResourceLocation OUTSIDER = power("orc/outsider");
    private static final ResourceLocation INTIMIDATING = power("orc/intimidating");
    private static final ResourceLocation GREEN_THUMB = power("dryad/green_thumb");
    private static final ResourceLocation HOARDER = power("kobold/hoarder");
    private static final ResourceLocation BREWERS_INSIGHT = power("alchemist/brewers_insight");
    private static final ResourceLocation HELMSMAN = power("sailor/helmsman");
    private static final ResourceLocation ANGLERS_PATIENCE = power("fisher/anglers_patience");
    private static final ResourceLocation HORSE_WHISPERER = power("cavalry/horse_whisperer");
    private static final ResourceLocation GATHERER = power("herbalist/gatherer");
    private static final ResourceLocation STEEPED = power("herbalist/steeped");
    private static final ResourceLocation FRIEND_OF_BEES = power("beekeeper/friend_of_bees");
    private static final ResourceLocation RICH_HIVES = power("beekeeper/rich_hives");
    private static final ResourceLocation GRAVE_ROBBER = power("gravedigger/grave_robber");
    private static final ResourceLocation QUICK_LEARNER = power("human/quick_learner");
    private static final ResourceLocation CULTIVATOR = power("herbalist/cultivator");
    private static final ResourceLocation HONEYED = power("beekeeper/honeyed");
    private static final ResourceLocation RESTOCKING = ResourceLocation.fromNamespaceAndPath("origins-classes", "trade_availability");
    private static final String RESTOCK_KEY = "mdvlcraft_merchant_restock";
    private static final long RESTOCK_TICKS = 12000L;

    /** Minor negative gossip each village holds about these players: Infernals reach the -100 that turns iron golems hostile. */
    private static final int INFAMY_GOSSIP = 150;
    private static final int OUTSIDER_GOSSIP = 40;
    private static final double HELMSMAN_BONUS = 0.15;
    private static final UUID HORSE_SPEED = UUID.fromString("5d0f3a62-8c1b-4e0e-9a4e-2f6b7d1c3a01");
    private static final UUID HORSE_JUMP = UUID.fromString("5d0f3a62-8c1b-4e0e-9a4e-2f6b7d1c3a02");
    private static final UUID HORSE_HEALTH = UUID.fromString("5d0f3a62-8c1b-4e0e-9a4e-2f6b7d1c3a03");
    private static final List<TagKey<Block>> HOARD_ORES = List.of(
        BlockTags.GOLD_ORES, BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES, BlockTags.LAPIS_ORES);

    private OriginTraits() {
    }

    private static ResourceLocation power(String path) {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", path);
    }

    public static boolean has(Entity entity, ResourceLocation power) {
        return IPowerContainer.get(entity).map(container -> container.hasPower(power)).orElse(false);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }

        Player player = event.player;
        sail(player);
        if (player.level() instanceof ServerLevel level && player.tickCount % 40 == 0) {
            if (has(player, INFAMY)) {
                spreadGossip(player, level, INFAMY_GOSSIP);
            } else if (has(player, OUTSIDER)) {
                spreadGossip(player, level, OUTSIDER_GOSSIP);
            }

            if (player.tickCount % 100 == 0 && has(player, GREEN_THUMB)) {
                tendGarden(player, level);
            }
        }
    }

    /** Ships steer like boats: whichever side controls the vessel moves it, so the extra distance is added there. */
    private static void sail(Player player) {
        Entity vessel = player.getRootVehicle();
        if (vessel != player && vessel.isControlledByLocalInstance() && vessel.getFirstPassenger() == player
            && ForgeRegistries.ENTITY_TYPES.getKey(vessel.getType()).getNamespace().equals("alekiships") && has(player, HELMSMAN)) {
            Vec3 motion = vessel.getDeltaMovement();
            vessel.move(MoverType.SELF, new Vec3(motion.x * HELMSMAN_BONUS, 0.0, motion.z * HELMSMAN_BONUS));
        }
    }

    private static void spreadGossip(Player player, ServerLevel level, int target) {
        for (Villager villager : level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16.0))) {
            int current = -villager.getGossips().getReputation(player.getUUID(), type -> type == GossipType.MINOR_NEGATIVE);
            if (current < target) {
                villager.getGossips().add(player.getUUID(), GossipType.MINOR_NEGATIVE, target - current);
            }
        }
    }

    /** Each pass gives four random growing plants within four blocks an extra growth tick. */
    private static void tendGarden(Player player, ServerLevel level) {
        BlockPos origin = player.blockPosition();
        for (int i = 0; i < 4; i++) {
            BlockPos pos = origin.offset(level.random.nextInt(9) - 4, level.random.nextInt(5) - 2, level.random.nextInt(9) - 4);
            BlockState state = level.getBlockState(pos);
            if (state.isRandomlyTicking() && (state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS))) {
                state.randomTick(level, pos, level.random);
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractIllager illager && fearful(illager)) {
            illager.goalSelector.addGoal(1, new AvoidEntityGoal<>(illager, Player.class, 10.0F, 1.0, 1.2, target -> has(target, INTIMIDATING)));
        }
    }

    /** Three in ten illagers lose their nerve around an Orc; which ones is fixed per illager. */
    private static boolean fearful(AbstractIllager illager) {
        return Math.floorMod(illager.getUUID().getLeastSignificantBits(), 10) < 3;
    }

    @SubscribeEvent
    public static void onBreak(BreakEvent event) {
        Player player = event.getPlayer();
        BlockState state = event.getState();
        if (!(event.getLevel() instanceof ServerLevel level) || player.isCreative() || !player.hasCorrectToolForDrops(state)) {
            return;
        }

        boolean hoard = HOARD_ORES.stream().anyMatch(state::is) && has(player, HOARDER) && level.random.nextFloat() < 0.25F;
        boolean gather = (state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.TALL_FLOWERS)) && has(player, GATHERER) && level.random.nextFloat() < 0.5F;
        if (hoard || gather) {
            BlockPos pos = event.getPos();
            Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem())
                .forEach(stack -> Block.popResource(level, pos, stack));
        }

        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state) && has(player, CULTIVATOR) && level.random.nextFloat() < 0.25F) {
            BlockPos pos = event.getPos();
            Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem()).stream()
                .filter(stack -> stack.is(crop.asItem()))
                .forEach(stack -> Block.popResource(level, pos, stack));
        }
    }

    /** Bone meal on a small flower (which vanilla ignores) grows a copy of it. */
    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (event.getBlock().is(BlockTags.SMALL_FLOWERS) && has(event.getEntity(), CULTIVATOR)) {
            if (event.getLevel() instanceof ServerLevel level) {
                Block.popResource(level, event.getPos(), new ItemStack(event.getBlock().getBlock()));
            }
            event.setResult(Result.ALLOW);
        }
    }

    /** Experience gains are 10% larger; the fraction rounds up by chance so small orbs still count. */
    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        int amount = event.getAmount();
        if (amount > 0 && has(event.getEntity(), QUICK_LEARNER)) {
            float bonus = amount * 0.1F;
            int whole = (int)bonus;
            event.setAmount(amount + whole + (event.getEntity().getRandom().nextFloat() < bonus - whole ? 1 : 0));
        }
    }

    /** A Merchant's villagers refill their sold-out trades when spoken to, at most once per 10 minutes each. */
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof Villager villager) || !has(event.getEntity(), RESTOCKING)) {
            return;
        }

        long now = event.getLevel().getGameTime();
        CompoundTag data = villager.getPersistentData();
        if (data.contains(RESTOCK_KEY) && now - data.getLong(RESTOCK_KEY) < RESTOCK_TICKS) {
            return;
        }

        if (villager.getOffers().stream().anyMatch(MerchantOffer::isOutOfStock)) {
            villager.getOffers().forEach(MerchantOffer::resetUses);
            data.putLong(RESTOCK_KEY, now);
        }
    }

    @SubscribeEvent
    public static void onBrew(PlayerBrewedPotionEvent event) {
        if (!event.getEntity().level().isClientSide() && has(event.getEntity(), BREWERS_INSIGHT)) {
            event.getEntity().giveExperiencePoints(2);
        }
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (!event.getDrops().isEmpty() && has(player, ANGLERS_PATIENCE) && player.getRandom().nextFloat() < 0.25F) {
            ItemStack extra = event.getDrops().get(player.getRandom().nextInt(event.getDrops().size())).copy();
            ItemHandlerHelper.giveItemToPlayer(player, extra);
        }
    }

    /** Angler's Patience works like one more level of Lure. */
    public static int lure(Player player, int lureSpeed) {
        return player != null && has(player, ANGLERS_PATIENCE) ? lureSpeed + 1 : lureSpeed;
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getAnimal() instanceof AbstractHorse horse && has(event.getTamer(), HORSE_WHISPERER)) {
            boost(horse, Attributes.MOVEMENT_SPEED, HORSE_SPEED);
            boost(horse, Attributes.JUMP_STRENGTH, HORSE_JUMP);
            boost(horse, Attributes.MAX_HEALTH, HORSE_HEALTH);
            horse.setHealth(horse.getMaxHealth());
        }
    }

    private static void boost(AbstractHorse horse, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id) {
        AttributeInstance instance = horse.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(id, "Horse Whisperer", 0.1, Operation.MULTIPLY_BASE));
        }
    }

    /** Suspicious stew has already applied its effects when eating finishes; they are re-applied at double length. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        LivingEntity eater = event.getEntity();
        ItemStack stew = event.getItem();
        if (!eater.level().isClientSide() && stew.is(Items.HONEY_BOTTLE) && has(eater, HONEYED)) {
            eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            return;
        }

        if (eater.level().isClientSide() || !stew.is(Items.SUSPICIOUS_STEW) || !has(eater, STEEPED) || stew.getTag() == null) {
            return;
        }

        ListTag effects = stew.getTag().getList("Effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effects.size(); i++) {
            CompoundTag entry = effects.getCompound(i);
            MobEffect effect = MobEffect.byId(entry.getInt("EffectId"));
            int duration = entry.contains("EffectDuration", Tag.TAG_ANY_NUMERIC) ? entry.getInt("EffectDuration") : 160;
            if (effect != null) {
                eater.addEffect(new MobEffectInstance(effect, duration * 2));
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Bee && has(event.getEntity(), FRIEND_OF_BEES)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BlockState state = level.getBlockState(event.getPos());
            ItemStack held = event.getItemStack();
            if (state.getBlock() instanceof BeehiveBlock && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS
                && (held.is(Items.SHEARS) || held.is(Items.GLASS_BOTTLE)) && has(event.getEntity(), RICH_HIVES)) {
                Block.popResource(level, event.getPos().above(), new ItemStack(Items.HONEYCOMB, 1 + level.random.nextInt(3)));
            }
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.getMobType() == MobType.UNDEAD && !event.getDrops().isEmpty() && event.getSource().getEntity() instanceof Player killer
            && has(killer, GRAVE_ROBBER) && dead.getRandom().nextFloat() < 0.3F) {
            ItemEntity pick = event.getDrops().stream().skip(dead.getRandom().nextInt(event.getDrops().size())).findFirst().orElseThrow();
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), pick.getItem().copy()));
        }
    }
}
