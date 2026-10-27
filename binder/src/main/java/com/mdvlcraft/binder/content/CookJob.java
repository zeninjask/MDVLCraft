package com.mdvlcraft.binder.content;

import dev.limonblaze.originsclasses.common.event.ModifyCraftResultEvent;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Cook job's cooking pot perks. Leftovers: what a cook takes out of a Farmer's Delight cooking pot comes out twice
 * over. Good Meals: it also comes out Perfect ("Perfect Bread"), and a Perfect meal gives 1.5x its saturation. Origins:
 * Classes reports the pot's result through ModifyCraftResultEvent, so its own crafted-food bonus still applies.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class CookJob {
    private static final ResourceLocation JOB_LAYER = ResourceLocation.fromNamespaceAndPath("origins-classes", "class");
    private static final ResourceLocation COOK = ResourceLocation.fromNamespaceAndPath("origins-classes", "cook");
    private static final String PERFECT = "mdvlcraft_perfect";
    private static final float PERFECT_SATURATION = 1.5F;

    private CookJob() {
    }

    public static boolean isCook(Player player) {
        ResourceKey<OriginLayer> layer = ResourceKey.create(OriginsAPI.getLayersRegistry().key(), JOB_LAYER);
        ResourceKey<Origin> origin = IOriginContainer.get(player).map(container -> container.getOrigin(layer)).orElse(null);
        return origin != null && origin.location().equals(COOK);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPotResult(ModifyCraftResultEvent event) {
        ItemStack crafted = event.getCrafted();
        if (event.getType() != ModifyCraftResultEvent.CraftingResultType.COOKING_POT || crafted.isEmpty() || !crafted.isEdible()
            || event.getEntity() == null || !isCook(event.getEntity())) {
            return;
        }
        ItemStack meal = crafted.copy();
        meal.setCount(Math.min(meal.getMaxStackSize(), meal.getCount() * 2));
        if (!meal.getOrCreateTag().getBoolean(PERFECT)) {
            meal.getOrCreateTag().putBoolean(PERFECT, true);
            meal.setHoverName(Component.translatable("item.mdvlcraft.perfect", crafted.getHoverName()).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GOLD)));
        }
        event.setCrafted(meal);
    }

    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        ItemStack food = event.getItem();
        if (event.getEntity() instanceof Player player && food.hasTag() && food.getTag().getBoolean(PERFECT)) {
            FoodProperties properties = food.getFoodProperties(player);
            if (properties != null) {
                FoodData data = player.getFoodData();
                float bonus = properties.getNutrition() * properties.getSaturationModifier() * 2.0F * (PERFECT_SATURATION - 1.0F);
                data.setSaturation(Math.min(data.getSaturationLevel() + bonus, data.getFoodLevel()));
            }
        }
    }
}
