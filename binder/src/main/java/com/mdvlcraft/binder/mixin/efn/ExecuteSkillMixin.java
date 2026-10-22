package com.mdvlcraft.binder.mixin.efn;

import com.hm.efn.skill.arts.ExecuteSkill;
import com.mdvlcraft.binder.modified.BlackRoot;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.DealDamageEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerKilledEvent;

/**
 * Soul Hunt (Nightfall's Execution): a kill with its own strike and a hit on a guard-broken target cut its cooldown nearly
 * to nothing, so it chained endlessly through groups. Only 2 such resets count per cast; after that a kill with its
 * strike is credited like any other kill and guard-broken hits leave the cooldown alone.
 * Underworld Gate's black roots (modified/BlackRoot) count as a neutralized state, so a rooted enemy can be executed; it is
 * freed from the roots when the execution takes it, so the execution's animation can move it.
 */
@Mixin(value = ExecuteSkill.class, remap = false)
public abstract class ExecuteSkillMixin {
    private static final String RESETS = "mdvlcraft:soul_hunt_resets";
    private static final int MAX_RESETS = 2;
    private static final String SET_DATA = "Lyesman/epicfight/skill/SkillDataManager;setDataSync(Lyesman/epicfight/skill/SkillDataKey;Ljava/lang/Object;)V";

    @Shadow
    private int killCooldownReduction;

    @Inject(method = "executeOnServer", at = @At("HEAD"), require = 1)
    private void mdvlcraft$newCast(SkillContainer container, FriendlyByteBuf args, CallbackInfo ci) {
        container.getExecutor().getOriginal().getPersistentData().putInt(RESETS, 0);
    }

    @Inject(method = "isEntityInNeutralizeState", at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$rootedIsNeutralized(LivingEntityPatch<?> target, CallbackInfoReturnable<Boolean> cir) {
        if (target.getOriginal().getVehicle() instanceof BlackRoot) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "findNeutralizeEntities", at = @At("RETURN"), require = 1)
    private void mdvlcraft$freeRooted(PlayerPatch<?> executor, CallbackInfoReturnable<List<LivingEntity>> cir) {
        for (LivingEntity target : cir.getReturnValue()) {
            if (target.getVehicle() instanceof BlackRoot) {
                target.stopRiding();
            }
        }
    }

    /** A guard-broken target was hit with Soul Hunt: its cooldown reduction is a reset. */
    @Redirect(method = "lambda$onInitiate$3", at = @At(value = "INVOKE", target = SET_DATA), require = 1)
    private void mdvlcraft$neutralizeReset(SkillDataManager data, SkillDataKey<Object> key, Object value, SkillContainer container, DealDamageEvent.Attack event) {
        if (takeReset(container)) {
            data.setDataSync(key, value);
        }
    }

    /** A kill: a reduction beyond a plain kill's is Soul Hunt's own kill reset; past the cap it counts as a plain kill. */
    @Redirect(method = "lambda$onInitiate$4", at = @At(value = "INVOKE", target = SET_DATA), require = 1)
    private void mdvlcraft$killReset(SkillDataManager data, SkillDataKey<Object> key, Object value, SkillContainer container, PlayerKilledEvent event) {
        int before = (Integer)data.getDataValue(key);
        int plain = Math.max(0, before - this.killCooldownReduction);
        int after = (Integer)value;
        data.setDataSync(key, after < plain && !takeReset(container) ? plain : after);
    }

    private static boolean takeReset(SkillContainer container) {
        CompoundTag tag = container.getExecutor().getOriginal().getPersistentData();
        int used = tag.getInt(RESETS);
        if (used >= MAX_RESETS) {
            return false;
        }
        tag.putInt(RESETS, used + 1);
        return true;
    }
}
