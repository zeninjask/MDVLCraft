package com.mdvlcraft.binder.gate;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Gate tempering (plan 6.1): a Temper Rune put on a weapon or armour piece in an anvil stores its rank in the item's NBT
 * ({@link #TAG}, 1 = E .. 6 = S). A tempered weapon deals 2% (E) to 12% (S) more damage to gate mobs; each tempered
 * armour piece takes a quarter of that off damage from gate mobs (a full S set: 12%). A higher rune replaces a lower one.
 * The plan's small general bonus at A-S is not done yet.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GateTempering {
    public static final String TAG = "mdvlcraft_temper";

    private GateTempering() {
    }

    static int percent(GateRank rank) {
        return 2 * (rank.ordinal() + 1);
    }

    @Nullable
    static GateRank temper(ItemStack stack) {
        int level = stack.getTag() == null ? 0 : stack.getTag().getInt(TAG);
        return level < 1 || level > GateRank.values().length ? null : GateRank.values()[level - 1];
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack gear = event.getLeft();
        if (!(event.getRight().getItem() instanceof TemperRuneItem rune) || !gear.isDamageableItem() || gear.getCount() != 1) {
            return;
        }
        GateRank current = temper(gear);
        if (current != null && current.ordinal() >= rune.rank().ordinal()) {
            return;
        }
        ItemStack out = gear.copy();
        out.getOrCreateTag().putInt(TAG, rune.rank().ordinal() + 1);
        event.setOutput(out);
        event.setCost(2 + rune.rank().ordinal() * 2);
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof Player player && GateSpawner.instanceOf(victim) >= 0) {
            GateRank rank = temper(player.getMainHandItem());
            if (rank != null) {
                event.setAmount(event.getAmount() * (1.0F + percent(rank) / 100.0F));
            }
        } else if (victim instanceof Player player && event.getSource().getEntity() != null && GateSpawner.instanceOf(event.getSource().getEntity()) >= 0) {
            float reduction = 0.0F;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack piece = player.getItemBySlot(slot);
                GateRank rank = slot.getType() == EquipmentSlot.Type.ARMOR && piece.getItem() instanceof ArmorItem ? temper(piece) : null;
                if (rank != null) {
                    reduction += percent(rank) / 4.0F / 100.0F;
                }
            }
            event.setAmount(event.getAmount() * (1.0F - reduction));
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        GateRank rank = temper(event.getItemStack());
        if (rank != null) {
            event.getToolTip().add(Component.translatable("gate.mdvlcraft.tempered", rank.name(), percent(rank)).withStyle(ChatFormatting.AQUA));
        }
    }
}
