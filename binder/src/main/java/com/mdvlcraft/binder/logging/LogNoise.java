package com.mdvlcraft.binder.logging;

import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * Drops warnings and errors the pack's mods log on every start or every few ticks that are known to be harmless (a
 * mixin aimed at a mod the pack doesn't have, an optional Epic Fight datapack field, a tag entry for an absent mod,
 * vanilla's chatter about passengers of unloaded entities, ...), so the logs show what matters. Each rule is a pattern
 * found in the pack's own client and server logs; anything else, and anything below WARN, is untouched. Installed as
 * early as possible (EarlyLogPlugin, before most mixin configs are read) and again when the mod is built.
 */
public final class LogNoise extends AbstractFilter {
    private static final List<Pattern> RULES = List.of(
        // mixin configs of other mods: optional targets, refmaps, version notes, conflicts Mixin resolves itself
        "^Reference map '.*' for .* could not be read",
        "^Error loading class: ",
        "^@Mixin target .* was not found ",
        "^Mixin config .* does not specify \"minVersion\" property",
        "^@(Redirect|ModifyConstant) conflict\\. Skipping ",
        "^Static binding violation: PRIVATE @Overwrite ",
        "^@Final field .* should be final",
        "^Mixin .* has multiple constructors",
        "^Force-disabling mixin '",
        "^Mod mixin into Embeddium internals detected",
        "^Attempted to load class .* for invalid dist ",
        // Epic Fight and its add-ons
        "^\\[Animation (Event|Group|Accessor) - Registry\\] Unknown animation .* Applying recovery",
        "^\\[Virtualization Protocol\\] Manual registration initializing",
        "^Unable to put an empty animation to weapon capability builder",
        "^FakeLevel\\.refLevel is null",
        "^Failed at connecting Epic Fight web server",
        "^Datapack animation reading failed: No constructor information",
        "^defineId called for: class .* from class yesman\\.epicfight",
        "^Can't find a particle type epicfight:",
        "^Skill [a-z0-9_]+:[a-z0-9_/]+ doesn't exist in the registry",
        "^Item Capability Exception: No item named ",
        "^\\[SLM EpicFight\\] No valid data folder found",
        "^\\[Data Finder\\] Directory not found",
        "^(=+ KRYPTON FNP =+|=+|Do not send messages about your game malfunctioning due to KryptonFNP.*|The author is unaware of this bug.*|Please send feedback via CurseForge.*)$",
        "^\\[SL Library\\] Mod not present:",
        // data of mods the pack only partly has
        "^Tag alshanex_familiars:.* is not known/registered",
        "^Power \".*\" will only be partially loaded",
        "^Error loading power \"medievalorigins:valkyrie/",
        "^Parsing error loading custom advancement iss_elden_ring:recipes/",
        "^Couldn't load tag (goety|iss_csw):",
        "^(earwear|facewear|knees) is not a registered slot type",
        "^Failed to parse recipe '.*'! Falling back to vanilla",
        "^Found loot table element validation problem in \\{loot_tables:alshanex_familiars:",
        "^Upgrade orb .* using legacy upgrade orb format",
        "^Duplicate MobEffect holder detected, skipping",
        "^Failed to load class for notation: com\\.lowdragmc",
        "^Fabric API detected! This is not a Fabric mod",
        "^Unknown recipe category: cataclysm:",
        "^Unknown biome 'minecraft:",
        "^Exception loading blockstate definition: 'epic_fight_avalon:blockstates/test_door\\.json'",
        "^Missing sprite: minecraft:textures/continuity_reserved/",
        "^Invalid frame index on sprite somakespells:",
        "^File alexscaves:sounds/.* does not exist",
        "^Item Group has no display items",
        "^Failed to load texture: .*:textures/models/armor/",
        // vanilla chatter
        "^Received passengers for unknown entity",
        "^Mismatch in destroy block pos",
        "^Couldn't set damage of loot item",
        "^Couldn't find a compatible enchantment for",
        "^LanServerPinger: Network is unreachable"
    ).stream().map(Pattern::compile).toList();
    private static final LogNoise INSTANCE = new LogNoise();

    private LogNoise() {
        super(Filter.Result.DENY, Filter.Result.NEUTRAL);
    }

    /** Adds the filter to every logging configuration in use (the one the game's loggers write through may not be the
     * one this class's own loader resolves to), once each; safe to call again. */
    public static void install() {
        java.util.Set<LoggerContext> contexts = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        try {
            for (ClassLoader loader : new ClassLoader[]{LogNoise.class.getClassLoader(), ClassLoader.getSystemClassLoader(),
                LogManager.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
                if (LogManager.getContext(loader, false) instanceof LoggerContext context) {
                    contexts.add(context);
                }
            }
            for (String name : new String[]{"mixin", LogManager.ROOT_LOGGER_NAME, "net.minecraft.server.MinecraftServer"}) {
                if (LogManager.getLogger(name) instanceof Logger logger) {
                    contexts.add(logger.getContext());
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // logging must never stop the game from starting
        }
        for (LoggerContext context : contexts) {
            try {
                Configuration config = context.getConfiguration();
                if (!config.hasFilter() || !containsSelf(config.getFilter())) {
                    config.addFilter(INSTANCE);
                }
                // and on every logger config, which checks each finished event (the configuration filter alone isn't
                // consulted on every path)
                java.util.List<org.apache.logging.log4j.core.config.LoggerConfig> loggers = new java.util.ArrayList<>(config.getLoggers().values());
                loggers.add(config.getRootLogger());
                for (org.apache.logging.log4j.core.config.LoggerConfig logger : loggers) {
                    if (!logger.hasFilter() || !containsSelf(logger.getFilter())) {
                        logger.addFilter(INSTANCE);
                    }
                }
                context.updateLoggers();
            } catch (RuntimeException | LinkageError e) {
                // as above
            }
        }
    }

    private static boolean containsSelf(Filter filter) {
        if (filter == INSTANCE) {
            return true;
        }
        return filter instanceof org.apache.logging.log4j.core.filter.CompositeFilter composite && composite.getFiltersArray() != null
            && java.util.Arrays.asList(composite.getFiltersArray()).contains(INSTANCE);
    }

    @Override
    public Filter.Result filter(LogEvent event) {
        return event.getMessage() == null ? Filter.Result.NEUTRAL : this.check(event.getLevel(), event.getMessage().getFormattedMessage());
    }

    // a configuration filter is asked through these before any event exists
    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object... params) {
        if (message == null || !level.isMoreSpecificThan(Level.WARN)) {
            return Filter.Result.NEUTRAL;
        }
        return this.check(level, params == null || params.length == 0 ? message : ParameterizedMessage.format(message, params));
    }

    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, Message message, Throwable thrown) {
        return message == null ? Filter.Result.NEUTRAL : this.check(level, message.getFormattedMessage());
    }

    @Override
    public Filter.Result filter(Logger logger, Level level, Marker marker, Object message, Throwable thrown) {
        return message == null ? Filter.Result.NEUTRAL : this.check(level, message.toString());
    }

    private Filter.Result check(Level level, String message) {
        if (!level.isMoreSpecificThan(Level.WARN)) {
            return Filter.Result.NEUTRAL;
        }
        for (Pattern rule : RULES) {
            if (rule.matcher(message).find()) {
                return Filter.Result.DENY;
            }
        }
        return Filter.Result.NEUTRAL;
    }
}
