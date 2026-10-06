package com.mdvlcraft.binder.content;

import com.mdvlcraft.binder.MDVLBinder;
import com.mojang.serialization.Codec;
import cursedfate.procedures.OnEntityDeathMProcedure;
import cursedfate.procedures.OnEntityDeathProcedure;
import cursedfate.procedures.OpenChestProcedure;
import cursedfate.procedures.PlayerTakesDamageProcedure;
import cursedfate.procedures.QuestEntityKilledProcedure;
import cursedfate.procedures.QuestRightClickedProcedure;
import cursedfate.procedures.ReturnAttackProcedure;
import cursedfate.procedures.SetTechniqueOnJoinProcedure;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Registers the biome-modifier serializer and switches off Cursed Fate's gameplay listeners. */
public final class DisabledContentSetup {
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MDVLBinder.MOD_ID);

    static {
        BIOME_MODIFIERS.register("strip_disabled", () -> StripDisabledBiomeModifier.CODEC);
    }

    /**
     * Cursed Fate listeners that act on every player: random technique on join, level/mastery XP from kills (levels raise
     * max health), Sukuna's fingers added to any opened chest, quests, the random Reverse Curse Technique unlock at low
     * health, and an attacker set that is never read or cleared (it holds every attacking entity forever). Its mob drops
     * stay: they are the cloth and residue its recipes need.
     */
    private static final List<Class<?>> CURSED_FATE_LISTENERS = List.of(
            SetTechniqueOnJoinProcedure.class,
            OnEntityDeathProcedure.class,
            OnEntityDeathMProcedure.class,
            OpenChestProcedure.class,
            QuestEntityKilledProcedure.class,
            QuestRightClickedProcedure.class,
            PlayerTakesDamageProcedure.class,
            ReturnAttackProcedure.class);

    private DisabledContentSetup() {
    }

    public static void register(IEventBus modBus) {
        BIOME_MODIFIERS.register(modBus);
        modBus.addListener(DisabledContentSetup::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CURSED_FATE_LISTENERS.forEach(MinecraftForge.EVENT_BUS::unregister));
    }
}
