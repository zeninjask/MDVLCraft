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
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Formless Sword (the samurai's ability, in place of Magic Blade: Absorb) and the Shadow Arsenal. Activating the ability
 * while sneaking cycles its mode: Absorb, Form, Select.
 * - Absorb takes the weapon in the main hand (else the off hand) into the player's arsenal: the item type only (enchantments are lost), each
 *   type once; the stack is used up.
 * - Select opens the arsenal screen (client/ArsenalScreen), where arsenal weapons are bound to the sword wheel's 8 slots.
 * - Form: holding the ability key opens the sword wheel (client/SwordWheelScreen) to pick a weapon; a tap summons its
 *   shadow (ShadowWeapons) into the first hotbar slot, or dismisses the one out. A shadow weapon drains MANA_PER_SECOND
 *   and fades when the mana runs out, on death, or if the player loses Formless Sword.
 * - A one-handed weapon in the arsenal also shows as "weapon - offhand" (id + OFFHAND_SUFFIX). Picking that on the
 *   wheel puts its shadow in the off hand while a one-handed shadow weapon is out in the shadow slot (whatever was in the
 *   off hand goes to the bag); it costs no extra mana and goes when the main one goes or stops being one-handed.
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
    private static final String OFFHAND = "mdvlcraft:formless_offhand";
    /** Marks an arsenal or wheel entry as the off-hand version of the weapon. */
    public static final String OFFHAND_SUFFIX = "#offhand";

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
        removeShadows(inventory, false);
        ItemStack there = inventory.getItem(MageHunterSlot.SLOT);
        if (!there.isEmpty()) {
            inventory.setItem(MageHunterSlot.SLOT, ItemStack.EMPTY);
            if (!inventory.add(there)) {
                player.drop(there, false);
            }
        }
        inventory.setItem(MageHunterSlot.SLOT, ShadowWeapons.make(item, ShadowEnchantReward.of(player)));
    }

    /** The shadow of `item` in the off hand; whatever else was there goes to the bag (or the ground). */
    private static void placeOffhand(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        ItemStack there = inventory.getItem(Inventory.SLOT_OFFHAND);
        inventory.setItem(Inventory.SLOT_OFFHAND, ShadowWeapons.make(item, ShadowEnchantReward.of(player)));
        if (!there.isEmpty() && !ShadowWeapons.is(there) && !inventory.add(there)) {
            player.drop(there, false);
        }
    }

    public static void dismiss(ServerPlayer player) {
        data(player).putString(OUT, "");
        data(player).putString(OFFHAND, "");
        removeShadows(player.getInventory(), true);
    }

    private static void dismissOffhand(ServerPlayer player) {
        data(player).putString(OFFHAND, "");
        if (ShadowWeapons.is(player.getOffhandItem())) {
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
        }
    }

    private static void removeShadows(Inventory inventory, boolean offhandToo) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (ShadowWeapons.is(inventory.getItem(i)) && (offhandToo || i != Inventory.SLOT_OFFHAND)) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** Whether the weapon can be held in one hand (Epic Fight's one-handed style), so it also has an off-hand version. */
    public static boolean oneHanded(Item item) {
        ItemStack stack = new ItemStack(item);
        return ShadowWeapons.absorbable(stack) && EpicFightCapabilities.getItemStackCapability(stack).canBePlacedOffhand();
    }

    private static Item item(String id) {
        return id.isEmpty() ? null : ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
    }

    /** New enchantments (a Shadow Arsenal node unlocked or lost) reach the shadow weapon that is out. */
    static void refresh(ServerPlayer player) {
        Item item = item(out(player));
        if (item != null) {
            place(player, item);
        }
        Item offhand = item(data(player).getString(OFFHAND));
        if (offhand != null) {
            placeOffhand(player, offhand);
        }
    }

    // ---- from the client -----------------------------------------------------------------------------------------
    /** The sword wheel's choice: the weapon Form summons; a shadow weapon out is swapped for it. */
    public static void pick(ServerPlayer player, String id) {
        if (!wheel(player).contains(id) || !granted(player)) {
            return;
        }
        if (id.endsWith(OFFHAND_SUFFIX)) {
            pickOffhand(player, id.substring(0, id.length() - OFFHAND_SUFFIX.length()));
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

    /** An off-hand entry of the wheel: its shadow goes in the off hand, if a one-handed shadow weapon is out. */
    private static void pickOffhand(ServerPlayer player, String id) {
        Item main = item(out(player));
        Item item = item(id);
        if (item == null || !oneHanded(item) || main == null || !oneHanded(main)) {
            tell(player, Component.translatable("ability.mdvlcraft.formless_sword.no_offhand"), ChatFormatting.GRAY);
            return;
        }
        data(player).putString(OFFHAND, id);
        placeOffhand(player, item);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 0.7F);
        tell(player, Component.translatable("ability.mdvlcraft.formless_sword.picked_offhand", item.getDescription()), ChatFormatting.DARK_PURPLE);
    }

    /** The arsenal screen binds an arsenal weapon to a sword wheel slot (or clears it). */
    public static void bind(ServerPlayer player, int slot, Optional<String> id) {
        if (slot < 0 || slot >= WHEEL || id.isPresent() && !entries(player).contains(id.get())) {
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
        Item offhand = item(data(player).getString(OFFHAND));
        if (offhand != null) {
            if (!oneHanded(item)) {
                dismissOffhand(player);
            } else if (!ShadowWeapons.is(player.getOffhandItem()) || player.getOffhandItem().getItem() != offhand) {
                placeOffhand(player, offhand);   // taken away (a disarm): it comes back
            }
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

    /** The arsenal as the screen lists it: each weapon, followed by its off-hand version if it is one-handed. */
    private static List<String> entries(Player player) {
        List<String> entries = new ArrayList<>();
        for (String id : arsenal(player)) {
            entries.add(id);
            Item item = item(id);
            if (item != null && oneHanded(item)) {
                entries.add(id + OFFHAND_SUFFIX);
            }
        }
        return entries;
    }

    public static void sync(ServerPlayer player) {
        BinderNetwork.sendTo(player, new ArsenalSyncPacket(mode(player).ordinal(), data(player).getString(PICK), entries(player), wheel(player)));
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
