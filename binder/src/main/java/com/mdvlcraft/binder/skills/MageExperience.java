package com.mdvlcraft.binder.skills;

import com.mdvlcraft.binder.gate.GateSpawner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.puffish.skillsmod.api.calculation.operation.OperationFactory;
import net.puffish.skillsmod.api.calculation.prototype.BuiltinPrototypes;

/**
 * The skill-tree operation mdvlcraft:is_mage: whether a living entity is in #mdvlcraft:mages, for the 150 experience a
 * mage kill gives (each tree's experience.json). Pufferfish's own test operation looks the tag up while it loads the
 * trees, before the server has bound its tags on start, and fails the whole mdvlcraft skill set; this checks the tag
 * at the kill.
 *
 * mdvlcraft:is_armor_stand: armour stands are living entities with 20 health, so breaking one gave kill experience; the
 * trees give them none.
 */
public final class MageExperience {
    private static final TagKey<EntityType<?>> MAGES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("mdvlcraft", "mages"));

    private MageExperience() {
    }

    public static void register() {
        BuiltinPrototypes.LIVING_ENTITY.registerOperation(
            ResourceLocation.fromNamespaceAndPath("mdvlcraft", "is_mage"),
            BuiltinPrototypes.BOOLEAN,
            OperationFactory.create((LivingEntity entity) -> entity.getType().is(MAGES))
        );
        BuiltinPrototypes.LIVING_ENTITY.registerOperation(
            ResourceLocation.fromNamespaceAndPath("mdvlcraft", "is_armor_stand"),
            BuiltinPrototypes.BOOLEAN,
            OperationFactory.create((LivingEntity entity) -> entity instanceof ArmorStand)
        );
        // mdvlcraft:is_gate_mob: gate mob kills go through a source without the per-chunk anti-farm cap (plan 6)
        BuiltinPrototypes.LIVING_ENTITY.registerOperation(
            ResourceLocation.fromNamespaceAndPath("mdvlcraft", "is_gate_mob"),
            BuiltinPrototypes.BOOLEAN,
            OperationFactory.create((LivingEntity entity) -> GateSpawner.instanceOf(entity) >= 0)
        );
    }
}
