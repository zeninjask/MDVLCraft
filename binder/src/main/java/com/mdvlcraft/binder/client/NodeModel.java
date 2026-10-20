package com.mdvlcraft.binder.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * A Blockbench glTF export converted for the binder (tools/seraph/convert.py, tools/haunted_lord/convert.py): every node
 * with its rest translation, rotation and scale and its cubes as triangles (glTF units, 1 = 1 block), and the animations
 * as per-node linear channels. Used by SeraphRenderer and GauntRenderer.
 */
final class NodeModel {
    record Node(String name, int parent, float[] rest, float[] tris, int[] children) {
    }

    /** One animated property of one node: path 0 translation, 1 rotation (quaternion), 2 scale. */
    record Channel(int node, int path, float[] times, float[] values) {
    }

    record Animation(float length, List<Channel> channels) {
    }

    final JsonObject json;
    final ResourceLocation texture;
    /** Drawn a second time, full bright, with the eyes render type (the glowing parts); null for none. */
    @Nullable
    final ResourceLocation glow;
    final List<Node> nodes;
    final List<Integer> roots;
    final Map<String, Animation> animations;

    private NodeModel(JsonObject json, List<Node> nodes, List<Integer> roots, Map<String, Animation> animations) {
        this.json = json;
        this.texture = new ResourceLocation(json.get("texture").getAsString());
        this.glow = json.has("glow") ? new ResourceLocation(json.get("glow").getAsString()) : null;
        this.nodes = nodes;
        this.roots = roots;
        this.animations = animations;
    }

    static NodeModel load(ResourceLocation location) {
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + location, e);
        }
    }

    static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static NodeModel parse(JsonObject json) {
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
            parsed.add(new Node(node.get("name").getAsString(), node.get("parent").getAsInt(), rest, floats(node.getAsJsonArray("tris")),
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
        return new NodeModel(json, parsed, roots, animations);
    }

    float length(String animation) {
        Animation found = this.animations.get(animation);
        if (found == null) {
            throw new IllegalStateException("No animation " + animation);
        }
        return found.length;
    }

    int node(String name) {
        for (int i = 0; i < this.nodes.size(); i++) {
            if (this.nodes.get(i).name.equals(name)) {
                return i;
            }
        }
        throw new IllegalStateException("No node " + name);
    }

    /** Every node's local translation, rotation and scale under an animation at a time (its rest values where it has no channel). */
    float[][] pose(String name, float time) {
        float[][] pose = new float[this.nodes.size()][];
        for (int i = 0; i < pose.length; i++) {
            pose[i] = this.nodes.get(i).rest.clone();
        }
        Animation animation = this.animations.get(name);
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
    static void mix(float[] from, float[] to, float weight) {
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

    static void normalise(float[] trs) {
        float length = Mth.sqrt(trs[3] * trs[3] + trs[4] * trs[4] + trs[5] * trs[5] + trs[6] * trs[6]);
        if (length > 1.0E-6F) {
            for (int c = 3; c < 7; c++) {
                trs[c] /= length;
            }
        }
    }

    /** What one entity's model showed last frame, and what it is blending from. */
    static final class Blend {
        private final float ticks;
        String animation = "";
        float time;
        String from = "";
        float fromTime;
        float since;

        Blend(float ticks) {
            this.ticks = ticks;
            this.since = -ticks;
        }

        /** Shows `animation` at `time` this frame (game time `now`), blending over the set ticks when it changes. */
        float[][] pose(NodeModel model, String animation, float time, float now) {
            if (!animation.equals(this.animation) && !this.animation.isEmpty()) {
                this.from = this.animation;
                this.fromTime = this.time;
                this.since = now;
            }
            this.animation = animation;
            this.time = time;
            float[][] pose = model.pose(animation, time);
            float weight = Mth.clamp((now - this.since) / this.ticks, 0.0F, 1.0F);
            if (weight < 1.0F) {
                float[][] from = model.pose(this.from, this.fromTime);
                for (int i = 0; i < pose.length; i++) {
                    mix(from[i], pose[i], weight);
                    pose[i] = from[i];
                }
            }
            return pose;
        }
    }

    void draw(float[][] pose, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        for (int root : this.roots) {
            this.draw(root, pose, poseStack, consumer, light, overlay);
        }
    }

    private void draw(int index, float[][] pose, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        Node node = this.nodes.get(index);
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
            this.draw(child, pose, poseStack, consumer, light, overlay);
        }
        poseStack.popPose();
    }
}
