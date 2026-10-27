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

    /** Every mob that no mod patches (hostile or not: animals, villagers, summons, NPCs of any category) gets a
     * PassiveMobPatch, so it can be assassinated and is staggered by hits; the target dummy gets its own. An entity of
     * those types that isn't a mob (projectiles, items, ...) gets no patch, as Epic Fight gives it none itself. */
    private static void onEntityPatchRegistry(EntityPatchRegistryEvent event) {
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (!event.getTypeEntry().containsKey(type)) {
                event.getTypeEntry().put(type, entity -> !(entity instanceof Mob) ? () -> null : TARGET_DUMMY.equals(id) ? TargetDummyPatch::new : PassiveMobPatch::new);
                PASSIVE.add(type);
            }
        }
        MDVLBinder.LOGGER.info("[Epic Fight] {} entity types have Epic Fight patches ({} passive)", event.getTypeEntry().size(), PASSIVE.size());
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> PASSIVE.forEach(type -> Armatures.registerEntityTypeArmature(type, Armatures.BIPED)));
    }
}
