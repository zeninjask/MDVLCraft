package com.mdvlcraft.binder.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.puffish.skillsmod.client.SkillsClientMod;

public class MenuScreen extends Screen {
    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private final MenuScreen.Page page;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public MenuScreen() {
        this(MenuScreen.Page.MAIN, null);
    }

    public MenuScreen(MenuScreen.Page page, Screen parent) {
        super(Component.translatable("screen.mdvlcraft.menu." + page.name().toLowerCase(Locale.ROOT)));
        this.page = page;
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<MenuScreen.Door> doors() {
        Minecraft mc = Minecraft.getInstance();
        List<MenuScreen.Door> doors = new ArrayList<>();
        switch (this.page) {
            case MAIN:
                doors.add(new MenuScreen.Door("status", () -> mc.setScreen(new CharacterScreen(this)), true));
                doors.add(new MenuScreen.Door("abilities", () -> MenuNav.open(this, AbilityScreen::new), true));
                doors.add(new MenuScreen.Door("skills", () -> MenuNav.openWith(this, () -> SkillsClientMod.getInstance().openScreen(Optional.empty())), true));
                doors.add(new MenuScreen.Door("respec", () -> mc.setScreen(new RespecScreen(this)), true));
                doors.add(new MenuScreen.Door("reset_skills", () -> mc.setScreen(RespecScreen.reset(this)), true));
                doors.add(
                    new MenuScreen.Door(
                        "advancements", () -> MenuNav.open(this, () -> new AdvancementsScreen(mc.player.connection.getAdvancements())), mc.player != null
                    )
                );
                doors.add(new MenuScreen.Door("quests", () -> mc.setScreen(new QuestLogScreen(this)), ClientQuestFile.exists()));
                doors.add(
                    new MenuScreen.Door(
                        "origin",
                        () -> MenuNav.open(this, MenuNav.make("io.github.apace100.origins.screen.ViewOriginScreen")),
                        MenuNav.exists("io.github.apace100.origins.screen.ViewOriginScreen")
                    )
                );
                doors.add(
                    new MenuScreen.Door(
                        "brewing",
                        () -> MenuNav.open(this, MenuNav.make("dev.obscuria.elixirum.client.screen.alchemy.pages.collection.CollectionScreen")),
                        MenuNav.exists("dev.obscuria.elixirum.client.screen.alchemy.pages.collection.CollectionScreen")
                    )
                );
                doors.add(new MenuScreen.Door("recruits", () -> mc.setScreen(new MenuScreen(MenuScreen.Page.RECRUITS, this)), true));
                doors.add(
                    new MenuScreen.Door(
                        "voice_chat",
                        () -> MenuNav.open(this, MenuNav.make("de.maxhenkel.voicechat.gui.VoiceChatScreen")),
                        MenuNav.exists("de.maxhenkel.voicechat.gui.VoiceChatScreen")
                    )
                );
                doors.add(new MenuScreen.Door("map", () -> mc.setScreen(new MenuScreen(MenuScreen.Page.MAP, this)), true));
                break;
            case RECRUITS:
                doors.add(
                    new MenuScreen.Door(
                        "claim_map",
                        () -> MenuNav.open(this, MenuNav.make("com.talhanation.recruits.client.gui.worldmap.WorldMapScreen")),
                        MenuNav.exists("com.talhanation.recruits.client.gui.worldmap.WorldMapScreen")
                            && mc.level != null
                            && mc.level.dimension() == Level.OVERWORLD
                    )
                );
                doors.add(
                    new MenuScreen.Door(
                        "faction",
                        () -> MenuNav.open(this, MenuNav.makeWithPlayer("com.talhanation.recruits.client.gui.faction.FactionMainScreen")),
                        MenuNav.exists("com.talhanation.recruits.client.gui.faction.FactionMainScreen")
                    )
                );
                break;
            case MAP:
                doors.add(
                    new MenuScreen.Door(
                        "world_map_settings",
                        () -> MenuNav.openOwnBack(MenuNav.makeWithParent("xaero.map.gui.GuiWorldMapSettings", this)),
                        MenuNav.exists("xaero.map.gui.GuiWorldMapSettings")
                    )
                );
                doors.add(
                    new MenuScreen.Door(
                        "minimap_settings",
                        () -> MenuNav.openOwnBack(MenuNav.makeWithParent("xaero.common.gui.GuiMinimapMain", this)),
                        MenuNav.exists("xaero.common.gui.GuiMinimapMain")
                    )
                );
        }

        return doors;
    }

    @Override
    protected void init() {
        List<MenuScreen.Door> doors = this.doors();
        int columns = this.page == MenuScreen.Page.MAIN ? 2 : 1;
        int rows = (doors.size() + columns - 1) / columns;
        this.panelW = columns * 150 + (columns - 1) * 10 + 40;
        this.panelH = 44 + rows * 26 + 34;
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        for (int i = 0; i < doors.size(); i++) {
            MenuScreen.Door door = doors.get(i);
            int x = this.panelX + 20 + i % columns * 160;
            if (columns > 1 && i == doors.size() - 1 && i % columns == 0) {
                x = this.panelX + (this.panelW - 150) / 2;
            }

            int y = this.panelY + 40 + i / columns * 26;
            String more = !door.key().equals("recruits") && !door.key().equals("map") ? "" : "  ›";
            DqGui.Button button = new DqGui.Button(
                x, y, 150, 20, Component.literal(Component.translatable("screen.mdvlcraft.menu." + door.key()).getString() + more), door.open()
            );
            button.active = door.available();
            this.addRenderableWidget(button);
        }

        this.addRenderableWidget(
            new DqGui.Button(
                this.panelX + (this.panelW - 80) / 2,
                this.panelY + this.panelH - 28,
                80,
                18,
                Component.translatable(this.parent == null ? "screen.mdvlcraft.menu.close" : "screen.mdvlcraft.menu.back"),
                () -> Minecraft.getInstance().setScreen(this.parent)
            )
        );
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (AbilityKeys.MENU.matches(key, scan)) {
            Minecraft.getInstance().setScreen(null);
            return true;
        } else {
            return super.keyPressed(key, scan, modifiers);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        DqGui.window(g, this.panelX, this.panelY, this.panelW, this.panelH);
        g.drawString(this.font, "✦ " + this.title.getString(), this.panelX + 12, this.panelY + 12, DqGui.TEXT, false);
        DqGui.rule(g, this.panelX + 3, this.panelX + this.panelW - 3, this.panelY + 28);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private record Door(String key, Runnable open, boolean available) {
    }

    public enum Page {
        MAIN,
        RECRUITS,
        MAP;
    }
}
