package com.mdvlcraft.binder.client;

import org.lwjgl.glfw.GLFW;

/**
 * Barbarian Arts (Ruins Greatsword charge) reads the mouse as right && left. Right-click alone now charges: the left
 * button reads as held unless it is pressed before a charge starts, since both buttons together cast the demon swords'
 * arts (DemonSwordInput). Once charging, the left button no longer matters until right is released.
 */
public final class RuinsChargeInput {
    private RuinsChargeInput() {
    }

    public static int mouseButton(long window, int button, boolean charging) {
        int state = GLFW.glfwGetMouseButton(window, button);
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return state;
        }
        return charging || state != GLFW.GLFW_PRESS ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
    }
}
