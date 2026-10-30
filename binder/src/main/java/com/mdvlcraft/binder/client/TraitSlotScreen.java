package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.TraitChoicePacket;
import com.mdvlcraft.binder.traits.Trait;
import com.mdvlcraft.binder.traits.TraitSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The trait slot machine (traits/TraitSlots): four reels of iron, diamond and netherite, three rows showing, in the
 * Dragon Quest window style. The server has already rolled the trait; the reels spin and stop from left to right on the
 * grid it sent. Two netherite on the middle row slow the third reel right down, three slow the fourth further, with a
 * heartbeat. Then the trait shows with a button per slot and a Discard button (closing the window discards too).
 */
public class TraitSlotScreen extends Screen {
    private static final int W = 288;
    private static final int H = 214;
    private static final int CELL = 28;
    private static final int GAP = 6;
    private static final int REELS = TraitSlots.REELS;
    private static final ItemStack[] SYMBOLS = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.DIAMOND), new ItemStack(Items.NETHERITE_INGOT)};
    private static final int SPIN_SPEED = 26;

    private final int[] grid;
    private final Trait trait;
    private final List<String> slots;
    private final long start = Util.getMillis();
    private final int[][] strips = new int[REELS][];
    private final long[] stopAt = new long[REELS];
    private final float[] ease = new float[REELS];
    private final int[] lastIndex = new int[REELS];
    private final boolean[] stopped = new boolean[REELS];
    private final boolean twoNetherite;
    private final boolean threeNetherite;
    private long lastClick;
    private long lastBeat;
    private boolean done;
    private boolean chosen;
    private final List<DqGui.Button> choices = new ArrayList<>();

    private TraitSlotScreen(int[] grid, Trait trait, List<String> slots) {
        super(Component.translatable("screen.mdvlcraft.trait_slots"));
        this.grid = grid;
        this.trait = trait;
        this.slots = slots;
        this.twoNetherite = grid[REELS] == TraitSlots.NETHERITE && grid[REELS + 1] == TraitSlots.NETHERITE;
        this.threeNetherite = this.twoNetherite && grid[REELS + 2] == TraitSlots.NETHERITE;
        long[] stops = {1100, 1600, this.twoNetherite ? 3900 : 2100, 0};
        stops[3] = stops[2] + (this.threeNetherite ? 3600 : 500);
        Random random = new Random();
        for (int reel = 0; reel < REELS; reel++) {
            this.stopAt[reel] = stops[reel];
            boolean suspense = reel == 2 && this.twoNetherite || reel == 3 && this.threeNetherite;
            this.ease[reel] = suspense ? (reel == 3 ? 5.0F : 4.0F) : 3.0F;
            int length = Math.max(8, Math.round(SPIN_SPEED * stops[reel] / 1000.0F / this.ease[reel])) + 3;
            int[] strip = new int[length + 3];
            strip[0] = grid[reel];
            strip[1] = grid[REELS + reel];
            strip[2] = grid[2 * REELS + reel];
            for (int i = 3; i < strip.length; i++) {
                strip[i] = random.nextInt(3);
            }
            this.strips[reel] = strip;
            this.lastIndex[reel] = -1;
        }
    }

    public static void open(int[] grid, String traitId, List<String> slots) {
        Trait trait = Trait.byId(traitId);
        if (trait != null && grid.length == 3 * REELS) {
            Minecraft.getInstance().setScreen(new TraitSlotScreen(grid, trait, slots));
        }
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return (this.height - H) / 2;
    }

    @Override
    protected void init() {
        this.choices.clear();
        int count = this.slots.size() + 1;
        int bw = (W - 24 - (count - 1) * 6) / count;
        int y = this.top() + H - 26;
        for (int i = 0; i < count; i++) {
            int slot = i < this.slots.size() ? i : -1;
            Component label;
            if (slot >= 0) {
                Trait there = Trait.byId(this.slots.get(slot));
                label = there == null ? Component.translatable("screen.mdvlcraft.trait_slots.take")
                    : Component.translatable("screen.mdvlcraft.trait_slots.replace", Component.translatable(there.nameKey()));
                if (this.font.width(label) > bw - 8) {
                    label = Component.literal(this.font.plainSubstrByWidth(label.getString(), bw - 8 - this.font.width("..")) + "..");
                }
            } else {
                label = Component.translatable("screen.mdvlcraft.trait_slots.discard");
            }
            DqGui.Button button = new DqGui.Button(this.left() + 12 + i * (bw + 6), y, bw, 18, label, () -> this.choose(slot));
            button.visible = this.done;
            this.choices.add(this.addRenderableWidget(button));
        }
    }

    private void choose(int slot) {
        if (!this.chosen) {
            this.chosen = true;
            BinderNetwork.sendToServer(new TraitChoicePacket(slot));
        }
        this.onClose();
    }

    @Override
    public void removed() {
        if (!this.chosen) {
            this.chosen = true;
            BinderNetwork.sendToServer(new TraitChoicePacket(-1));
        }
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.done;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Where reel `reel` is, in symbols from its final place (0 = stopped). */
    private float position(int reel, long now) {
        long t = now - this.start;
        if (t >= this.stopAt[reel]) {
            return 0.0F;
        }
        float x = (float)t / this.stopAt[reel];
        int length = this.strips[reel].length - 3;
        return length * (float)Math.pow(1.0 - x, this.ease[reel]);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        long now = Util.getMillis();
        int left = this.left();
        int top = this.top();
        DqGui.window(g, this.font, this.title, left, top, W, H);
        int reelX = left + 16;
        int reelY = top + 30;

        for (int reel = 0; reel < REELS; reel++) {
            int x = reelX + reel * (CELL + GAP);
            DqGui.panel(g, x - 4, reelY - 4, CELL + 8, 3 * CELL + 8);
            float q = this.position(reel, now);
            int[] strip = this.strips[reel];
            g.enableScissor(x, reelY, x + CELL, reelY + 3 * CELL);
            int first = (int)Math.floor(q);
            for (int k = first; k <= first + 3 && k < strip.length; k++) {
                float y = reelY + (k - q) * CELL;
                g.pose().pushPose();
                g.pose().translate(x + 2, y + 2, 0.0F);
                g.pose().scale(1.5F, 1.5F, 1.0F);
                g.renderItem(SYMBOLS[strip[k]], 0, 0);
                g.pose().popPose();
            }
            g.disableScissor();
            this.sounds(reel, first, q == 0.0F, now);
        }

        // the payline
        int lineY = reelY + CELL;
        int lineRight = reelX + REELS * (CELL + GAP) - GAP;
        g.drawString(this.font, ">", reelX - 11, lineY + CELL / 2 - 4, DqGui.HEADING, false);
        g.drawString(this.font, "<", lineRight + 6, lineY + CELL / 2 - 4, DqGui.HEADING, false);
        boolean allStopped = now - this.start >= this.stopAt[REELS - 1];
        if (allStopped && !this.done) {
            this.finish();
        }
        if (this.done) {
            boolean flash = this.trait.tier == Trait.Tier.MYTHIC && (now / 200) % 2 == 0;
            g.renderOutline(reelX - 2, lineY - 1, lineRight - reelX + 4, CELL + 2, flash ? 0xFFFFD84A : DqGui.BLUE);
            g.renderOutline(reelX - 3, lineY - 2, lineRight - reelX + 6, CELL + 4, flash ? 0xFFFFD84A : DqGui.BLUE);
        }

        this.paytable(g, lineRight + 18, reelY);
        if (this.done) {
            this.result(g, left, reelY + 3 * CELL + 8);
        } else if (this.twoNetherite && now - this.start > this.stopAt[1]) {
            g.drawCenteredString(this.font, Component.translatable("screen.mdvlcraft.trait_slots.suspense"), left + W / 2,
                reelY + 3 * CELL + 16, (now / 250) % 2 == 0 ? 0xFFFFD84A : DqGui.HEADING);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Clicks as the reels turn, a thunk when one stops, a heartbeat while the last reels hang on netherite. */
    private void sounds(int reel, int index, boolean stoppedNow, long now) {
        if (stoppedNow) {
            if (!this.stopped[reel]) {
                this.stopped[reel] = true;
                this.play(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.8F, 0.9F);
                if (reel == 1 && this.twoNetherite) {
                    this.play(SoundEvents.NOTE_BLOCK_BELL.value(), 1.2F, 0.7F);
                }
            }
            return;
        }
        if (index != this.lastIndex[reel]) {
            this.lastIndex[reel] = index;
            if (now - this.lastClick > 55) {
                this.lastClick = now;
                this.play(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F + 0.2F * reel, 0.15F);
            }
        }
        boolean hanging = reel == 2 && this.twoNetherite && this.stopped[1] || reel == 3 && this.threeNetherite && this.stopped[2];
        if (hanging && now - this.lastBeat > (reel == 3 ? 520 : 700)) {
            this.lastBeat = now;
            this.play(SoundEvents.WARDEN_HEARTBEAT, 1.0F, reel == 3 ? 0.9F : 0.6F);
        }
    }

    private void play(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private void finish() {
        this.done = true;
        this.choices.forEach(button -> button.visible = true);
        switch (this.trait.tier) {
            case MYTHIC -> {
                this.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                this.play(SoundEvents.TOTEM_USE, 1.2F, 0.5F);
            }
            case CURSED -> this.play(SoundEvents.ELDER_GUARDIAN_CURSE, 1.0F, 0.5F);
            case RARE -> this.play(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.8F);
            default -> this.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
        }
    }

    /** What each payline gives, with the symbols drawn small. */
    private void paytable(GuiGraphics g, int x, int y) {
        g.drawString(this.font, Component.translatable("screen.mdvlcraft.trait_slots.paytable"), x, y, DqGui.HEADING, false);
        DqGui.rule(g, x, x + 96, y + 11);
        int[][] lines = {{2, 2, 2, 2}, {2, 2, 2}, {1, 1, 1}, {2, 2}, {}};
        Trait.Tier[] tiers = {Trait.Tier.MYTHIC, Trait.Tier.CURSED, Trait.Tier.RARE, Trait.Tier.RARE, Trait.Tier.COMMON};
        for (int i = 0; i < lines.length; i++) {
            int rowY = y + 15 + i * 16;
            for (int s = 0; s < lines[i].length; s++) {
                g.renderItem(SYMBOLS[lines[i][s]], x + s * 11, rowY);
            }
            if (lines[i].length == 0) {
                g.drawString(this.font, Component.translatable("screen.mdvlcraft.trait_slots.anything_else"), x, rowY + 4, DqGui.MUTED, false);
            }
            Component tier = Component.translatable("trait.mdvlcraft.tier." + tiers[i].name().toLowerCase(Locale.ROOT)).withStyle(tiers[i].colour);
            g.drawString(this.font, tier, x + 96 - this.font.width(tier), rowY + 4, DqGui.TEXT, false);
        }
    }

    /** The rolled trait: its name in its tier's colour, the tier, and what it does. */
    private void result(GuiGraphics g, int left, int y) {
        Component name = Component.translatable(this.trait.nameKey()).withStyle(this.trait.tier.colour, ChatFormatting.BOLD);
        g.drawCenteredString(this.font, name, left + W / 2, y, DqGui.TEXT);
        Component tier = Component.translatable("trait.mdvlcraft.tier." + this.trait.tier.name().toLowerCase(Locale.ROOT));
        g.drawCenteredString(this.font, tier, left + W / 2, y + 11, DqGui.MUTED);
        List<FormattedCharSequence> lines = this.font.split(Component.translatable(this.trait.descKey()), W - 28);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            g.drawCenteredString(this.font, lines.get(i), left + W / 2, y + 25 + i * 9, DqGui.TEXT);
        }
    }
}
