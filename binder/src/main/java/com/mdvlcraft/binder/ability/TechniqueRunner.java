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
 * Every technique has a cooldown ({@link TechniqueCooldowns}). {@link Technique#held} techniques (Dismantle and Cleave)
 * keep casting while the cast key is held, each time their cooldown ends. While one of them is held the owner's
 * Malevolent Shrine cuts too ({@link #isSlashing}); otherwise the domain stays quiet.
 * Each Shrine opens its own Cursed Fate domain variant (closed barrier dome or open shrine). Casting either Shrine while
 * your domain is up closes it, and closing is free. {@link DomainUpkeep} follows a domain from wind-up to close.
 */
final class TechniqueRunner {
    private static final Map<UUID, Technique> HELD = new HashMap<>();

    private TechniqueRunner() {
    }

    /** True while the player holds the cast key on Dismantle or Cleave (and can pay for it). */
    static boolean isSlashing(ServerPlayer player) {
        Technique held = HELD.get(player.getUUID());
        return held != null && held.isSlash();
    }

    static void press(ServerPlayer player, Technique technique) {
        if (!technique.held) {
            cast(player, technique);
            return;
        }
        // Held while cooling down: it casts as soon as the cooldown ends (tick).
        HELD.put(player.getUUID(), technique);
        if (TechniqueCooldowns.remaining(player, technique) == 0 && !cast(player, technique)) {
            HELD.remove(player.getUUID());
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

    /** Server tick: recasts held techniques when their cooldown ends; stops when the player leaves or runs out of mana. */
    static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Technique>> entries = HELD.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Technique> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
                continue;
            }
            if (TechniqueCooldowns.remaining(player, entry.getValue()) == 0 && !cast(player, entry.getValue())) {
                entries.remove();
            }
        }
    }

    /** Pays, casts once and starts the cooldown. Returns false (with an action bar message) while it is cooling down or unaffordable. */
    private static boolean cast(ServerPlayer player, Technique technique) {
        if (technique == Technique.FOLLOW_UP_KICK && player.isShiftKeyDown()) {
            ProjectionSorcery.toggleFlow(player);
            return false;
        }
        if (technique.isShrine()) {
            if (DomainUpkeep.isWindingUp(player)) {
                // Cursed Fate reads a press during the wind-up as "switch domain variant"; ignore it instead.
                return false;
            }
            // Cursed Fate cannot close a closed domain by recasting; the binder closes it instead.
            if (DomainUpkeep.closeClosedDomain(player)) {
                return true;
            }
        }
        long cooldown = TechniqueCooldowns.remaining(player, technique);
        if (cooldown > 0) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ability.mdvlcraft.on_cooldown",
                    technique.displayName(), (cooldown + 19) / 20).withStyle(ChatFormatting.RED)));
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
        if (technique.projection()) {
            ProjectionSorcery.prepare(player);
        }
        Runnable execute = () -> AbilitysProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ(), player);
        if (technique.weaponFree()) {
            WeaponFreeCasts.run(execute);
        } else {
            execute.run();
        }
        if (!modeToggle(player, technique)) {
            TechniqueCooldowns.start(player, technique);
        }
        if (technique.isShrine() && variables.SelectDomainVarient) {
            // Cursed Fate started its wind-up and reset the variant to its closed barrier domain; it reads the
            // variant again when the wind-up ends.
            variables.DomainVarientNumber = technique.domainVariant();
            variables.syncPlayerVariables(player);
            DomainUpkeep.windingUp(player, technique);
        }
        return true;
    }

    /** Sneak and cast on Don't You Have a Heart? only switches its mode (the addon's own toggle): no mana, no cooldown. */
    private static boolean modeToggle(ServerPlayer player, Technique technique) {
        return technique == Technique.PROJECTION_DASH && player.isShiftKeyDown();
    }

    private static int cost(ServerPlayer player, Technique technique) {
        if (modeToggle(player, technique)) {
            return 0;
        }
        // Cursed Fate closes the domain (or refuses to clash) when a Shrine is cast near an open one; neither costs.
        if (technique.isShrine()
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
