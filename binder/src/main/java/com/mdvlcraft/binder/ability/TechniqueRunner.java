package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import cursedfate.network.CursedfateModVariables;
import cursedfate.procedures.AbilitysProcedure;
import cursedfate.procedures.ReturnPlayerNearAOpenedDomainProcedure;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Starts Cursed Fate techniques the way its ability key does, minus its checks: the binder pays with Iron's mana,
 * so cursed energy is refilled on every cast, and Cursed Fate's cooldown check (which lives only in its key handler) is
 * never reached. Mastery is reset before each cast so technique damage only grows with gear and the skill tree.
 *
 * Techniques with {@link Technique#repeatTicks} keep casting while the cast key is held (Dismantle and Cleave, e.g.
 * inside the Malevolent Shrine). The Shrine always opens Cursed Fate's open-domain variant, which is a toggle: casting
 * it near your open domain closes it, and closing is free. {@link DomainUpkeep} follows it from wind-up to close.
 */
final class TechniqueRunner {
    private record Held(Technique technique, long nextTick) {
    }

    private static final Map<UUID, Held> HELD = new HashMap<>();

    private TechniqueRunner() {
    }

    static void press(ServerPlayer player, Technique technique) {
        if (cast(player, technique) && technique.repeatTicks > 0) {
            HELD.put(player.getUUID(), new Held(technique, player.level().getGameTime() + technique.repeatTicks));
        }
    }

    /** Key released: stops repeating and clears Cursed Fate's held state, exactly like its key-release handler. */
    static void release(ServerPlayer player) {
        HELD.remove(player.getUUID());
        CursedfateModVariables.PlayerVariables variables = variables(player);
        if (!variables.holdability1) {
            return;
        }
        variables.holdability1 = false;
        variables.holdability = false;
        variables.AbilityToCast = 0;
        variables.syncPlayerVariables(player);
    }

    /** Server tick: recasts held techniques when due; stops when the player leaves or runs out of mana. */
    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Held>> entries = HELD.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Held> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
                continue;
            }
            Held held = entry.getValue();
            long now = player.level().getGameTime();
            if (now < held.nextTick()) {
                continue;
            }
            if (cast(player, held.technique())) {
                entry.setValue(new Held(held.technique(), now + held.technique().repeatTicks));
            } else {
                entries.remove();
            }
        }
    }

    /** Pays and casts once. Returns false (with the action bar message) if the player cannot afford it. */
    private static boolean cast(ServerPlayer player, Technique technique) {
        if (technique == Technique.MALEVOLENT_SHRINE && DomainUpkeep.isWindingUp(player)) {
            // Cursed Fate reads a press during the wind-up as "switch domain variant"; ignore it instead.
            return false;
        }
        MagicData magic = MagicData.getPlayerMagicData(player);
        int cost = cost(player, technique);
        if (!player.isCreative()) {
            if (magic.getMana() < cost) {
                player.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.cast_error_mana", technique.displayName()).withStyle(ChatFormatting.RED)));
                return false;
            }
            magic.setMana(magic.getMana() - cost);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        }
        CursedfateModVariables.PlayerVariables variables = variables(player);
        // Never lower it: an open domain's upkeep may hold it above the maximum.
        variables.CursedEnegry = Math.max(variables.CursedEnegry, variables.CursedEnegryMax);
        variables.InnateMastery = 0;
        variables.InnateMasteryXp = 0;
        variables.AbilityNum1 = technique.abilityId;
        variables.holdability1 = true;
        variables.holdability = true;
        variables.syncPlayerVariables(player);
        AbilitysProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ(), player);
        if (technique == Technique.MALEVOLENT_SHRINE && variables.SelectDomainVarient) {
            // Cursed Fate started its wind-up and reset the variant to its closed barrier domain, which recasting
            // cannot close. Variant 1 is the open Malevolent Shrine; Cursed Fate reads it when the wind-up ends.
            variables.DomainVarientNumber = 1;
            variables.syncPlayerVariables(player);
            DomainUpkeep.windingUp(player);
        }
        return true;
    }

    private static int cost(ServerPlayer player, Technique technique) {
        // Cursed Fate closes the domain (or refuses to clash) when the Shrine is cast near an open one; neither costs.
        if (technique == Technique.MALEVOLENT_SHRINE
                && ReturnPlayerNearAOpenedDomainProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ())) {
            return 0;
        }
        return Math.round(technique.manaCost(1) * (float) (1 - player.getAttributeValue(BinderAttributes.TECHNIQUE_EFFICIENCY.get())));
    }

    static CursedfateModVariables.PlayerVariables variables(ServerPlayer player) {
        return player.getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Cursed Fate variables missing on " + player.getGameProfile().getName()));
    }
}
