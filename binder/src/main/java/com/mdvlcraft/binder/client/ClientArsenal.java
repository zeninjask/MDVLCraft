package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.FormlessSwordAbility;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/** The local player's Formless Sword state (network/ArsenalSyncPacket, from shadow/ShadowArsenal). */
public final class ClientArsenal {
    /** ShadowArsenal.Mode.FORM's ordinal. */
    private static final int FORM = 1;
    private static int mode;
    private static String pick = "";
    private static List<Item> arsenal = List.of();
    private static List<Optional<Item>> wheel = Collections.nCopies(8, Optional.empty());

    private ClientArsenal() {
    }

    public static void update(int newMode, String newPick, List<String> newArsenal, List<String> newWheel) {
        mode = newMode;
        pick = newPick;
        arsenal = newArsenal.stream().map(ClientArsenal::item).filter(Objects::nonNull).toList();
        wheel = newWheel.stream().map(id -> Optional.ofNullable(id.isEmpty() ? null : item(id))).toList();
    }

    private static Item item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null || !ForgeRegistries.ITEMS.containsKey(key) ? null : ForgeRegistries.ITEMS.getValue(key);
    }

    public static void openScreen() {
        Minecraft.getInstance().setScreen(new ArsenalScreen());
    }

    static List<Item> arsenal() {
        return arsenal;
    }

    static Optional<Item> wheel(int slot) {
        return slot >= 0 && slot < wheel.size() ? wheel.get(slot) : Optional.empty();
    }

    static String pick() {
        return pick;
    }

    /** Formless Sword is the selected ability and in Form mode: holding the cast key opens the sword wheel. */
    static boolean formSelected() {
        return mode == FORM && ClientAbilities.slot(ClientAbilities.selected()).filter(a -> a == FormlessSwordAbility.INSTANCE).isPresent();
    }

    static String id(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).toString();
    }
}
