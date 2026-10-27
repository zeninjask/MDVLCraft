package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.SkillCalloutPacket;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The name of every skill and spell a player uses, called out above their head in the colour of its school or theme, like
 * Cursed Fate's chant words (client/SkillCalloutClient): Iron's spells (whatever casts them), the wheel's other abilities
 * (AbilityActions) and Epic Fight's own skills (mixin/epicfight/SkillCastCalloutMixin). Attacks, evocation and anything
 * without a theme are white. A second callout within half a second of the last is dropped (a technique that runs a spell
 * shows once).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SkillCallout {
    public static final int WHITE = 0xFFFFFF;
    private static final int SHRINE = 0xE03030;
    private static final int FIRE = 0xFF8C1A;
    private static final int NECROMANCY = 0xA040E0;
    private static final int GAP_TICKS = 10;
    private static final Map<String, Integer> SCHOOLS = Map.ofEntries(
        Map.entry("irons_spellbooks:fire", FIRE),
        Map.entry("irons_spellbooks:ice", 0x8FE3FF),
        Map.entry("irons_spellbooks:lightning", 0x4DA6FF),
        Map.entry("irons_spellbooks:holy", 0xFFE066),
        Map.entry("irons_spellbooks:nature", 0x6BD34A),
        Map.entry("irons_spellbooks:blood", NECROMANCY),
        Map.entry("irons_spellbooks:ender", 0xD05CFF),
        Map.entry("irons_spellbooks:eldritch", 0x1FB8A6),
        Map.entry("irons_spellbooks:void", 0x7B5CC9),
        Map.entry("irons_spellbooks:evocation", WHITE),
        Map.entry("cursed_school:cursed", SHRINE),
        Map.entry("iss_magicfromtheeast:symmetry", 0x3FCF9A),
        Map.entry("iss_magicfromtheeast:spirit", 0xA8ECFF),
        Map.entry("hazennstuff:radiance", 0xFFF2A8),
        Map.entry("hazennstuff:shadow", 0x8A7FA8),
        Map.entry("irons_chronomancy:time", 0xE0C060),
        Map.entry("iss_elden_ring:glintstone", 0x8FB8FF),
        Map.entry("somakespells:aqua", 0x3A8CFF),
        Map.entry("traveloptics:aqua", 0x3A8CFF),
        Map.entry("wind_spellbooks:wind", 0xCFF5E0),
        Map.entry("alshanex_familiars:sound", 0xFF9ECF),
        Map.entry("familiarslib:sound", 0xFF9ECF),
        Map.entry("antimagic:anti_magic", 0xB0B0B0),
        Map.entry("esoteric_spell:esoteric", 0xC9A0FF),
        Map.entry("esoteric_spell:barrier", 0xC9A0FF),
        Map.entry("iss_csw:fusion", 0xFF7FD0));
    private static final Set<Technique> SHRINE_TECHNIQUES = EnumSet.of(Technique.DISMANTLE, Technique.CLEAVE, Technique.MALEVOLENT_SHRINE,
        Technique.OPEN_MALEVOLENT_SHRINE, Technique.MALEVOLENT_KITCHEN, Technique.CHOP, Technique.MINCE, Technique.DICING_BOARD);
    private static final Set<Technique> BLOOD_TECHNIQUES = EnumSet.of(Technique.BLOOD_METEORITE, Technique.SLICING_EXORCISM, Technique.CONVERGENCE,
        Technique.PIERCING_BLOOD, Technique.CRIMSON_BINDING);
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private SkillCallout() {
    }

    public static void announce(ServerPlayer player, Component name, int colour) {
        // DEBUGCALLOUT (temporary): callouts don't appear in the test client yet; see docs/AGENT_HANDOFF.md
        org.apache.logging.log4j.LogManager.getLogger().info("DEBUGCALLOUT server {} {}", name.getString(), Integer.toHexString(colour));
        long now = player.level().getGameTime();
        Long last = LAST.put(player.getUUID(), now);
        if (last == null || now - last >= GAP_TICKS || now < last) {
            BinderNetwork.sendToTrackingAndSelf(player, new SkillCalloutPacket(player.getId(), name, colour));
        }
    }

    /** A wheel ability other than an Iron's spell (those are called out when Iron's casts them). */
    static void announce(ServerPlayer player, Ability ability) {
        int colour = WHITE;
        if (ability instanceof Technique technique) {
            colour = SHRINE_TECHNIQUES.contains(technique) ? SHRINE : BLOOD_TECHNIQUES.contains(technique) ? NECROMANCY
                : technique == Technique.SEAR ? FIRE : WHITE;
        } else if (ability instanceof StanceAbility) {
            StanceElement element = Stances.element(player);
            announce(player, element.displayName(), element.colour);
            return;
        }
        announce(player, ability.displayName(), colour);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCast(SpellOnCastEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
            if (spell != SpellRegistry.none()) {
                announce(player, spell.getDisplayName(player), SCHOOLS.getOrDefault(spell.getSchoolType().getId().toString(), WHITE));
            }
        }
    }
}
