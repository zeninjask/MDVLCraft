package com.mdvlcraft.binder.gate.tower.client;

import com.mdvlcraft.binder.client.DqGui;
import com.mdvlcraft.binder.gate.tower.CastleStartPacket;
import com.mdvlcraft.binder.network.BinderNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The Castle Sigil's start menu (plan 10.3), opened by CastleMenuPacket: continue from the reached floor, start from the
 * checkpoint or from floor 1, or step through the other block starts (6, 11 ...) below the checkpoint. The pick goes to
 * the server as a CastleStartPacket, which checks it again (CastleSigilItem.menuChoice).
 */
public class CastleStartScreen extends Screen {
    private static final int W = 220;
    private static final int ROW = 22;
    private final BlockPos base;
    private final int reached;
    private final int checkpoint;
    /** The block start shown in the stepper row (only when there are block starts besides 1 and the checkpoint). */
    private int other;
    private int top;
    private int height2;

    private CastleStartScreen(BlockPos base, int reached, int checkpoint) {
        super(Component.translatableWithFallback("tower.mdvlcraft.menu.title", "Demon Castle"));
        this.base = base;
        this.reached = reached;
        this.checkpoint = checkpoint;
        this.other = Math.max(1, checkpoint - 5);
    }

    public static void open(BlockPos base, int reached, int checkpoint) {
        Minecraft.getInstance().setScreen(new CastleStartScreen(base, reached, checkpoint));
    }

    @Override
    protected void init() {
        boolean showCheckpoint = this.checkpoint != this.reached;
        boolean showFirst = this.reached != 1 && this.checkpoint != 1;
        boolean showOther = this.checkpoint >= 11;
        int rows = 2 + (showCheckpoint ? 1 : 0) + (showFirst ? 1 : 0) + (showOther ? 1 : 0);
        this.height2 = 44 + rows * ROW + 6;
        this.top = (this.height - this.height2) / 2;
        int left = (this.width - W) / 2;
        int x = left + 14;
        int w = W - 28;
        int y = this.top + 44;
        this.addRenderableWidget(new DqGui.Button(x, y, w, 18,
            Component.translatableWithFallback("tower.mdvlcraft.menu.continue", "Continue from floor %s", this.reached), () -> this.pick(this.reached)));
        y += ROW;
        if (showCheckpoint) {
            this.addRenderableWidget(new DqGui.Button(x, y, w, 18,
                Component.translatableWithFallback("tower.mdvlcraft.menu.checkpoint", "From the checkpoint: floor %s", this.checkpoint),
                () -> this.pick(this.checkpoint)));
            y += ROW;
        }
        if (showFirst) {
            this.addRenderableWidget(new DqGui.Button(x, y, w, 18,
                Component.translatableWithFallback("tower.mdvlcraft.menu.first", "From floor 1"), () -> this.pick(1)));
            y += ROW;
        }
        if (showOther) {
            DqGui.Button floor = new DqGui.Button(x + 22, y, w - 44 - 60, 18, this.otherLabel(), () -> this.pick(this.other));
            this.addRenderableWidget(new DqGui.Button(x, y, 18, 18, Component.literal("<"), () -> this.step(-5, floor)));
            this.addRenderableWidget(floor);
            this.addRenderableWidget(new DqGui.Button(x + w - 60 - 18, y, 18, 18, Component.literal(">"), () -> this.step(5, floor)));
            this.addRenderableWidget(new DqGui.Button(x + w - 56, y, 56, 18,
                Component.translatableWithFallback("tower.mdvlcraft.menu.start", "Start"), () -> this.pick(this.other)));
            y += ROW;
        }
        this.addRenderableWidget(new DqGui.Button(x + w / 2 - 40, y, 80, 18,
            Component.translatableWithFallback("tower.mdvlcraft.menu.cancel", "Cancel"), this::onClose));
    }

    private Component otherLabel() {
        return Component.translatableWithFallback("tower.mdvlcraft.menu.floor", "Floor %s", this.other);
    }

    /** Steps the block start by five, kept within 1 .. checkpoint. */
    private void step(int by, DqGui.Button floor) {
        this.other = Math.max(1, Math.min(this.checkpoint, this.other + by));
        floor.setMessage(this.otherLabel());
    }

    private void pick(int floor) {
        BinderNetwork.sendToServer(new CastleStartPacket(this.base, floor));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int left = (this.width - W) / 2;
        DqGui.panel(g, this.font, this.title, left, this.top, W, this.height2);
        g.drawCenteredString(this.font, Component.translatableWithFallback("tower.mdvlcraft.menu.hint", "Where do you start?"),
            left + W / 2, this.top + 28, DqGui.MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
