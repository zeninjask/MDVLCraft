package com.mdvlcraft.binder.client;

import app.sedu.mc.parties._lytecore.client.render.MatrixCache;
import app.sedu.mc.parties.client.render.PartyTrackerRenderer;
import app.sedu.mc.parties.client.render.TrackerOverlay;
import com.mdvlcraft.binder.content.PartyPositions;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Vector4f;

/**
 * While the player list key is held, Parties' tracker square (head, direction arrow and distance) is drawn for every
 * party member in the dimension, however far: Parties only draws it for members loaded on this client, so the ones
 * further away are drawn here from the positions the server sends (content/PartyPositions), with a stand-in player at
 * that spot. On screen the square sits where they are; off screen Parties' own edge arrow points to them.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class PartyFarTrackers {
    private static final Map<UUID, RemotePlayer> STAND_INS = new HashMap<>();
    private static Method renderElements;
    private static boolean looked;

    private PartyFarTrackers() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null || !mc.options.keyPlayerList.isDown()
            || TrackerOverlay.hideOverlay() || mc.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float partialTick = event.getPartialTick();
        STAND_INS.keySet().retainAll(PartyPositions.client().keySet());
        PartyPositions.client().forEach((id, pos) -> {
            if (mc.level.getPlayerByUUID(id) != null) {
                return;   // loaded: Parties draws it already
            }
            PlayerInfo info = mc.getConnection().getPlayerInfo(id);
            if (info == null) {
                return;
            }
            RemotePlayer standIn = STAND_INS.get(id);
            if (standIn == null || standIn.level() != mc.level) {
                standIn = new RemotePlayer(mc.level, new GameProfile(id, info.getProfile().getName()));
                STAND_INS.put(id, standIn);
            }
            standIn.setPos(pos.x, pos.y, pos.z);
            standIn.xo = pos.x;
            standIn.yo = pos.y;
            standIn.zo = pos.z;
            draw(graphics, mc, partialTick, width, height, standIn);
        });
    }

    /** Off screen: Parties' edge arrow. On screen: the same square, just above where they stand. */
    private static void draw(GuiGraphics graphics, Minecraft mc, float partialTick, int width, int height, Player target) {
        PartyTrackerRenderer.renderTracker(graphics, mc, partialTick, width, height, target);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Vector4f point = new Vector4f((float)(target.getX() - camera.x), (float)(target.getY() + target.getBbHeight() + 0.5 - camera.y),
            (float)(target.getZ() - camera.z), 1.0F);
        point.mul(MatrixCache.getViewMatrix());
        point.mul(MatrixCache.getProjectionMatrix());
        if (point.w <= 0.0F) {
            return;
        }
        float x = (point.x / point.w * 0.5F + 0.5F) * width;
        float y = (1.0F - (point.y / point.w * 0.5F + 0.5F)) * height;
        int padding = 24;
        if (x < padding || x > width - padding || y < padding || y > height - padding) {
            return;   // renderTracker drew the edge arrow
        }
        Method method = method();
        if (method != null) {
            try {
                int distance = (int)Math.sqrt(mc.player.distanceToSqr(target));
                method.invoke(null, graphics, mc, target, x, y - 12.0F, 8, 8, 180.0F, 1.0F, distance);
            } catch (ReflectiveOperationException ignored) {
                renderElements = null;
            }
        }
    }

    private static Method method() {
        if (!looked) {
            looked = true;
            try {
                renderElements = PartyTrackerRenderer.class.getDeclaredMethod("renderElements", GuiGraphics.class, Minecraft.class, Player.class,
                    float.class, float.class, int.class, int.class, float.class, float.class, int.class);
                renderElements.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                renderElements = null;
            }
        }
        return renderElements;
    }
}
