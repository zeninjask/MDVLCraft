package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.FormlessSwordAbility;
import com.mdvlcraft.binder.content.MageHunterSlot;
import com.mdvlcraft.binder.shadow.ShadowArsenal;
import com.mdvlcraft.binder.shadow.ShadowWeapons;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** The local player's Formless Sword state (network/ArsenalSyncPacket, from shadow/ShadowArsenal). */
public final class ClientArsenal {
    /** ShadowArsenal.Mode.FORM's ordinal. */
    private static final int FORM = 1;
    private static int mode;
    private static String pick = "";
    private static List<Entry> arsenal = List.of();
    private static List<Optional<Entry>> wheel = Collections.nCopies(8, Optional.empty());

    /** An arsenal or wheel entry: a weapon, or its off-hand version (ShadowArsenal.OFFHAND_SUFFIX). */
    record Entry(Item item, boolean offhand) {
        String id() {
            return ForgeRegistries.ITEMS.getKey(this.item) + (this.offhand ? ShadowArsenal.OFFHAND_SUFFIX : "");
        }

        Component name() {
            return this.offhand ? Component.translatable("screen.mdvlcraft.arsenal.offhand", this.item.getDescription()) : this.item.getDescription();
        }

        ItemStack stack() {
            return new ItemStack(this.item);
        }

        /** Off-hand versions only work while a one-handed shadow weapon is out in the shadow slot. */
        boolean usable() {
            if (!this.offhand) {
                return true;
            }
            ItemStack main = Minecraft.getInstance().player.getInventory().getItem(MageHunterSlot.SLOT);
            return ShadowWeapons.is(main) && arsenal.contains(new Entry(main.getItem(), true));
        }
    }

    private ClientArsenal() {
    }

    public static void update(int newMode, String newPick, List<String> newArsenal, List<String> newWheel) {
        mode = newMode;
        pick = newPick;
        arsenal = newArsenal.stream().map(ClientArsenal::entry).filter(Objects::nonNull).toList();
        wheel = newWheel.stream().map(id -> Optional.ofNullable(id.isEmpty() ? null : entry(id))).toList();
    }

    private static Entry entry(String id) {
        boolean offhand = id.endsWith(ShadowArsenal.OFFHAND_SUFFIX);
        ResourceLocation key = ResourceLocation.tryParse(offhand ? id.substring(0, id.length() - ShadowArsenal.OFFHAND_SUFFIX.length()) : id);
        return key == null || !ForgeRegistries.ITEMS.containsKey(key) ? null : new Entry(ForgeRegistries.ITEMS.getValue(key), offhand);
    }

    public static void openScreen() {
        Minecraft.getInstance().setScreen(new ArsenalScreen());
    }

    static List<Entry> arsenal() {
        return arsenal;
    }

    static Optional<Entry> wheel(int slot) {
        return slot >= 0 && slot < wheel.size() ? wheel.get(slot) : Optional.empty();
    }

    static String pick() {
        return pick;
    }

    /** Formless Sword is the selected ability and in Form mode: holding the cast key opens the sword wheel. */
    static boolean formSelected() {
        return mode == FORM && ClientAbilities.slot(ClientAbilities.selected()).filter(a -> a == FormlessSwordAbility.INSTANCE).isPresent();
    }
}
