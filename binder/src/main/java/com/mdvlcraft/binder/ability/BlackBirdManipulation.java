package com.mdvlcraft.binder.ability;

import com.bless.naoyaaddon.network.NaoyaAddonVariables;
import com.cursedfate_roslon_meimei.network.CursedfateRoslonMeimeiModVariables;
import com.cursedfate_roslon_meimei.network.CursedfateRoslonMeimeiModVariables.PlayerVariables;
import net.minecraft.server.level.ServerPlayer;

public final class BlackBirdManipulation {
    private static final String INACTIVE = "mdvlcraft:inactive";

    private BlackBirdManipulation() {
    }

    static void activate(ServerPlayer player) {
        player.<PlayerVariables>getCapability(CursedfateRoslonMeimeiModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            variables.NewTechnique = variables.CursedfateTechnique;
            variables.syncPlayerVariables(player);
        });
        player.<com.bless.naoyaaddon.network.NaoyaAddonVariables.PlayerVariables>getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY)
            .ifPresent(variables -> {
                variables.NewTechnique = "mdvlcraft:inactive";
                variables.syncPlayerVariables(player);
            });
    }

    public static void deactivate(ServerPlayer player) {
        player.<PlayerVariables>getCapability(CursedfateRoslonMeimeiModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(variables -> {
            if (!"mdvlcraft:inactive".equals(variables.NewTechnique)) {
                variables.NewTechnique = "mdvlcraft:inactive";
                variables.syncPlayerVariables(player);
                player.getPersistentData().putBoolean("simpleCheck", false);
            }
        });
        player.<com.bless.naoyaaddon.network.NaoyaAddonVariables.PlayerVariables>getCapability(NaoyaAddonVariables.PLAYER_VARIABLES_CAPABILITY)
            .ifPresent(variables -> {
                if ("mdvlcraft:inactive".equals(variables.NewTechnique)) {
                    variables.NewTechnique = variables.CursedfateTechnique;
                    variables.syncPlayerVariables(player);
                }
            });
    }
}
