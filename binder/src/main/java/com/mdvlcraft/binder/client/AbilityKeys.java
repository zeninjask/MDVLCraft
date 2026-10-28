package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.network.BinderNetwork;
import com.mdvlcraft.binder.network.CastPacket;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import org.lwjgl.glfw.GLFW;

public final class AbilityKeys {
    private static final String CATEGORY = "key.categories.mdvlcraft";
    public static final KeyMapping MENU = key("menu", 77);
    public static final KeyMapping WHEEL = key("ability_wheel", 96);
    public static final KeyMapping CAST = key("cast_ability", 280);
    public static final KeyMapping GIVE_UP = key("give_up", 90);
    /** Quick Cast 1-8 (default keys 1-8, in place of the hotbar keys, RemovedKeys): each casts its wheel slot, held like the
     * cast key, without changing the slot the cast key uses. */
    private static final KeyMapping[] QUICK = IntStream.range(0, 8)
        .mapToObj(i -> key("quick_cast_" + (i + 1), GLFW.GLFW_KEY_1 + i)).toArray(KeyMapping[]::new);
    private static final List<KeyMapping> ALL = Stream.concat(Stream.of(MENU, WHEEL, CAST, GIVE_UP), Arrays.stream(QUICK)).toList();
    private static final boolean[] quickDown = new boolean[QUICK.length];
    private static boolean castDown;
    /** How long the cast key has been held (Formless Sword's Form mode opens the sword wheel at HOLD_TICKS). */
    private static int holdTicks;
    private static final int HOLD_TICKS = 6;

    private AbilityKeys() {
    }

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.mdvlcraft." + name, KeyConflictContext.IN_GAME, Type.KEYSYM, defaultKey, "key.categories.mdvlcraft");
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        ALL.forEach(event::register);
    }

    static void logConflicts(Minecraft minecraft) {
        for (KeyMapping mine : ALL) {
            Arrays.stream(minecraft.options.keyMappings)
                .filter(other -> other != mine && other.same(mine) && !RemovedKeys.removed(other))
                .forEach(
                    other -> MDVLBinder.LOGGER.warn("Key {} ({}) is also bound to {}", new Object[]{mine.getName(), mine.getKey().getName(), other.getName()})
                );
        }
    }

    /** A Quick Cast key: pressing casts the slot's ability (a held ability goes on while the key is held), releasing lets go. */
    private static void quickCast(Minecraft minecraft, int slot) {
        boolean tapped = false;
        while (QUICK[slot].consumeClick()) {
            tapped = true;
        }
        boolean down = QUICK[slot].isDown() && minecraft.screen == null;
        if (tapped && !down && !quickDown[slot]) {
            BinderNetwork.sendToServer(new CastPacket(true, slot));   // pressed and let go within a tick
            BinderNetwork.sendToServer(new CastPacket(false, slot));
        } else if (down != quickDown[slot]) {
            quickDown[slot] = down;
            BinderNetwork.sendToServer(new CastPacket(down, slot));
        }
    }

    static void onClientTick(ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase == Phase.END && minecraft.player != null) {
            while (MENU.consumeClick()) {
                minecraft.setScreen(new MenuScreen());
            }

            while (WHEEL.consumeClick()) {
                minecraft.setScreen(new WheelScreen());
            }

            for (int slot = 0; slot < QUICK.length; slot++) {
                quickCast(minecraft, slot);
            }

            boolean tapped = false;

            while (CAST.consumeClick()) {
                tapped = true;
            }

            boolean down = CAST.isDown() && minecraft.screen == null;
            if (ClientArsenal.formSelected() && !minecraft.player.isShiftKeyDown() && (down || castDown || tapped)) {
                // Formless Sword in Form mode: holding opens the sword wheel, a tap summons (or dismisses) the shadow weapon
                if (down) {
                    holdTicks++;
                    if (holdTicks == HOLD_TICKS) {
                        minecraft.setScreen(new SwordWheelScreen());
                    }
                } else if (holdTicks < HOLD_TICKS) {
                    BinderNetwork.sendToServer(new CastPacket(true, ClientAbilities.selected()));
                    BinderNetwork.sendToServer(new CastPacket(false, ClientAbilities.selected()));
                }
                castDown = down;
                if (!down) {
                    holdTicks = 0;
                }
            } else if (tapped && !down && !castDown) {
                BinderNetwork.sendToServer(new CastPacket(true, ClientAbilities.selected()));
                BinderNetwork.sendToServer(new CastPacket(false, ClientAbilities.selected()));
            } else if (down != castDown) {
                castDown = down;
                BinderNetwork.sendToServer(new CastPacket(down, ClientAbilities.selected()));
            }
        }
    }
}
