package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.content.DisabledContent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Structures from disabled mods never start generating. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$skipDisabledStructure(StructureSet.StructureSelectionEntry entry, StructureManager structureManager,
                                                 RegistryAccess registryAccess, RandomState random, StructureTemplateManager templateManager,
                                                 long seed, ChunkAccess chunk, ChunkPos chunkPos, SectionPos sectionPos,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (entry.structure().unwrapKey().map(key -> DisabledContent.isDisabled(key.location())).orElse(false)) {
            cir.setReturnValue(false);
        }
    }
}
