package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.MDVLBinder;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent.Init.Post;
import net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre;

public final class MenuNav {
    private static Screen pendingParent;
    private static Screen target;
    private static DqGui.Button back;

    private MenuNav() {
    }

    public static void open(Screen parent, Supplier<Screen> screen) {
        openWith(parent, () -> {
            Screen s = screen.get();
            if (s != null) {
                Minecraft.getInstance().setScreen(s);
            }
        });
    }

    public static void openOwnBack(Supplier<Screen> screen) {
        try {
            Screen s = screen.get();
            if (s != null) {
                Minecraft.getInstance().setScreen(s);
            }
        } catch (RuntimeException e) {
            MDVLBinder.LOGGER.warn("MDVLCraft menu could not open a screen", e);
        }
    }

    public static void openWith(Screen parent, Runnable opener) {
        pendingParent = parent;

        try {
            opener.run();
        } catch (RuntimeException e) {
            MDVLBinder.LOGGER.warn("MDVLCraft menu could not open a screen", e);
        } finally {
            pendingParent = null;
        }
    }

    public static boolean exists(String className) {
        try {
            Class.forName(className, false, MenuNav.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Supplier<Screen> make(String className) {
        return () -> construct(className, new Class[0]);
    }

    public static Supplier<Screen> makeWithParent(String className, Screen parent) {
        return () -> construct(className, new Class[]{Screen.class}, parent);
    }

    public static Supplier<Screen> makeWithPlayer(String className) {
        return () -> construct(className, new Class[]{Player.class}, Minecraft.getInstance().player);
    }

    private static Screen construct(String className, Class<?>[] types, Object... args) {
        try {
            return (Screen)Class.forName(className).getConstructor(types).newInstance(args);
        } catch (ReflectiveOperationException | ClassCastException e) {
            MDVLBinder.LOGGER.warn("MDVLCraft menu could not open {}", className, e);
            return null;
        }
    }

    static void onScreenInit(Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof MenuScreen) {
            target = null;
            back = null;
        }

        if (pendingParent != null && !(screen instanceof MenuScreen) && !(screen instanceof CharacterScreen)) {
            target = screen;
            Screen parent = pendingParent;
            back = new DqGui.Button(0, 0, 64, 18, Component.translatable("screen.mdvlcraft.menu.back"), () -> Minecraft.getInstance().setScreen(parent));
        }

        if (screen == target && back != null) {
            back.setPosition(6, screen.height - 24);
        }
    }

    static void onScreenRender(net.minecraftforge.client.event.ScreenEvent.Render.Post event) {
        if (event.getScreen() == target && back != null) {
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, 0.0F, 400.0F);
            back.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            event.getGuiGraphics().pose().popPose();
        }
    }

    static void onMouseClicked(Pre event) {
        if (event.getScreen() == target && back != null && event.getButton() == 0 && back.isMouseOver(event.getMouseX(), event.getMouseY())) {
            back.playDownSound(Minecraft.getInstance().getSoundManager());
            back.onPress();
            event.setCanceled(true);
        }
    }
}
