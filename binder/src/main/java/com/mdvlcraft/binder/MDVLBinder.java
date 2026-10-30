package com.mdvlcraft.binder;

import com.mdvlcraft.binder.ability.AbilityReward;
import com.mdvlcraft.binder.client.GateRenderer;
import com.mdvlcraft.binder.client.ModifiedClientSetup;
import com.mdvlcraft.binder.gate.GateItems;
import com.mdvlcraft.binder.gate.GateRewards;
import com.mdvlcraft.binder.modified.ModifiedSpells;
import com.mdvlcraft.binder.modified.Summons;
import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.client.ClientAbilitySetup;
import com.mdvlcraft.binder.client.ClientContentSetup;
import com.mdvlcraft.binder.client.HerobrineRender;
import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.compat.HerobrineInvitation;
import com.mdvlcraft.binder.compat.HerobrineLook;
import com.mdvlcraft.binder.compat.issponder.PreviewCasts;
import com.mdvlcraft.binder.config.BinderConfig;
import com.mdvlcraft.binder.content.AddItemModifier;
import com.mdvlcraft.binder.content.DisabledContentSetup;
import com.mdvlcraft.binder.content.LanternBelt;
import com.mdvlcraft.binder.content.OriginIcon;
import com.mdvlcraft.binder.doppelganger.DoppelgangerSetup;
import com.mdvlcraft.binder.haunted.HauntedSetup;
import com.mdvlcraft.binder.epicskill.EpicSkillReward;
import com.mdvlcraft.binder.integration.CataclysmIntegration;
import com.mdvlcraft.binder.integration.EpicFightIntegration;
import com.mdvlcraft.binder.integration.BossArena;
import com.mdvlcraft.binder.integration.PartyTravel;
import com.mdvlcraft.binder.integration.ReturnCrystal;
import com.mdvlcraft.binder.monk.UnarmedParry;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.music.Jingles;
import com.mdvlcraft.binder.skills.ArchetypeTabs;
import com.mdvlcraft.binder.skills.MageExperience;
import com.mdvlcraft.binder.skills.ClassTitles;
import com.mdvlcraft.binder.integration.BinderAnimations;
import com.mdvlcraft.binder.superchef.SuperChefSetup;
import com.mdvlcraft.binder.wukong.WukongStaffSetup;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod("mdvlcraft")
public final class MDVLBinder {
    public static final String MOD_ID = "mdvlcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MDVLBinder(FMLJavaModLoadingContext context) {
        com.mdvlcraft.binder.logging.LogNoise.install();
        IEventBus modBus = context.getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        context.registerConfig(Type.COMMON, BinderConfig.SPEC);
        DisabledContentSetup.register(modBus);
        OriginIcon.register(modBus);
        BinderAttributes.register(modBus);
        BinderEffects.register(modBus);
        DoppelgangerSetup.register(modBus);
        UnarmedParry.register(modBus);
        AddItemModifier.register(modBus);
        WukongStaffSetup.register(modBus);
        com.mdvlcraft.binder.orca.PlayfulCloudSetup.register(modBus);
        SuperChefSetup.register(modBus);
        BinderAnimations.register(modBus);
        ModifiedSpells.register(modBus);
        HauntedSetup.register(modBus);
        modBus.addListener(MDVLBinder::onCommonSetup);
        modBus.addListener(com.mdvlcraft.binder.content.WildSpawns::onSpawnPlacements);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientContentSetup.register(modBus);
            GateRenderer.register(modBus);
            com.mdvlcraft.binder.gate.tower.client.TowerClient.register(modBus);
            ClientAbilitySetup.register(modBus);
            HerobrineRender.register(modBus);
            ModifiedClientSetup.register(modBus);
        }

        EpicFightIntegration.register(modBus, forgeBus);
        PreviewCasts.register();
        CataclysmIntegration.register(modBus, forgeBus);
        ReturnCrystal.register(modBus, forgeBus);
        GateItems.register(modBus);
        GateRewards.register(modBus);
        com.mdvlcraft.binder.gate.GateAdvancements.register(modBus);
        com.mdvlcraft.binder.gate.tower.TowerItems.register(modBus);
        com.mdvlcraft.binder.traits.TraitSlots.register(modBus);
        com.mdvlcraft.binder.orca.OrcaWeapons.register(modBus);
        BossArena.register(modBus, forgeBus);
        com.mdvlcraft.binder.integration.Willow.register(modBus, forgeBus);
        com.mdvlcraft.binder.integration.YamatoUnbound.register(modBus);
        PartyTravel.register(forgeBus);
        HerobrineLook.register();
        HerobrineInvitation.register();
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        BinderNetwork.register();
        event.enqueueWork(() -> {
            AbilityReward.register();
            com.mdvlcraft.binder.shadow.ShadowEnchantReward.register();
            EpicSkillReward.register();
            ArchetypeTabs.register();
            MageExperience.register();
            Jingles.register();
            com.mdvlcraft.binder.compat.ftbquests.ActiveQuest.register();
            com.mdvlcraft.binder.compat.ftbquests.QuestBonuses.register();
            ClassTitles.register();
            LanternBelt.register();
            Summons.registerPartiesOwner();
        });
    }
}
