package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.gameasset.animations.EFNYamatoAnimations;
import com.hm.efn.skill.weapon_innate.YamatoInnate;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

@Mixin(value = YamatoInnate.class, remap = false)
public abstract class YamatoInnateMixin {
    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;JUDGECUT_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$judgecutAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT_ALL;
    }

    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;QUICK_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$quickAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT;
    }

    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;JUST_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$justAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT_JUST;
    }

    @Redirect(
        method = {"isChargeRelatedAnimation", "releaseAttack", "lambda$onInitiate$0"},
        at = @At(
            value = "FIELD",
            target = "Lcom/hm/efn/skill/weapon_innate/YamatoInnate;CHARGE_ANIM:Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            opcode = 178
        ),
        require = 0
    )
    private AnimationAccessor<?> mdvlcraft$chargeAnim() {
        return EFNYamatoAnimations.YAMATO_JUDEMENCUT_CHARGE;
    }

    /** The skill tooltip leaves out Judgement Cut End and the Doppelganger, which the pack removed (integration/YamatoUnbound). */
    @Inject(method = "getTooltipOnItem", at = @At("RETURN"), cancellable = true)
    private void mdvlcraft$noRemovedLines(ItemStack stack, CapabilityItem item, PlayerPatch<?> patch, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> lines = new ArrayList<>(cir.getReturnValue());
        lines.removeIf(line -> line.getContents() instanceof TranslatableContents text
            && (text.getKey().equals("skill.efn.yamato.tooltip13") || text.getKey().equals("skill.efn.yamato.tooltip14")));
        cir.setReturnValue(lines);
    }
}
