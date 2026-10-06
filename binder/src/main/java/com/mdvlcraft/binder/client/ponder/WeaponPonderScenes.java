package com.mdvlcraft.binder.client.ponder;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.arc.epic_ponder.api.ponder.EpicFightSceneBuilder;
import org.arc.epic_ponder.client.ponder.EFPPonderPlugin;
import org.arc.epic_ponder.client.ponder.EFPSceneUtils;
import org.arc.epic_ponder.mixin.epicfight.WeaponCapabilityAccessor;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.List;

/** Generic weapon scenes built from the weapon type's own moveset, in Epic Fight Ponder's style. */
final class WeaponPonderScenes {
    private static final int SIZE = 11;
    private static final List<Style> STYLE_ORDER = List.of(CapabilityItem.Styles.TWO_HAND, CapabilityItem.Styles.ONE_HAND, CapabilityItem.Styles.COMMON);

    private WeaponPonderScenes() {
    }

    static void showcase(SceneBuilder scene, SceneBuildingUtil util, ResourceLocation weaponType) {
        ItemStack weapon = representative(weaponType);
        WeaponCategory category = EpicFightCapabilities.getItemStackCapability(weapon).getWeaponCategory();
        if (category == CapabilityItem.WeaponCategories.BOW || category == CapabilityItem.WeaponCategories.CROSSBOW) {
            ranged(scene, util, weapon, category == CapabilityItem.WeaponCategories.CROSSBOW);
        } else {
            EFPSceneUtils.showcaseStandardWeaponCombo(scene, util, SIZE, "mdvlcraft:weapon_combo", weapon, style(weapon));
        }
    }

    /**
     * The first registered item whose weapon type has this path; every item of a type shares its moveset. Matched by path
     * because Epic Fight Ponder keys scenes by path, so this scene also serves same-named types from other mods.
     */
    private static ItemStack representative(ResourceLocation weaponType) {
        String path = weaponType.getPath();
        return ForgeRegistries.ITEMS.getValues().stream()
                .map(ItemStack::new)
                .filter(stack -> {
                    String type = EFPPonderPlugin.getWeaponPresetId(stack);
                    return type != null && ResourceLocation.parse(type).getPath().equals(path);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No item uses an Epic Fight weapon type named " + path));
    }

    /** The style the weapon's combo is defined for: two-handed, one-handed or common, in that order. */
    private static Style style(ItemStack weapon) {
        if (EpicFightCapabilities.getItemStackCapability(weapon) instanceof WeaponCapability capability) {
            var motions = ((WeaponCapabilityAccessor) capability).getAutoAttackMotions();
            return STYLE_ORDER.stream().filter(motions::containsKey).findFirst().orElse(CapabilityItem.Styles.COMMON);
        }
        return CapabilityItem.Styles.COMMON;
    }

    private static void ranged(SceneBuilder scene, SceneBuildingUtil util, ItemStack weapon, boolean crossbow) {
        EpicFightSceneBuilder builder = new EpicFightSceneBuilder(scene);
        EpicFightSceneBuilder.EpicFightWorldInstructions world = builder.world();
        EFPSceneUtils.setupStandardScene(builder, SIZE, "weapon_ranged", "mdvlcraft.ponder.weapon_ranged.title");
        ElementLink<EntityElement> actor = EFPSceneUtils.spawnDummyActorWithItem(builder, 5.5, 1.0, 5.5, 180.0F, weapon);
        EFPSceneUtils.showText(builder, util, "mdvlcraft.ponder.weapon_ranged.text_1", 70, 5, 1, 5);
        builder.idle(20);
        for (int shot = 0; shot < 2; shot++) {
            world.playAnimation(actor, crossbow ? Animations.BIPED_CROSSBOW_AIM : Animations.BIPED_BOW_AIM, 0.0F);
            builder.idle(25);
            world.playAnimation(actor, crossbow ? Animations.BIPED_CROSSBOW_SHOT : Animations.BIPED_BOW_SHOT, 0.0F);
            world.waitForInaction(actor);
            if (crossbow) {
                world.playAnimation(actor, Animations.BIPED_CROSSBOW_RELOAD, 0.0F);
                world.waitForInaction(actor);
            }
            builder.idle(10);
        }
        builder.markAsFinished();
    }
}
