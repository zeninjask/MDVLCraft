package com.mdvlcraft.binder.gate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mdvlcraft.binder.gate.GateInstances.Instance;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.slf4j.Logger;

/**
 * Theme blocks and the gate's own look. The shared rooms (structures/gate/shared, tools/gates/gen_gate_variety.py) are
 * drawn in placeholder blocks, one per palette role (data/mdvlcraft/gate_shared_roles.json); the processor swaps each
 * for the theme's block (the theme file's "palette"). "crack:" roles are blocks players may break (GateBuilder records
 * where they land).
 *
 * Every gate also gets a style from its id (the same on a rebuild): a mood (ruined, flooded, overgrown, dark, haunted or
 * none), a lighting set and a decor set. The processor applies them to every room's blocks as it is placed, and
 * decorate() adds what a template can't hold (vines, puddles, rubble) once a room is down.
 */
public final class GatePalettes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Block, String> SENTINELS = new HashMap<>();
    private static final Map<GateTheme, Map<String, String>> PALETTES = new EnumMap<>(GateTheme.class);
    private static final Map<String, BlockState> PARSED = new HashMap<>();
    private static boolean loaded;

    public enum Mood { NONE, RUINED, FLOODED, OVERGROWN, DARK, HAUNTED }

    /** Lighting sets: the wall light block and the hanging light (null: the theme's own). */
    private static final String[][] LIGHTING = {
        {null, null},
        {"pearlescent_froglight", "soul_lantern[hanging=true]"},
        {"ochre_froglight", "lantern[hanging=true]"},
        {"sea_lantern", "end_rod[facing=down]"},
        {"glowstone", "lantern[hanging=true]"},
        {"shroomlight", "soul_lantern[hanging=true]"}};
    /** Decor sets: prop, litter and web (null: the theme's own). */
    private static final String[][] DECOR = {
        {null, null, null},
        {"bone_block[axis=y]", "skeleton_skull", "cobweb"},
        {"barrel[facing=up]", "candle[candles=2,lit=true]", null},
        {"bookshelf", "candle[candles=3,lit=true]", "cobweb"},
        {"chain[axis=y]", "lantern", null},
        {"hay_block[axis=y]", "brown_mushroom", "cobweb"}};

    /** A gate's look, drawn from its id. */
    public record Style(Mood mood, int lighting, int decor) {
    }

    private GatePalettes() {
    }

    public static Style style(Instance instance) {
        Random random = new Random(instance.id * 7919L + 13);
        Mood mood = random.nextInt(100) < 35 ? Mood.NONE : Mood.values()[1 + random.nextInt(Mood.values().length - 1)];
        return new Style(mood, random.nextInt(100) < 40 ? 0 : 1 + random.nextInt(LIGHTING.length - 1),
            random.nextInt(100) < 40 ? 0 : 1 + random.nextInt(DECOR.length - 1));
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        JsonObject roles = json("/data/mdvlcraft/gate_shared_roles.json");
        if (roles != null) {
            for (Map.Entry<String, JsonElement> e : roles.entrySet()) {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(e.getKey()));
                SENTINELS.put(block, e.getValue().getAsString());
            }
        }
        for (GateTheme theme : GateTheme.values()) {
            JsonObject file = json("/data/mdvlcraft/gate_themes/" + theme.key() + ".json");
            Map<String, String> palette = new HashMap<>();
            if (file != null && file.has("palette")) {
                for (Map.Entry<String, JsonElement> e : file.getAsJsonObject("palette").entrySet()) {
                    palette.put(e.getKey(), e.getValue().isJsonNull() ? null : e.getValue().getAsString());
                }
            }
            PALETTES.put(theme, palette);
        }
    }

    @Nullable
    private static JsonObject json(String path) {
        try (InputStream in = GatePalettes.class.getResourceAsStream(path)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Gate palettes: can't read {}", path, e);
            return null;
        }
    }

    /** The placeholder blocks whose role is breakable ("crack:..."). */
    static List<Block> breakableSentinels() {
        load();
        List<Block> out = new ArrayList<>();
        SENTINELS.forEach((block, role) -> {
            if (role.startsWith("crack:")) {
                out.add(block);
            }
        });
        return out;
    }

    /** A role string ("wall", "steps[facing=north]", "crack:bookshelf", "lantern") as a block state of the theme. */
    static BlockState resolve(GateTheme theme, String role) {
        load();
        if (role.startsWith("crack:")) {
            role = role.substring(6);
        }
        Map<String, String> palette = PALETTES.getOrDefault(theme, Map.of());
        String base = role.contains("[") ? role.substring(0, role.indexOf('[')) : role;
        String text;
        if (palette.containsKey(base)) {
            String block = palette.get(base);
            if (block == null) {
                return Blocks.AIR.defaultBlockState();
            }
            text = role.contains("[") ? block.split("\\[")[0] + role.substring(role.indexOf('[')) : block;
        } else {
            text = role;
        }
        return parse(text);
    }

    private static BlockState parse(String text) {
        return PARSED.computeIfAbsent(text, t -> {
            try {
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), t.contains(":") ? t : "minecraft:" + t, false).blockState();
            } catch (Exception e) {
                LOGGER.warn("Gate palettes: bad block {}", t);
                return Blocks.STONE.defaultBlockState();
            }
        });
    }

    public static StructureProcessor processor(Instance instance) {
        load();
        return new Processor(instance.theme, style(instance));
    }

    private static final class Processor extends StructureProcessor {
        private final GateTheme theme;
        private final Style style;
        private final Map<BlockState, String> roles = new HashMap<>();

        Processor(GateTheme theme, Style style) {
            this.theme = theme;
            this.style = style;
            for (String role : List.of("glow", "hang", "prop", "litter", "web", "wall", "wall2", "floor", "floor3")) {
                String block = PALETTES.getOrDefault(theme, Map.of()).get(role);
                if (block != null) {
                    this.roles.putIfAbsent(parse(block), role);
                }
            }
        }

        @Nullable
        @Override
        public StructureBlockInfo processBlock(LevelReader level, BlockPos pos, BlockPos pivot, StructureBlockInfo original,
                                               StructureBlockInfo current, StructurePlaceSettings settings) {
            BlockState state = current.state();
            String role = SENTINELS.get(state.getBlock());
            if (role != null) {
                String base = role.startsWith("crack:") ? role.substring(6) : role;
                state = resolve(this.theme, role);
                role = base.contains("[") ? base.substring(0, base.indexOf('[')) : base;
            } else {
                role = this.roles.get(state);
            }
            if (role != null) {
                state = this.restyle(role, state, settings.getRandom(current.pos()));
            }
            return state == current.state() ? current : new StructureBlockInfo(current.pos(), state, current.nbt());
        }

        private BlockState restyle(String role, BlockState state, RandomSource random) {
            String[] light = LIGHTING[this.style.lighting()];
            String[] decor = DECOR[this.style.decor()];
            switch (role) {
                case "glow" -> {
                    if (this.style.mood() == Mood.DARK && random.nextFloat() < 0.6F) {
                        return resolve(this.theme, "wall");
                    }
                    if (this.style.mood() == Mood.RUINED && random.nextFloat() < 0.15F) {
                        return resolve(this.theme, "wall2");
                    }
                    return light[0] != null ? parse(light[0]) : state;
                }
                case "hang" -> {
                    if (this.style.mood() == Mood.DARK && random.nextFloat() < 0.5F) {
                        return Blocks.AIR.defaultBlockState();
                    }
                    return light[1] != null ? parse(light[1]) : state;
                }
                case "prop" -> {
                    return decor[0] != null ? parse(decor[0]) : state;
                }
                case "litter" -> {
                    return decor[1] != null ? parse(decor[1]) : state;
                }
                case "web" -> {
                    return decor[2] != null ? parse(decor[2]) : state;
                }
                case "wall" -> {
                    if (this.style.mood() == Mood.RUINED && random.nextFloat() < 0.2F
                        || this.style.mood() == Mood.OVERGROWN && random.nextFloat() < 0.08F) {
                        return this.style.mood() == Mood.OVERGROWN ? Blocks.MOSS_BLOCK.defaultBlockState() : resolve(this.theme, "wall2");
                    }
                    return state;
                }
                case "floor", "floor3" -> {
                    if (this.style.mood() == Mood.RUINED && random.nextFloat() < 0.25F) {
                        return resolve(this.theme, "floor3");
                    }
                    if (this.style.mood() == Mood.OVERGROWN && random.nextFloat() < 0.2F) {
                        return Blocks.MOSS_BLOCK.defaultBlockState();
                    }
                    return state;
                }
                default -> {
                    return state;
                }
            }
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.NOP;   // never saved: it only runs as rooms are placed
        }
    }

    /** What a mood adds to a placed room that its template can't hold: vines and moss carpet, puddles, rubble and webs. */
    static void decorate(ServerLevel level, Instance instance, BoundingBox box) {
        Mood mood = style(instance).mood();
        if (mood == Mood.NONE || mood == Mood.DARK) {
            return;
        }
        RandomSource random = RandomSource.create(instance.id * 31L + box.minX() * 7L + box.minZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int tries = (box.getXSpan() * box.getZSpan()) / 6;
        for (int i = 0; i < tries; i++) {
            pos.set(box.minX() + random.nextInt(box.getXSpan()), box.minY() + 2 + random.nextInt(Math.max(1, box.getYSpan() - 3)),
                box.minZ() + random.nextInt(box.getZSpan()));
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            BlockState below = level.getBlockState(pos.below());
            switch (mood) {
                case OVERGROWN -> {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (level.getBlockState(pos.relative(d)).isSolidRender(level, pos.relative(d))) {
                            level.setBlock(pos, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(d), true), Block.UPDATE_CLIENTS);
                            break;
                        }
                    }
                    if (level.getBlockState(pos).isAir() && below.isSolidRender(level, pos.below()) && random.nextFloat() < 0.4F) {
                        level.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                case FLOODED -> {
                    if (below.isSolidRender(level, pos.below()) && pos.getY() == box.minY() + 2 && random.nextFloat() < 0.5F) {
                        level.setBlock(pos.below(), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                case RUINED -> {
                    if (below.isSolidRender(level, pos.below()) && random.nextFloat() < 0.3F) {
                        level.setBlock(pos, (random.nextBoolean() ? Blocks.COBBLESTONE : Blocks.GRAVEL).defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else if (level.getBlockState(pos.above()).isSolidRender(level, pos.above()) && random.nextFloat() < 0.2F) {
                        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                case HAUNTED -> {
                    if (below.isSolidRender(level, pos.below()) && random.nextFloat() < 0.08F) {
                        level.setBlock(pos, Blocks.SOUL_FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
                        level.setBlock(pos.below(), Blocks.SOUL_SOIL.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                default -> {
                }
            }
        }
    }
}
