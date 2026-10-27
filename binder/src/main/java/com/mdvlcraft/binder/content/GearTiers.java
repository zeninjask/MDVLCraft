package com.mdvlcraft.binder.content;

import com.stereowalker.reforged.Reforged;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Reforged tiers (data: kubejs/data/mdvlcraft/tiered_modifiers, Reforged's own pools are switched off).
 * Its pools name mdvlcraft:tiered/weapons, /bows and /armour, which ItemVerifierMixin resolves here: a weapon is anything
 * that deals attack damage in the main hand except pickaxes, shovels and hoes; a bow is any projectile weapon; armour is
 * any armour piece. Reforged itself tiers the main inventory and open containers; on login the armour and off-hand slots
 * are rolled too, so gear made before Reforged was installed gets its tier once.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class GearTiers {
    public static final String TAG_PREFIX = "mdvlcraft:tiered/";

    private GearTiers() {
    }

    public static boolean matches(String kind, Item item) {
        return switch (kind) {
            case "weapons" -> !(item instanceof ProjectileWeaponItem || item instanceof PickaxeItem || item instanceof ShovelItem || item instanceof HoeItem)
                && item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
            case "bows" -> item instanceof ProjectileWeaponItem;
            case "armour" -> item instanceof ArmorItem;
            default -> throw new IllegalArgumentException("unknown Reforged gear kind " + kind);
        };
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.getInventory().armor.forEach(GearTiers::roll);
        player.getInventory().offhand.forEach(GearTiers::roll);
    }

    private static void roll(ItemStack stack) {
        Reforged.attemptToAffixTier(stack);
    }
}
