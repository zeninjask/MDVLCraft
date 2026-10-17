package com.mdvlcraft.binder.mixin;

import java.util.Map;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** content/TradeTuning rebuilds this trade (a fisherman buying the boat of the villager's biome) without calling it. */
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EmeraldsForVillagerTypeItem")
public interface EmeraldsForVillagerTypeItemAccessor {
    @Accessor("trades")
    Map<VillagerType, Item> mdvlcraft$trades();

    @Accessor("cost")
    int mdvlcraft$cost();

    @Accessor("maxUses")
    int mdvlcraft$maxUses();

    @Accessor("villagerXp")
    int mdvlcraft$villagerXp();
}
