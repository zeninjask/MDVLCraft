package com.mdvlcraft.binder.mixin.jer;

import com.mdvlcraft.binder.compat.jer.JerLoot;
import java.util.Map;
import java.util.function.Supplier;
import jeresources.util.LootTableHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** JER doesn't load the mod jars' loot tables or list mobs from them: compat/jer/JerLoot gives it the server's. */
@Mixin(value = LootTableHelper.class, remap = false)
public abstract class LootTableHelperMixin {
    @Inject(method = "getLootDataManager", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noJarLootTables(CallbackInfoReturnable<LootDataManager> cir) {
        cir.setReturnValue(JerLoot.NO_LOOT_TABLES);
    }

    @Inject(method = "getAllMobLootTables", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$noJarMobs(CallbackInfoReturnable<Map<ResourceLocation, Supplier<LivingEntity>>> cir) {
        cir.setReturnValue(Map.of());
    }
}
