package com.mdvlcraft.binder.mixin.naoyaaddon;

import com.bless.naoyaaddon.network.NaoyaAddonVariables;
import com.mdvlcraft.binder.compat.CursedFateVars;
import java.util.Arrays;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Naoya's add-on copies Cursed Fate's Technique, InnateMenuOpened and AbilityToCast into its own variables every tick for
 * every player, reading each through a `/cursedfate Datapack ReturnAndPrint` command and sending a full variable sync
 * after the copy. The reads are answered directly (CursedFateVars) and the sync goes out only when a value changed.
 */
@Mixin(targets = "com.bless.naoyaaddon.procedures.PlayerTicksProcedure", remap = false)
public abstract class CursedFateVarsSyncMixin {
    private static final ThreadLocal<Object[]> mdvlcraft$before = new ThreadLocal<>();

    private static Object[] mdvlcraft$snapshot(Entity entity) {
        NaoyaAddonVariables.PlayerVariables vars = entity.getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
        return vars == null ? null : new Object[] {vars.TextureOveride, vars.CursedfateTechnique, vars.Cursedfateinnatemenuopened, vars.CursedfateAbilityToCast};
    }

    @Inject(method = "getCommandResult", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$direct(Entity entity, String command, CallbackInfoReturnable<String> cir) {
        String result = CursedFateVars.returnAndPrint(entity, command);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "updateCursedFateVariables", at = @At("HEAD"))
    private static void mdvlcraft$remember(Entity entity, LevelAccessor level, CallbackInfo ci) {
        mdvlcraft$before.set(entity == null ? null : mdvlcraft$snapshot(entity));
    }

    @Redirect(method = {"lambda$updateCursedFateVariables$0", "lambda$updateCursedFateVariables$1"},
        at = @At(value = "INVOKE", target = "Lcom/bless/naoyaaddon/network/NaoyaAddonVariables$PlayerVariables;syncPlayerVariables(Lnet/minecraft/world/entity/Entity;)V"))
    private static void mdvlcraft$deferSync(NaoyaAddonVariables.PlayerVariables vars, Entity entity) {
    }

    @Inject(method = "updateCursedFateVariables", at = @At("RETURN"))
    private static void mdvlcraft$syncIfChanged(Entity entity, LevelAccessor level, CallbackInfo ci) {
        Object[] before = mdvlcraft$before.get();
        mdvlcraft$before.remove();
        Object[] after = entity == null ? null : mdvlcraft$snapshot(entity);
        if (after != null && !Arrays.equals(before, after)) {
            entity.getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> vars.syncPlayerVariables(entity));
        }
    }
}
