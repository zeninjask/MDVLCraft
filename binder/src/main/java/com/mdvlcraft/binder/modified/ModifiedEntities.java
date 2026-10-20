package com.mdvlcraft.binder.modified;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Entities of the Modified abilities (renderers in client/ModifiedClientSetup). */
public final class ModifiedEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mdvlcraft");
    public static final RegistryObject<EntityType<JavelinProjectile>> JAVELIN = ENTITY_TYPES.register(
        "javelin", () -> Builder.<JavelinProjectile>of(JavelinProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(64).build("javelin")
    );
    public static final RegistryObject<EntityType<BoltProjectile>> BOLT = ENTITY_TYPES.register(
        "bolt", () -> Builder.<BoltProjectile>of(BoltProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(64).build("bolt")
    );

    private ModifiedEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
