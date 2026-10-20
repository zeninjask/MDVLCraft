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

final class TechniqueRunner {
    private static final Map<UUID, Technique> HELD = new HashMap<>();

    private TechniqueRunner() {
    }

    /** Whether the player is holding the cast key on their open Malevolent Shrine (it only cuts then). */
    static boolean isSlashing(ServerPlayer player) {
        Technique held = HELD.get(player.getUUID());
        return held != null && held.isShrine();
    }

    static void press(ServerPlayer player, Technique technique) {
        if (technique.isShrine()) {
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
    private static void pressShrine(ServerPlayer player, Technique shrine) {
        if (!DomainUpkeep.isActive(player)) {
            cast(player, shrine);
        } else if (player.isShiftKeyDown()) {
            DomainUpkeep.close(player);
        } else {
            HELD.put(player.getUUID(), shrine);
        }
    }

    /** Holds the cast key on `technique` for the player (the open shrine cuts while held) until release(). */
    static void hold(ServerPlayer player, Technique technique) {
        HELD.put(player.getUUID(), technique);
    }

    static void release(ServerPlayer player) {
        HELD.remove(player.getUUID());
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
        Iterator<Entry<UUID, Technique>> entries = HELD.entrySet().iterator();

        while (entries.hasNext()) {
            Entry<UUID, Technique> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                entries.remove();
            } else if (entry.getValue().isShrine()) {
                if (!DomainUpkeep.isActive(player)) {
                    entries.remove();
                }
            } else if (TechniqueCooldowns.remaining(player, entry.getValue()) == 0L && !cast(player, entry.getValue())) {
                entries.remove();
            }
        }
    }

    private static boolean cast(ServerPlayer player, Technique technique) {
        if (technique == Technique.FOLLOW_UP_KICK && player.isShiftKeyDown()) {
            ProjectionSorcery.toggleFlow(player);
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
                float needed = technique.isShrine() ? DomainUpkeep.MANA_PER_SECOND : cost;
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
            variables.CursedEnegry = Math.max(variables.CursedEnegry, variables.CursedEnegryMax);
            variables.InnateMastery = 0.0;
            variables.InnateMasteryXp = 0.0;
            if (technique.blackBird()) {
                variables.Chant = false;
                variables.Flow = false;
                variables.MaximumOutput = false;
            }

            variables.AbilityNum1 = technique.abilityId;
            variables.holdability1 = true;
            variables.holdability = true;
            variables.syncPlayerVariables(player);
            if (technique.projection()) {
                BlackBirdManipulation.deactivate(player);
                ProjectionSorcery.prepare(player);
            } else if (technique.blackBird()) {
                BlackBirdManipulation.activate(player);
            }

            Runnable execute = () -> AbilitysProcedure.execute(player.level(), player.getX(), player.getY(), player.getZ(), player);
            MonkArts.noteCast(player, technique);
            if (technique.weaponFree()) {
                WeaponFreeCasts.run(execute);
            } else {
                execute.run();
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
