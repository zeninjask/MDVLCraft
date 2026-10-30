package com.mdvlcraft.binder.mixin.puffish;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.config.CategoryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puffish offers every advancement trigger (the location one fires each tick, enter-block on every step) and every stat
 * increase to each category's experience sources of that kind, walking all of the pack's categories each time. The pack's
 * categories only gain experience from kills, so an update for a kind of source no category has now returns at once.
 */
@Mixin(value = SkillsAPI.class, remap = false)
public abstract class ExperienceSourceSkipMixin {
    private static Map<ResourceLocation, CategoryConfig> mdvlcraft$seen;
    private static Set<Class<?>> mdvlcraft$kinds = Set.of();

    @Inject(method = "updateExperienceSources(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/Class;Ljava/util/function/Function;)V", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$skipUnused(ServerPlayer player, Class<?> kind, Function<?, ?> function, CallbackInfo ci) {
        Map<ResourceLocation, CategoryConfig> categories = ((SkillsModAccessor)SkillsMod.getInstance()).mdvlcraft$categories().get().orElse(null);
        if (categories == null) {
            return;
        }
        if (categories != mdvlcraft$seen) {   // rebuilt on every reload of the skill data
            Set<Class<?>> kinds = new HashSet<>();
            categories.values().forEach(category -> category.experience().ifPresent(
                experience -> experience.experienceSources().forEach(source -> kinds.add(source.instance().getClass()))));
            mdvlcraft$kinds = kinds;
            mdvlcraft$seen = categories;
        }
        for (Class<?> present : mdvlcraft$kinds) {
            if (kind.isAssignableFrom(present)) {
                return;
            }
        }
        ci.cancel();
    }
}
