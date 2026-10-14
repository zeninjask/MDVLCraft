package com.mdvlcraft.binder.client;

import net.minecraft.Util;
import org.lwjgl.glfw.GLFW;

/**
 * Barbarian Arts (Ruins Greatsword charge) reads the mouse as right && left. Holding left-click alone now charges, once it
 * has been held for {@link #HOLD_MS} (a tap stays a combo attack): the left button reads as released until then, and the
 * right button reads as held unless it is pressed before a charge starts, since both buttons together cast the demon
 * swords' arts (DemonSwordInput). Once charging, only releasing left matters.
 */
public final class RuinsChargeInput {
    private static final long HOLD_MS = 350;
    private static long leftDownSince;

    private RuinsChargeInput() {
    }

    public static int mouseButton(long window, int button, boolean charging) {
        boolean down = GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (!down) {
                leftDownSince = 0;
                return GLFW.GLFW_RELEASE;
            }
            if (leftDownSince == 0) {
                leftDownSince = Util.getMillis();
            }
            return charging || Util.getMillis() - leftDownSince >= HOLD_MS ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
        }
        return charging || !down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
    }
}
