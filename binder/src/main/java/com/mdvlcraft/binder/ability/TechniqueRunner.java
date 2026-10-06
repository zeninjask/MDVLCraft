package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import cursedfate.network.CursedfateModVariables;
import cursedfate.procedures.AbilitysProcedure;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

/**
 * Starts Cursed Fate techniques the way its ability key does, minus its checks: the binder pays with Iron's mana,
 * so cursed energy is refilled on every cast, and Cursed Fate's cooldown check (which lives only in its key handler) is
 * never reached. Mastery is reset before each cast so technique damage only grows with gear and the skill tree.
 */
final class TechniqueRunner {
    private TechniqueRunner() {
    }

    static void press(ServerPlayer player, Technique technique) {
        MagicData magic = MagicData.getPlayerMagicData(player);
        int cost = Math.round(technique.manaCost(1) * (float) (1 - player.getAttributeValue(BinderAttributes.TECHNIQUE_EFFICIENCY.get())));
        if (!player.isCreative()) {
            if (magic.getMana() < cost) {
                player.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.cast_error_mana", technique.displayName()).withStyle(ChatFormatting.RED)));
                return;
            }
            magic.setMana(magic.getMana() - cost);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        }
        CursedfateModVariables.PlayerVariables variables = variables(player);
        variables.CursedEnegry = variables.CursedEnegryMax;
        variables.InnateMastery = 0;
        variables.InnateMasteryXp = 0;
        variables.AbilityNum1 = technique.abilityId;
        variables.holdability1 = true;
        variables.holdability = true;
        variables.syncPlayerVariables(player);
        AbilitysProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ(), player);
    }

    /** Key released: ends held techniques (Cleave, the domain) exactly like Cursed Fate's key-release handler. */
    static void release(ServerPlayer player) {
        CursedfateModVariables.PlayerVariables variables = variables(player);
        if (!variables.holdability1) {
            return;
        }
        variables.holdability1 = false;
        variables.holdability = false;
        variables.AbilityToCast = 0;
        variables.syncPlayerVariables(player);
    }

    private static CursedfateModVariables.PlayerVariables variables(ServerPlayer player) {
        return player.getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Cursed Fate variables missing on " + player.getGameProfile().getName()));
    }
}
