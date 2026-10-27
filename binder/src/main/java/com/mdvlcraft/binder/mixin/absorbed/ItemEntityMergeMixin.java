package com.mdvlcraft.binder.mixin.absorbed;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * From Get It Together, Drops! (bl4ckscor3, MIT): dropped items merge with others within RADIUS blocks (above and below
 * too) instead of vanilla's half block. Items tagged getittogetherdrops:ignored keep vanilla merging; items tagged
 * getittogetherdrops:do_not_combine never merge.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeMixin {
    private static final double RADIUS = 2.0;
    private static final TagKey<Item> IGNORED = ItemTags.create(new ResourceLocation("getittogetherdrops", "ignored"));
    private static final TagKey<Item> DO_NOT_COMBINE = ItemTags.create(new ResourceLocation("getittogetherdrops", "do_not_combine"));

    @Shadow
    private boolean isMergable() {
        throw new AssertionError();
    }

    @Shadow
    private void tryToMerge(ItemEntity other) {
        throw new AssertionError();
    }

    @Inject(method = "mergeWithNeighbours", at = @At("HEAD"), cancellable = true)
    private void mdvlcraft$widerMerge(CallbackInfo ci) {
        ItemEntity self = (ItemEntity)(Object)this;
        ItemStack stack = self.getItem();
        if (stack.is(IGNORED) || !this.isMergable()) {
            return;
        }
        if (!stack.is(DO_NOT_COMBINE)) {
            for (ItemEntity other : self.level().getEntitiesOfClass(ItemEntity.class, self.getBoundingBox().inflate(RADIUS),
                    other -> other != self && ((ItemEntityMergeMixin)(Object)other).isMergable())) {
                this.tryToMerge(other);
                if (self.isRemoved()) {
                    break;
                }
            }
        }
        ci.cancel();
    }
}
