package com.mdvlcraft.binder.mixin.traits;

import com.mdvlcraft.binder.traits.Trait;
import com.mdvlcraft.binder.traits.Traits;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Lord of the Hunt and Summoner's Pact: summons registered with Iron's SummonManager (Iron's spells and the binder's own) last twice as long. */
@Mixin(value = SummonManager.class, remap = false)
public abstract class SummonDurationMixin {
    @ModifyVariable(method = "initSummon", at = @At("HEAD"), argsOnly = true)
    private static int mdvlcraft$lordOfTheHunt(int ticks, Entity owner) {
        return ticks > 0 && Traits.has(owner, Trait.LORD_OF_THE_HUNT) ? ticks * 2 : ticks;
    }

    /** Summoner's Pact (traits/TraitPicks.onSummoned). */
    @org.spongepowered.asm.mixin.injection.Inject(method = "initSummon", at = @At("TAIL"))
    private static void mdvlcraft$summonersPact(Entity owner, Entity summon, int ticks,
                                                io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData data,
                                                org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.mdvlcraft.binder.traits.TraitPicks.onSummoned(owner, summon);
    }
}
