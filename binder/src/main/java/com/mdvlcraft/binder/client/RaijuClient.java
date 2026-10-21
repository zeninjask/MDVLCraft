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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Yakusanoikazuchi on the client (raiju/RaijuForm): players in the form wear its skin (mixin/raiju/RaijuSkinMixin, slim arms)
 * and six darkened Haran Geppaku Futsu blades (models/item/raiju_sword, tools/raiju/convert_sword.py) circle them, tips
 * down, flats facing out, now and then throwing off a small spark of lightning. The local player's wheel is the form's own
 * (ClientAbilities: Transform Back only), the old pick back on leaving.
 */
@EventBusSubscriber(modid = "mdvlcraft", value = Dist.CLIENT)
public final class RaijuClient {
    public static final ResourceLocation SKIN = new ResourceLocation("mdvlcraft", "textures/entity/yakusanoikazuchi.png");
    public static final ResourceLocation SWORD_MODEL = new ResourceLocation("mdvlcraft", "item/raiju_sword");
    private static final int SWORDS = 6;
    private static final double RADIUS = 1.15;
    /** How dark the blades are drawn (1 = as textured). */
    private static final float DARK = 0.6F;
    /** The model is 31 px (about 2 blocks) long: drawn at this, it is as long as the Vatansever blades were (about 1.07). */
    private static final float SCALE = 0.55F;
    /** Each sword sparks about once in this many ticks. */
    private static final int SPARK_CHANCE = 50;
    private static final ItemStack STACK = new ItemStack(Items.IRON_SWORD);
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(4096));
    private static final RandomSource RANDOM = RandomSource.create();
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

    /** Where sword `i` of a player in the form circles, `time` ticks in, from the player's feet. */
    private static Vec3 orbit(int i, float time) {
        double angle = Math.toRadians(time * 2.5F + i * (360.0F / SWORDS));
        double bob = 0.12 * Math.sin(time * 0.09 + i * 1.3);
        return new Vec3(RADIUS * Math.cos(angle), 1.15 + bob, RADIUS * Math.sin(angle));
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
                Vec3 sword = at.add(orbit(i, time));
                pose.pushPose();
                pose.translate(sword.x, sword.y, sword.z);
                // the model's blade runs up +y, its flats face ±x: turned so the flats face out from the player, tip down
                pose.mulPose(Axis.YP.rotationDegrees(-(time * 2.5F + i * (360.0F / SWORDS))));
                pose.mulPose(Axis.XP.rotationDegrees(180.0F + 6.0F * Mth.sin(time * 0.07F + i)));
                pose.scale(SCALE, SCALE, SCALE);
                minecraft.getItemRenderer().render(STACK, ItemDisplayContext.NONE, false, pose, tinted(BUFFERS, DARK, DARK, DARK), light,
                    OverlayTexture.NO_OVERLAY, model);
                pose.popPose();
            }
        }
        BUFFERS.endBatch();
    }

    /** Now and then a small spark of lightning off one of the swords. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (event.phase != TickEvent.Phase.END || level == null || minecraft.isPaused() || RaijuForm.CLIENT.isEmpty()) {
            return;
        }
        for (int id : RaijuForm.CLIENT) {
            if (!(level.getEntity(id) instanceof Player player) || player.isInvisibleTo(minecraft.player)) {
                continue;
            }
            for (int i = 0; i < SWORDS; i++) {
                if (RANDOM.nextInt(SPARK_CHANCE) == 0) {
                    Vec3 at = player.position().add(orbit(i, player.tickCount)).add(0.0, (RANDOM.nextFloat() - 0.5F) * 0.9F, 0.0);
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, (RANDOM.nextFloat() - 0.5F) * 0.2F,
                        (RANDOM.nextFloat() - 0.5F) * 0.2F, (RANDOM.nextFloat() - 0.5F) * 0.2F);
                }
            }
        }
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

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RaijuForm.CLIENT.clear();
        pickedBefore = -1;
    }
}
