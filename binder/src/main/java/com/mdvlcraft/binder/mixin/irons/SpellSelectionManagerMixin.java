package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Iron's "selected spell" collects the spells carried by your spell book, held items, armour and curios. Everything
 * that casts it (staffs, casting implements, Iron's own keys, Epic Fight Skill - ISS's Magic Blade) would reach the
 * spells built into Iron's unique weapons without the skill trees. The selection is always empty; spells are cast
 * from the ability wheel, which never uses it.
 */
@Mixin(value = SpellSelectionManager.class, remap = false)
public abstract class SpellSelectionManagerMixin {
    @Shadow
    @Final
    private List<SpellSelectionManager.SelectionOption> selectionOptionList;

    @Shadow
    private int selectionIndex;

    @Inject(method = "init", at = @At("TAIL"))
    private void mdvlcraft$noItemSpells(Player player, CallbackInfo ci) {
        selectionOptionList.clear();
        selectionIndex = -1;
    }
}
