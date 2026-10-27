package com.mdvlcraft.binder.mixin.origins;

import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import java.util.List;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The layers Origins is asking the player to choose, for client/picker/OriginPicker. */
@Mixin(value = ChooseOriginScreen.class, remap = false)
public interface ChooseOriginScreenAccessor {
    @Accessor("layerList")
    List<Holder<OriginLayer>> mdvlcraft$layerList();

    @Accessor("currentLayerIndex")
    int mdvlcraft$currentLayerIndex();
}
