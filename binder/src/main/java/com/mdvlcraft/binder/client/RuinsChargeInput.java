package com.mdvlcraft.binder.client;

import org.lwjgl.glfw.GLFW;

/**
 * Barbarian Arts (Ruins Greatsword charge) reads the mouse as right && left. Holding left-click alone now charges: the
 * right button reads as held unless it is pressed before a charge starts, since both buttons together cast the demon
 * swords' arts (DemonSwordInput). Once charging, the right button no longer matters until left is released.
 */
public final class RuinsChargeInput {
    private RuinsChargeInput() {
    }

    public static int mouseButton(long window, int button, boolean charging) {
        int state = GLFW.glfwGetMouseButton(window, button);
        if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return state;
        }
        return charging || state != GLFW.GLFW_PRESS ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
    }
}
