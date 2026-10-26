package com.mdvlcraft.binder.content;

import com.cursedfate_roslon_meimei.procedures.OnPlayerJoinProcedure;
import com.cursedfate_roslon_meimei.procedures.TechniqueNameProcedure;
import com.mojang.serialization.Codec;
import cursedfate.network.CursedfateModVariables;
import cursedfate.procedures.OnEntityDeathMProcedure;
import cursedfate.procedures.OnEntityDeathProcedure;
import cursedfate.procedures.OpenChestProcedure;
import cursedfate.procedures.PlayerTakesDamageProcedure;
import cursedfate.procedures.QuestEntityKilledProcedure;
import cursedfate.procedures.QuestRightClickedProcedure;
import cursedfate.procedures.ReturnAttackProcedure;
import cursedfate.procedures.SetTechniqueOnJoinProcedure;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries.Keys;

public final class DisabledContentSetup {
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS = DeferredRegister.create(
        Keys.BIOME_MODIFIER_SERIALIZERS, "mdvlcraft"
    );
    private static final List<Class<?>> CURSED_FATE_LISTENERS = List.of(
        SetTechniqueOnJoinProcedure.class,
        OnEntityDeathProcedure.class,
        OnEntityDeathMProcedure.class,
        OpenChestProcedure.class,
        QuestEntityKilledProcedure.class,
        QuestRightClickedProcedure.class,
        PlayerTakesDamageProcedure.class,
        ReturnAttackProcedure.class,
        OnPlayerJoinProcedure.class,
        TechniqueNameProcedure.class
    );

    private DisabledContentSetup() {
    }

    public static void register(IEventBus modBus) {
        BIOME_MODIFIERS.register(modBus);
        modBus.addListener(DisabledContentSetup::onCommonSetup);
        MinecraftForge.EVENT_BUS.addListener(DisabledContentSetup::onServerStarted);
    }

    /** Cursed Fate's WorldDestruction (a world variable, default on) stays off, on new and existing worlds. */
    private static void onServerStarted(ServerStartedEvent event) {
        CursedfateModVariables.MapVariables vars = CursedfateModVariables.MapVariables.get(event.getServer().overworld());
        vars.WorldDestruction = false;
        vars.syncData(event.getServer().overworld());
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CURSED_FATE_LISTENERS.forEach(MinecraftForge.EVENT_BUS::unregister));
        DisabledContent.checkAllowedItems();
    }

    static {
        BIOME_MODIFIERS.register("strip_disabled", () -> StripDisabledBiomeModifier.CODEC);
        BIOME_MODIFIERS.register("wild_spawns", () -> WildSpawnsBiomeModifier.CODEC);
    }
}
