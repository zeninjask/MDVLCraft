package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.setup.ModTiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Anti-Magic's demon_sword tier is sorted above netherite but names minecraft:needs_diamond_tool as the blocks it needs,
 * so Forge's tier sorting decides every diamond-level block (obsidian, ancient debris, ...) needs a tier above netherite:
 * no pickaxe could harvest them. It gets a tag of its own instead, which no block is in.
 */
@Mixin(value = ModTiers.class, remap = false)
public abstract class DemonSwordTierMixin {
    @Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/tags/BlockTags;NEEDS_DIAMOND_TOOL:Lnet/minecraft/tags/TagKey;",
        opcode = Opcodes.GETSTATIC, remap = true))
    private static TagKey<Block> mdvlcraft$ownTag() {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("mdvlcraft", "needs_demon_sword_tool"));
    }
}
