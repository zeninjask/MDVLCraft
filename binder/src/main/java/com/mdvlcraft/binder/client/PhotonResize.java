package com.mdvlcraft.binder.client;

/**
 * Photon's bloom targets (Cursed Fate's effects and other Photon particles draw through them) are wired to the textures of
 * Minecraft's main render target, and Photon rewires them only when it sees the target's size change, which it checks
 * only while drawing an effect. Going fullscreen and back rebuilds the main target twice; if no effect drew in between,
 * the size Photon remembers matches again and it keeps drawing into the deleted textures: effects and particles blur
 * until the next size change. Every display resize marks Photon's targets stale (mixin/photon/MinecraftResizeMixin), and
 * the next time Photon checks its size it rebuilds them (mixin/photon/BloomEffectMixin). Photon isn't touched before it
 * draws on its own, so its shaders aren't loaded early.
 */
public final class PhotonResize {
    private static volatile boolean stale;

    private PhotonResize() {
    }

    public static void markStale() {
        stale = true;
    }

    /** Whether the display was resized since Photon last rebuilt its targets (and clears it). */
    public static boolean takeStale() {
        boolean was = stale;
        stale = false;
        return was;
    }
}
