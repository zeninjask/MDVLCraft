package com.mdvlcraft.binder.absorbed;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.mdvlcraft.binder.mixin.absorbed.RangedAttributeAccessor;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From AttributeFix (Darkhax, LGPL-2.1): attributes' minimum and maximum values come from config/attributefix.json (the
 * pack's file, written by AttributeFix), lifting vanilla's caps such as 1024 health and 30 armour.
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = EventBusSubscriber.Bus.MOD)
public final class AttributeFix {
    private static final Logger LOG = LoggerFactory.getLogger("AttributeFix");

    private AttributeFix() {
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        Path path = FMLPaths.CONFIGDIR.get().resolve("attributefix.json");
        File file;
        try (Reader reader = Files.newBufferedReader(path)) {
            file = new Gson().fromJson(reader, File.class);
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
        int changed = 0;
        for (Map.Entry<String, Entry> entry : file.attributes.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            Attribute attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
            if (!(attribute instanceof RangedAttribute ranged)) {
                continue;
            }
            double min = entry.getValue().min.value;
            double max = entry.getValue().max.value;
            if (min > max) {
                LOG.error("Attribute {} has a minimum above its maximum; left as it is", id);
                continue;
            }
            ((RangedAttributeAccessor)ranged).mdvlcraft$setMinValue(min);
            ((RangedAttributeAccessor)ranged).mdvlcraft$setMaxValue(max);
            changed++;
        }
        LOG.info("Applied limits to {} attributes", changed);
    }

    private static final class File {
        Map<String, Entry> attributes = Map.of();
    }

    private static final class Entry {
        Value min;
        Value max;
    }

    private static final class Value {
        @SerializedName("default")
        double defaultValue;
        double value;
    }
}
