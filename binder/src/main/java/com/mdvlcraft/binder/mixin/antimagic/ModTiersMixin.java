package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.setup.ModTiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Anti-Magic's demon sword tier is sorted above netherite but uses minecraft:needs_diamond_tool as its tag, so Forge's
 * tier sorting made every diamond-level block (ancient debris, obsidian, crying obsidian, respawn anchors) need a tier
 * above netherite: no pickaxe could harvest them. The tier gets its own (empty) tag instead.
 */
@Mixin(value = ModTiers.class, remap = false)
public abstract class ModTiersMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/common/ForgeTier;<init>(IIFFILnet/minecraft/tags/TagKey;Ljava/util/function/Supplier;)V"), index = 5, require = 1)
    private static TagKey<Block> mdvlcraft$ownTag(TagKey<Block> tag) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("antimagic", "needs_demon_sword_tool"));
    }
}
