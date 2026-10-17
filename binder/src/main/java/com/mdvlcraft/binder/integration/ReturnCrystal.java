package com.mdvlcraft.binder.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The Cataclysm Dimension eyes send a player into a boss dimension with no way back. Every boss killed there drops a
 * Return Crystal (loot modifier {@code dimension_boss_return_crystal}), which takes its user back to where they stood
 * when they used the eye.
 */
public final class ReturnCrystal {
    public static final String DIMENSION_MOD = "cataclysm_dimension";
    private static final String ORIGIN = "mdvlcraft_dimension_return";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> RETURN_CRYSTAL = ITEMS.register("return_crystal", CrystalItem::new);

    private ReturnCrystal() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        ITEMS.register(modBus);
        forgeBus.addListener(ReturnCrystal::onTravel);
        forgeBus.addListener(ReturnCrystal::onChangedDimension);
    }

    private static boolean isBossDimension(ResourceKey<Level> dimension) {
        return dimension.location().getNamespace().equals(DIMENSION_MOD);
    }

    /** Fired before the move, so the player still stands where the eye was used. */
    private static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isBossDimension(event.getDimension()) && !isBossDimension(player.level().dimension())) {
            CompoundTag origin = new CompoundTag();
            origin.putString("dimension", player.level().dimension().location().toString());
            origin.putDouble("x", player.getX());
            origin.putDouble("y", player.getY());
            origin.putDouble("z", player.getZ());
            origin.putFloat("yaw", player.getYRot());
            origin.putFloat("pitch", player.getXRot());
            persisted(player).put(ORIGIN, origin);
        }
    }

    private static void onChangedDimension(PlayerChangedDimensionEvent event) {
        if (isBossDimension(event.getFrom()) && !isBossDimension(event.getTo())) {
            persisted(event.getEntity()).remove(ORIGIN);
        }
    }

    /** The persisted tag survives death; the next eye used overwrites the origin. */
    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static final class CrystalItem extends Item {
        CrystalItem() {
            super(new Properties().stacksTo(16).rarity(Rarity.RARE));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResultHolder.success(stack);
            }
            CompoundTag origin = persisted(player).getCompound(ORIGIN);
            if (!isBossDimension(level.dimension()) || origin.isEmpty()) {
                player.displayClientMessage(Component.translatable("item.mdvlcraft.return_crystal.dim").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            ResourceLocation dimension = ResourceLocation.parse(origin.getString("dimension"));
            ServerLevel target = serverPlayer.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (target == null) {
                throw new IllegalStateException("Return Crystal origin dimension " + dimension + " does not exist");
            }
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            serverPlayer.teleportTo(target, origin.getDouble("x"), origin.getDouble("y"), origin.getDouble("z"), origin.getFloat("yaw"), origin.getFloat("pitch"));
            target.playSound(null, serverPlayer.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.mdvlcraft.return_crystal.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
