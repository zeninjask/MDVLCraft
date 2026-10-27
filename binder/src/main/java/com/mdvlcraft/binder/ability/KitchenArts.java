package com.mdvlcraft.binder.ability;

import cursedfate.entity.DivineFlameProjectileEntity;
import cursedfate.init.CursedfateModEntities;
import cursedfate.network.CursedfateModVariables.PlayerVariables;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/**
 * The Master Chef's arts.
 * - Malevolent Kitchen: the first cast opens a few seconds to pick the domain (each further cast switches closed / open),
 *   as Cursed Fate's own domain picker; then that Malevolent Shrine opens at maximum output. While it stands it costs 10
 *   mana a second (DomainUpkeep), holding the cast key cuts every mob nearby, and sneak-casting closes it.
 * - Sear: Divine Flame, Cursed Fate's fire arrow, loosed where the chef looks.
 * Chop (Dismantle, the chanted Calamitous Shrine Dismantle when sneaking), Mince (Cleave) and Dicing Board (the Calamitous
 * Shrine's Enhanced Spiderweb) are Cursed Fate abilities cast by TechniqueRunner.
 */
final class KitchenArts {
    /** The Calamitous Shrine's Dismantle (its chanted form when Chant is set). */
    static final int CALAMITOUS_DISMANTLE = 95;
    private static final int PICK_TICKS = 60;
    private static final double MAX_OUTPUT = 100.0;
    private static final float SEAR_SPEED = 1.5F;
    /** Players picking their domain: whether open was picked, and when the pick closes (game time). */
    private static final Map<UUID, Pick> PICKING = new HashMap<>();

    private KitchenArts() {
    }

    static void press(ServerPlayer player) {
        if (DomainUpkeep.isActive(player)) {
            TechniqueRunner.pressShrine(player, Technique.MALEVOLENT_SHRINE);
            return;
        }
        Pick pick = PICKING.get(player.getUUID());
        if (pick != null) {
            pick.open = !pick.open;
            show(player, pick);
            return;
        }
        long cooldown = TechniqueCooldowns.remaining(player, Technique.MALEVOLENT_KITCHEN);
        if (cooldown > 0L) {
            player.displayClientMessage(Component.translatable("ability.mdvlcraft.on_cooldown", Technique.MALEVOLENT_KITCHEN.displayName(),
                (cooldown + 19L) / 20L).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!player.isCreative() && MagicData.getPlayerMagicData(player).getMana() < DomainUpkeep.manaPerSecond(Technique.MALEVOLENT_SHRINE)) {
            player.displayClientMessage(Component.translatable("ui.irons_spellbooks.cast_error_mana", Technique.MALEVOLENT_KITCHEN.displayName())
                .withStyle(ChatFormatting.RED), true);
            return;
        }
        pick = new Pick(player.level().getGameTime() + PICK_TICKS);
        PICKING.put(player.getUUID(), pick);
        show(player, pick);
    }

    static void tick(MinecraftServer server) {
        PICKING.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                return true;
            }
            if (player.level().getGameTime() < entry.getValue().until) {
                return false;
            }
            PlayerVariables variables = TechniqueRunner.variables(player);
            variables.Output = MAX_OUTPUT;
            variables.MaximumOutput = true;
            variables.syncPlayerVariables(player);
            if (TechniqueRunner.cast(player, entry.getValue().open ? Technique.OPEN_MALEVOLENT_SHRINE : Technique.MALEVOLENT_SHRINE)) {
                TechniqueCooldowns.start(player, Technique.MALEVOLENT_KITCHEN);
            }
            return true;
        });
    }

    private static void show(ServerPlayer player, Pick pick) {
        player.displayClientMessage(Component.translatable(pick.open ? "ability.mdvlcraft.malevolent_kitchen.open" : "ability.mdvlcraft.malevolent_kitchen.closed")
            .withStyle(ChatFormatting.DARK_RED), true);
    }

    static boolean sear(ServerPlayer player) {
        AbstractArrow flame = new DivineFlameProjectileEntity(CursedfateModEntities.DIVINE_FLAME_PROJECTILE.get(), player.level());
        flame.setOwner(player);
        flame.setBaseDamage(0.0);
        flame.setSilent(true);
        flame.setPierceLevel((byte)99);
        flame.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        Vec3 look = player.getLookAngle();
        flame.shoot(look.x, look.y, look.z, SEAR_SPEED, 0.0F);
        player.level().addFreshEntity(flame);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    private static final class Pick {
        final long until;
        boolean open;

        Pick(long until) {
            this.until = until;
        }
    }
}
