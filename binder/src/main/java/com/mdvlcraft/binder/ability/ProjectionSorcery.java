package com.mdvlcraft.binder.ability;

import com.bless.naoyaaddon.network.NaoyaAddonVariables;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The Projection Sorcery addon's abilities on the binder's wheel (Follow Up Kick, Don't You Have a Heart?).
 * <ul>
 * <li>The addon only runs its abilities for a player whose Cursed Fate technique is its own; before a cast the binder
 * records the player's technique as the addon's, so the cast runs whatever Cursed Fate technique they hold.</li>
 * <li>The projection style is always Naobito's (its style toggle is switched off, see ToggleProjectionStylePacketMixin).</li>
 * <li>Follow Up Kick has a Normal and a Flow form; Cursed Fate's Flow state is not used for it. Sneak and cast toggles
 * the form instead ({@link #flow}, read by DodgeProcedureMixin). Don't You Have a Heart? toggles Tracing and Continuous
 * the addon's own way (sneak and cast).</li>
 * <li>Speed always uses the highest cursed energy output and the continuous dash does not shake the screen
 * (BarrageProcedureMixin, ProjectionDashProcedureMixin).</li>
 * <li>Cursed energy never blocks a cast: the wheel charges mana (BarrageProcedureMixin).</li>
 * </ul>
 */
public final class ProjectionSorcery {
    /** Cursed Fate's highest output, which the binder's players cannot change. */
    public static final String MAX_OUTPUT = "100";
    /** More than any ability asks for (the dash's Tracing mode needs 250, above Cursed Fate's default maximum of 100). */
    public static final String ENOUGH_ENERGY = "1000000";
    private static final String FLOW_KEY = "mdvlcraft:kick_flow";

    private ProjectionSorcery() {
    }

    /** Before a Projection Sorcery cast: the addon runs it for this player, in Naobito's style. */
    static void prepare(ServerPlayer player) {
        player.getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            // The addon compares this with the technique it reads from Cursed Fate every tick (CursedfateTechnique).
            variables.NewTechnique = variables.CursedfateTechnique;
            variables.NaobitoProjectionStyle = true;
            variables.JaronaProjectionStyle = false;
            variables.syncPlayerVariables(player);
        });
    }

    /** Sneak and cast on Follow Up Kick: switches between its Normal and Flow forms. */
    static void toggleFlow(ServerPlayer player) {
        boolean flow = !flow(player);
        player.getPersistentData().putBoolean(FLOW_KEY, flow);
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                flow ? "ability.mdvlcraft.follow_up_kick.flow" : "ability.mdvlcraft.follow_up_kick.normal").withStyle(ChatFormatting.BLUE)));
    }

    /** Whether the player's Follow Up Kick is in its Flow form. */
    public static boolean flow(Player player) {
        return player.getPersistentData().getBoolean(FLOW_KEY);
    }
}
