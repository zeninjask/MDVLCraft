package com.mdvlcraft.binder.compat.antimagic;

import com.mdvlcraft.binder.content.MageHunterSlot;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * The Mage Hunter tree's Plunder I, II and III (looting_1 to looting_3, one after another past Summon Demon-Destroyer
 * Sword) give every demon sword the player carries Looting I, II or III. Checked twice a second, so a freshly summoned
 * sword has it at once and losing the nodes (respec) takes it off again.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SwordLooting {
    private static final ResourceLocation TREE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "magehunter");
    private static final String[] NODES = {"looting_1", "looting_2", "looting_3"};
    private static final int CHECK_INTERVAL = 10;

    private SwordLooting() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        Inventory inventory = player.getInventory();
        int level = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (MageHunterSlot.isSword(stack)) {
                if (level < 0) {
                    level = level(player);
                }
                if (level < 0) {
                    return;
                }
                apply(stack, level);
            }
        }
    }

    /** The Looting level the Mage Hunter tree gives, or -1 without that tree (the vocation rework removed it). */
    private static int level(ServerPlayer player) {
        Category tree = SkillsAPI.getCategory(TREE).orElse(null);
        if (tree == null) {
            return -1;
        }
        int level = 0;
        for (int i = 0; i < NODES.length; i++) {
            String node = NODES[i];
            Skill skill = tree.getSkill(node).orElseThrow(() -> new IllegalStateException("Mage Hunter node " + node + " is missing"));
            if (skill.getState(player) == Skill.State.UNLOCKED) {
                level = i + 1;
            }
        }
        return level;
    }

    private static void apply(ItemStack stack, int level) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.getOrDefault(Enchantments.MOB_LOOTING, 0) == level) {
            return;
        }

        if (level == 0) {
            enchantments.remove(Enchantments.MOB_LOOTING);
        } else {
            enchantments.put(Enchantments.MOB_LOOTING, level);
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }
}
