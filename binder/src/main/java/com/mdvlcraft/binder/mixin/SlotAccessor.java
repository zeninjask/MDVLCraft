package com.mdvlcraft.binder.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Moves a slot on screen: client/DqInventoryScreen lays Curios' inventory menu out its own way. */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void mdvlcraft$setX(int x);

    @Mutable
    @Accessor("y")
    void mdvlcraft$setY(int y);
}
