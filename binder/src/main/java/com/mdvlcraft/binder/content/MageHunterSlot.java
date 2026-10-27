package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.AbilityGrants;
import com.mdvlcraft.binder.shadow.ShadowWeapons;
import com.mdvlcraft.binder.skills.Respec;
import java.util.Collection;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import com.tiago.antimagic.item.AbstractAntiMagicSwordItem;
import java.util.function.Predicate;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * A Mage Hunter's first hotbar slot is the sword slot. It holds only the demon sword they summon, and nothing else goes
 * in. The sword can't be moved out by clicking, number keys, shift-clicking, dropping (Q) or swapping hands (F).
 * - Summoning puts the sword there and selects it (mixin/antimagic/SummonDemonSwordSpellMixin).
 * - Summoning a different sword desummons the one there.
 * - Casting the same summon again desummons it and gives back the slot held before.
 * The guards:
 * - mixin/SlotMixin: no clicks in the slot.
 * - mixin/AbstractContainerMenuMixin: no number-key swaps with it.
 * - mixin/InventoryMixin: picked-up items skip it.
 * - mixin/ServerPlayerDropMixin and mixin/LocalPlayerDropMixin: no Q.
 * - Here: no F.
 * A shadow weapon in the off hand (ShadowArsenal's off-hand weapons) is guarded the same way: no clicks, no swap keys, no F.
 * A check twice a second also catches what other mods put in or take out: an item in the slot goes back into the bag
 * (or on the ground if the bag is full). A demon sword anywhere else goes into the slot if it is empty, else it is
 * desummoned. The sword isn't dropped on death.
 * A Mage Hunter is anyone with a demon sword summon unlocked (no tree grants one since the vocations rework). The client
 * knows it for the local player from its granted abilities (client/ClientAbilities, set by setClientCheck).
 * A samurai (the archetype picked) has the same slot for Formless Sword's shadow weapons (shadow/ShadowArsenal): only
 * shadow weapons go in, from the moment the archetype is picked.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MageHunterSlot {
    public static final int SLOT = 0;
    /** The archetype whose pick gives the slot (for shadow weapons). */
    public static final String SAMURAI = "samurai";
    /** The spells that summon a demon sword; whoever has one of them unlocked has the sword slot. */
    private static final Set<ResourceLocation> SWORD_SUMMONS = Set.of(
        ResourceLocation.fromNamespaceAndPath("antimagic", "summon_demon_slayer"),
        ResourceLocation.fromNamespaceAndPath("antimagic", "summon_demon_dweller"),
        ResourceLocation.fromNamespaceAndPath("antimagic", "summon_demon_destroyer")
    );
    private static final int CHECK_INTERVAL = 10;
    private static Predicate<Player> clientCheck = player -> false;

    private MageHunterSlot() {
    }

    public static void setClientCheck(Predicate<Player> check) {
        clientCheck = check;
    }

    public static boolean isMageHunter(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return summonsSwords(AbilityGrants.of(serverPlayer).keySet()) || Respec.isPicked(serverPlayer, SAMURAI);
        }
        return player != null && player.level().isClientSide && clientCheck.test(player);
    }

    /** Whether these granted abilities include a demon sword summon. */
    public static boolean summonsSwords(Collection<Ability> granted) {
        return granted.stream().anyMatch(ability -> SWORD_SUMMONS.contains(ability.id()));
    }

    /**
     * Whether the container slot is a Mage Hunter's sword slot, a first slot kept empty by Keep Slot Open (content/KeepSlotOpen),
     * or the off hand holding a shadow weapon (creative mode leaves all open).
     */
    public static boolean isSwordSlot(Container container, int index) {
        if (!(container instanceof Inventory inventory) || inventory.player.isCreative()) {
            return false;
        }
        return index == SLOT ? isMageHunter(inventory.player) || KeepSlotOpen.applies(inventory.player)
            : index == Inventory.SLOT_OFFHAND && ShadowWeapons.is(inventory.getItem(index));
    }

    public static boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof AbstractAntiMagicSwordItem;
    }

    /** What the slot holds: a demon sword or a shadow weapon (shadow/ShadowArsenal). */
    public static boolean belongs(ItemStack stack) {
        return isSword(stack) || ShadowWeapons.is(stack);
    }

    /**
     * Before a Mage Hunter's summon: the slot is cleared of anything but this sword and selected, so the spell
     * summons into it or, if this sword is there, desummons it.
     * @return the slot to select again after a desummon, or -1
     */
    public static int beforeSummon(ServerPlayer player, Item sword) {
        enforce(player);
        Inventory inventory = player.getInventory();
        ItemStack held = inventory.getItem(SLOT);
        int previous = held.is(sword) && inventory.selected != SLOT ? inventory.selected : -1;
        if (!held.isEmpty() && !held.is(sword)) {
            inventory.setItem(SLOT, ItemStack.EMPTY);
        }

        select(player, SLOT);
        return previous;
    }

    public static void select(ServerPlayer player, int slot) {
        if (player.getInventory().selected != slot) {
            player.getInventory().selected = slot;
            player.connection.send(new ClientboundSetCarriedItemPacket(slot));
        }
    }

    private static void enforce(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack first = inventory.getItem(SLOT);
        if (!first.isEmpty() && !belongs(first)) {
            inventory.setItem(SLOT, ItemStack.EMPTY);
            if (!inventory.add(first)) {
                player.drop(first, false);
            }
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (i != SLOT && belongs(stack) && !(i == Inventory.SLOT_OFFHAND && ShadowWeapons.is(stack))) {   // the off-hand shadow stays (ShadowArsenal)
                inventory.setItem(i, ItemStack.EMPTY);
                if (inventory.getItem(SLOT).isEmpty()) {
                    inventory.setItem(SLOT, stack);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
            && player.tickCount % CHECK_INTERVAL == 0 && !player.isCreative() && isMageHunter(player)) {
            enforce(player);
        }
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof Player player && !player.isCreative()
            && (player.getInventory().selected == SLOT && isMageHunter(player) || ShadowWeapons.is(player.getOffhandItem()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isMageHunter(player)) {
            event.getDrops().removeIf(drop -> belongs(drop.getItem()));
        }
    }
}
