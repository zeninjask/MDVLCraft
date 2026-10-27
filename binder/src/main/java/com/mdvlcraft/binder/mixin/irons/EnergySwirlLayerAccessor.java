package com.mdvlcraft.binder.mixin.irons;

import io.redspace.ironsspellbooks.render.EnergySwirlLayer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The swirl's texture and when it shows, for client/irons/SpellEffectLayers. */
@Mixin(value = EnergySwirlLayer.Vanilla.class, remap = false)
public interface EnergySwirlLayerAccessor {
    @Accessor("TEXTURE")
    ResourceLocation mdvlcraft$texture();

    @Accessor("shouldRender")
    Predicate<LivingEntity> mdvlcraft$shouldRender();
}
