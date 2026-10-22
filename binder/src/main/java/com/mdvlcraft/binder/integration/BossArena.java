package com.mdvlcraft.binder.integration;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Boss arenas for the quest bosses whose lairs the pack doesn't generate (tools/generate_quests.py). A Boss Sigil (NBT
 * Boss = entity id, BossName = its name) takes its user to the arena dimension (data/mdvlcraft/dimension/boss_arena: a
 * superflat world of three bedrock layers), standing on the floor, with the boss 20 blocks ahead; their party follows
 * (PartyTravel). Every use gets its own arena, ARENA_SPACING blocks apart. The boss drops an Arena Return when it dies,
 * which takes its user back to where they used the sigil, and the party with them.
 */
public final class BossArena {
    public static final ResourceKey<Level> ARENA = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("mdvlcraft", "boss_arena"));
    private static final String BOSS_TAG = "mdvlcraft_arena_boss";
    private static final int ARENA_SPACING = 1024;
    /** The top of the three bedrock layers (the dimension starts at y 0). */
    private static final double FLOOR = 3.0;
    private static final double BOSS_DISTANCE = 20.0;
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> BOSS_SIGIL = ITEMS.register("boss_sigil", SigilItem::new);
    public static final RegistryObject<Item> ARENA_RETURN = ITEMS.register("arena_return", ReturnItem::new);

    private BossArena() {
    }

    public static void register(IEventBus modBus, IEventBus forgeBus) {
        ITEMS.register(modBus);
        forgeBus.addListener(BossArena::onDeath);
    }

    /** The arena boss drops an Arena Return where it dies. */
    private static void onDeath(LivingDeathEvent event) {
        Entity boss = event.getEntity();
        if (boss.level() instanceof ServerLevel level && level.dimension() == ARENA && boss.getTags().contains(BOSS_TAG)) {
            boss.removeTag(BOSS_TAG);   // a boss that dies twice (a second phase) drops one
            ItemEntity drop = new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), new ItemStack(ARENA_RETURN.get()));
            drop.setUnlimitedLifetime();
            drop.setGlowingTag(true);
            level.addFreshEntity(drop);
        }
    }

    /** The next free arena (saved with the overworld). */
    private static int nextArena(MinecraftServer server) {
        Counter counter = server.overworld().getDataStorage().computeIfAbsent(Counter::load, Counter::new, "mdvlcraft_boss_arenas");
        int index = counter.next++;
        counter.setDirty();
        return index;
    }

    private static final class Counter extends SavedData {
        private int next;

        private static Counter load(CompoundTag tag) {
            Counter counter = new Counter();
            counter.next = tag.getInt("next");
            return counter;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("next", this.next);
            return tag;
        }
    }

    private static final class SigilItem extends Item {
        SigilItem() {
            super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
        }

        @Override
        public Component getName(ItemStack stack) {
            String boss = stack.getOrCreateTag().getString("BossName");
            return boss.isEmpty() ? super.getName(stack) : Component.translatable("item.mdvlcraft.boss_sigil.named", boss);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer user)) {
                return InteractionResultHolder.success(stack);
            }
            EntityType<?> type = EntityType.byString(stack.getOrCreateTag().getString("Boss")).orElse(null);
            ServerLevel arena = user.server.getLevel(ARENA);
            if (type == null || arena == null || PartyTravel.isBossDimension(level.dimension())) {
                user.displayClientMessage(Component.translatable("item.mdvlcraft.boss_sigil.fail").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            double x = nextArena(user.server) * ARENA_SPACING + 0.5;
            double z = 0.5;
            arena.getChunk((int)Math.floor(x) >> 4, (int)Math.floor(z + BOSS_DISTANCE) >> 4);   // generate the boss's chunk
            level.playSound(null, user.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 1.4F);
            user.teleportTo(arena, x, FLOOR, z, 0.0F, 0.0F);   // facing south, toward the boss
            user.fallDistance = 0.0F;
            Entity boss = type.create(arena);
            if (boss != null) {
                boss.moveTo(x, FLOOR, z + BOSS_DISTANCE, 180.0F, 0.0F);
                if (boss instanceof Mob mob) {
                    mob.setPersistenceRequired();
                    mob.finalizeSpawn(arena, arena.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
                }
                boss.addTag(BOSS_TAG);
                arena.addFreshEntityWithPassengers(boss);
            }
            if (!user.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.mdvlcraft.boss_sigil.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static final class ReturnItem extends Item {
        ReturnItem() {
            super(new Properties().stacksTo(16).rarity(Rarity.RARE));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer user)) {
                return InteractionResultHolder.success(stack);
            }
            if (level.dimension() != ARENA) {
                user.displayClientMessage(Component.translatable("item.mdvlcraft.arena_return.dim").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, user.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            if (!PartyTravel.returnHome(user)) {   // no origin: the world spawn
                ServerLevel overworld = user.server.overworld();
                user.teleportTo(overworld, overworld.getSharedSpawnPos().getX() + 0.5, overworld.getSharedSpawnPos().getY(),
                    overworld.getSharedSpawnPos().getZ() + 0.5, user.getYRot(), user.getXRot());
            }
            if (!user.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }

        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.mdvlcraft.arena_return.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
