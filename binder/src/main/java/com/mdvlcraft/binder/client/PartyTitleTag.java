package com.mdvlcraft.binder.client;

import app.sedu.mc.parties.client.render.TrackerOverlay;
import app.sedu.mc.parties.common.group.ClientDataHandler;
import com.fen.mmorpgtitles.TitleSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;

/**
 * Parties hides the name tag of party members and draws its own tracker, so Fen's RPG Titles (which draws the title
 * inside the name tag) loses the title. This draws the title just above the tracker's name, placed the way
 * PartyTrackerRenderer.renderWorldSpaceTracker places the name, while the tracker shows the name and not the distance.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class PartyTitleTag {
    private static final int FONT_HEIGHT = 9;
    /** Parties swaps the name for the distance marker beyond 20 blocks. */
    private static final int MAX_DISTANCE = 20;
    private static final float BASE_SCALE = 0.025F;

    private PartyTitleTag() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderName(RenderNameTagEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getResult() != Result.DENY || !(event.getEntity() instanceof Player target) || target == mc.player
            || TrackerOverlay.hideOverlay() || ClientDataHandler.getWrapper(target.getUUID()) == null) {
            return;
        }

        Component title = TitleSystem.getPlayerTitle(target);
        int distance = (int)Math.sqrt(mc.player.distanceToSqr(target));
        if (title == null || distance > MAX_DISTANCE) {
            return;
        }

        Camera camera = mc.getEntityRenderDispatcher().camera;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0, target.getBbHeight() + 0.5, 0.0);
        poseStack.mulPose(camera.rotation());
        float scale = BASE_SCALE * Math.max(1.0F, distance / 12.5F);
        poseStack.scale(-scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        Font font = mc.font;
        float x = -font.width(title) / 2.0F;
        float y = -(FONT_HEIGHT + 1);
        int background = (int)(mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(title, x, y, 0x20FFFFFF, false, matrix, event.getMultiBufferSource(), DisplayMode.SEE_THROUGH, background,
            event.getPackedLight());
        font.drawInBatch(title, x, y, -1, false, matrix, event.getMultiBufferSource(), DisplayMode.NORMAL, 0, event.getPackedLight());
        poseStack.popPose();
    }
}
