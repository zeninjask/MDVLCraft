package com.mdvlcraft.binder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;

/**
 * A marker over friendly summons (modified/FriendlySummons tells the client which): a green arrow over the player's own,
 * a blue one over an ally's, seen through walls faintly like a name tag. It sits above any name tag.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class FriendlySummonMarker {
    private static final String MARKER = "▼";
    private static final int OWN = 0xFF55FF55;
    private static final int ALLY = 0xFF55CCFF;
    private static final float SCALE = 0.03F;
    private static Map<Integer, Boolean> summons = Map.of();

    private FriendlySummonMarker() {
    }

    public static void set(Map<Integer, Boolean> friendly) {
        summons = Map.copyOf(friendly);
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Boolean own = summons.get(entity.getId());
        if (own == null || entity.isInvisible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // above where a name tag goes, so both show
        pose.translate(0.0, entity.getBbHeight() + (entity.hasCustomName() ? 0.85 : 0.5), 0.0);
        pose.mulPose(camera.rotation());
        pose.scale(-SCALE, -SCALE, SCALE);
        Matrix4f matrix = pose.last().pose();
        Font font = mc.font;
        float x = -font.width(MARKER) / 2.0F;
        int colour = own ? OWN : ALLY;
        font.drawInBatch(MARKER, x, 0.0F, (colour & 0x00FFFFFF) | 0x40000000, false, matrix, event.getMultiBufferSource(),
            DisplayMode.SEE_THROUGH, 0, event.getPackedLight());
        font.drawInBatch(MARKER, x, 0.0F, colour, false, matrix, event.getMultiBufferSource(), DisplayMode.NORMAL, 0, event.getPackedLight());
        pose.popPose();
    }
}
