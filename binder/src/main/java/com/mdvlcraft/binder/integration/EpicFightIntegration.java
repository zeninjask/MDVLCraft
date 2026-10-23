package com.mdvlcraft.binder.integration;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;

public final class EpicFightIntegration {
    /** Dummmmmmy's target dummy: patched (TargetDummyPatch) so it can be assassinated. */
    private static final ResourceLocation TARGET_DUMMY = ResourceLocation.fromNamespaceAndPath("dummmmmmy", "target_dummy");

    private EpicFightIntegration() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(EpicFightIntegration::onEntityPatchRegistry);
        modBus.addListener(EpicFightIntegration::onCommonSetup);
    }

    private static void onEntityPatchRegistry(EntityPatchRegistryEvent event) {
        EntityType<?> dummy = ForgeRegistries.ENTITY_TYPES.getValue(TARGET_DUMMY);
        if (dummy != null && ForgeRegistries.ENTITY_TYPES.containsKey(TARGET_DUMMY)) {
            event.getTypeEntry().put(dummy, entity -> TargetDummyPatch::new);
        }
        MDVLBinder.LOGGER.info("[Epic Fight] {} entity types have Epic Fight patches", event.getTypeEntry().size());
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ForgeRegistries.ENTITY_TYPES.containsKey(TARGET_DUMMY)) {
                Armatures.registerEntityTypeArmature(ForgeRegistries.ENTITY_TYPES.getValue(TARGET_DUMMY), Armatures.BIPED);
            }
        });
    }
}
