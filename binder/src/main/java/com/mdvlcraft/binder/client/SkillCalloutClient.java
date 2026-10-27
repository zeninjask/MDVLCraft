package com.mdvlcraft.binder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Skill callouts (ability/SkillCallout): the skill's name in capitals and its colour, outlined, above the caster's head for
 * 1.5 times as long as Cursed Fate's chant words stay up (25 ticks). Your own shows near the top of the screen while you
 * look through your own eyes, where the one above your head can't be seen.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class SkillCalloutClient {
    private static final int TICKS = 38;
    private static final float SCALE = 0.03F;
    private static final double ABOVE_HEAD = 0.7;
    private static final float SCREEN_SCALE = 2.0F;
    private static final Map<Integer, Callout> SHOWN = new HashMap<>();

    private SkillCalloutClient() {
    }

    public static void show(int entityId, Component name, int colour) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            SHOWN.put(entityId, new Callout(Component.literal(name.getString().toUpperCase(Locale.ROOT)).getVisualOrderText(), colour, level.getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || level == null || SHOWN.isEmpty()) {
            return;
        }
        SHOWN.values().removeIf(callout -> level.getGameTime() - callout.start >= TICKS || level.getGameTime() < callout.start);
        Camera camera = event.getCamera();
        Vec3 view = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;
        SHOWN.forEach((id, callout) -> {
            Entity entity = level.getEntity(id);
            if (entity == null || entity.isInvisibleTo(minecraft.player) || (entity == camera.getEntity() && !camera.isDetached())) {
                return;
            }
            Vec3 at = entity.getPosition(event.getPartialTick()).add(0.0, entity.getBbHeight() + ABOVE_HEAD, 0.0);
            pose.pushPose();
            pose.translate(at.x - view.x, at.y - view.y, at.z - view.z);
            pose.mulPose(camera.rotation());
            pose.scale(-SCALE, -SCALE, SCALE);
            font.drawInBatch8xOutline(callout.text, -font.width(callout.text) / 2.0F, 0.0F, callout.colour, outline(callout.colour),
                pose.last().pose(), buffers, LightTexture.FULL_BRIGHT);
            pose.popPose();
        });
        buffers.endBatch();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson() || minecraft.options.hideGui) {
            return;
        }
        Callout callout = SHOWN.get(minecraft.player.getId());
        if (callout == null || minecraft.level.getGameTime() - callout.start >= TICKS) {
            return;
        }
        PoseStack pose = event.getGuiGraphics().pose();
        pose.pushPose();
        pose.translate(event.getWindow().getGuiScaledWidth() / 2.0F, event.getWindow().getGuiScaledHeight() * 0.28F, 0.0F);
        pose.scale(SCREEN_SCALE, SCREEN_SCALE, 1.0F);
        minecraft.font.drawInBatch8xOutline(callout.text, -minecraft.font.width(callout.text) / 2.0F, 0.0F, callout.colour, outline(callout.colour),
            pose.last().pose(), event.getGuiGraphics().bufferSource(), LightTexture.FULL_BRIGHT);
        event.getGuiGraphics().flush();
        pose.popPose();
    }

    /** The colour at a quarter brightness, for the outline. */
    private static int outline(int colour) {
        return (colour >> 2) & 0x3F3F3F;
    }

    private record Callout(FormattedCharSequence text, int colour, long start) {
    }
}
