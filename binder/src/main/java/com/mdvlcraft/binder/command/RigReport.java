package com.mdvlcraft.binder.command;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

/**
 * Every living entity type and the Epic Fight patch it gets (mdvl rigs -> rigs.tsv): each type is created once in the
 * overworld (not added to it) and its patch read; "-" = no patch, PassiveMobPatch = the binder's stagger-only fallback.
 */
final class RigReport {
    private RigReport() {
    }

    static List<String> build(ServerLevel level) {
        List<String> lines = new ArrayList<>(List.of("entity\tpatch\tclass"));
        ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
            try {
                Entity entity = entry.getValue().create(level);
                if (entity instanceof LivingEntity living) {
                    EntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(living, EntityPatch.class);
                    lines.add(entry.getKey().location() + "\t" + (patch == null ? "-" : patch.getClass().getName()) + "\t" + living.getClass().getName());
                }
            } catch (RuntimeException e) {   // some modded entities can't exist outside the world
                lines.add(entry.getKey().location() + "\terror: " + e + "\t-");
            }
        });
        return lines;
    }
}
