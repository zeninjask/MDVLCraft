package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.AbilityIcons;
import com.mdvlcraft.binder.client.picker.VocationPicker;
import com.mdvlcraft.binder.network.ArchetypePickPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.ResetSkillsPacket;
import com.mdvlcraft.binder.network.RespecPacket;
import com.mdvlcraft.binder.skills.Respec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientCategoryData;

public class RespecScreen extends Screen {
    private static final int GAP = 8;
    private static final int ROW = 24;
    private final Screen parent;
    private RespecScreen.Step step = RespecScreen.Step.CHOOSE;
    private String from;
    private int fromLevel;
    private String to;
    private List<FormattedCharSequence> lines = List.of();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    /** The level a player from before the vocations rework keeps (skills/LegacyMigration), or -1 for a normal respec. */
    private int migrationLevel = -1;
    /** An admin's free respec (Respec.offerFree): the new vocation keeps this whole level; -1 otherwise. */
    private int freeLevel = -1;

    public RespecScreen(Screen parent) {
        super(Component.translatable("screen.mdvlcraft.menu.respec"));
        this.parent = parent;
    }

    /** The pop-up where a player from before the vocations rework picks their two archetypes. */
    public static RespecScreen migration(int level) {
        RespecScreen screen = new RespecScreen(null);
        screen.migrationLevel = level;
        screen.step = RespecScreen.Step.FIRST;
        return screen;
    }

    /** The pop-up of a free respec offered by an admin: pick two archetypes, keep the whole level. */
    public static RespecScreen free(int level) {
        RespecScreen screen = new RespecScreen(null);
        screen.freeLevel = level;
        screen.step = RespecScreen.Step.FIRST;
        return screen;
    }

    /** The menu's Reset Skills: unlearn every skill of the vocation and spend all its points again, nothing lost. */
    public static RespecScreen reset(Screen parent) {
        RespecScreen screen = new RespecScreen(parent);
        screen.step = RespecScreen.Step.RESET;
        return screen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void go(RespecScreen.Step step) {
        this.step = step;
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        List<String> picked = ClientVocation.archetypes();
        int level = ClientVocation.tab().map(ClientCategoryData::getCurrentLevel).orElse(0);
        List<RespecScreen.Choice> choices = new ArrayList<>();
        int columns = 1;
        int buttonW = 170;
        Component text;
        switch (this.step) {
            case CHOOSE:
                boolean ready = picked.size() == 2;
                text = ready
                    ? Component.translatable("screen.mdvlcraft.respec.desc.choose", ClientVocation.name(), level, Respec.keptLevel(level))
                    : Component.translatable("screen.mdvlcraft.respec.desc.none");
                choices.add(new RespecScreen.Choice(Component.translatable("screen.mdvlcraft.respec.change"), null, () -> {
                    this.fromLevel = level;
                    this.go(RespecScreen.Step.FIRST);
                }, ready));
                break;
            case RESET:
                boolean vocation = picked.size() == 2;
                text = vocation
                    ? Component.translatable("screen.mdvlcraft.respec.desc.reset", ClientVocation.name(), level)
                    : Component.translatable("screen.mdvlcraft.respec.desc.none");
                choices.add(new RespecScreen.Choice(Component.translatable("screen.mdvlcraft.respec.reset"), null, () -> {
                    BinderNetwork.sendToServer(new ResetSkillsPacket());
                    SkillsClientMod.getInstance().openScreen(ClientVocation.tab().map(tab -> tab.getConfig().id()));
                }, vocation));
                break;
            case FIRST:
            case SECOND:
                // The two archetypes are picked on the vocation picker; its choice comes back here to be confirmed.
                Minecraft.getInstance().tell(() -> VocationPicker.open(pair -> {
                    this.from = pair[0];
                    this.to = pair[1];
                    this.step = RespecScreen.Step.CONFIRM;
                    Minecraft.getInstance().setScreen(this);
                }));
                return;
            default:
                text = this.migrationLevel >= 0
                    ? Component.translatable(
                        "screen.mdvlcraft.migration.confirm", title(this.from), title(this.to), vocationName(this.from, this.to), this.migrationLevel
                    )
                    : this.freeLevel >= 0
                    ? Component.translatable(
                        "screen.mdvlcraft.respec.confirm.free", title(this.from), title(this.to), vocationName(this.from, this.to), this.freeLevel
                    )
                    : Component.translatable(
                        "screen.mdvlcraft.respec.confirm.change", title(this.from), title(this.to), vocationName(this.from, this.to),
                        Respec.keptLevel(this.fromLevel), this.fromLevel
                    );
                choices.add(new RespecScreen.Choice(Component.translatable("screen.mdvlcraft.respec.confirm"), null, () -> {
                    BinderNetwork.sendToServer(this.migrationLevel >= 0
                        ? new ArchetypePickPacket(this.from, this.to)
                        : new RespecPacket(this.from, this.to));
                    Minecraft.getInstance().setScreen(null);
                }, true));
        }

        int rows = (choices.size() + columns - 1) / columns;
        this.panelW = Math.max(240, columns * buttonW + (columns - 1) * 8 + 40);
        this.lines = this.font.split(text, this.panelW - 32);
        int textH = this.lines.size() * 10;
        this.panelH = 40 + textH + 8 + rows * 24 + 34;
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        int gridW = columns * buttonW + (columns - 1) * 8;

        for (int i = 0; i < choices.size(); i++) {
            RespecScreen.Choice choice = choices.get(i);
            int x = this.panelX + (this.panelW - gridW) / 2 + i % columns * (buttonW + 8);
            int y = this.panelY + 40 + textH + 8 + i / columns * 24;
            DqGui.Button button = new RespecScreen.IconButton(x, y, buttonW, 20, choice.label(), choice.icon(), choice.action());
            button.active = choice.active();
            this.addRenderableWidget(button);
        }

        this.addRenderableWidget(
            new DqGui.Button(
                this.panelX + (this.panelW - 80) / 2, this.panelY + this.panelH - 28, 80, 18, Component.translatable("screen.mdvlcraft.menu.back"), this::back
            )
        );
    }

    private void back() {
        switch (this.step) {
            case CHOOSE:
            case RESET:
                Minecraft.getInstance().setScreen(this.parent);
                break;
            case FIRST:
                if (this.migrationLevel >= 0) {
                    Minecraft.getInstance().setScreen(null);   // asked again on the next join
                } else {
                    this.go(RespecScreen.Step.CHOOSE);
                }
                break;
            case SECOND:
                this.go(RespecScreen.Step.FIRST);
                break;
            case CONFIRM:
                this.go(RespecScreen.Step.FIRST);
        }
    }

    /** The vocation two archetypes make (lang key screen.mdvlcraft.vocation.<a>.<b>, a first in Respec.ARCHETYPES). */
    private static String vocationName(String first, String second) {
        boolean ordered = Respec.ARCHETYPES.indexOf(first) < Respec.ARCHETYPES.indexOf(second);
        return Component.translatable("screen.mdvlcraft.vocation." + (ordered ? first + "." + second : second + "." + first)).getString();
    }

    private static String title(String archetype) {
        return Character.toUpperCase(archetype.charAt(0)) + archetype.substring(1);
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
        Component heading = this.migrationLevel >= 0
            ? Component.translatable("screen.mdvlcraft.migration.title")
            : Component.translatable("screen.mdvlcraft.respec.title." + this.step.name().toLowerCase(Locale.ROOT));
        g.drawString(this.font, "✦ " + heading.getString(), this.panelX + 12, this.panelY + 12, DqGui.TEXT, false);
        DqGui.rule(g, this.panelX + 3, this.panelX + this.panelW - 3, this.panelY + 28);

        for (int i = 0; i < this.lines.size(); i++) {
            g.drawString(this.font, this.lines.get(i), this.panelX + 16, this.panelY + 38 + i * 10, DqGui.TEXT, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private record Choice(Component label, String icon, Runnable action, boolean active) {
    }

    private static class IconButton extends DqGui.Button {
        private final ResourceLocation icon;

        IconButton(int x, int y, int w, int h, Component label, String archetype, Runnable action) {
            super(x, y, w, h, label, action);
            this.icon = archetype == null ? null : AbilityIcons.tab(archetype);
            this.labelOffset = archetype == null ? 0 : 8;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (this.icon != null) {
                g.blit(this.icon, this.getX() + 3, this.getY() + (this.height - 16) / 2, 16, 16, 0.0F, 0.0F, 18, 18, 18, 18);
            }
        }
    }

    private enum Step {
        CHOOSE,
        FIRST,
        SECOND,
        CONFIRM,
        RESET;
    }
}
