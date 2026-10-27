package com.mdvlcraft.binder.shadow;

import com.mdvlcraft.binder.ability.AbilityGrants;
import com.mdvlcraft.binder.ability.FormlessSwordAbility;
import com.mdvlcraft.binder.content.MageHunterSlot;
import com.mdvlcraft.binder.network.ArsenalSyncPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.OpenArsenalPacket;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Formless Sword (the samurai's ability, in place of Magic Blade: Absorb) and the Shadow Arsenal. Activating the ability
 * while sneaking cycles its mode: Absorb, Form, Select.
 * - Absorb takes the weapon in the main hand (else the off hand) into the player's arsenal: the item type only (enchantments are lost), each
 *   type once; the stack is used up.
 * - Select opens the arsenal screen (client/ArsenalScreen), where arsenal weapons are bound to the sword wheel's 8 slots.
 * - Form: holding the ability key opens the sword wheel (client/SwordWheelScreen) to pick a weapon; a tap summons its
 *   shadow (ShadowWeapons) into the first hotbar slot, or dismisses the one out. A shadow weapon drains MANA_PER_SECOND
 *   and fades when the mana runs out, on death, or if the player loses Formless Sword.
 * The first hotbar slot is the samurai's shadow slot (content/MageHunterSlot): only shadow weapons go in, and they can't
 * be moved, dropped or swapped out. One that is taken anyway (a disarm) comes back at the next check.
 * Saved in the player's persisted data; the client gets it through ArsenalSyncPacket.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ShadowArsenal {
    public enum Mode { ABSORB, FORM, SELECT }

    public static final int WHEEL = 8;
    private static final float MANA_PER_SECOND = 2.0F;
    private static final String ARSENAL = "mdvlcraft:arsenal";
    private static final String WHEEL_KEY = "mdvlcraft:arsenal_wheel";
    private static final String MODE = "mdvlcraft:formless_mode";
    private static final String PICK = "mdvlcraft:formless_pick";
    private static final String OUT = "mdvlcraft:formless_out";

    private ShadowArsenal() {
    }

    // ---- activation ----------------------------------------------------------------------------------------------
    public static void press(ServerPlayer player) {
        if (player.isShiftKeyDown()) {
            Mode next = Mode.values()[(mode(player).ordinal() + 1) % Mode.values().length];
            data(player).putInt(MODE, next.ordinal());
            sync(player);
            tell(player, Component.translatable("ability.mdvlcraft.formless_sword.mode",
                Component.translatable("ability.mdvlcraft.formless_sword.mode." + next.name().toLowerCase())), ChatFormatting.DARK_PURPLE);
            return;
        }
        switch (mode(player)) {
            case ABSORB -> absorb(player);
            case SELECT -> {
                sync(player);
                BinderNetwork.sendTo(player, new OpenArsenalPacket());
            }
            case FORM -> {
                if (!out(player).isEmpty()) {
                    dismiss(player);
                } else {
                    summon(player);
                }
            }
        }
    }

    private static void absorb(ServerPlayer player) {
        // the main hand's weapon first, else the off hand's
        InteractionHand hand = ShadowWeapons.absorbable(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack weapon = player.getItemInHand(hand);
        if (!ShadowWeapons.absorbable(weapon)) {
            tell(player, Component.translatable("ability.mdvlcraft.formless_sword.no_weapon"), ChatFormatting.GRAY);
            return;
        }
        String id = ForgeRegistries.ITEMS.getKey(weapon.getItem()).toString();
        List<String> arsenal = arsenal(player);
        if (arsenal.contains(id)) {
            tell(player, Component.translatable("ability.mdvlcraft.formless_sword.already", weapon.getItem().getDescription()), ChatFormatting.GRAY);
            return;
        }
        Component name = weapon.getItem().getDescription();
        player.setItemInHand(hand, ItemStack.EMPTY);
        arsenal.add(id);
        ListTag tag = new ListTag();
        arsenal.forEach(entry -> tag.add(StringTag.valueOf(entry)));
        data(player).put(ARSENAL, tag);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.6F);
        tell(player, Component.translatable("ability.mdvlcraft.formless_sword.absorbed", name), ChatFormatting.DARK_PURPLE);
        sync(player);
    }

    private static void summon(ServerPlayer player) {
        String pick = data(player).getString(PICK);
        Item item = pick.isEmpty() || !arsenal(player).contains(pick) ? null : ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(pick));
        if (item == null) {
            tell(player, Component.translatable("ability.mdvlcraft.formless_sword.no_pick"), ChatFormatting.GRAY);
            return;
        }
        if (MagicData.getPlayerMagicData(player).getMana() < MANA_PER_SECOND) {
            tell(player, Component.translatable("ability.mdvlcraft.formless_sword.no_mana"), ChatFormatting.GRAY);
            return;
        }
        data(player).putString(OUT, pick);
        place(player, item);
        MageHunterSlot.select(player, MageHunterSlot.SLOT);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    /** The shadow of `item` in the shadow slot; whatever else was there goes to the bag (or the ground). */
    private static void place(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        removeShadows(inventory);
        ItemStack there = inventory.getItem(MageHunterSlot.SLOT);
        if (!there.isEmpty()) {
            inventory.setItem(MageHunterSlot.SLOT, ItemStack.EMPTY);
            if (!inventory.add(there)) {
                player.drop(there, false);
            }
        }
        inventory.setItem(MageHunterSlot.SLOT, ShadowWeapons.make(item, ShadowEnchantReward.of(player)));
    }

    public static void dismiss(ServerPlayer player) {
        data(player).putString(OUT, "");
        removeShadows(player.getInventory());
    }

    private static void removeShadows(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (ShadowWeapons.is(inventory.getItem(i))) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** New enchantments (a Shadow Arsenal node unlocked or lost) reach the shadow weapon that is out. */
    static void refresh(ServerPlayer player) {
        String out = out(player);
        Item item = out.isEmpty() ? null : ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(out));
        if (item != null) {
            place(player, item);
        }
    }

    // ---- from the client -----------------------------------------------------------------------------------------
    /** The sword wheel's choice: the weapon Form summons; a shadow weapon out is swapped for it. */
    public static void pick(ServerPlayer player, String id) {
        if (!wheel(player).contains(id) || !granted(player)) {
            return;
        }
        data(player).putString(PICK, id);
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
        if (item != null) {
            tell(player, Component.translatable("ability.mdvlcraft.formless_sword.picked", item.getDescription()), ChatFormatting.DARK_PURPLE);
            if (!out(player).isEmpty()) {
                data(player).putString(OUT, id);
                place(player, item);
            }
        }
        sync(player);
    }

    /** The arsenal screen binds an arsenal weapon to a sword wheel slot (or clears it). */
    public static void bind(ServerPlayer player, int slot, Optional<String> id) {
        if (slot < 0 || slot >= WHEEL || id.isPresent() && !arsenal(player).contains(id.get())) {
            return;
        }
        List<String> wheel = wheel(player);
        wheel.set(slot, id.orElse(""));
        ListTag tag = new ListTag();
        wheel.forEach(entry -> tag.add(StringTag.valueOf(entry)));
        data(player).put(WHEEL_KEY, tag);
        sync(player);
    }

    // ---- upkeep --------------------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        String out = out(player);
        if (out.isEmpty()) {
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(out));
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (item == null || !granted(player) || !player.isAlive()) {
            dismiss(player);
            return;
        }
        if (!player.isCreative()) {
            if (magic.getMana() < MANA_PER_SECOND) {
                dismiss(player);
                tell(player, Component.translatable("ability.mdvlcraft.formless_sword.no_mana"), ChatFormatting.GRAY);
                return;
            }
            magic.setMana(magic.getMana() - MANA_PER_SECOND);
        }
        ItemStack held = player.getInventory().getItem(MageHunterSlot.SLOT);
        if (!ShadowWeapons.is(held) || held.getItem() != item) {
            place(player, item);   // taken away (a disarm): it comes back
        }
    }

    /** A shadow weapon never becomes a dropped item. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity drop && ShadowWeapons.is(drop.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !out(player).isEmpty()) {
            dismiss(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    // ---- data ----------------------------------------------------------------------------------------------------
    private static boolean granted(ServerPlayer player) {
        return AbilityGrants.level(player, FormlessSwordAbility.INSTANCE) > 0;
    }

    public static Mode mode(Player player) {
        int mode = data(player).getInt(MODE);
        return Mode.values()[Math.floorMod(mode, Mode.values().length)];
    }

    private static String out(Player player) {
        return data(player).getString(OUT);
    }

    public static List<String> arsenal(Player player) {
        List<String> arsenal = new ArrayList<>();
        data(player).getList(ARSENAL, Tag.TAG_STRING).forEach(tag -> arsenal.add(tag.getAsString()));
        return arsenal;
    }

    public static List<String> wheel(Player player) {
        List<String> wheel = new ArrayList<>(Collections.nCopies(WHEEL, ""));
        ListTag tag = data(player).getList(WHEEL_KEY, Tag.TAG_STRING);
        for (int i = 0; i < Math.min(WHEEL, tag.size()); i++) {
            wheel.set(i, tag.getString(i));
        }
        return wheel;
    }

    public static void sync(ServerPlayer player) {
        BinderNetwork.sendTo(player, new ArsenalSyncPacket(mode(player).ordinal(), data(player).getString(PICK), arsenal(player), wheel(player)));
    }

    private static void tell(ServerPlayer player, Component message, ChatFormatting colour) {
        player.displayClientMessage(message.copy().withStyle(colour), true);
    }

    private static CompoundTag data(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return persistent.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
