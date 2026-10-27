package com.mdvlcraft.binder.client.picker;

import com.mdvlcraft.binder.mixin.origins.ChooseOriginScreenAccessor;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.C2SChooseOrigin;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

/**
 * Origins' layer screens (origin, Martial class, Job) open as the pack's PickerScreen instead: every choosable origin of
 * the layer in a list, picked the same way as the vocation. The choice goes to the server through Origins' own packet,
 * then Origins' wait screen takes over and opens the next layer, which is replaced in turn.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class OriginPicker {
    /** Alphabetical, as the pack sorts Origins' own screen (mixin/origins/ChooseOriginScreenMixin). */
    private static final Comparator<Holder<Origin>> ORDER = Comparator.comparing(o -> o.value().getName().getString().toLowerCase(java.util.Locale.ROOT));

    private OriginPicker() {
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof ChooseOriginScreen choose) || Minecraft.getInstance().player == null) {
            return;
        }
        ChooseOriginScreenAccessor accessor = (ChooseOriginScreenAccessor)choose;
        List<Holder<OriginLayer>> layers = accessor.mdvlcraft$layerList();
        int index = accessor.mdvlcraft$currentLayerIndex();
        OriginLayer layer = layers.get(index).value();
        List<PickerScreen.Entry> entries = layer.origins(Minecraft.getInstance().player).stream()
            .filter(o -> o.isBound() && o.value().isChoosable())
            .sorted(ORDER)
            .map(o -> new PickerScreen.Entry(o.value().getName(), o.value().getDescription(), powers(o.value()), o.value().getIcon(), key(o).toString()))
            .toList();
        if (entries.isEmpty()) {
            return;   // Origins skips a layer with nothing to choose
        }
        ResourceLocation layerId = OriginsAPI.getLayersRegistry().getKey(layer);
        event.setNewScreen(new PickerScreen(layer.name(), entries, false, picked -> {
            OriginsCommon.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SChooseOrigin(layerId, new ResourceLocation(picked.id())));
            Minecraft.getInstance().setScreen(new WaitForNextLayerScreen(layers, index, false) {
            });
        }));
    }

    /** The origin's visible powers, each with its description on hover. */
    private static List<PickerScreen.Detail> powers(Origin origin) {
        return origin.getValidPowers()
            .map(Holder::value)
            .filter(power -> !power.getData().hidden())
            .map(power -> new PickerScreen.Detail(power.getData().getName(), power.getData().getDescription()))
            .toList();
    }

    private static ResourceLocation key(Holder<Origin> origin) {
        return origin.unwrapKey().map(k -> k.location()).orElseGet(() -> OriginsAPI.getOriginsRegistry().getKey(origin.value()));
    }
}
