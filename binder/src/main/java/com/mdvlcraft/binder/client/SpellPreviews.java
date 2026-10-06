package com.mdvlcraft.binder.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.MDVLBinder;
import com.p1nero.iss_ponder.client.ClientPreviewController;
import net.createmod.ponder.enums.PonderKeybinds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Iron's Spell Scroll Ponder previews a spell when its scroll's tooltip is held with the Ponder key, but the pack has no
 * scrolls (spells come from the skill trees). Here the same preview opens from the spells the player does see: hold
 * the Ponder key (W) over a spell in the abilities screen or over a spell skill in the skill tree.
 * Skill tree spells are looked up in assets/mdvlcraft/spell_previews.json ("category/skill" -> spell and level), which
 * tools/generate_skill_trees.py writes, since the client's skill data does not say what a skill grants.
 */
public final class SpellPreviews {
    /** Iron's Spell Scroll Ponder's own hold time. */
    private static final int HOLD_TICKS = 12;
    /** A hover counts while it was seen this recently (the screen reports it every frame). */
    private static final long HOVER_MILLIS = 200;
    private static final ResourceLocation SKILL_SPELLS = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "spell_previews.json");

    private record Spell(ResourceLocation id, int level) {
    }

    @Nullable
    private static Map<String, Spell> skillSpells;
    @Nullable
    private static Spell hovered;
    private static long hoveredAt;
    private static int held;

    private SpellPreviews() {
    }

    /** The screen shows this spell under the mouse this frame. */
    static void hover(ResourceLocation spell, int level) {
        Spell now = new Spell(spell, level);
        if (!now.equals(hovered)) {
            held = 0;
        }
        hovered = now;
        hoveredAt = Util.getMillis();
    }

    /** The skill screen shows this skill of this category under the mouse this frame (SkillsScreenMixin). */
    public static void hoverSkill(ResourceLocation category, String skillId) {
        Spell spell = skillSpells().get(category + "/" + skillId);
        if (spell != null) {
            hover(spell.id(), spell.level());
        }
    }

    static Component prompt() {
        return Component.translatable("tooltip.iss_ponder.hold_to_preview", PonderKeybinds.PONDER.message());
    }

    static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean hovering = hovered != null && Util.getMillis() - hoveredAt < HOVER_MILLIS;
        if (!hovering || !PonderKeybinds.PONDER.isDown() || ClientPreviewController.isPending() || ClientPreviewController.isPreviewOpen()) {
            held = 0;
            return;
        }
        if (++held >= HOLD_TICKS) {
            ClientPreviewController.request(hovered.id(), hovered.level());
            hovered = null;
            held = 0;
        }
    }

    private static Map<String, Spell> skillSpells() {
        if (skillSpells == null) {
            Map<String, Spell> spells = new HashMap<>();
            try (Reader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(SKILL_SPELLS).openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                json.entrySet().forEach(entry -> {
                    JsonObject spell = entry.getValue().getAsJsonObject();
                    spells.put(entry.getKey(), new Spell(ResourceLocation.parse(spell.get("spell").getAsString()), spell.get("level").getAsInt()));
                });
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + SKILL_SPELLS, e);
            }
            skillSpells = spells;
        }
        return skillSpells;
    }
}
