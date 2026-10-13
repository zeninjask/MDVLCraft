package com.mdvlcraft.binder.compat;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.content.DisabledContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

/**
 * Hides from JEI what a survival player can never get: forbidden items (Iron's own JEI pages are never registered, see
 * JeiPluginMixin), spawn eggs, operator-only blocks, and the debug, icon and model-part items mods register for themselves.
 */
@JeiPlugin
public final class JeiHiddenContent implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "hidden_content");
    private static final Set<String> UNOBTAINABLE = Set.of(
        // Operator-only
        "minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block", "minecraft:command_block_minecart",
        "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw", "minecraft:barrier", "minecraft:light",
        "minecraft:debug_stick", "minecraft:knowledge_book", "minecraft:petrified_oak_slab",
        // Debug and example items
        "citadel:debug", "citadel:citadel_book", "farmersdelight:debug_pumpkin_pie", "fdlib:test_multiblock", "efn:test",
        "invincible:debug", "invincible:custom_combo_demo", "invincible:custom_skill_demo",
        "gtbcs_spell_lib:gsl_example_lore_item", "gtbcs_spell_lib:gsl_example_crit_staff", "gtbcs_spell_lib:painting_spell_lib",
        // Icons, model parts, projectiles and technical blocks
        "citadel:fancy_item", "citadel:icon_item", "citadel:effect_item", "alekiships:sloop_icon_only", "alekiships:rowboat_icon_only",
        "sereneseasons:ss_icon", "mdvlcraft:origin_icon", "moonlight:placeable_item",
        "wom:herrscher_aura", "wom:gesetz_aura", "wom:gesetz_handle", "wom:moonless_handle",
        "project_babylon_weapons:fire_spell_projectile", "project_babylon_weapons:frozen_debuff_ice_block",
        "lootr:lootr_chest", "lootr:lootr_barrel", "lootr:lootr_trapped_chest", "lootr:lootr_shulker", "lootr:lootr_inventory"
    );

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        UNOBTAINABLE.stream().filter(id -> !ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id))).findFirst().ifPresent(id -> {
            throw new IllegalStateException("JEI hiding names an unregistered item: " + id);
        });
        List<ItemStack> items = ForgeRegistries.ITEMS.getEntries().stream()
                .filter(entry -> hidden(entry.getKey().location(), entry.getValue()))
                .map(entry -> new ItemStack(entry.getValue()))
                .toList();
        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, items);
    }

    private static boolean hidden(ResourceLocation id, Item item) {
        return DisabledContent.isForbidden(item) || item instanceof SpawnEggItem || UNOBTAINABLE.contains(id.toString());
    }
}
