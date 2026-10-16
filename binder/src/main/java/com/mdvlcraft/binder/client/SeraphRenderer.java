package com.mdvlcraft.binder.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.seraph.SeraphForm;
import com.mdvlcraft.binder.seraph.SeraphForm.Seen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Draws a player in Seraph Embodiment as the Seraphim (assets/mdvlcraft/seraph/seraph.json, from tools/seraph/convert.py):
 * the glTF nodes posed by their own keyframes, at SeraphForm.SCALE, with the eye centred on the eye-sized hitbox.
 * Entering plays spawn, leaving plays it backwards; in the form a spell plays pose (and keeps it while casting), moving
 * plays fly and keeping still plays idle, each change blended over BLEND_TICKS.
 */
final class SeraphRenderer {
    private static final ResourceLocation MODEL = new ResourceLocation("mdvlcraft", "seraph/seraph.json");
    private static final float SPAWN_SPEED = 4.5F * 20.0F / SeraphForm.SPAWN_TICKS;
    /** A spell's pose (4 s) plays twice as fast. */
    private static final float POSE_SPEED = 2.0F;
    private static final float BLEND_TICKS = 5.0F;
    private static Model model;
    private static final Map<Integer, Blend> BLENDS = new HashMap<>();

    private SeraphRenderer() {
    }

    private record Node(int parent, float[] rest, float[] tris, int[] children) {
    }

    /** One animated property of one node: path 0 translation, 1 rotation (quaternion), 2 scale. */
    private record Channel(int node, int path, float[] times, float[] values) {
    }

    private record Animation(float length, List<Channel> channels) {
    }

    private record Model(ResourceLocation texture, float[] eye, List<Node> nodes, List<Integer> roots, Map<String, Animation> animations) {
    }

    /** What a player's seraph showed last frame, and what it is blending from. */
    private static final class Blend {
        String animation = "";
        float time;
        String from = "";
        float fromTime;
        float since = -BLEND_TICKS;
    }

    private static Model model() {
        if (model == null) {
            try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(MODEL)) {
                model = parse(JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + MODEL, e);
            }
        }
        return model;
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static Model parse(JsonObject json) {
        JsonArray nodes = json.getAsJsonArray("nodes");
        List<List<Integer>> children = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            children.add(new ArrayList<>());
        }
        List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            int parent = nodes.get(i).getAsJsonObject().get("parent").getAsInt();
            (parent < 0 ? roots : children.get(parent)).add(i);
        }
        List<Node> parsed = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            JsonObject node = nodes.get(i).getAsJsonObject();
            float[] rest = new float[10];
            System.arraycopy(floats(node.getAsJsonArray("t")), 0, rest, 0, 3);
            System.arraycopy(floats(node.getAsJsonArray("r")), 0, rest, 3, 4);
            System.arraycopy(floats(node.getAsJsonArray("s")), 0, rest, 7, 3);
            parsed.add(new Node(node.get("parent").getAsInt(), rest, floats(node.getAsJsonArray("tris")),
                children.get(i).stream().mapToInt(Integer::intValue).toArray()));
        }
        Map<String, Animation> animations = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("animations").entrySet()) {
            JsonObject animation = entry.getValue().getAsJsonObject();
            List<Channel> channels = new ArrayList<>();
            for (JsonElement element : animation.getAsJsonArray("channels")) {
                JsonObject channel = element.getAsJsonObject();
                int path = switch (channel.get("path").getAsString()) {
                    case "t" -> 0;
                    case "r" -> 1;
                    case "s" -> 2;
                    default -> throw new IllegalStateException("Unknown channel path " + channel.get("path"));
                };
                channels.add(new Channel(channel.get("node").getAsInt(), path, floats(channel.getAsJsonArray("times")),
                    floats(channel.getAsJsonArray("values"))));
            }
            animations.put(entry.getKey(), new Animation(animation.get("length").getAsFloat(), channels));
        }
        return new Model(new ResourceLocation(json.get("texture").getAsString()), floats(json.getAsJsonArray("eye_center")), parsed, roots,
            animations);
    }

    static void render(Player player, Seen seen, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Model model = model();
        float now = player.level().getGameTime() + partialTick;
        Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new Blend());
        choose(model, player, seen, now, blend);
        float[][] pose = pose(model, blend.animation, blend.time);
        float weight = Mth.clamp((now - blend.since) / BLEND_TICKS, 0.0F, 1.0F);
        if (weight < 1.0F) {
            float[][] from = pose(model, blend.from, blend.fromTime);
            for (int i = 0; i < pose.length; i++) {
                mix(from[i], pose[i], weight);
                pose[i] = from[i];
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, SeraphForm.EYE / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot)));
        poseStack.scale(SeraphForm.SCALE, SeraphForm.SCALE, SeraphForm.SCALE);
        poseStack.translate(-model.eye[0], -model.eye[1], -model.eye[2]);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        for (int root : model.roots) {
            draw(model, root, pose, poseStack, consumer, light, overlay);
        }
        poseStack.popPose();
    }

    /** Picks the animation and its time (seconds) for this frame, starting a blend when the animation changes. */
    private static void choose(Model model, Player player, Seen seen, float now, Blend blend) {
        float age = (now - seen.since) / 20.0F;
        float spawn = model.animations.get("spawn").length;
        String animation;
        float time;
        switch (seen.phase) {
            case ENTERING -> {
                animation = "spawn";
                time = Math.min(age * SPAWN_SPEED, spawn);
            }
            case EXITING -> {
                animation = "spawn";
                time = Math.max(spawn - age * SPAWN_SPEED, 0.0F);
            }
            default -> {
                float pose = model.animations.get("pose").length / POSE_SPEED;
                if (ClientMagicData.getSyncedSpellData(player).isCasting() && (now - seen.castAt) / 20.0F >= pose) {
                    seen.castAt = (long)now;
                }
                float sinceCast = (now - seen.castAt) / 20.0F;
                double dx = player.getX() - player.xo;
                double dy = player.getY() - player.yo;
                double dz = player.getZ() - player.zo;
                if (sinceCast < pose) {
                    animation = "pose";
                    time = sinceCast * POSE_SPEED;
                } else if (dx * dx + dy * dy + dz * dz > 4.0E-4) {
                    animation = "fly";
                    time = now / 20.0F % model.animations.get("fly").length;
                } else {
                    animation = "idle";
                    time = now / 20.0F % model.animations.get("idle").length;
                }
            }
        }
        if (!animation.equals(blend.animation) && !blend.animation.isEmpty()) {
            blend.from = blend.animation;
            blend.fromTime = blend.time;
            blend.since = now;
        }
        blend.animation = animation;
        blend.time = time;
    }

    /** Every node's local translation, rotation and scale under an animation at a time (its rest values where it has no channel). */
    private static float[][] pose(Model model, String name, float time) {
        float[][] pose = new float[model.nodes.size()][];
        for (int i = 0; i < pose.length; i++) {
            pose[i] = model.nodes.get(i).rest.clone();
        }
        Animation animation = model.animations.get(name);
        if (animation == null) {
            return pose;
        }
        for (Channel channel : animation.channels) {
            int size = channel.path == 1 ? 4 : 3;
            int offset = channel.path == 0 ? 0 : channel.path == 1 ? 3 : 7;
            float[] times = channel.times;
            int last = times.length - 1;
            int k = 0;
            while (k < last && times[k + 1] <= time) {
                k++;
            }
            int next = Math.min(k + 1, last);
            float t = next == k || time <= times[k] ? 0.0F : Mth.clamp((time - times[k]) / (times[next] - times[k]), 0.0F, 1.0F);
            float[] out = pose[channel.node];
            float sign = 1.0F;
            if (size == 4) {
                float dot = 0.0F;
                for (int c = 0; c < 4; c++) {
                    dot += channel.values[k * 4 + c] * channel.values[next * 4 + c];
                }
                sign = dot < 0.0F ? -1.0F : 1.0F;
            }
            for (int c = 0; c < size; c++) {
                float a = channel.values[k * size + c];
                float b = channel.values[next * size + c] * sign;
                out[offset + c] = a + (b - a) * t;
            }
            if (size == 4) {
                normalise(out);
            }
        }
        return pose;
    }

    /** Blends `from` towards `to` by `weight`, in place in `from`. */
    private static void mix(float[] from, float[] to, float weight) {
        float dot = 0.0F;
        for (int c = 3; c < 7; c++) {
            dot += from[c] * to[c];
        }
        float sign = dot < 0.0F ? -1.0F : 1.0F;
        for (int c = 0; c < 10; c++) {
            float target = c >= 3 && c < 7 ? to[c] * sign : to[c];
            from[c] += (target - from[c]) * weight;
        }
        normalise(from);
    }

    private static void normalise(float[] trs) {
        float length = Mth.sqrt(trs[3] * trs[3] + trs[4] * trs[4] + trs[5] * trs[5] + trs[6] * trs[6]);
        if (length > 1.0E-6F) {
            for (int c = 3; c < 7; c++) {
                trs[c] /= length;
            }
        }
    }

    private static void draw(Model model, int index, float[][] pose, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        Node node = model.nodes.get(index);
        float[] trs = pose[index];
        poseStack.pushPose();
        poseStack.translate(trs[0], trs[1], trs[2]);
        poseStack.mulPose(new Quaternionf(trs[3], trs[4], trs[5], trs[6]));
        poseStack.scale(trs[7], trs[8], trs[9]);
        float[] tris = node.tris;
        if (tris.length > 0) {
            Matrix4f matrix = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();
            for (int v = 0; v < tris.length; v += 24) {
                // Entity render types draw quads: each triangle is a quad with its last corner doubled.
                for (int corner : new int[] {0, 1, 2, 2}) {
                    int o = v + corner * 8;
                    consumer.vertex(matrix, tris[o], tris[o + 1], tris[o + 2]).color(1.0F, 1.0F, 1.0F, 1.0F).uv(tris[o + 3], tris[o + 4])
                        .overlayCoords(overlay).uv2(light).normal(normal, tris[o + 5], tris[o + 6], tris[o + 7]).endVertex();
                }
            }
        }
        for (int child : node.children) {
            draw(model, child, pose, poseStack, consumer, light, overlay);
        }
        poseStack.popPose();
    }
}
