package com.mdvlcraft.binder.integration;

import com.mdvlcraft.binder.MDVLBinder;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.GlobalMobPatch;

public final class EpicFightIntegration {
    /** Dummmmmmy's target dummy: patched so it can be assassinated. */
    private static final ResourceLocation TARGET_DUMMY = ResourceLocation.fromNamespaceAndPath("dummmmmmy", "target_dummy");
    /** The types given a PassiveMobPatch (they need an armature too). */
    private static final Set<EntityType<?>> PASSIVE = new LinkedHashSet<>();   // the event can fire more than once

    private EpicFightIntegration() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(EventPriority.LOWEST, EpicFightIntegration::onEntityPatchRegistry);   // after every mod that patches its own mobs
        modBus.addListener(EpicFightIntegration::onCommonSetup);
    }

    /** Every hostile mob (and the target dummy) that no mod patches gets a PassiveMobPatch, so it can be assassinated. A
     * non-mob of those types falls back to Epic Fight's own global patch. */
    private static void onEntityPatchRegistry(EntityPatchRegistryEvent event) {
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            boolean wanted = type.getCategory() == MobCategory.MONSTER || TARGET_DUMMY.equals(id);
            if (wanted && !event.getTypeEntry().containsKey(type)) {
                event.getTypeEntry().put(type, entity -> entity instanceof Mob ? PassiveMobPatch::new : GlobalMobPatch::new);
                PASSIVE.add(type);
            }
        }
        MDVLBinder.LOGGER.info("[Epic Fight] {} entity types have Epic Fight patches ({} passive)", event.getTypeEntry().size(), PASSIVE.size());
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> PASSIVE.forEach(type -> Armatures.registerEntityTypeArmature(type, Armatures.BIPED)));
    }
}
