package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import cursedfate.network.CursedfateModVariables;
import cursedfate.network.CursedfateModVariables.PlayerVariables;
import cursedfate.procedures.AbilitysProcedure;
import cursedfate.procedures.ReturnPlayerNearAOpenedDomainProcedure;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class TechniqueRunner {
    private static final Map<UUID, Technique> HELD = new HashMap<>();
    /** Cursed energy a binder cast runs with: more than any Cursed Fate cost (the Calamitous web's is Output / 2 + 1000). */
    private static final double FREE_ENERGY = 1.0E9;
    /** Chant Focus points a binder cast runs with (Cursed Fate holds at most 3). */
    private static final double FREE_FOCUS = 3.0;
    /** Cursed Fate's highest technique mastery. */
    private static final double FULL_MASTERY = 100.0;
    /** Cursed Fate's backflip from its Calamitous Shrine's evasive Dismantle, played on sneak Chop. */
    private static final String EVASIVE_FLIP = "evasivedismantle";
    /** Sneak Chop's back-hop (about 4 blocks). */
    private static final double DODGE_SPEED = 0.9;
    private static final double DODGE_LIFT = 0.42;

    private TechniqueRunner() {
    }

    /** Whether the player is holding the cast key on their open Malevolent Shrine (it only cuts then). */
    static boolean isSlashing(ServerPlayer player) {
        Technique held = HELD.get(player.getUUID());
        return held != null && held.isShrine();
    }

    static void press(ServerPlayer player, Technique technique) {
        if (technique == Technique.MALEVOLENT_KITCHEN) {
            KitchenArts.press(player);
        } else if (technique.isShrine()) {
            pressShrine(player, technique);
        } else if (!technique.held) {
            cast(player, technique);
        } else {
            HELD.put(player.getUUID(), technique);
            if (TechniqueCooldowns.remaining(player, technique) == 0L && !cast(player, technique)) {
                HELD.remove(player.getUUID());
            }
        }
    }

    /** Malevolent Shrine: cast to open it, hold while it stands to cut, sneak and cast to close it. */
    static void pressShrine(ServerPlayer player, Technique shrine) {
        if (!DomainUpkeep.isActive(player)) {
            cast(player, shrine);
        } else if (player.isShiftKeyDown()) {
            DomainUpkeep.close(player);
        } else {
            HELD.put(player.getUUID(), shrine);
            ShrineSign.start(player);
        }
    }

    static void release(ServerPlayer player) {
        HELD.remove(player.getUUID());
        ShrineSign.stop(player);
        KitchenArts.endSear(player);
        BlackBirdManipulation.deactivate(player);
        PlayerVariables variables = variables(player);
        if (variables.holdability1) {
            variables.holdability1 = false;
            variables.holdability = false;
            variables.AbilityToCast = 0.0;
            variables.syncPlayerVariables(player);
        }
    }

    static void tick(MinecraftServer server) {
        KitchenArts.tick(server);
        Iterator<Entry<UUID, Technique>> entries = HELD.entrySet().iterator();

        while (entries.hasNext()) {
            Entry<UUID, Technique> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
            } else if (entry.getValue().isShrine()) {
                if (!DomainUpkeep.isActive(player)) {
                    entries.remove();
                    ShrineSign.stop(player);
                }
            } else if (TechniqueCooldowns.remaining(player, entry.getValue()) == 0L && !cast(player, entry.getValue())) {
                entries.remove();
            }
        }
    }

    static boolean cast(ServerPlayer player, Technique technique) {
        if (technique == Technique.FOLLOW_UP_KICK && player.isShiftKeyDown()) {
            ProjectionSorcery.toggleFlow(player);
            return false;
        }

        if (technique == Technique.PHANTOM_MOVEMENT && PhantomRush.handles(player)) {
            PhantomRush.press(player, cost(player, technique));
            return false;
        }

        long cooldown = TechniqueCooldowns.remaining(player, technique);
        if (cooldown > 0L) {
            player.connection
                .send(
                    new ClientboundSetActionBarTextPacket(
                        Component.translatable("ability.mdvlcraft.on_cooldown", technique.displayName(), (cooldown + 19L) / 20L).withStyle(ChatFormatting.RED)
                    )
                );
            return false;
        }

        MagicData magic = MagicData.getPlayerMagicData(player);
        int cost = cost(player, technique);
        if (technique.custom()) {
            if (!player.isCreative() && magic.getMana() < cost) {
                player.connection
                    .send(
                        new ClientboundSetActionBarTextPacket(
                            Component.translatable("ui.irons_spellbooks.cast_error_mana", technique.displayName()).withStyle(ChatFormatting.RED)
                        )
                    );
                return false;
            }

            if (!MonkArts.cast(player, technique)) {
                return false;
            }

            if (!player.isCreative()) {
                magic.setMana(magic.getMana() - cost);
                PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
            }

            TechniqueCooldowns.start(player, technique);
            return true;
        } else {
            if (!player.isCreative()) {
                // The shrine is paid for while it stands (DomainUpkeep); opening needs its first second.
                float needed = technique.isShrine() ? DomainUpkeep.manaPerSecond(technique) : cost;
                if (magic.getMana() < needed) {
                    player.connection
                        .send(
                            new ClientboundSetActionBarTextPacket(
                                Component.translatable("ui.irons_spellbooks.cast_error_mana", technique.displayName()).withStyle(ChatFormatting.RED)
                            )
                        );
                    return false;
                }

                if (!technique.isShrine()) {
                    magic.setMana(magic.getMana() - cost);
                    PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
                }
            }

            PlayerVariables variables = variables(player);
            // Cursed Fate's own mastery levelling is off for binder players: they cast at full mastery (its damage bonus is mastery / 8)
            variables.InnateMastery = FULL_MASTERY;
            variables.InnateMasteryXp = 0.0;
            variables.Chant = false;   // binder casts are never Cursed Fate's chanted forms
            // Cursed Fate's own keys can leave these set, and each makes its dispatcher run another slot or skip the cast:
            // Flow (Dismantle needs it off), the preset swap (reads AbilityNum8, not 1) and a held copied technique
            variables.Flow = false;
            variables.PresetSwich = false;
            variables.CopyNumHeld = 0.0;
            if (technique.blackBird()) {
                variables.MaximumOutput = false;
            }

            int abilityId = technique.abilityId;
            // sneak Chop: a plain Dismantle, then Cursed Fate's evasive Dismantle flip and a short hop back (below)
            boolean evasive = technique == Technique.CHOP && player.isShiftKeyDown();
            if (technique == Technique.DICING_BOARD) {
                variables.MaximumOutput = false;   // the plain branch of the Calamitous Shrine's web is the Enhanced Spiderweb
                variables.Flow = false;
            }
            variables.AbilityNum1 = abilityId;
            variables.holdability1 = true;
            variables.holdability = true;
            variables.syncPlayerVariables(player);
            if (technique.projection()) {
                BlackBirdManipulation.deactivate(player);
                ProjectionSorcery.prepare(player);
            } else if (technique.blackBird()) {
                BlackBirdManipulation.activate(player);
            }

            if (technique == Technique.SEAR) {
                KitchenArts.startSear(player);
            }

            boolean[] spent = new boolean[1];
            Runnable execute = () -> spent[0] = runFree(player, () -> AbilitysProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ(), player));
            MonkArts.noteCast(player, technique);
            SlashPierce.noteCast(player, technique);
            if (technique.weaponFree()) {
                WeaponFreeCasts.run(execute);
            } else {
                execute.run();
            }

            if (technique.isSlash() && !spent[0]) {
                // a slash spends cursed energy whenever it fires: none spent means Cursed Fate refused it, so it costs nothing
                if (!player.isCreative()) {
                    magic.setMana(magic.getMana() + cost);
                    PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
                }
                return false;
            }

            if (evasive) {
                ShrineSign.animate(player, EVASIVE_FLIP);
                // Cursed Fate's own back-dodge is set on the server only and throws the caster ~17 blocks: a short hop back instead
                Vec3 back = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(-DODGE_SPEED);
                player.setDeltaMovement(back.x, DODGE_LIFT, back.z);
                player.hurtMarked = true;
            }

            TechniqueCooldowns.start(player, technique);

            if (technique.isShrine() && variables.SelectDomainVarient) {
                variables.DomainVarientNumber = technique.domainVariant();
                variables.syncPlayerVariables(player);
                DomainUpkeep.windingUp(player, technique);
            }

            return true;
        }
    }

    /**
     * Runs a Cursed Fate cast free of Cursed Fate's own resources: binder players pay mana (and cooldowns) instead, so the
     * cast runs with unlimited cursed energy and full chant Focus, whatever any technique's cost check asks for, and the
     * player's own cursed energy and Focus are put back afterwards.
     */
    /** Runs a Cursed Fate ability again with no cost or cooldown (each cast flips the shrine's domain picker). */
    static void recast(ServerPlayer player, int abilityId) {
        PlayerVariables variables = variables(player);
        variables.AbilityNum1 = abilityId;
        variables.holdability1 = true;
        variables.holdability = true;
        variables.syncPlayerVariables(player);
        runFree(player, () -> AbilitysProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ(), player));
    }

    /** @return whether the cast spent any cursed energy (Cursed Fate takes its cost whenever an ability fires) */
    static boolean runFree(ServerPlayer player, Runnable cast) {
        PlayerVariables before = variables(player);
        double energy = Math.max(before.CursedEnegry, before.CursedEnegryMax);
        double focus = before.Focus;
        before.CursedEnegry = FREE_ENERGY;
        before.Focus = FREE_FOCUS;
        before.syncPlayerVariables(player);
        boolean spent = false;
        try {
            cast.run();
        } finally {
            PlayerVariables after = variables(player);
            spent = after.CursedEnegry < FREE_ENERGY;
            after.CursedEnegry = energy;
            after.Focus = focus;
            after.syncPlayerVariables(player);
        }
        return spent;
    }

    private static int cost(ServerPlayer player, Technique technique) {
        return technique.isShrine() && ReturnPlayerNearAOpenedDomainProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ())
            ? 0
            : Math.round(technique.manaCost(1) * (float)(1.0 - player.getAttributeValue(BinderAttributes.TECHNIQUE_EFFICIENCY.get())));
    }

    static PlayerVariables variables(ServerPlayer player) {
        return player.<PlayerVariables>getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY)
            .orElseThrow(() -> new IllegalStateException("Cursed Fate variables missing on " + player.getGameProfile().getName()));
    }
}
