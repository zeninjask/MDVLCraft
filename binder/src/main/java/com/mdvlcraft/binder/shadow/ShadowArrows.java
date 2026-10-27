package com.mdvlcraft.binder.shadow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Shadow bows and crossbows (ShadowWeapons) never use the player's arrows: while the player has ARROW_MANA mana their
 * projectile is a shadow arrow, so drawing (or Epic Fight bow attacks, ShadowBows) loads one; without the mana they can't
 * draw. Each arrow that leaves a shadow weapon costs ARROW_MANA and can't be picked up.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ShadowArrows {
    private static final float ARROW_MANA = 5.0F;

    private ShadowArrows() {
    }

    @SubscribeEvent
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        if (event.getEntity() instanceof Player player && ShadowWeapons.is(event.getProjectileWeaponItemStack())) {
            event.setProjectileItemStack(player.isCreative() || mana(player) >= ARROW_MANA ? new ItemStack(Items.ARROW) : ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow) || arrow instanceof ThrownTrident
            || !(arrow.getOwner() instanceof ServerPlayer player) || event.loadedFromDisk() || !shooting(player)) {
            return;
        }
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        if (player.isCreative()) {
            return;
        }
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < ARROW_MANA) {
            event.setCanceled(true);   // the mana went between drawing and loosing
            return;
        }
        magic.setMana(magic.getMana() - ARROW_MANA);
    }

    private static boolean shooting(Player player) {
        return isShadowLauncher(player.getMainHandItem()) || isShadowLauncher(player.getOffhandItem());
    }

    private static boolean isShadowLauncher(ItemStack stack) {
        return ShadowWeapons.is(stack) && stack.getItem() instanceof ProjectileWeaponItem;
    }

    private static float mana(Player player) {
        return player.level().isClientSide ? ClientMagicData.getPlayerMana() : MagicData.getPlayerMagicData(player).getMana();
    }
}
