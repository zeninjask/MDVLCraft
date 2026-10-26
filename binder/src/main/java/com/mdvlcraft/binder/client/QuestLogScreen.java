package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.compat.ftbquests.ActiveQuest;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.net.SubmitTaskMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/**
 * The quest book as a Dragon Quest IX quest list, opened from the MDVLCraft menu: the chapter's numbered quests on the
 * left with a slowly flashing arrow on the selected one, its request, reward and description on the right, and Accept /
 * Abandon (compat/ftbquests/ActiveQuest decides what they do on the server). Numbers run through the whole book, except
 * the Gates chapter (filename "gates"), which numbers its own quests 001G, 002G, … so the others never shift.
 */
public class QuestLogScreen extends Screen {
    private static final int ROW = 11;
    private static final int LIST_W = 170;
    private static final int GOLD = 0xFFFFD64A;
    private static final int RED = 0xFFE8322B;
    private static final int GREEN = 0xFF6FD36A;
    private final Screen parent;
    private final List<Chapter> chapters;
    /** Chapters numbered on their own, with a letter: Gates 001G..., Food 001F..., Brewing 001B.... */
    private static final Map<String, String> LETTERED = Map.of("gates", "G", "food", "F", "brewing", "B");
    /** Each quest's number as shown ("007", or "007G" in the Gates chapter). */
    private final Map<Quest, String> numbers = new HashMap<>();
    private int chapter;
    private int selected;
    private int scroll;
    private int x;
    private int y;
    private int w;
    private int h;
    private DqGui.Button command;

    private enum State { CLEAR, ACTIVE, OPEN, LOCKED }

    public QuestLogScreen(Screen parent) {
        super(Component.translatable("screen.mdvlcraft.quests"));
        this.parent = parent;
        this.chapters = ClientQuestFile.INSTANCE.getAllChapters().stream().filter(c -> !c.getQuests().isEmpty()).toList();
        int book = 0;
        for (Chapter c : this.chapters) {
            String letter = LETTERED.get(c.getFilename());
            List<Quest> quests = c.getQuests();
            for (int i = 0; i < quests.size(); i++) {
                this.numbers.put(quests.get(i), letter != null ? String.format("%03d", i + 1) + letter : String.format("%03d", ++book));
            }
        }

        // Open on the active quest, else the first one that can be taken.
        TeamData team = team();
        for (State wanted : new State[] {State.ACTIVE, State.OPEN}) {
            for (int c = 0; c < this.chapters.size(); c++) {
                List<Quest> quests = this.chapters.get(c).getQuests();
                for (int i = 0; i < quests.size(); i++) {
                    if (state(team, quests.get(i)) == wanted) {
                        this.chapter = c;
                        this.selected = i;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.w = Math.min(this.width - 16, 440);
        this.h = Math.min(this.height - 16, 250);
        this.x = (this.width - this.w) / 2;
        this.y = (this.height - this.h) / 2;
        this.addRenderableWidget(new DqGui.Button(this.x + 6, this.y + 5, 14, 14, Component.literal("<"), () -> this.turn(-1)));
        this.addRenderableWidget(new DqGui.Button(this.x + LIST_W - 20, this.y + 5, 14, 14, Component.literal(">"), () -> this.turn(1)));
        int dx = this.x + LIST_W + 4;
        int dw = this.w - LIST_W - 4;
        this.command = this.addRenderableWidget(new DqGui.Button(dx + dw / 2 - 76, this.y + this.h - 24, 72, 16, Component.empty(), this::runCommand));
        this.addRenderableWidget(new DqGui.Button(dx + dw / 2 + 4, this.y + this.h - 24, 72, 16, Component.translatable("screen.mdvlcraft.menu.back"), this::onClose));
        this.keepVisible();
        this.tick();
    }

    @Override
    public void tick() {
        TeamData team = team();
        Quest quest = this.quest();
        State state = state(team, quest);
        Optional<Task> accept = ActiveQuest.acceptTask(quest);
        this.command.visible = true;
        this.command.active = true;
        if (accept.isPresent() && state == State.ACTIVE) {
            this.command.setMessage(Component.translatable("screen.mdvlcraft.quests.abandon"));
        } else if (accept.isPresent() && state == State.OPEN) {
            this.command.setMessage(Component.translatable("screen.mdvlcraft.quests.accept"));
            this.command.active = this.active(team).isEmpty();
        } else if (accept.isEmpty() && state != State.CLEAR && state != State.LOCKED && this.checkmark(team, quest).isPresent()) {
            this.command.setMessage(Component.translatable("screen.mdvlcraft.quests.done"));
        } else {
            this.command.visible = false;
        }
    }

    private void runCommand() {
        TeamData team = team();
        Quest quest = this.quest();
        Optional<Task> accept = ActiveQuest.acceptTask(quest);
        if (accept.isPresent() && state(team, quest) == State.ACTIVE) {
            this.minecraft.player.connection.sendCommand("quest cancel");
        } else {
            accept.or(() -> this.checkmark(team, quest)).ifPresent(task -> new SubmitTaskMessage(task.id).sendToServer());
        }
    }

    /** The first unticked checkmark of a quest without an Accept Quest box (the welcome quest's "tick this quest"). */
    private Optional<Task> checkmark(TeamData team, Quest quest) {
        return quest.getTasksAsList().stream().filter(t -> t instanceof CheckmarkTask && !team.isCompleted(t)).findFirst();
    }

    private Optional<Quest> active(TeamData team) {
        return this.numbers.keySet().stream().filter(q -> ActiveQuest.acceptTask(q).isPresent() && state(team, q) == State.ACTIVE).findFirst();
    }

    private static TeamData team() {
        return ClientQuestFile.INSTANCE.selfTeamData;
    }

    private static State state(TeamData team, Quest quest) {
        if (team.isCompleted(quest)) {
            return State.CLEAR;
        }

        Optional<Task> accept = ActiveQuest.acceptTask(quest);
        if (accept.isPresent() ? team.isCompleted(accept.get()) : team.isStarted(quest)) {
            return State.ACTIVE;
        }

        return team.areDependenciesComplete(quest) ? State.OPEN : State.LOCKED;
    }

    private List<Quest> quests() {
        return this.chapters.get(this.chapter).getQuests();
    }

    private Quest quest() {
        return this.quests().get(this.selected);
    }

    private int rows() {
        return (this.h - 30) / ROW;
    }

    private void turn(int by) {
        this.chapter = Math.floorMod(this.chapter + by, this.chapters.size());
        this.selected = 0;
        this.scroll = 0;
        this.tick();
    }

    private void select(int index) {
        this.selected = Math.max(0, Math.min(this.quests().size() - 1, index));
        this.keepVisible();
        this.tick();
    }

    private void keepVisible() {
        this.scroll = Math.max(Math.min(this.scroll, this.selected), this.selected - this.rows() + 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int top = this.y + 26;
        if (button == 0 && mouseX >= this.x + 4 && mouseX < this.x + LIST_W - 4 && mouseY >= top) {
            int row = (int)((mouseY - top) / ROW);
            if (row < this.rows() && this.scroll + row < this.quests().size()) {
                this.select(this.scroll + row);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scroll = Math.max(0, Math.min(Math.max(0, this.quests().size() - this.rows()), this.scroll - (int)Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_UP -> this.select(this.selected - 1);
            case GLFW.GLFW_KEY_DOWN -> this.select(this.selected + 1);
            case GLFW.GLFW_KEY_LEFT -> this.turn(-1);
            case GLFW.GLFW_KEY_RIGHT -> this.turn(1);
            default -> {
                return super.keyPressed(key, scan, modifiers);
            }
        }

        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        TeamData team = team();
        this.renderList(g, team);
        this.renderDetail(g, team);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderList(GuiGraphics g, TeamData team) {
        DqGui.window(g, this.x, this.y, LIST_W, this.h);
        g.drawCenteredString(this.font, this.font.plainSubstrByWidth(this.chapters.get(this.chapter).getRawTitle(), LIST_W - 50), this.x + LIST_W / 2, this.y + 8, DqGui.TEXT);
        DqGui.rule(g, this.x + 3, this.x + LIST_W - 3, this.y + 22);
        List<Quest> quests = this.quests();
        boolean arrowOn = Util.getMillis() / 500 % 2 == 0;
        for (int row = 0; row < this.rows() && this.scroll + row < quests.size(); row++) {
            int index = this.scroll + row;
            Quest quest = quests.get(index);
            State state = state(team, quest);
            int ry = this.y + 27 + row * ROW;
            if (index == this.selected && arrowOn) {
                g.drawString(this.font, "▶", this.x + 6, ry, DqGui.TEXT, false);
            }

            String number = this.numbers.get(quest);
            g.drawString(this.font, number, this.x + 16, ry, DqGui.MUTED, false);
            String badge = switch (state) {
                case CLEAR -> "CLEAR";
                case ACTIVE -> "!";
                case OPEN -> "NEW";
                case LOCKED -> "";
            };
            int badgeW = badge.isEmpty() ? 0 : this.font.width(badge) + 4;
            int titleX = this.x + 18 + this.font.width(number);
            String title = this.font.plainSubstrByWidth(quest.getRawTitle(), this.x + LIST_W - 8 - badgeW - titleX);
            g.drawString(this.font, title, titleX, ry, state == State.LOCKED ? DqGui.MUTED : DqGui.TEXT, false);
            int bx = this.x + LIST_W - 6 - badgeW;
            if (state == State.ACTIVE) {
                g.fill(bx, ry - 1, bx + badgeW, ry + 9, GOLD);
                g.drawString(this.font, badge, bx + 2, ry, DqGui.BLACK, false);
            } else if (!badge.isEmpty()) {
                g.drawString(this.font, badge, bx + 2, ry, state == State.CLEAR ? RED : DqGui.MUTED, false);
            }
        }

        if (quests.size() > this.rows()) {
            int trackTop = this.y + 26;
            int trackH = this.rows() * ROW;
            int thumbH = Math.max(8, trackH * this.rows() / quests.size());
            int thumbY = trackTop + (trackH - thumbH) * this.scroll / Math.max(1, quests.size() - this.rows());
            g.fill(this.x + LIST_W - 4, thumbY, this.x + LIST_W - 3, thumbY + thumbH, DqGui.MUTED);
        }
    }

    private void renderDetail(GuiGraphics g, TeamData team) {
        int dx = this.x + LIST_W + 4;
        int dw = this.w - LIST_W - 4;
        Quest quest = this.quest();
        State state = state(team, quest);
        DqGui.window(g, dx, this.y, dw, this.h);
        String heading = "No. " + this.numbers.get(quest) + " · " + quest.getRawTitle();
        g.drawString(this.font, this.font.plainSubstrByWidth(heading, dw - 16), dx + 8, this.y + 8, DqGui.TEXT, false);
        DqGui.rule(g, dx + 3, dx + dw - 3, this.y + 22);
        int labelW = 52;
        int ty = this.y + 28;
        g.drawString(this.font, Component.translatable("screen.mdvlcraft.quests.chapter"), dx + 8, ty, GOLD, false);
        g.drawString(this.font, this.chapters.get(this.chapter).getRawTitle(), dx + 8 + labelW, ty, DqGui.TEXT, false);
        ty += ROW + 2;
        g.drawString(this.font, Component.translatable("screen.mdvlcraft.quests.request"), dx + 8, ty, GOLD, false);
        Optional<Task> accept = ActiveQuest.acceptTask(quest);
        // With several objectives, a cleared one is struck through (abandoning resets the progress, so the lines go too).
        boolean several = quest.getTasksAsList().size() - (accept.isPresent() ? 1 : 0) > 1;
        for (Task task : quest.getTasksAsList()) {
            if (accept.isPresent() && accept.get() == task) {
                continue;
            }

            String progress = team.isCompleted(task) ? "✔"
                : task.getMaxProgress() > 1 ? task.formatProgress(team, team.getProgress(task)) + "/" + task.formatProgress(team, task.getMaxProgress()) : "";
            int pw = this.font.width(progress);
            Component title = Component.literal(this.font.plainSubstrByWidth(task.getTitle().getString(), dw - labelW - 24 - pw))
                .withStyle(style -> style.withStrikethrough(several && team.isCompleted(task)));
            g.drawString(this.font, title, dx + 8 + labelW, ty, DqGui.TEXT, false);
            g.drawString(this.font, progress, dx + dw - 10 - pw, ty, team.isCompleted(task) ? GREEN : DqGui.MUTED, false);
            ty += ROW;
        }

        ty += 2;
        g.drawString(this.font, Component.translatable("screen.mdvlcraft.quests.reward"), dx + 8, ty, GOLD, false);
        for (Reward reward : quest.getRewards()) {
            g.drawString(this.font, this.font.plainSubstrByWidth(reward.getTitle().getString(), dw - labelW - 16), dx + 8 + labelW, ty, DqGui.TEXT, false);
            ty += ROW;
        }

        ty += 6;
        int bottom = this.y + this.h - 30;
        String text = state == State.LOCKED ? Component.translatable("screen.mdvlcraft.quests.locked").getString() : String.join(" ", quest.getRawDescription()).trim();
        for (FormattedCharSequence line : this.font.split(Component.literal(text), dw - 16)) {
            if (ty + 9 > bottom) {
                break;
            }

            g.drawString(this.font, line, dx + 8, ty, state == State.LOCKED ? DqGui.MUTED : DqGui.TEXT, false);
            ty += 10;
        }

        if (state == State.OPEN && accept.isPresent() && !this.command.active) {
            Component busy = Component.translatable("screen.mdvlcraft.quests.busy");
            g.drawCenteredString(this.font, busy, dx + dw / 2, this.y + this.h - 36, DqGui.MUTED);
        }

        if (state == State.CLEAR) {
            this.stamp(g, dx + dw - 46, this.y + this.h - 46);
        }
    }

    /** The red "cleared" seal, tilted, over the top right of the details. */
    private void stamp(GuiGraphics g, int cx, int cy) {
        String word = Component.translatable("screen.mdvlcraft.quests.cleared").getString();
        int tw = this.font.width(word);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 200);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-14));
        int bw = tw + 10;
        g.renderOutline(-bw / 2, -8, bw, 16, RED);
        g.renderOutline(-bw / 2 + 2, -6, bw - 4, 12, RED);
        g.drawString(this.font, word, -tw / 2, -4, RED, false);
        g.pose().popPose();
    }
}
