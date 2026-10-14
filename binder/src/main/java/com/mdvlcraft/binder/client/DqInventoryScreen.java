package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.mixin.SlotAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.anti_ad.mc.ipn.api.IPNGuiHints;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.inventory.container.CuriosContainer;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;

/**
 * The survival inventory as Dragon Quest IX's equipment screen. The left half is the bag (inventory and hotbar), the
 * 2x2 crafting grid and the player's stats. The right half is the equipment page: the player model, one row per
 * equipment slot (main hand, off hand, head, body, legs, feet: the item's name with a green E, or "Nothing equipped"),
 * and the Curios accessories underneath.
 *
 * It draws Curios' own inventory menu, which holds the vanilla slots, the crafting grid and the accessory slots and
 * already handles moving, shift-clicking, crafting and syncing: opening the inventory asks the server for that menu
 * (onScreenOpening), and the screen Curios would show is replaced by this one. Slots are placed by what they hold, not
 * by index, every frame, since Curios rebuilds them when accessory slots are gained or lost. The main hand has no slot
 * of its own; its row is the selected hotbar slot.
 *
 * Inventory Profiles Next's buttons are moved by its hints, which count from the right and bottom of imageWidth x
 * imageHeight (its defaults sit above the vanilla inventory's rows): the move-all and sort buttons go to the right of
 * the Bag title, the continuous crafting box to the right of the Craft title and the profile selector above the
 * windows. imageHeight stays HEIGHT so they stay put; more accessory rows make the screen taller below it (fullHeight).
 */
@IPNGuiHints({
    @IPNGuiHint(button = IPNButton.MOVE_TO_CONTAINER, bottom = 113, horizontalOffset = 267),
    @IPNGuiHint(button = IPNButton.SORT, bottom = 125, horizontalOffset = 231),
    @IPNGuiHint(button = IPNButton.SORT_COLUMNS, bottom = 125, horizontalOffset = 231),
    @IPNGuiHint(button = IPNButton.SORT_ROWS, bottom = 125, horizontalOffset = 231),
    @IPNGuiHint(button = IPNButton.CONTINUOUS_CRAFTING, bottom = -13, horizontalOffset = 207),
    @IPNGuiHint(button = IPNButton.PROFILE_SELECTOR, bottom = -251)
})
public final class DqInventoryScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final ResourceLocation SLOTS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    private static final int LEFT_WIDTH = 9 * 18 + 16;
    private static final int RIGHT_X = LEFT_WIDTH + 6;
    private static final int RIGHT_WIDTH = 224;
    private static final int TITLE = 23;
    private static final int BAG_HEIGHT = 104;
    private static final int CRAFT_Y = 110;
    private static final int CRAFT_HEIGHT = 62;
    private static final int STATUS_Y = 178;
    private static final int STATUS_MIN_HEIGHT = 48;
    private static final int HEIGHT = STATUS_Y + STATUS_MIN_HEIGHT;
    private static final int EQUIP_HEIGHT = 156;
    private static final int MODEL_X = RIGHT_X + 8;
    private static final int MODEL_W = 74;
    private static final int ROWS_X = RIGHT_X + 8 + MODEL_W + 6;
    private static final int ROW_W = RIGHT_WIDTH - (8 + MODEL_W + 6) - 8;
    private static final int ROW_H = 21;
    private static final int MODEL_H = 6 * ROW_H - 1;
    private static final int ACCESSORIES_Y = EQUIP_HEIGHT + 6;
    private static final int ACCESSORY_COLUMNS = (RIGHT_WIDTH - 16) / 18;
    /** Equipment rows: the player inventory index each shows (-1: the selected hotbar slot) and its empty-slot name. */
    private static final int MAIN_HAND = -1;
    private static final int[] ROW_SLOTS = {MAIN_HAND, Inventory.SLOT_OFFHAND, 39, 38, 37, 36};
    private static final int ROW_FILL = 0xFF262626;
    private static final int ROW_FILL_HOVERED = 0xFF3A3A3A;
    private static final int ROW_EDGE = 0xFF787878;
    private static final int ROW_EDGE_HOVERED = 0xFFBDBDBD;
    private static final int ICON_FILL = 0xFF343434;
    private static final int NOTHING = 0xFF787878;
    /** The game's highlighted item name and its "equipped" E. */
    private static final int PICKED = 0xFFB5EF63;
    private static final int EQUIPPED = 0xFF63DE52;
    private static final String[] E = {"EEE", "E..", "EE.", "E..", "EEE"};
    /** The gold pointer of the game's menus, as on the title screen (16 x 7). */
    private static final String[] POINTER = {
        "....OO..........", "....OYO..OOOO...", "OOO.OHYOOOYYYOO.", "OHYOOHYYYYHHYYYO", "OOO.OYDOOODYYOO.", "....ODO..OOOO...", "....OO.........."
    };
    private static final Map<Character, Integer> POINTER_COLOURS = Map.of('O', 0xFF3A2000, 'D', 0xFFC08000, 'Y', 0xFFF8D020, 'H', 0xFFFFF8A0);
    private final List<Slot> accessories = new ArrayList<>();
    /** The screen's real height, imageHeight or more with many accessory rows. */
    private int fullHeight = HEIGHT;

    DqInventoryScreen(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = RIGHT_X + RIGHT_WIDTH;
        this.imageHeight = HEIGHT;
    }

    /**
     * Opening the survival inventory asks the server for Curios' menu instead (Curios' own button does the same), and
     * the screen Curios opens for it becomes this one.
     */
    static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == InventoryScreen.class) {
            event.setCanceled(true);
            NetworkHandler.INSTANCE.sendToServer(new CPacketOpenCurios(ItemStack.EMPTY));
        } else if (event.getNewScreen() instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof CuriosContainer menu
            && !(screen instanceof DqInventoryScreen)) {
            event.setNewScreen(new DqInventoryScreen(menu, Minecraft.getInstance().player.getInventory(), screen.getTitle()));
        }
    }

    /** Places every slot by what it holds; slots this screen does not show (cosmetic accessories) go off screen. */
    private void layout() {
        Inventory inventory = this.minecraft.player.getInventory();
        this.accessories.clear();
        for (Slot slot : this.menu.slots) {
            int x = -10000;
            int y = -10000;
            if (slot.container == inventory) {
                int i = slot.getContainerSlot();
                if (i < 9) {
                    x = 9 + i * 18;
                    y = TITLE + 3 * 18 + 6 + 1;
                } else if (i < 36) {
                    x = 9 + (i - 9) % 9 * 18;
                    y = TITLE + 1 + (i - 9) / 9 * 18;
                } else {
                    int row = rowOf(i);
                    x = ROWS_X + 2;
                    y = TITLE + row * ROW_H + 2;
                }
            } else if (slot instanceof ResultSlot) {
                x = 101;
                y = CRAFT_Y + TITLE + 10;
            } else if (slot.container instanceof CraftingContainer) {
                x = 31 + slot.getContainerSlot() % 2 * 18;
                y = CRAFT_Y + TITLE + 1 + slot.getContainerSlot() / 2 * 18;
            } else if (slot instanceof CurioSlot curio && !curio.isCosmetic()) {
                this.accessories.add(slot);
            }

            ((SlotAccessor)slot).mdvlcraft$setX(x);
            ((SlotAccessor)slot).mdvlcraft$setY(y);
        }

        for (int i = 0; i < this.accessories.size(); i++) {
            Slot slot = this.accessories.get(i);
            ((SlotAccessor)slot).mdvlcraft$setX(RIGHT_X + 9 + i % ACCESSORY_COLUMNS * 18);
            ((SlotAccessor)slot).mdvlcraft$setY(ACCESSORIES_Y + TITLE + 1 + i / ACCESSORY_COLUMNS * 18);
        }

        this.fullHeight = Math.max(HEIGHT, this.accessoriesBottom());
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.fullHeight) / 2;
    }

    private static int rowOf(int inventoryIndex) {
        for (int row = 0; row < ROW_SLOTS.length; row++) {
            if (ROW_SLOTS[row] == inventoryIndex) {
                return row;
            }
        }

        throw new IllegalStateException("No equipment row for inventory slot " + inventoryIndex);
    }

    private int accessoryRows() {
        return Math.max(1, (this.accessories.size() + ACCESSORY_COLUMNS - 1) / ACCESSORY_COLUMNS);
    }

    private int accessoriesBottom() {
        return ACCESSORIES_Y + TITLE + this.accessoryRows() * 18 + 5;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.layout();
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        int row = this.hoveredRow(mouseX, mouseY);
        if (row == 0 && this.menu.getCarried().isEmpty() && !this.mainHand().isEmpty()) {
            g.renderTooltip(this.font, this.mainHand(), mouseX, mouseY);
        } else {
            this.renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        Inventory inventory = this.minecraft.player.getInventory();
        DqGui.window(g, this.font, Component.translatable("screen.mdvlcraft.inventory.bag"), x, y, LEFT_WIDTH, BAG_HEIGHT);
        DqGui.window(g, this.font, Component.translatable("screen.mdvlcraft.inventory.craft"), x, y + CRAFT_Y, LEFT_WIDTH, CRAFT_HEIGHT);
        int ay = y + CRAFT_Y + TITLE + 13;
        g.fill(x + 79, ay, x + 89, ay + 2, DqGui.TEXT);
        for (int k = 0; k < 4; k++) {
            g.fill(x + 89 + k, ay - 3 + k, x + 90 + k, ay + 5 - k, DqGui.TEXT);
        }

        this.status(g, x, y + STATUS_Y, this.fullHeight - STATUS_Y);
        this.equipment(g, x + RIGHT_X, y, mouseX, mouseY);
        int ah = this.accessoriesBottom() - ACCESSORIES_Y;
        DqGui.window(g, this.font, Component.translatable("screen.mdvlcraft.inventory.accessories"), x + RIGHT_X, y + ACCESSORIES_Y, RIGHT_WIDTH, ah);
        for (Slot slot : this.menu.slots) {
            boolean inRow = slot.container == inventory && slot.getContainerSlot() >= 36;
            if (slot.x > -10000 && !inRow) {
                g.blit(SLOTS, x + slot.x - 1, y + slot.y - 1, 7, 83, 18, 18);
            }
        }

        // the selected hotbar slot: the light and blue lines of the game's selected frame
        int hx = x + 8 + inventory.selected * 18;
        int hy = y + TITLE + 3 * 18 + 6;
        outline(g, hx - 1, hy - 1, 20, 20, 0xFFE7DEDE);
        outline(g, hx, hy, 18, 18, DqGui.BLUE);
    }

    private void status(GuiGraphics g, int x, int y, int h) {
        DqGui.window(g, this.font, Component.translatable("screen.mdvlcraft.inventory.status"), x, y, LEFT_WIDTH, h);
        Player player = this.minecraft.player;
        stat(g, "max_health", player.getAttributeValue(Attributes.MAX_HEALTH), x + 10, y + TITLE + 1);
        stat(g, "attack", player.getAttributeValue(Attributes.ATTACK_DAMAGE), x + 92, y + TITLE + 1);
        stat(g, "defence", player.getAttributeValue(Attributes.ARMOR), x + 10, y + TITLE + 12);
        stat(g, "toughness", player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), x + 92, y + TITLE + 12);
    }

    private void stat(GuiGraphics g, String key, double value, int x, int y) {
        g.drawString(this.font, Component.translatable("screen.mdvlcraft.inventory.stat." + key), x, y, DqGui.MUTED, false);
        String number = value == Math.rint(value) ? String.valueOf((long)value) : String.format("%.1f", value);
        g.drawString(this.font, number, x + 76 - this.font.width(number), y, DqGui.TEXT, false);
    }

    private void equipment(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        DqGui.window(g, this.font, Component.translatable("screen.mdvlcraft.inventory.equipment"), x, y, RIGHT_WIDTH, EQUIP_HEIGHT);
        int mx = this.leftPos + MODEL_X;
        int my = y + TITLE;
        g.enableScissor(mx, my, mx + MODEL_W, my + MODEL_H);
        int feetX = mx + MODEL_W / 2;
        int feetY = my + MODEL_H - 8;
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, feetX, feetY, 52, feetX - mouseX, feetY - 80 - mouseY, this.minecraft.player);
        g.disableScissor();
        int hovered = this.hoveredRow(mouseX, mouseY);
        Inventory inventory = this.minecraft.player.getInventory();
        for (int row = 0; row < ROW_SLOTS.length; row++) {
            int rx = this.leftPos + ROWS_X;
            int ry = y + TITLE + row * ROW_H;
            boolean lit = row == hovered;
            int edge = lit ? ROW_EDGE_HOVERED : ROW_EDGE;
            g.fill(rx + 1, ry, rx + ROW_W - 1, ry + ROW_H - 1, edge);
            g.fill(rx, ry + 1, rx + ROW_W, ry + ROW_H - 2, edge);
            g.fill(rx + 1, ry + 1, rx + ROW_W - 1, ry + ROW_H - 2, lit ? ROW_FILL_HOVERED : ROW_FILL);
            g.fill(rx + 2, ry + 2, rx + 18, ry + 18, ICON_FILL);
            ItemStack stack = ROW_SLOTS[row] == MAIN_HAND ? this.mainHand() : inventory.getItem(ROW_SLOTS[row]);
            if (ROW_SLOTS[row] == MAIN_HAND && !stack.isEmpty()) {
                g.renderItem(stack, rx + 2, ry + 2);
                g.renderItemDecorations(this.font, stack, rx + 2, ry + 2);
            }

            if (stack.isEmpty()) {
                g.drawString(this.font, Component.translatable("screen.mdvlcraft.inventory.nothing"), rx + 22, ry + 6, NOTHING, false);
            } else {
                String name = this.font.plainSubstrByWidth(stack.getHoverName().getString(), ROW_W - 24);
                g.drawString(this.font, name, rx + 22, ry + 6, lit ? PICKED : DqGui.TEXT, false);
            }
        }
    }

    private ItemStack mainHand() {
        return this.minecraft.player.getInventory().getSelected();
    }

    /** The equipment row under the mouse, or -1. */
    private int hoveredRow(double mouseX, double mouseY) {
        double rx = mouseX - this.leftPos - ROWS_X;
        double ry = mouseY - this.topPos - TITLE;
        if (rx < 0 || rx >= ROW_W || ry < 0 || ry >= ROW_SLOTS.length * ROW_H) {
            return -1;
        }

        return (int)(ry / ROW_H);
    }

    /** The main-hand row is the selected hotbar slot: clicking it clicks that slot. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.hoveredRow(mouseX, mouseY) == 0 && (button == 0 || button == 1)) {
            int selected = this.minecraft.player.getInventory().selected;
            for (Slot slot : this.menu.slots) {
                if (slot.container == this.minecraft.player.getInventory() && slot.getContainerSlot() == selected) {
                    this.slotClicked(slot, slot.index, button, hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                    return true;
                }
            }

            throw new IllegalStateException("Inventory menu has no hotbar slot " + selected);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Clicks off the slots drop the carried stack only outside the windows, which can reach below imageHeight. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return mouseX < left || mouseY < top || mouseX >= left + this.imageWidth || mouseY >= top + this.fullHeight;
    }

    /** Drawn after the items, above them: the green E on worn equipment and the pointer at the row under the mouse. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Inventory inventory = this.minecraft.player.getInventory();
        int hovered = this.hoveredRow(mouseX, mouseY);
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        for (int row = 0; row < ROW_SLOTS.length; row++) {
            ItemStack stack = ROW_SLOTS[row] == MAIN_HAND ? this.mainHand() : inventory.getItem(ROW_SLOTS[row]);
            int ry = TITLE + row * ROW_H;
            if (!stack.isEmpty()) {
                pattern(g, E, ROWS_X + 15, ry + 13, Map.of('E', EQUIPPED));
            }

            if (row == hovered) {
                pattern(g, POINTER, ROWS_X - 13, ry + 6, POINTER_COLOURS);
            }
        }

        g.pose().popPose();
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    private static void pattern(GuiGraphics g, String[] rows, int x, int y, Map<Character, Integer> colours) {
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                Integer colour = colours.get(rows[row].charAt(col));
                if (colour != null) {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, colour);
                }
            }
        }
    }
}
