package com.mdvlcraft.binder.specs;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import com.mdvlcraft.binder.modified.Summons;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Job specialisations (Specs): helpers the job abilities call into, and the specs that work on their own (events). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class JobSpecs {
    private static final UUID STRONG_STROKE = UUID.fromString("6a1f3c2e-8b4d-4e5f-9a7c-2d1b0e3f4a5c");
    private static final ResourceLocation NOURISHMENT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment");
    private static final ResourceLocation COMFORT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "comfort");
    /** True while Tolerance or Hearty Meal re-applies a shortened or lengthened effect. */
    private static boolean reapplying;
    /** True while Scythe Swing hits the enemies beside the target. */
    private static boolean sweeping;
    private JobSpecs() {
    }

    /** Barbed Hook: 1 damage a second for `seconds`. */
    public static void bleed(LivingEntity by, LivingEntity target, int seconds) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        for (int s = 1; s <= seconds; s++) {
            SpecKit.later(level, s * 20, () -> {
                if (target.isAlive()) {
                    target.invulnerableTime = 0;
                    target.hurt(by.damageSources().indirectMagic(by, by), 1.0F);
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.6), target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                }
            });
        }
    }

    /**
     * Rich Soil: for 5 minutes, crops within 6 blocks of `centre` grow 20% faster: each second every crop gets an extra
     * random tick with a fifth of the chance the game gives it (3 in 4096 a tick, so about 1.5% a second).
     */
    public static void richSoil(ServerLevel level, BlockPos centre) {
        SpecKit.zone(level, centre.getCenter(), 6.0, 6000, ParticleTypes.HAPPY_VILLAGER, zone -> {
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-6, -2, -6), centre.offset(6, 2, 6))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.CROPS) && state.isRandomlyTicking() && level.random.nextFloat() < 0.003F) {
                    state.randomTick(level, pos.immutable(), level.random);
                }
            }
        });
    }

    // ---- crafting and repairs: Field Repairs, Well Made, Sawmill, Master Stew ----

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack out = event.getOutput();
        ItemStack before = event.getLeft();
        if (Specs.has(event.getEntity(), "field_repairs") && out.isDamageableItem() && before.isDamageableItem()) {
            int repaired = before.getDamageValue() - out.getDamageValue();
            if (repaired > 0) {
                out.setDamageValue(Math.max(0, out.getDamageValue() - repaired / 4));
            }
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        ItemStack result = event.getCrafting();
        Container grid = event.getInventory();
        if (player.level().isClientSide) {
            return;
        }
        if (result.isDamageableItem() && Specs.has(player, "field_repairs")) {
            int best = 0;
            int same = 0;
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack in = grid.getItem(i);
                if (in.is(result.getItem())) {
                    same++;
                    best = Math.max(best, in.getMaxDamage() - in.getDamageValue());
                }
            }
            int repaired = result.getMaxDamage() - result.getDamageValue() - best;
            if (same >= 2 && repaired > 0) {
                result.setDamageValue(Math.max(0, result.getDamageValue() - repaired / 4));
            }
        }
        if (result.isDamageableItem() && Specs.has(player, "well_made")) {
            result.getOrCreateTag().putBoolean("mdvlcraft_well_made", true);
        }
        if (result.is(Items.SUSPICIOUS_STEW) && Specs.has(player, "master_stew") && result.getTag() != null) {
            for (Tag effect : result.getTag().getList("Effects", Tag.TAG_COMPOUND)) {
                CompoundTag one = (CompoundTag)effect;
                one.putInt("EffectDuration", one.getInt("EffectDuration") * 2);
            }
        }
        if (result.is(ItemTags.PLANKS) && Specs.has(player, "sawmill")) {
            for (int i = 0; i < grid.getContainerSize(); i++) {
                if (grid.getItem(i).is(ItemTags.LOGS)) {
                    ItemStack extra = new ItemStack(result.getItem());
                    if (!player.getInventory().add(extra)) {
                        player.drop(extra, false);
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getSmelting().isEdible() && Specs.has(event.getEntity(), "spice_rack")) {
            event.getSmelting().getOrCreateTag().putBoolean("mdvlcraft_spiced", true);
        }
    }

    // ---- eating and drinking: Spice Rack, Royal Jelly, Blessed Brew ----

    @SubscribeEvent
    public static void onFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item.getTag() != null && item.getTag().getBoolean("mdvlcraft_spiced") && Specs.has(player, "spice_rack")) {
            FoodProperties food = item.getFoodProperties(player);
            if (food != null) {
                FoodData data = player.getFoodData();
                data.setSaturation(Math.min(data.getFoodLevel(), data.getSaturationLevel() + food.getNutrition() * food.getSaturationModifier() * 0.2F));
            }
        }
        if (item.is(Items.HONEY_BOTTLE) && Specs.has(player, "royal_jelly")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        }
    }

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getDuration() <= 1 && event.getItem().getItem() instanceof PotionItem && Specs.has(event.getEntity(), "blessed_brew")
            && PotionUtils.getMobEffects(event.getItem()).stream().anyMatch(e -> e.getEffect() == MobEffects.HEAL)) {
            event.getEntity().getPersistentData().putLong("mdvlcraft_blessed_brew", event.getEntity().level().getGameTime() + 2);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().getLong("mdvlcraft_blessed_brew") >= entity.level().getGameTime()) {
            event.setAmount(event.getAmount() * 1.25F);
            entity.getPersistentData().remove("mdvlcraft_blessed_brew");
        }
    }

    // ---- effects: Tolerance, Antidote, Hearty Meal ----

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (reapplying || !(event.getEntity() instanceof Player player)) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        if (effect.getEffect() == MobEffects.POISON && effect.getAmplifier() == 0 && Specs.has(player, "antidote")) {
            event.setResult(Event.Result.DENY);
            return;
        }
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
        float factor = 1.0F;
        if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && Specs.has(player, "tolerance")) {
            factor = 0.75F;
        } else if ((NOURISHMENT.equals(id) || COMFORT.equals(id)) && Specs.has(player, "hearty_meal")) {
            factor = 1.25F;
        }
        if (factor != 1.0F && effect.getDuration() > 1 && !effect.isInfiniteDuration()) {
            event.setResult(Event.Result.DENY);
            reapplying = true;
            try {
                player.addEffect(new MobEffectInstance(effect.getEffect(), Math.max(1, Math.round(effect.getDuration() * factor)), effect.getAmplifier(),
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            } finally {
                reapplying = false;
            }
        }
    }

    // ---- fights: Scythe Swing, Kitchen Knife, Angry Swarm ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (source instanceof Player player && SpecKit.melee(event.getSource(), player)) {
            if (Specs.has(player, "kitchen_knife") && SpecKit.named(player.getMainHandItem(), "knife", "cleaver")) {
                event.setAmount(event.getAmount() * 1.15F);
            }
            if (!sweeping && player.getMainHandItem().getItem() instanceof HoeItem && Specs.has(player, "scythe_swing")) {
                Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                sweeping = true;
                try {
                    for (LivingEntity other : SpecKit.enemies(player, player.position(), 3.0)) {
                        Vec3 to = other.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
                        if (other != victim && to.dot(look) > 0.3) {
                            other.hurt(player.damageSources().playerAttack(player), event.getAmount());
                        }
                    }
                } finally {
                    sweeping = false;
                }
            }
        }
        if (source instanceof Bee bee && "swarm".equals(Summons.kind(bee)) && SpecKit.owner(bee) instanceof Player owner && Specs.has(owner, "angry_swarm")) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }
    }

    // ---- animals: Farmhand, Quick Breeding, Shearer ----

    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent event) {
        Player player = event.getCausedByPlayer();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (Specs.has(player, "farmhand") && event.getParentA() != null) {
            level.addFreshEntity(new ExperienceOrb(level, event.getParentA().getX(), event.getParentA().getY(), event.getParentA().getZ(), 1 + level.random.nextInt(3)));
        }
        if (Specs.has(player, "quick_breeding")) {
            SpecKit.later(level, 1, () -> {
                for (Mob parent : new Mob[]{event.getParentA(), event.getParentB()}) {
                    if (parent instanceof Animal animal && animal.getAge() > 3000) {
                        animal.setAge(3000);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Sheep sheep && event.getItemStack().getItem() instanceof ShearsItem && sheep.readyForShearing()
            && Specs.has(event.getEntity(), "shearer") && sheep.level() instanceof ServerLevel level) {
            SpecKit.later(level, 1, () -> {
                if (sheep.isSheared()) {
                    sheep.spawnAtLocation(new ItemStack(WOOL.get(sheep.getColor())), 1);
                }
            });
        }
    }

    // ---- blocks: Cave Dweller, Gem Cutter, Orchard Keeper ----

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.getY() < player.level().getSeaLevel() && Specs.has(player, "cave_dweller")) {
            event.setNewSpeed(event.getNewSpeed() * 1.1F);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        boolean silk = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, player.getMainHandItem()) > 0;
        if (!silk && Specs.has(player, "gem_cutter") && level.random.nextFloat() < 0.1F) {
            if (state.is(Tags.Blocks.ORES_DIAMOND)) {
                Block.popResource(level, event.getPos(), new ItemStack(Items.DIAMOND));
            } else if (state.is(Tags.Blocks.ORES_EMERALD)) {
                Block.popResource(level, event.getPos(), new ItemStack(Items.EMERALD));
            }
        }
        if ((state.is(Blocks.OAK_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES)) && !silk && !(player.getMainHandItem().getItem() instanceof ShearsItem)
            && Specs.has(player, "orchard_keeper") && level.random.nextFloat() < 0.005F) {
            Block.popResource(level, event.getPos(), new ItemStack(Items.APPLE));
        }
    }

    // ---- brewing, potions, trading, fishing, experience ----

    @SubscribeEvent
    public static void onBrewed(PlayerBrewedPotionEvent event) {
        Player player = event.getEntity();
        if (Specs.has(player, "double_batch") && player.getRandom().nextFloat() < 0.15F) {
            ItemStack extra = event.getStack().copy();
            if (!player.getInventory().add(extra)) {
                player.drop(extra, false);
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion && !event.loadedFromDisk() && potion.getOwner() instanceof Player owner
            && Specs.has(owner, "strong_arm")) {
            potion.setDeltaMovement(potion.getDeltaMovement().scale(1.5));
        }
    }

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof MerchantMenu menu && Specs.has(event.getEntity(), "haggler")) {
            for (MerchantOffer offer : menu.getOffers()) {
                if (offer.getBaseCostA().is(Items.EMERALD)) {
                    offer.addToSpecialPriceDiff(-1);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        FishingHook hook = event.getHookEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        float chance = (Specs.has(player, "sea_bounty") && player.getVehicle() instanceof Boat ? 0.05F : 0.0F)
            + (Specs.has(player, "lucky_line") ? 0.03F : 0.0F);
        if (chance <= 0.0F || level.random.nextFloat() >= chance) {
            return;
        }
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, hook.position())
            .withParameter(LootContextParams.TOOL, player.getMainHandItem()).withParameter(LootContextParams.THIS_ENTITY, hook)
            .withLuck(player.getLuck()).create(LootContextParamSets.FISHING);
        List<ItemStack> treasure = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING_TREASURE).getRandomItems(params);
        if (!treasure.isEmpty()) {
            event.setCanceled(true);
            treasure.forEach(stack -> {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            });
            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ(), 1 + level.random.nextInt(6)));
        }
    }

    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (event.getAmount() <= 0 || !Specs.has(player, "inspiring_tale")) {
            return;
        }
        boolean company = !player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(16.0), other -> other != player).isEmpty();
        if (company) {
            float extra = event.getAmount() * 0.05F;
            event.setAmount(event.getAmount() + (int)extra + (player.getRandom().nextFloat() < extra % 1.0F ? 1 : 0));
        }
    }

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (event.getSpellId().equals("mdvlcraft:deep_breath") && Specs.has(event.getEntity(), "pearl_diver")) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
        }
    }

    /** Grave Robber: the first opening of a grave or tomb chest gives one more roll of its loot. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || !Specs.has(player, "grave_robber")) {
            return;
        }
        BlockEntity block = level.getBlockEntity(event.getPos());
        if (!(block instanceof RandomizableContainerBlockEntity)) {
            return;
        }
        String table = block.saveWithoutMetadata().getString("LootTable");
        if (table.isEmpty() || !(table.contains("tomb") || table.contains("grave") || table.contains("crypt") || table.contains("catacomb"))) {
            return;
        }
        String key = level.dimension().location() + "@" + event.getPos().asLong();
        ListTag robbed = player.getPersistentData().getList("mdvlcraft_robbed", Tag.TAG_STRING);
        for (Tag tag : robbed) {
            if (tag.getAsString().equals(key)) {
                return;
            }
        }
        robbed.add(StringTag.valueOf(key));
        if (robbed.size() > 200) {
            robbed.remove(0);
        }
        player.getPersistentData().put("mdvlcraft_robbed", robbed);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, event.getPos().getCenter())
            .withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
        List<ItemStack> loot = level.getServer().getLootData().getLootTable(ResourceLocation.parse(table)).getRandomItems(params);
        if (!loot.isEmpty()) {
            ItemStack one = loot.get(level.random.nextInt(loot.size()));
            if (!player.getInventory().add(one)) {
                player.drop(one, false);
            }
        }
    }

    // ---- every second: Strong Stroke, Pollinator, Forager's Eye, Soul Lantern, Regular Customer; Climber on the client ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (player.level().isClientSide) {
            if (player.isLocalPlayer() && player.onClimbable() && player.getDeltaMovement().y > 0.0 && Specs.has(player, "climber")) {
                player.move(MoverType.SELF, new Vec3(0.0, 0.1, 0.0));
            }
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0) {
            return;
        }
        AttributeInstance swim = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        boolean stroke = Specs.has(player, "strong_stroke");
        if (swim != null && stroke && swim.getModifier(STRONG_STROKE) == null) {
            swim.addTransientModifier(new AttributeModifier(STRONG_STROKE, "mdvlcraft strong_stroke", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (swim != null && !stroke && swim.getModifier(STRONG_STROKE) != null) {
            swim.removeModifier(STRONG_STROKE);
        }
        BlockPos at = player.blockPosition();
        if (Specs.has(player, "pollinator")) {
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-6, -2, -6), at.offset(6, 2, 6))) {
                BlockState state = level.getBlockState(pos);
                if ((state.is(BlockTags.CROPS) || state.is(BlockTags.FLOWERS)) && state.isRandomlyTicking() && level.random.nextFloat() < 0.002F) {
                    state.randomTick(level, pos.immutable(), level.random);
                }
            }
        }
        if (player.isShiftKeyDown() && player instanceof ServerPlayer server && Specs.has(player, "forager_s_eye")) {
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-8, -3, -8), at.offset(8, 3, 8))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.FERN)) {
                    level.sendParticles(server, ParticleTypes.HAPPY_VILLAGER, true, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.0);
                }
            }
        }
        if ((player.getMainHandItem().is(Items.LANTERN) || player.getMainHandItem().is(Items.SOUL_LANTERN)
            || player.getOffhandItem().is(Items.LANTERN) || player.getOffhandItem().is(Items.SOUL_LANTERN)) && Specs.has(player, "soul_lantern")) {
            for (LivingEntity undead : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0), e -> e.getMobType() == MobType.UNDEAD)) {
                undead.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
            }
        }
        if (Specs.has(player, "regular_customer")) {
            long day = level.getDayTime() / 24000L;
            CompoundTag data = player.getPersistentData();
            if (data.getLong("mdvlcraft_customer_day") != day) {
                data.putLong("mdvlcraft_customer_day", day);
                if (level.random.nextFloat() < 0.25F) {
                    trader(level, player);
                }
            }
        }
    }

    /** Regular Customer: a wandering trader walks up within 16 blocks. */
    private static void trader(ServerLevel level, Player player) {
        for (int tries = 0; tries < 10; tries++) {
            BlockPos column = player.blockPosition().offset(level.random.nextInt(25) - 12, 0, level.random.nextInt(25) - 12);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (Math.abs(pos.getY() - player.getY()) < 8 && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                WanderingTrader trader = EntityType.WANDERING_TRADER.create(level);
                if (trader != null) {
                    trader.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
                    trader.setDespawnDelay(48000);
                    trader.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
                    level.addFreshEntity(trader);
                }
                return;
            }
        }
    }

    private static final Map<DyeColor, Item> WOOL = Map.ofEntries(
        Map.entry(DyeColor.WHITE, Items.WHITE_WOOL), Map.entry(DyeColor.ORANGE, Items.ORANGE_WOOL), Map.entry(DyeColor.MAGENTA, Items.MAGENTA_WOOL),
        Map.entry(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL), Map.entry(DyeColor.YELLOW, Items.YELLOW_WOOL), Map.entry(DyeColor.LIME, Items.LIME_WOOL),
        Map.entry(DyeColor.PINK, Items.PINK_WOOL), Map.entry(DyeColor.GRAY, Items.GRAY_WOOL), Map.entry(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL),
        Map.entry(DyeColor.CYAN, Items.CYAN_WOOL), Map.entry(DyeColor.PURPLE, Items.PURPLE_WOOL), Map.entry(DyeColor.BLUE, Items.BLUE_WOOL),
        Map.entry(DyeColor.BROWN, Items.BROWN_WOOL), Map.entry(DyeColor.GREEN, Items.GREEN_WOOL), Map.entry(DyeColor.RED, Items.RED_WOOL),
        Map.entry(DyeColor.BLACK, Items.BLACK_WOOL));
}
