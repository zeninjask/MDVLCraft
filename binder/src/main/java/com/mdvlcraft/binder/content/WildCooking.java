package com.mdvlcraft.binder.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.NetworkHooks;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.registry.ModBlocks;

/**
 * Wild Cooking (the Cook's ability, cast as Spice Mix's spell): opens a Farmer's Delight cooking pot screen anywhere. Each
 * player has one pot that exists nowhere in the world: it cooks (always heated) while they are online, and its contents are
 * kept in their persistent data. The pot's menu would close at once without a pot block, and the client finds no block
 * entity at its position, so mixin/farmersdelight lets the menu stay and gives the client a pot of its own ({@link #isVirtual}).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class WildCooking {
    private static final String SAVED = "mdvlcraft_wild_pot";
    private static final Map<UUID, CookingPotBlockEntity> POTS = new HashMap<>();
    /** Pots that stand in no world, on either side (the server's, and the client's stand-in for its screen). */
    private static final Set<BlockEntity> VIRTUAL = Collections.newSetFromMap(new WeakHashMap<>());

    private WildCooking() {
    }

    public static boolean isVirtual(BlockEntity pot) {
        return VIRTUAL.contains(pot);
    }

    /** A pot in no world, at `pos` of `level`. */
    public static CookingPotBlockEntity virtualPot(Level level, BlockPos pos) {
        CookingPotBlockEntity pot = new CookingPotBlockEntity(pos, ModBlocks.COOKING_POT.get().defaultBlockState());
        pot.setLevel(level);
        VIRTUAL.add(pot);
        return pot;
    }

    /** The player's pot, loaded from their saved data the first time it is needed. */
    private static CookingPotBlockEntity pot(ServerPlayer player) {
        return POTS.computeIfAbsent(player.getUUID(), id -> {
            CookingPotBlockEntity made = virtualPot(player.level(), player.blockPosition());
            made.load(player.getPersistentData().getCompound(SAVED));
            return made;
        });
    }

    public static void open(ServerPlayer player) {
        CookingPotBlockEntity pot = pot(player);
        BlockPos pos = player.blockPosition();
        pot.setLevel(player.level());
        NetworkHooks.openScreen(player, pot, buf -> buf.writeBlockPos(pos));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        // a pot left cooking keeps cooking after a relog, before its screen is opened again
        CookingPotBlockEntity pot = POTS.containsKey(player.getUUID()) || player.getPersistentData().contains(SAVED) ? pot(player) : null;
        if (pot != null) {
            pot.setLevel(player.level());
            CookingPotBlockEntity.cookingTick(player.level(), pot.getBlockPos(), pot.getBlockState(), pot);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CookingPotBlockEntity pot = POTS.remove(event.getEntity().getUUID());
        if (pot != null) {
            event.getEntity().getPersistentData().put(SAVED, pot.saveWithoutMetadata());
        }
    }

    @SubscribeEvent
    public static void onSave(PlayerEvent.SaveToFile event) {
        CookingPotBlockEntity pot = POTS.get(event.getEntity().getUUID());
        if (pot != null) {
            event.getEntity().getPersistentData().put(SAVED, pot.saveWithoutMetadata());
        }
    }
}
