package com.mdvlcraft.binder.client.ponder;

import com.mdvlcraft.binder.MDVLBinder;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Epic Fight Ponder only has scenes for Epic Fight's eight built-in weapon types, and looks scenes up by
 * {@code epic_fight_ponder:weapon_<type path>}. This plugin registers a scene under that key for every other weapon type
 * (Weapons of Minecraft, Nightfall, Cataclysm compat, Epic Knights pikes, staves, bows...), so every weapon can be pondered.
 * The scene plays the combo of a weapon of that type; the actor holds the item being pondered.
 */
public final class WeaponPonderPlugin implements PonderPlugin {
    /** Weapon type paths Epic Fight Ponder already has scenes for (EFPPonderPlugin.registerScenes). */
    private static final Set<String> EPIC_FIGHT_PONDER_TYPES = Set.of("sword", "dagger", "spear", "axe", "tachi", "uchigatana", "longsword", "greatsword");
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath("epic_fight_ponder", "epicfight_showcase");

    @Override
    public String getModId() {
        return MDVLBinder.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        Set<String> covered = new HashSet<>(EPIC_FIGHT_PONDER_TYPES);
        for (var entry : WeaponTypeReloadListener.entries()) {
            ResourceLocation type = entry.getKey();
            // Epic Fight Ponder keys scenes by path only, so one scene serves every type sharing a path.
            if (!covered.add(type.getPath())) {
                continue;
            }
            helper.withKeyFunction(WeaponPonderPlugin::sceneKey)
                    .forComponents(type)
                    .addStoryBoard(STRUCTURE, (scene, util) -> WeaponPonderScenes.showcase(scene, util, type));
        }
    }

    private static ResourceLocation sceneKey(ResourceLocation weaponType) {
        return ResourceLocation.fromNamespaceAndPath("epic_fight_ponder", "weapon_" + weaponType.getPath());
    }
}
