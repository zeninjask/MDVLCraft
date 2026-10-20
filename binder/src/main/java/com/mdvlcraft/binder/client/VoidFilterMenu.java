package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.compat.backpacked.BackpackAugments;
import com.mrcrayfish.backpacked.client.TextureDefinitions;
import com.mrcrayfish.backpacked.client.augment.AugmentHolder;
import com.mrcrayfish.backpacked.client.augment.AugmentSettingsFactories;
import com.mrcrayfish.backpacked.client.augment.AugmentSettingsMenu;
import com.mrcrayfish.backpacked.client.gui.screen.widget.Divider;
import com.mrcrayfish.backpacked.client.gui.screen.widget.ItemGrid;
import com.mrcrayfish.backpacked.client.gui.screen.widget.TitleWidget;
import com.mrcrayfish.backpacked.client.gui.screen.widget.popup.PopupMenuHandler;
import com.mrcrayfish.backpacked.common.ItemCollection;
import com.mrcrayfish.backpacked.util.ScreenUtil;
import com.mrcrayfish.framework.api.client.screen.widget.FrameworkEditBox;
import com.mrcrayfish.framework.api.client.screen.widget.texture.WidgetTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

/**
 * The Void augment's settings: Backpacked's searchable item grid, as in its Funnelling menu, where each activated item
 * is destroyed on pickup (compat/backpacked/AugmentEffects).
 */
public final class VoidFilterMenu {
    private static final Component SEARCH_HINT = Component.translatable("backpacked.gui.search_hint");
    private static final Component FILTERS_LABEL = Component.translatable("gui.mdvlcraft.void_filters");
    private static final int MIN_CONTENT_WIDTH = 162;
    private static String lastQuery = "";

    private VoidFilterMenu() {
    }

    private static AugmentSettingsMenu create(PopupMenuHandler handler, AugmentHolder<BackpackAugments.VoidJunk> holder) {
        return new AugmentSettingsMenu(handler, menu -> {
            GridLayout root = new GridLayout().spacing(2);
            GridLayout.RowHelper rows = root.createRowHelper(1);
            TitleWidget title = rows.addChild(new TitleWidget(() -> ScreenUtil.join(" ", FILTERS_LABEL,
                Component.translatable("backpacked.gui.x_of_y", holder.get().filters().ids().size(), ItemCollection.MAX_ENTRIES)), Minecraft.getInstance().font));
            Divider divider = rows.addChild(Divider.horizontal(Math.max(MIN_CONTENT_WIDTH, title.getWidth())).colour(-2044489));
            title.setWidth(divider.getWidth());
            ItemGrid<BackpackAugments.VoidJunk> grid = ItemGrid.builder(holder::get, holder::update)
                .setWidth(divider.getWidth())
                .setHeight(84)
                .setInitialQuery(lastQuery)
                .build();
            rows.addChild(FrameworkEditBox.builder()
                .setWidth(divider.getWidth())
                .setPadding(2, 0, 2, 0)
                .setHeight(16)
                .setIcon(TextureDefinitions.SEARCH_ICON)
                .setInitialText(lastQuery)
                .setHint(SEARCH_HINT)
                .setCallback(query -> {
                    grid.setSearchQuery(query);
                    lastQuery = query;
                })
                .setBackground(new WidgetTextures(TextureDefinitions.EDIT_BOX_ENABLED, TextureDefinitions.EDIT_BOX_DISABLED, TextureDefinitions.EDIT_BOX_ENABLED_HOVERED))
                .build());
            rows.addChild(grid);
            return root;
        });
    }

    public static void register() {
        AugmentSettingsFactories.registerFactory(BackpackAugments.VOID_TYPE, VoidFilterMenu::create);
    }
}
