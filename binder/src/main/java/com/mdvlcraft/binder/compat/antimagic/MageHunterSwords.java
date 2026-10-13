package com.mdvlcraft.binder.compat.antimagic;

import com.tiago.antimagic.item.AbstractAntiMagicSwordItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * The summoned anti-magic greatswords hit like an Epic Fight iron greatsword for a fresh Mage Hunter and like a netherite
 * one once about half the Mage Hunter tree's points are spent, scaling in between.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MageHunterSwords {
    private static final ResourceLocation TREE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "magehunter");
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("epicfight", "iron_greatsword");
    private static final ResourceLocation NETHERITE = ResourceLocation.fromNamespaceAndPath("epicfight", "netherite_greatsword");
    /** About half of the Mage Hunter tree's 503 points. */
    static final int FULL_STRENGTH_POINTS = 250;

    private MageHunterSwords() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getSource().getDirectEntity() == player) {
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof AbstractAntiMagicSwordItem) {
                double sword = damage(held);
                double target = targetDamage(player);
                event.setAmount((float)(event.getAmount() * (1.0 + target) / (1.0 + sword)));
            }
        }
    }

    static double targetDamage(ServerPlayer player) {
        Category tree = SkillsAPI.getCategory(TREE).orElseThrow(() -> new IllegalStateException("Skill tab " + TREE + " is missing"));
        double progress = Math.min(1.0, tree.getSpentPoints(player) / (double)FULL_STRENGTH_POINTS);
        double iron = damage(new ItemStack(item(IRON)));
        return iron + (damage(new ItemStack(item(NETHERITE))) - iron) * progress;
    }

    private static Item item(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || !id.equals(ForgeRegistries.ITEMS.getKey(item))) {
            throw new IllegalStateException(id + " is not registered");
        }
        return item;
    }

    /** The main-hand attack damage the item adds on top of the player's base 1. */
    private static double damage(ItemStack stack) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
            .filter(m -> m.getOperation() == AttributeModifier.Operation.ADDITION).mapToDouble(AttributeModifier::getAmount).sum();
    }
}
