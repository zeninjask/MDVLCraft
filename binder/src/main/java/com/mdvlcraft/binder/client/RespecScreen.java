package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.ability.AbilityIcons;
import com.mdvlcraft.binder.network.BinderNetwork;
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

    public RespecScreen(Screen parent) {
        super(Component.translatable("screen.mdvlcraft.menu.respec"));
        this.parent = parent;
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
        List<ClientCategoryData> picked = CharacterScreen.pickedArchetypes();
        List<RespecScreen.Choice> choices = new ArrayList<>();
        int columns = 1;
        int buttonW = 170;
        Component text;
        switch (this.step) {
            case CHOOSE:
                text = Component.translatable(picked.isEmpty() ? "screen.mdvlcraft.respec.desc.none" : "screen.mdvlcraft.respec.desc.choose");
                choices.add(
                    new RespecScreen.Choice(
                        Component.translatable("screen.mdvlcraft.respec.reset"), null, () -> this.go(RespecScreen.Step.RESET), !picked.isEmpty()
                    )
                );
                choices.add(
                    new RespecScreen.Choice(
                        Component.translatable("screen.mdvlcraft.respec.change"), null, () -> this.go(RespecScreen.Step.CHANGE_FROM), !picked.isEmpty()
                    )
                );
                break;
            case RESET:
            case CHANGE_FROM:
                boolean reset = this.step == RespecScreen.Step.RESET;
                text = Component.translatable(reset ? "screen.mdvlcraft.respec.desc.reset" : "screen.mdvlcraft.respec.desc.change_from");

                for (ClientCategoryData c : picked) {
                    String id = c.getConfig().id().getPath();
                    int level = c.getCurrentLevel();
                    choices.add(new RespecScreen.Choice(Component.translatable("screen.mdvlcraft.respec.level", title(id), level), id, () -> {
                        this.from = id;
                        this.fromLevel = level;
                        this.to = null;
                        this.go(reset ? RespecScreen.Step.CONFIRM : RespecScreen.Step.CHANGE_TO);
                    }, true));
                }
                break;
            case CHANGE_TO:
                text = Component.translatable("screen.mdvlcraft.respec.desc.change_to", Respec.keptLevel(this.fromLevel), title(this.from), this.fromLevel);
                List<String> taken = picked.stream().map(c -> c.getConfig().id().getPath()).toList();

                for (String id : Respec.ARCHETYPES) {
                    if (!taken.contains(id)) {
                        choices.add(new RespecScreen.Choice(Component.literal(title(id)), id, () -> {
                            this.to = id;
                            this.go(RespecScreen.Step.CONFIRM);
                        }, true));
                    }
                }

                columns = 3;
                buttonW = 100;
                break;
            default:
                text = this.to == null
                    ? Component.translatable("screen.mdvlcraft.respec.confirm.reset", title(this.from))
                    : Component.translatable(
                        "screen.mdvlcraft.respec.confirm.change",
                        title(this.from),
                        this.fromLevel,
                        title(this.to),
                        Respec.keptLevel(this.fromLevel),
                        title(this.from)
                    );
                choices.add(new RespecScreen.Choice(Component.translatable("screen.mdvlcraft.respec.confirm"), null, () -> {
                    BinderNetwork.sendToServer(new RespecPacket(this.from, this.to == null ? "" : this.to));
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
                Minecraft.getInstance().setScreen(this.parent);
                break;
            case RESET:
            case CHANGE_FROM:
                this.go(RespecScreen.Step.CHOOSE);
                break;
            case CHANGE_TO:
                this.go(RespecScreen.Step.CHANGE_FROM);
                break;
            case CONFIRM:
                this.go(this.to == null ? RespecScreen.Step.RESET : RespecScreen.Step.CHANGE_TO);
        }
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
        Component heading = Component.translatable("screen.mdvlcraft.respec.title." + this.step.name().toLowerCase(Locale.ROOT));
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
        RESET,
        CHANGE_FROM,
        CHANGE_TO,
        CONFIRM;
    }
}
