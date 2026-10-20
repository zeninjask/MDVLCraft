package com.mdvlcraft.binder.mixin.backpacked;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * With more than one backpack equipped, Backpacked puts its previous/next backpack arrows beside the backpack's name,
 * where Inventory Profiles Next draws its sort buttons. They go above the name instead: the previous arrow over its
 * left end, the next arrow over its right end (Backpacked places them 7 px outside the name, 12 px wide).
 */
@Pseudo
@Mixin(targets = "com.mrcrayfish.backpacked.client.gui.screen.inventory.BackpackScreen", remap = false)
public abstract class BackpackPageButtonsMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final String MINI_BUTTON = "Lcom/mrcrayfish/backpacked/client/gui/screen/widget/MiniButton;<init>"
        + "(IIIILcom/mrcrayfish/framework/api/client/screen/widget/texture/FrameworkTexture;Lnet/minecraft/client/gui/components/Button$OnPress;)V";
    private static final int ABOVE = 14;

    private BackpackPageButtonsMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArg(method = "m_7856_", at = @At(value = "INVOKE", target = MINI_BUTTON, ordinal = 0), index = 0)
    private int mdvlcraft$previousX(int x) {
        return x + 12 + 7;
    }

    @ModifyArg(method = "m_7856_", at = @At(value = "INVOKE", target = MINI_BUTTON, ordinal = 1), index = 0)
    private int mdvlcraft$nextX(int x) {
        return x - 7 - 12;
    }

    @ModifyArg(method = "m_7856_", at = @At(value = "INVOKE", target = MINI_BUTTON), index = 1)
    private int mdvlcraft$aboveName(int y) {
        return this.topPos - ABOVE;
    }
}
