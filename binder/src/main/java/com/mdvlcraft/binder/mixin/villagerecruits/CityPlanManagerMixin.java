package com.mdvlcraft.binder.mixin.villagerecruits;

import com.example.villagerecruits.structures.TowerFootprint;
import com.example.villagerecruits.structures.cityplan.CityPlan;
import com.example.villagerecruits.structures.cityplan.Plot;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.example.villagerecruits.structures.cityplan.CityPlanManager", remap = false)
public abstract class CityPlanManagerMixin {
    @Redirect(method = "reconcileWithTower", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0), require = 0)
    private static boolean mdvlcraft$ignoreBuiltPlots(List<Object> intruding, Object plot) {
        return plot instanceof Plot p && !p.built && intruding.add(plot);
    }

    @Inject(method = "reconcileWithTower", at = @At("RETURN"), require = 0)
    private static void mdvlcraft$dropStillOverlapping(ServerLevel level, CityPlan plan, CallbackInfoReturnable<CityPlan> cir) {
        CityPlan fixed = (CityPlan)cir.getReturnValue();
        if (fixed != null && fixed != plan && fixed.center != null) {
            try {
                fixed.plots
                    .removeIf(
                        p -> !p.built
                            && p.anchor != null
                            && TowerFootprint.overlapsAnyTowerOfVillage(
                                fixed.factionId, fixed.center, p.anchor.getX(), p.anchor.getZ(), Math.max(p.width, p.depth) / 2, 2
                            )
                    );
            } catch (Throwable var5) {
            }
        }
    }
}
