package com.mdvlcraft.binder.compat.villagerecruits;

import com.example.villagerecruits.faction.VillageFaction;
import com.example.villagerecruits.structures.workers.PlayerBuildConsent;
import com.mdvlcraft.binder.config.BinderConfig;

public final class AiVillageBuilding {
    private AiVillageBuilding() {
    }

    public static boolean allowed(VillageFaction faction) {
        return !BinderConfig.stopAiVillageBuilding() || faction != null && PlayerBuildConsent.isPlayerFaction(faction.id);
    }
}
