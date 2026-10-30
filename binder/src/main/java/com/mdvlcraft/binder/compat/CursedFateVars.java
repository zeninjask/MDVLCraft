package com.mdvlcraft.binder.compat;

import cursedfate.network.CursedfateModVariables;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

/**
 * Cursed Fate add-ons (Mei Mei, Naoya) read Cursed Fate's player variables every tick by running
 * `/cursedfate Datapack ReturnAndPrint @s <name>`: a command parse, an entity selector and a reflective field lookup per
 * read. This answers such a command directly, printed as that command prints it (the value's toString, "N/A" when unset).
 */
public final class CursedFateVars {
    private static final String RETURN_AND_PRINT = "Datapack ReturnAndPrint @s ";
    private static final Map<String, Optional<Field>> FIELDS = new ConcurrentHashMap<>();

    private CursedFateVars() {
    }

    /** The command's printed result, or null when it isn't a ReturnAndPrint of the entity's own variable (run it then). */
    @Nullable
    public static String returnAndPrint(@Nullable Entity entity, String command) {
        int at = command.indexOf(RETURN_AND_PRINT);
        if (entity == null || at < 0 || entity.level().isClientSide) {
            return null;
        }
        Optional<Field> field = FIELDS.computeIfAbsent(command.substring(at + RETURN_AND_PRINT.length()).trim(), name -> {
            try {
                return Optional.of(CursedfateModVariables.PlayerVariables.class.getDeclaredField(name));
            } catch (NoSuchFieldException e) {
                return Optional.empty();
            }
        });
        CursedfateModVariables.PlayerVariables vars = entity.getCapability(CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
        if (field.isEmpty() || vars == null) {
            return null;
        }
        try {
            Object value = field.get().get(vars);
            return value == null ? "N/A" : value.toString();
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
