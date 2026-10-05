package com.mdvlcraft.binder;

import com.mdvlcraft.binder.integration.CataclysmIntegration;
import com.mdvlcraft.binder.integration.EpicFightIntegration;
import com.mdvlcraft.binder.integration.IronsSpellsIntegration;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Binder mod for the MDVLCraft modpack. Each integration class owns the glue for one mod;
 * the integrated mods are hard dependencies (see mods.toml), so the game refuses to start
 * without them instead of silently skipping glue code.
 */
@Mod(MDVLBinder.MOD_ID)
public final class MDVLBinder {
    public static final String MOD_ID = "mdvlcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MDVLBinder(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        context.registerConfig(ModConfig.Type.COMMON, BinderConfig.SPEC);

        EpicFightIntegration.register(modBus, forgeBus);
        IronsSpellsIntegration.register(modBus, forgeBus);
        CataclysmIntegration.register(modBus, forgeBus);
    }
}
