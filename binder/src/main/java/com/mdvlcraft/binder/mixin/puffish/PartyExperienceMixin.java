package com.mdvlcraft.binder.mixin.puffish;

import com.mdvlcraft.binder.content.PartyExperience;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shares a kill's skill experience across the killer's party (PartyExperience). Each experience source is evaluated
 * once, for the killer, so anti-farming counts the kill once; every player then gets their share of that amount in the
 * categories they have unlocked.
 */
@Mixin(value = SkillsMod.class, remap = false)
public abstract class PartyExperienceMixin {
    @Inject(method = "visitExperienceSources(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Function;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$shareKill(ServerPlayer killer, Function<ExperienceSource, Integer> function, CallbackInfo ci) {
        LivingEntity victim = PartyExperience.takeDying();
        if (victim == null) {
            return;
        }
        Map<ServerPlayer, Double> shares = PartyExperience.shares(killer, victim);
        if (shares.size() == 1) {
            return;
        }

        Map<ExperienceSource, Integer> evaluated = new HashMap<>();
        shares.forEach((player, share) -> ((SkillsMod)(Object)this).visitExperienceSources(
            player, source -> (int)Math.round(evaluated.computeIfAbsent(source, function) * share)
        ));
        ci.cancel();
    }
}
