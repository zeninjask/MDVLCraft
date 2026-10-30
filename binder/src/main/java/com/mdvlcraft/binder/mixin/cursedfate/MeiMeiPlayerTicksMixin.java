package com.mdvlcraft.binder.mixin.cursedfate;

import com.cursedfate_roslon_meimei.network.CursedfateRoslonMeimeiModVariables;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mei Mei's add-on copies four Cursed Fate variables into its own every tick for every player, whatever their technique,
 * and sent a full variable sync to the client after each copy: four packets per player per tick. The copies now send one
 * sync, and only when a value changed (the reads themselves: MeiMeiReturnAndPrintMixin).
 */
@Mixin(targets = "com.cursedfate_roslon_meimei.procedures.PlayerTicksProcedure", remap = false)
public abstract class MeiMeiPlayerTicksMixin {
    private static final ThreadLocal<Object[]> mdvlcraft$before = new ThreadLocal<>();

    private static Object[] mdvlcraft$snapshot(Entity entity) {
        CursedfateRoslonMeimeiModVariables.PlayerVariables vars = entity.getCapability(CursedfateRoslonMeimeiModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
        return vars == null ? null : new Object[] {vars.TextureOveride, vars.CursedfateTechnique, vars.Cursedfateinnatemenuopened, vars.CursedfateAbilityToCast};
    }

    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private static void mdvlcraft$remember(Event event, LevelAccessor level, double x, double y, double z, Entity entity, CallbackInfo ci) {
        mdvlcraft$before.set(entity == null ? null : mdvlcraft$snapshot(entity));
    }

    @Redirect(method = {"lambda$execute$0", "lambda$execute$1", "lambda$execute$2", "lambda$execute$3"},
        at = @At(value = "INVOKE", target = "Lcom/cursedfate_roslon_meimei/network/CursedfateRoslonMeimeiModVariables$PlayerVariables;syncPlayerVariables(Lnet/minecraft/world/entity/Entity;)V"))
    private static void mdvlcraft$deferSync(CursedfateRoslonMeimeiModVariables.PlayerVariables vars, Entity entity) {
    }

    @Inject(method = "execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private static void mdvlcraft$syncIfChanged(Event event, LevelAccessor level, double x, double y, double z, Entity entity, CallbackInfo ci) {
        Object[] before = mdvlcraft$before.get();
        mdvlcraft$before.remove();
        Object[] after = entity == null ? null : mdvlcraft$snapshot(entity);
        if (after != null && !java.util.Arrays.equals(before, after)) {
            entity.getCapability(CursedfateRoslonMeimeiModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> vars.syncPlayerVariables(entity));
        }
    }
}
