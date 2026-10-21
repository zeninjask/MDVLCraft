package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.network.RaijuFormPacket;
import com.mdvlcraft.binder.raiju.RaijuForm;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Yakusanoikazuchi on the client (raiju/RaijuForm): players in the form wear its skin (mixin/raiju/RaijuSkinMixin, slim arms)
 * and six darkened Vatansever blades (models/item/raiju_sword, cut from Sword Soaring's Vatansever) circle them, tips
 * down, each wrapped in its own charged energy swirl (the charged creeper's). The local player's wheel is the form's own
 * (ClientAbilities: Transform Back only), the old pick back on leaving.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class RaijuClient {
    public static final ResourceLocation SKIN = new ResourceLocation("mdvlcraft", "textures/entity/yakusanoikazuchi.png");
    public static final ResourceLocation SWORD_MODEL = new ResourceLocation("mdvlcraft", "item/raiju_sword");
    private static final ResourceLocation SWIRL = new ResourceLocation("minecraft", "textures/entity/creeper/creeper_armor.png");
    private static final int SWORDS = 6;
    private static final double RADIUS = 1.15;
    /** How dark the blades are drawn (1 = as textured). */
    private static final float DARK = 0.3F;
    private static final ItemStack STACK = new ItemStack(Items.IRON_SWORD);
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(4096));
    private static int pickedBefore = -1;

    private RaijuClient() {
    }

    public static void handle(RaijuFormPacket packet) {
        LocalPlayer self = Minecraft.getInstance().player;
        boolean mine = self != null && self.getId() == packet.entity();
        if (packet.on()) {
            RaijuForm.CLIENT.add(packet.entity());
            if (mine && pickedBefore < 0) {
                pickedBefore = ClientAbilities.selected();
                ClientAbilities.select(0);
            }
        } else {
            RaijuForm.CLIENT.remove(packet.entity());
            if (mine && pickedBefore >= 0) {
                ClientAbilities.select(pickedBefore);
                pickedBefore = -1;
            }
        }
    }

    /** Whether the player a tab-list entry belongs to is in the form (mixin/raiju/RaijuSkinMixin). */
    public static boolean inForm(PlayerInfo info) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || RaijuForm.CLIENT.isEmpty()) {
            return false;
        }
        Player player = level.getPlayerByUUID(info.getProfile().getId());
        return player != null && RaijuForm.CLIENT.contains(player.getId());
    }

    public static boolean local() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && RaijuForm.CLIENT.contains(player.getId());
    }

    /** Registered on the mod bus (client/ModifiedClientSetup). */
    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(SWORD_MODEL);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || level == null || RaijuForm.CLIENT.isEmpty()) {
            return;
        }
        RaijuForm.CLIENT.removeIf(id -> !(level.getEntity(id) instanceof Player));
        BakedModel model = minecraft.getModelManager().getModel(SWORD_MODEL);
        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        for (int id : RaijuForm.CLIENT) {
            if (!(level.getEntity(id) instanceof Player player) || player.isInvisibleTo(minecraft.player)
                || (player == minecraft.player && camera.getEntity() == player && !camera.isDetached())) {
                continue;
            }
            Vec3 at = player.getPosition(partialTick).subtract(camera.getPosition());
            float time = player.tickCount + partialTick;
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(player.getEyePosition(partialTick)));
            for (int i = 0; i < SWORDS; i++) {
                float angle = time * 2.5F + i * (360.0F / SWORDS);
                double bob = 0.12 * Math.sin(time * 0.09 + i * 1.3);
                pose.pushPose();
                pose.translate(at.x, at.y + 1.15 + bob, at.z);
                pose.mulPose(Axis.YP.rotationDegrees(-angle));
                pose.translate(RADIUS, 0.0, 0.0);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.sin(time * 0.07F + i)));
                pose.scale(1.1F, 1.1F, 1.1F);
                minecraft.getItemRenderer().render(STACK, ItemDisplayContext.NONE, false, pose, tinted(BUFFERS, DARK, DARK, DARK), light,
                    OverlayTexture.NO_OVERLAY, model);
                // the charge: the same blade a little larger, drawn as a moving energy swirl
                float scroll = time * 0.01F % 1.0F;
                VertexConsumer swirl = new Swirl(BUFFERS.getBuffer(RenderType.energySwirl(SWIRL, scroll, scroll)), model.getParticleIcon());
                pose.scale(1.12F, 1.06F, 2.2F);
                minecraft.getItemRenderer().render(STACK, ItemDisplayContext.NONE, false, pose, type -> new Tinted(swirl, 0.45F, 0.65F, 1.0F),
                    0xF000F0, OverlayTexture.NO_OVERLAY, model);
                pose.popPose();
            }
        }
        BUFFERS.endBatch();
    }

    private static MultiBufferSource tinted(MultiBufferSource buffers, float r, float g, float b) {
        return type -> new Tinted(buffers.getBuffer(type), r, g, b);
    }

    /** Scales the colour of everything drawn through it. */
    private record Tinted(VertexConsumer delegate, float r, float g, float b) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.delegate.color((int)(red * this.r), (int)(green * this.g), (int)(blue * this.b), alpha);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            this.delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            this.delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            this.delegate.endVertex();
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light,
                           float nx, float ny, float nz) {
            this.delegate.vertex(x, y, z, red * this.r, green * this.g, blue * this.b, alpha, u, v, overlay, light, nx, ny, nz);
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            this.delegate.defaultColor((int)(red * this.r), (int)(green * this.g), (int)(blue * this.b), alpha);
        }

        @Override
        public void unsetDefaultColor() {
            this.delegate.unsetDefaultColor();
        }
    }

    /** Maps the blade's place on the item atlas back to the whole swirl texture (twice over), so the swirl shows. */
    private record Swirl(VertexConsumer delegate, TextureAtlasSprite sprite) implements VertexConsumer {
        private float u(float u) {
            return (u - this.sprite.getU0()) / (this.sprite.getU1() - this.sprite.getU0()) * 2.0F;
        }

        private float v(float v) {
            return (v - this.sprite.getV0()) / (this.sprite.getV1() - this.sprite.getV0()) * 2.0F;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.delegate.uv(this.u(u), this.v(v));
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            this.delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            this.delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            this.delegate.endVertex();
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light,
                           float nx, float ny, float nz) {
            this.delegate.vertex(x, y, z, red, green, blue, alpha, this.u(u), this.v(v), overlay, light, nx, ny, nz);
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            this.delegate.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            this.delegate.unsetDefaultColor();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RaijuForm.CLIENT.clear();
        pickedBefore = -1;
    }
}
