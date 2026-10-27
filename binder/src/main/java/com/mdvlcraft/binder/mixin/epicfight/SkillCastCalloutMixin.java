package com.mdvlcraft.binder.mixin.epicfight;

import com.mdvlcraft.binder.ability.SkillCallout;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** An Epic Fight skill a player casts is called out by name (ability/SkillCallout), except dodges, guards and passives. */
@Mixin(value = SkillContainer.class, remap = false)
public abstract class SkillCastCalloutMixin {
    private static final Set<SkillCategory> QUIET = Set.of(SkillCategories.BASIC_ATTACK, SkillCategories.DODGE, SkillCategories.GUARD,
        SkillCategories.PASSIVE, SkillCategories.WEAPON_PASSIVE, SkillCategories.KNOCKDOWN_WAKEUP);

    @Inject(method = "requestCasting", at = @At("RETURN"))
    private void mdvlcraft$callout(ServerPlayerPatch patch, FriendlyByteBuf buf, CallbackInfoReturnable<Boolean> cir) {
        Skill skill = ((SkillContainer)(Object)this).getSkill();
        if (cir.getReturnValue() && skill != null && !QUIET.contains(skill.getCategory())) {
            SkillCallout.announce(patch.getOriginal(), skill.getDisplayName(), SkillCallout.WHITE);
        }
    }
}
