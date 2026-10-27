package com.mdvlcraft.binder.client;

import app.sedu.mc.parties.common.data.PlayerData;
import app.sedu.mc.parties.common.data.core.DataTypes;
import app.sedu.mc.parties.common.group.ClientDataHandler;
import app.sedu.mc.parties.common.group.ClientGroupManager;
import app.sedu.mc.parties.common.group.GroupType;
import app.sedu.mc.parties.common.group.TrackedGroup;
import app.sedu.mc.parties.compat.modules.irons_spellbooks.ManaModule;
import com.mdvlcraft.binder.ability.AbilityIcons;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The Dragon Quest IX party status windows in the top left: you first, then each party member (by name), one window
 * each with the name, HP and MP bars, two vocation boxes with the emblems of the player's archetypes (the higher level
 * on the left, empty boxes before any is picked) and that higher archetype's level. The windows are
 * copied pixel for pixel from the game's (64 x 56, rows stretched by one pixel per text line for the pack's 7 px
 * capitals). The game cuts names at 8 letters; here every window widens alike to fit the longest name in full, and the
 * bars stretch with it. Other members' values are the ones Parties syncs to the client; Parties' own HUD frames are hidden
 * (onRenderOverlay), its world trackers stay.
 *
 * When a player gains archetype experience, a small window under theirs shows it Pokemon style (ExpBars): the bar
 * filling towards the next level and "+N" beneath. A row of windows with a bar showing pushes the rows below it down.
 */
final class PartyPanels implements IGuiOverlay {
    static final PartyPanels INSTANCE = new PartyPanels();
    private static final ResourceLocation PARTIES_HUD = ResourceLocation.fromNamespaceAndPath("parties", "parties_hud");
    private static final int MIN_WIDTH = 64;
    /** Where "Lv." starts, right of the two vocation boxes. */
    private static final int LEVEL_LEFT = 39;
    private static final int H = 61;
    private static final int GAP = 2;
    private static final int MARGIN = 4;
    private static final int HEADER_END = 16;
    /** The experience window under a party window: its height and the space above it. */
    private static final int EXP_H = 26;
    private static final int EXP_GAP = 1;
    /** Left edge of each row (the right edge mirrors it) and the colour across the middle. */
    private static final String[][] ROWS = {
        {"...OOOO", "O"}, {"..ORRRR", "R"}, {".ORBBBB", "F"}, {"ORBBBBB", "F"}, {"OGRRRRR", "R"}, {"ORFFFFF", "F"},
        {"ORGFFFF", "F"}, {".ORGFFF", "F"}, {"..ORRRR", "R"}, {"...OOOO", "O"}
    };
    private static final Map<Character, Integer> COLOURS = Map.of(
        'O', DqGui.BLACK, 'R', 0xFFE7E7E7, 'B', DqGui.BLUE, 'F', DqGui.CHARCOAL, 'G', 0xFF8C8C8C
    );
    private static final int BAR_EDGE = 0xFF8C8C8C;
    private static final int BAR_TOP = 0xFFC6C6C6;
    private static final int HP_FILL = 0xFF106BFF;
    private static final int HP_SHINE = 0xFF9CC6F7;
    private static final int MP_FILL = 0xFFFF7B18;
    private static final int MP_SHINE = 0xFFFFBD63;
    private static final int EXP_FILL = 0xFF2BC8F0;
    private static final int EXP_SHINE = 0xFFA8ECFF;
    private static final int BOX = 0xFFE7E7E7;
    /** The game's text colours for a member at a quarter of their HP or less, and for a fallen one. */
    private static final int LOW = 0xFFFFBD63;
    private static final int DOWN = 0xFFFF3131;

    private PartyPanels() {
    }

    static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(PARTIES_HUD)) {
            event.setCanceled(true);
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        List<Member> members = members(minecraft.player);
        // The header leaves 7 px each side for the blue tabs and 1 px of space inside them; a name's last column is spacing.
        int w = Math.max(MIN_WIDTH, members.stream().mapToInt(m -> Math.max(
            minecraft.font.width(m.name()) - 1 + 16 + (m.leader() ? 2 * minecraft.font.width(PartyLeader.MARK) : 0),
            LEVEL_LEFT + minecraft.font.width("Lv. ") + 7 + minecraft.font.width(String.valueOf(m.archetypes().level()))
        )).max().getAsInt());
        int perRow = Math.max(1, (screenWidth - 2 * MARGIN + GAP) / (w + GAP));
        int y = MARGIN;
        for (int row = 0; row * perRow < members.size(); row++) {
            boolean bars = false;
            for (int i = row * perRow; i < Math.min(members.size(), (row + 1) * perRow); i++) {
                Member member = members.get(i);
                int x = MARGIN + i % perRow * (w + GAP);
                ExpBars.Bar bar = ExpBars.get(member.id());
                draw(graphics, minecraft, member, bar == null ? member.archetypes().level() : bar.level(), x, y, w);
                if (bar != null) {
                    exp(graphics, minecraft.font, bar, x, y + H + EXP_GAP, w);
                    bars = true;
                }
            }

            y += H + GAP + (bars ? EXP_GAP + EXP_H : 0);
        }
    }

    private static List<Member> members(LocalPlayer self) {
        List<Member> members = new ArrayList<>();
        double maxMana = self.getAttributeValue((Attribute)AttributeRegistry.MAX_MANA.get());
        TrackedGroup party = ClientGroupManager.getGroup(GroupType.PARTY);
        members.add(new Member(self.getUUID(), self.getGameProfile().getName(), self.getHealth(), self.getMaxHealth(),
            ClientMagicData.getPlayerMana(), (float)maxMana, ClientArchetypes.get(self.getUUID()), true,
            party != null && self.getUUID().equals(party.getLeader())));
        if (party == null) {
            return members;
        }

        Objects.requireNonNull(ManaModule.MANA, "Parties' Iron's Spellbooks mana module is off (config/parties/modules.properties)");
        List<Member> others = new ArrayList<>();
        for (UUID id : party.getMembers()) {
            if (!id.equals(self.getUUID())) {
                PlayerData data = ClientDataHandler.getPlayerData(id);
                others.add(new Member(id, data.getData(DataTypes.NAME), data.getData(DataTypes.HEALTH),
                    data.getData(DataTypes.MAX_HEALTH).floatValue(), data.getData(ManaModule.MANA),
                    data.getData(ManaModule.MAX_MANA).floatValue(), ClientArchetypes.get(id), data.isOnline(), id.equals(party.getLeader())));
            }
        }

        others.sort(Comparator.comparing(Member::name, String.CASE_INSENSITIVE_ORDER));
        members.addAll(others);
        return members;
    }

    /** The level shown follows a filling experience bar, going up as the bar wraps. */
    private static void draw(GuiGraphics g, Minecraft minecraft, Member member, int level, int x, int y, int w) {
        Font font = minecraft.font;
        window(g, x, y, w);
        int text = !member.online() ? DqGui.MUTED : member.hp() <= 0.0F ? DOWN : member.hp() <= member.maxHp() / 4.0F ? LOW : DqGui.TEXT;
        int nameX = x + (w - font.width(member.name()) + 1) / 2;
        g.drawString(font, member.name(), nameX, y + 5, text, false);
        if (member.leader()) {   // the party leader's crown, left of the name
            g.drawString(font, PartyLeader.MARK, nameX - font.width(PartyLeader.MARK) - 1, y + 5, PartyLeader.COLOUR, false);
        }
        g.drawString(font, "HP", x + 7, y + 18, text, false);
        number(g, font, Mth.ceil(member.hp()), x + w - 10, y + 18, text);
        bar(g, x + 6, y + 26, w - 12, member.hp() / member.maxHp(), HP_SHINE, HP_FILL);
        g.drawString(font, "MP", x + 7, y + 31, text, false);
        number(g, font, Mth.floor(member.mana()), x + w - 10, y + 31, text);
        bar(g, x + 6, y + 39, w - 12, member.maxMana() <= 0.0F ? 0.0F : member.mana() / member.maxMana(), MP_SHINE, MP_FILL);
        List<String> archetypes = member.archetypes().archetypes();
        box(g, archetypes.size() > 0 ? archetypes.get(0) : null, x + 6, y + 44);
        box(g, archetypes.size() > 1 ? archetypes.get(1) : null, x + 22, y + 44);
        if (!archetypes.isEmpty()) {
            g.drawString(font, "Lv.", x + LEVEL_LEFT, y + 47, text, false);
            number(g, font, level, x + w - 8, y + 47, text);
        }
    }

    /** The experience window: "EXP" and the bar, "+N" beneath. */
    private static void exp(GuiGraphics g, Font font, ExpBars.Bar bar, int x, int y, int w) {
        rows(g, x, y, w, 0, 0, 0);
        rows(g, x, y, w, 1, 1, 1);
        rows(g, x, y, w, 7, 2, 2);
        rows(g, x, y, w, 6, 3, 3);
        rows(g, x, y, w, 5, 4, EXP_H - 5);
        for (int i = 6; i < ROWS.length; i++) {
            rows(g, x, y, w, i, EXP_H - 10 + i, EXP_H - 10 + i);
        }

        g.drawString(font, "EXP", x + 7, y + 5, DqGui.TEXT, false);
        int barX = x + 7 + font.width("EXP") + 2;
        bar(g, barX, y + 7, x + w - 6 - barX, (float)bar.fill(), EXP_SHINE, EXP_FILL);
        number(g, font, "+" + bar.counted(), x + w - 10, y + 15, DqGui.TEXT);
    }

    private static void window(GuiGraphics g, int x, int y, int w) {
        rows(g, x, y, w, 0, 0, 0);
        rows(g, x, y, w, 1, 1, 1);
        rows(g, x, y, w, 2, 2, 2);
        rows(g, x, y, w, 3, 3, HEADER_END - 1);
        rows(g, x, y, w, 4, HEADER_END, HEADER_END);
        rows(g, x, y, w, 5, HEADER_END + 1, H - 5);
        for (int i = 6; i < ROWS.length; i++) {
            rows(g, x, y, w, i, H - 10 + i, H - 10 + i);
        }
    }

    /** Draws ROWS[pattern] on rows from..to of the window. */
    private static void rows(GuiGraphics g, int x, int y, int w, int pattern, int from, int to) {
        String left = ROWS[pattern][0];
        for (int i = 0; i < left.length(); i++) {
            char c = left.charAt(i);
            if (c != '.') {
                g.fill(x + i, y + from, x + i + 1, y + to + 1, COLOURS.get(c));
                g.fill(x + w - 1 - i, y + from, x + w - i, y + to + 1, COLOURS.get(c));
            }
        }

        g.fill(x + left.length(), y + from, x + w - left.length(), y + to + 1, COLOURS.get(ROWS[pattern][1].charAt(0)));
    }

    /** A gauge 4 px tall: grey sides and bottom, the fill's light shine along the top of the filled part. */
    private static void bar(GuiGraphics g, int x, int y, int width, float fraction, int shine, int fill) {
        int inner = width - 2;
        int filled = Mth.clamp(Mth.ceil(fraction * inner), 0, inner);
        g.fill(x, y, x + inner + 2, y + 1, BAR_TOP);
        g.fill(x + 1, y, x + 1 + filled, y + 1, shine);
        g.fill(x, y + 1, x + 1, y + 3, BAR_EDGE);
        g.fill(x + inner + 1, y + 1, x + inner + 2, y + 3, BAR_EDGE);
        g.fill(x + 1, y + 1, x + 1 + filled, y + 3, fill);
        g.fill(x, y + 3, x + inner + 2, y + 4, BAR_EDGE);
    }

    /**
     * A 14 x 14 vocation box with rounded corners, holding an archetype's emblem (null: empty). The 32 px emblem is
     * drawn at 16 px (two screen pixels per icon pixel at GUI scale 4, as in the skill trees); its item sits in the
     * middle 12, leaving a pixel of box round it.
     */
    private static void box(GuiGraphics g, String archetype, int x, int y) {
        g.fill(x + 1, y, x + 13, y + 14, BOX);
        g.fill(x, y + 1, x + 14, y + 13, BOX);
        if (archetype != null) {
            g.blit(AbilityIcons.tab(archetype), x - 1, y - 1, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    /** A number whose last pixel column is right - 1. */
    private static void number(GuiGraphics g, Font font, int value, int right, int y, int colour) {
        number(g, font, String.valueOf(value), right, y, colour);
    }

    private static void number(GuiGraphics g, Font font, String s, int right, int y, int colour) {
        g.drawString(font, s, right + 1 - font.width(s), y, colour, false);
    }

    private record Member(UUID id, String name, float hp, float maxHp, float mana, float maxMana, ClientArchetypes.Entry archetypes, boolean online,
                          boolean leader) {
    }
}
