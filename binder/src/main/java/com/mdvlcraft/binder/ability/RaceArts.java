package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.modified.Buffs;
import com.mdvlcraft.binder.modified.MartialSpells;
import com.mdvlcraft.binder.modified.Summons;
import com.mdvlcraft.binder.modified.Targets;
import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.icicle.IcicleProjectile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * The races' 2 abilities each (ability/RaceAbilities grants them; PlannedAbility race_<race>_<name> ids). Each is a
 * cooldown and a cast that returns false when it couldn't happen (nothing to target, nothing to eat), which spends no
 * cooldown. Cooldowns live in the player's persistent data, so they survive relogs.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RaceArts {
    private record Art(int cooldown, Predicate<ServerPlayer> cast) {
    }

    private static final Map<String, Art> ARTS = new HashMap<>();
    private static final String COOLDOWN = "mdvlcraft_race_cd_";
    private static final String NO_FALL = "race_no_fall";
    private static final String LANTERN = "race_lantern";
    private static final UUID SKITTER = UUID.fromString("0f6c3f43-8a3e-4b5e-9d0c-6b1c1a8e2f11");
    private static final Map<UUID, BlockPos> LIGHTS = new HashMap<>();
    private static final Map<UUID, Display.BlockDisplay> CHESTS = new HashMap<>();
    private static final Map<UUID, Vec3> CHEST_AT = new HashMap<>();
    private static final Map<UUID, Vec3> BLOOM = new HashMap<>();

    private RaceArts() {
    }

    private static void art(String key, int seconds, Predicate<ServerPlayer> cast) {
        ARTS.put(key, new Art(seconds * 20, cast));
    }

    /** Casts the race ability `key` (race_<race>_<name>); false if `key` has no art (still a placeholder). */
    static boolean cast(ServerPlayer player, String key, Component name) {
        Art art = ARTS.get(key);
        if (art == null) {
            return false;
        }
        long now = player.level().getGameTime();
        long ready = player.getPersistentData().getLong(COOLDOWN + key);
        if (now < ready && !player.isCreative()) {
            say(player, Component.translatable("ability.mdvlcraft.race.cooldown", name, (ready - now + 19) / 20).withStyle(ChatFormatting.GRAY));
            return true;
        }
        if (art.cast.test(player)) {
            player.getPersistentData().putLong(COOLDOWN + key, now + art.cooldown);
            SkillCallout.announce(player, PlannedAbility.byId(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mdvlcraft", "planned_" + key)).orElseThrow());
        }
        return true;
    }

    private static void say(ServerPlayer player, Component text) {
        player.connection.send(new ClientboundSetActionBarTextPacket(text));
    }

    private static boolean fail(ServerPlayer player, String why) {
        say(player, Component.translatable("ability.mdvlcraft.race.fail." + why).withStyle(ChatFormatting.RED));
        return false;
    }

    // ---- small helpers ----

    private static void effect(LivingEntity entity, MobEffect effect, int ticks, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    private static void burst(ServerLevel level, Entity at, ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, at.getX(), at.getY(0.6), at.getZ(), count, spread, 0.5, spread, 0.02);
    }

    private static List<LivingEntity> enemies(ServerPlayer player, double radius) {
        return Targets.enemies(player, player.position(), radius);
    }

    private static List<Player> players(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius), p -> p.isAlive() && !p.isSpectator());
    }

    private static Player nearestOther(ServerPlayer player, double radius) {
        return players(player, radius).stream().filter(p -> p != player).min(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(player))).orElse(null);
    }

    private static void cleanse(LivingEntity entity) {
        new ArrayList<>(entity.getActiveEffects()).stream().filter(e -> e.getEffect().getCategory() == MobEffectCategory.HARMFUL)
            .forEach(e -> entity.removeEffect(e.getEffect()));
    }

    private static void leap(ServerPlayer player, double forward, double up) {
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        player.setDeltaMovement(look.scale(forward).add(0.0, up, 0.0));
        player.hurtMarked = true;
        Buffs.give(player, NO_FALL, 100);
    }

    private static BlockPos lookedBlock(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(reach)), ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    private static <T extends Mob> List<T> summon(ServerPlayer player, EntityType<T> type, int count, int seconds, String kind, java.util.function.Consumer<T> setup) {
        return Summons.summon(player.serverLevel(), player, type, count, seconds * 20, kind, new SummonedEntitiesCastData(), setup);
    }

    private static boolean consume(ServerPlayer player, Predicate<ItemStack> what) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && what.test(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void feed(ServerPlayer player, int food, float saturation) {
        player.getFoodData().eat(food, saturation / Math.max(1, food) / 2.0F);
        sound(player, SoundEvents.GENERIC_EAT, 1.0F);
    }

    /** A chest-like store kept in the player's persistent data under `key`. */
    private static void openStore(ServerPlayer player, String key, int rows, Component title) {
        SimpleContainer store = new SimpleContainer(rows * 9) {
            @Override
            public void stopOpen(Player who) {
                super.stopOpen(who);
                who.getPersistentData().put(key, this.createTag());
            }
        };
        store.fromTag(player.getPersistentData().getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND));
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> rows == 1
            ? new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inventory, store, 1)
            : ChestMenu.threeRows(id, inventory, store), title));
    }

    private static boolean humanoid(LivingEntity entity) {
        return entity instanceof Player || entity instanceof AbstractIllager || entity instanceof AbstractSkeleton || entity instanceof Zombie
            || entity instanceof AbstractPiglin || entity instanceof Witch || entity instanceof AbstractVillager;
    }

    static {
        // ---- Human ----
        art("race_human_second_wind", 180, p -> {
            var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(p,
                yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch.class);
            if (patch != null) {
                patch.setStamina(Math.min(patch.getMaxStamina(), patch.getStamina() + patch.getMaxStamina() * 0.3F));
            }
            p.getFoodData().eat(6, 0.25F);
            burst(p.serverLevel(), p, ParticleTypes.CLOUD, 12, 0.3);
            sound(p, SoundEvents.PLAYER_BREATH, 1.0F);
            return true;
        });
        art("race_human_signal_flare", 120, p -> {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag explosion = new CompoundTag();
            explosion.putByte("Type", (byte)1);
            explosion.putIntArray("Colors", new int[]{0xFF3030});
            CompoundTag fireworks = new CompoundTag();
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            list.add(explosion);
            fireworks.put("Explosions", list);
            fireworks.putByte("Flight", (byte)2);
            rocket.getOrCreateTag().put("Fireworks", fireworks);
            p.level().addFreshEntity(new FireworkRocketEntity(p.level(), p.getX(), p.getY(1.0), p.getZ(), rocket));
            enemies(p, 24.0).forEach(e -> effect(e, MobEffects.GLOWING, 200, 0));
            return true;
        });
        // ---- Feline ----
        art("race_feline_pounce", 15, p -> {
            leap(p, 1.5, 0.45);
            return true;
        });
        art("race_feline_twitching_whiskers", 60, p -> {
            p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(20.0), e -> e instanceof Enemy)
                .forEach(e -> effect(e, MobEffects.GLOWING, 160, 0));
            sound(p, SoundEvents.CAT_PURR, 1.2F);
            return true;
        });
        // ---- Merling ----
        art("race_merling_dolphin_s_grace", 90, p -> {
            effect(p, MobEffects.DOLPHINS_GRACE, 600, 0);
            return true;
        });
        art("race_merling_rain_dance", 1800, p -> {
            p.serverLevel().setWeatherParameters(0, 2400, true, false);
            sound(p, SoundEvents.WEATHER_RAIN, 1.0F);
            return true;
        });
        // ---- Arachnae ----
        art("race_arachnae_web_shot", 12, p -> {
            LivingEntity target = Targets.looked(p, 16.0F);
            if (target == null) {
                return fail(p, "no_target");
            }
            MartialSpells.root(target, 40);
            p.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
                target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.5, 0.4, 0.0);
            sound(p, SoundEvents.SPIDER_AMBIENT, 1.4F);
            return true;
        });
        art("race_arachnae_egg_sac", 120, p -> {
            summon(p, EntityType.CAVE_SPIDER, 2, 20, "race_egg_sac", m -> {
            });
            sound(p, SoundEvents.TURTLE_EGG_HATCH, 1.0F);
            return true;
        });
        // ---- Dwarf ----
        art("race_dwarf_field_forge", 300, p -> {
            ItemStack held = p.getMainHandItem();
            if (!held.isDamageableItem() || held.getDamageValue() == 0) {
                return fail(p, "nothing_to_repair");
            }
            held.setDamageValue(Math.max(0, held.getDamageValue() - Math.max(1, held.getMaxDamage() * 15 / 100)));
            sound(p, SoundEvents.ANVIL_USE, 1.2F);
            return true;
        });
        art("race_dwarf_smelt", 180, p -> {
            ItemStack held = p.getMainHandItem();
            var recipe = p.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(held), p.level());
            if (held.isEmpty() || recipe.isEmpty()) {
                return fail(p, "nothing_to_smelt");
            }
            int n = Math.min(8, held.getCount());
            ItemStack out = recipe.get().getResultItem(p.level().registryAccess()).copy();
            out.setCount(out.getCount() * n);
            held.shrink(n);
            p.getInventory().placeItemBackInInventory(out);
            burst(p.serverLevel(), p, ParticleTypes.FLAME, 16, 0.3);
            sound(p, SoundEvents.FURNACE_FIRE_CRACKLE, 1.0F);
            return true;
        });
        // ---- Wood Elf ----
        art("race_wood_elf_fletch", 180, p -> {
            p.getInventory().placeItemBackInInventory(new ItemStack(Items.ARROW, 8));
            sound(p, SoundEvents.VILLAGER_WORK_FLETCHER, 1.0F);
            return true;
        });
        art("race_wood_elf_elven_grace", 120, p -> {
            effect(p, MobEffects.MOVEMENT_SPEED, 400, 0);
            Buffs.give(p, NO_FALL, 400);
            burst(p.serverLevel(), p, ParticleTypes.HAPPY_VILLAGER, 12, 0.4);
            return true;
        });
        // ---- Goblin ----
        art("race_goblin_snare_trap", 30, p -> {
            ServerLevel level = p.serverLevel();
            Vec3 at = p.position();
            boolean[] sprung = {false};
            SpecKit.zone(level, at, 1.2, 1200, ParticleTypes.CRIT, zone -> {
                if (sprung[0]) {
                    return;
                }
                for (LivingEntity enemy : Targets.enemies(p, at, 1.2)) {
                    MartialSpells.root(enemy, 60);
                    level.playSound(null, BlockPos.containing(at), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
                    sprung[0] = true;
                    break;
                }
            });
            sound(p, SoundEvents.TRIPWIRE_ATTACH, 1.0F);
            return true;
        });
        art("race_goblin_sneaky_git", 90, p -> {
            effect(p, MobEffects.INVISIBILITY, 160, 0);
            burst(p.serverLevel(), p, ParticleTypes.SMOKE, 20, 0.3);
            return true;
        });
        // ---- Siren ----
        art("race_siren_lure", 45, p -> {
            List<Mob> mobs = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(10.0), m -> m.isAlive() && !Summons.allied(p, m));
            if (mobs.isEmpty()) {
                return fail(p, "no_target");
            }
            for (int s = 0; s < 3; s++) {
                SpecKit.later(p.serverLevel(), s * 20, () -> mobs.forEach(m -> {
                    m.setTarget(null);
                    m.getNavigation().moveTo(p, 1.0);
                }));
            }
            mobs.forEach(m -> SpecKit.blind(m, 60));
            burst(p.serverLevel(), p, ParticleTypes.NOTE, 10, 0.6);
            sound(p, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, 0.8F);
            return true;
        });
        art("race_siren_lullaby", 1200, p -> {
            Buffs.give(p, "race_lullaby", 1200);
            Player other = nearestOther(p, 16.0);
            if (other != null) {
                Buffs.give(other, "race_lullaby", 1200);
            }
            burst(p.serverLevel(), p, ParticleTypes.NOTE, 12, 0.5);
            return true;
        });
        // ---- Ogre ----
        art("race_ogre_heave", 20, p -> {
            LivingEntity target = Targets.looked(p, 4.0F);
            if (target == null) {
                return fail(p, "no_target");
            }
            target.setDeltaMovement(p.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(1.4).add(0.0, 0.7, 0.0));
            target.hurtMarked = true;
            sound(p, SoundEvents.RAVAGER_ATTACK, 1.0F);
            return true;
        });
        art("race_ogre_big_appetite", 300, p -> {
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(20.0F);
            sound(p, SoundEvents.PLAYER_BURP, 0.8F);
            return true;
        });
        // ---- Land Shark ----
        art("race_land_shark_burrow_dive", 20, p -> {
            if (!p.getBlockStateOn().is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                return fail(p, "need_soft_ground");
            }
            Vec3 ahead = p.position().add(p.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(8.0));
            BlockPos base = BlockPos.containing(ahead);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos feet = base.above(dy);
                if (p.level().getBlockState(feet.below()).isSolid() && p.level().getBlockState(feet).isAir() && p.level().getBlockState(feet.above()).isAir()) {
                    burst(p.serverLevel(), p, new BlockParticleOption(ParticleTypes.BLOCK, p.getBlockStateOn()), 30, 0.4);
                    p.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                    burst(p.serverLevel(), p, new BlockParticleOption(ParticleTypes.BLOCK, p.getBlockStateOn()), 30, 0.4);
                    sound(p, SoundEvents.ROOTED_DIRT_BREAK, 0.8F);
                    return true;
                }
            }
            return fail(p, "no_room");
        });
        art("race_land_shark_gulp", 180, p -> {
            Buffs.give(p, "race_gulp", 1200);
            return true;
        });
        // ---- Kirin ----
        art("race_kirin_purifying_light", 120, p -> {
            players(p, 6.0).forEach(RaceArts::cleanse);
            burst(p.serverLevel(), p, ParticleTypes.END_ROD, 30, 1.5);
            sound(p, SoundEvents.BEACON_POWER_SELECT, 1.4F);
            return true;
        });
        art("race_kirin_blessing", 180, p -> {
            for (LivingEntity who : new LivingEntity[]{p, nearestOther(p, 8.0)}) {
                if (who != null) {
                    effect(who, MobEffects.LUCK, 600, 0);
                    effect(who, MobEffects.REGENERATION, 600, 0);
                }
            }
            burst(p.serverLevel(), p, ParticleTypes.HAPPY_VILLAGER, 16, 0.6);
            return true;
        });
        // ---- Raptus ----
        art("race_raptus_talon_dive", 20, p -> {
            BlockPos spot = lookedBlock(p, 24.0);
            if (spot == null) {
                return fail(p, "no_target");
            }
            Vec3 to = Vec3.atBottomCenterOf(spot.above()).subtract(p.position());
            p.setDeltaMovement(to.normalize().scale(Math.min(2.5, 0.25 * to.length() + 0.8)).add(0.0, 0.3, 0.0));
            p.hurtMarked = true;
            Buffs.give(p, NO_FALL, 60);
            Buffs.give(p, "race_talon", 60);
            return true;
        });
        art("race_raptus_feather_fall", 60, p -> {
            effect(p, MobEffects.SLOW_FALLING, 600, 0);
            return true;
        });
        // ---- Rat ----
        art("race_rat_swarm_call", 90, p -> {
            summon(p, EntityType.SILVERFISH, 2, 15, "race_rat", m -> {
                m.setCustomName(Component.translatable("entity.mdvlcraft.race_rat"));
                m.goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().startsWith("Silverfish"));
            });
            sound(p, SoundEvents.SILVERFISH_AMBIENT, 1.2F);
            return true;
        });
        art("race_rat_hoard", 0, p -> {
            openStore(p, "mdvlcraft_race_hoard", 1, Component.translatable("ability.mdvlcraft.planned_race_rat_hoard"));
            return true;
        });
        // ---- Shadow ----
        art("race_shadow_shadow_pocket", 30, p -> {
            CompoundTag data = p.getPersistentData();
            ItemStack held = p.getMainHandItem();
            if (data.contains("mdvlcraft_race_pocket") && held.isEmpty()) {
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.of(data.getCompound("mdvlcraft_race_pocket")));
                data.remove("mdvlcraft_race_pocket");
            } else if (!data.contains("mdvlcraft_race_pocket") && !held.isEmpty()) {
                data.put("mdvlcraft_race_pocket", held.save(new CompoundTag()));
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            } else {
                return fail(p, data.contains("mdvlcraft_race_pocket") ? "hand_full" : "hand_empty");
            }
            burst(p.serverLevel(), p, ParticleTypes.SQUID_INK, 12, 0.3);
            return true;
        });
        art("race_shadow_grasp", 15, p -> {
            LivingEntity target = Targets.looked(p, 16.0F);
            if (target == null) {
                return fail(p, "no_target");
            }
            MartialSpells.root(target, 40);
            p.serverLevel().sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.2), target.getZ(), 30, 0.4, 0.2, 0.4, 0.01);
            return true;
        });
        // ---- Sporeling ----
        art("race_sporeling_spore_burst", 30, p -> {
            enemies(p, 4.0).forEach(e -> {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 80, 0);
                effect(e, MobEffects.WEAKNESS, 80, 0);
            });
            p.serverLevel().sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY(1.0), p.getZ(), 80, 2.0, 1.0, 2.0, 0.0);
            return true;
        });
        art("race_sporeling_healing_spores", 90, p -> {
            players(p, 6.0).forEach(o -> effect(o, MobEffects.REGENERATION, 160, 0));
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY(1.0), p.getZ(), 40, 2.0, 1.0, 2.0, 0.0);
            return true;
        });
        // ---- Stoneborn ----
        art("race_stoneborn_stone_shape", 45, p -> {
            Direction facing = p.getDirection();
            BlockPos centre = p.blockPosition().relative(facing, 2);
            List<BlockPos> placed = new ArrayList<>();
            for (int side = -1; side <= 1; side++) {
                for (int up = 0; up < 2; up++) {
                    BlockPos pos = centre.relative(facing.getClockWise(), side).above(up);
                    if (p.level().getBlockState(pos).canBeReplaced()) {
                        p.level().setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                        placed.add(pos);
                    }
                }
            }
            if (placed.isEmpty()) {
                return fail(p, "no_room");
            }
            sound(p, SoundEvents.STONE_PLACE, 0.7F);
            SpecKit.later(p.serverLevel(), 600, () -> placed.forEach(pos -> {
                if (p.level().getBlockState(pos).is(Blocks.COBBLESTONE)) {
                    p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }));
            return true;
        });
        art("race_stoneborn_eat_stone", 30, p -> {
            if (!consume(p, s -> s.is(Items.COBBLESTONE))) {
                return fail(p, "nothing_to_eat");
            }
            feed(p, 4, 2.0F);
            return true;
        });
        // ---- Infernal ----
        art("race_infernal_brimstone_meal", 30, p -> {
            if (!consume(p, s -> s.is(Items.COAL) || s.is(Items.CHARCOAL))) {
                return fail(p, "nothing_to_eat");
            }
            feed(p, 3, 1.5F);
            return true;
        });
        art("race_infernal_fire_breath", 30, p -> {
            for (int t = 0; t < 12; t++) {
                SpecKit.later(p.serverLevel(), t * 5, () -> {
                    Vec3 eye = p.getEyePosition();
                    Vec3 look = p.getLookAngle();
                    for (int i = 1; i <= 5; i++) {
                        Vec3 at = eye.add(look.scale(i));
                        p.serverLevel().sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.15 * i, 0.15 * i, 0.15 * i, 0.01);
                    }
                    for (LivingEntity e : enemies(p, 5.0)) {
                        Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                        if (to.normalize().dot(look) > 0.7) {
                            e.setSecondsOnFire(3);
                            e.invulnerableTime = 0;
                            e.hurt(p.damageSources().onFire(), 1.0F);
                        }
                    }
                });
            }
            sound(p, SoundEvents.BLAZE_SHOOT, 0.8F);
            return true;
        });
        // ---- Dryad ----
        art("race_dryad_grow_tree", 120, p -> {
            BlockPos pos = lookedBlock(p, 6.0);
            if (pos == null || !(p.level().getBlockState(pos).getBlock() instanceof SaplingBlock)) {
                return fail(p, "need_sapling");
            }
            for (int i = 0; i < 10 && p.level().getBlockState(pos).getBlock() instanceof SaplingBlock sapling; i++) {
                sapling.advanceTree(p.serverLevel(), pos, p.level().getBlockState(pos), p.getRandom());
            }
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 1.0, 1.0, 1.0, 0.0);
            return true;
        });
        art("race_dryad_entangle", 30, p -> {
            List<LivingEntity> enemies = enemies(p, 4.0);
            if (enemies.isEmpty()) {
                return fail(p, "no_target");
            }
            enemies.forEach(e -> {
                MartialSpells.root(e, 40);
                p.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                    e.getX(), e.getY(0.2), e.getZ(), 15, 0.3, 0.2, 0.3, 0.0);
            });
            return true;
        });
        // ---- Orc ----
        art("race_orc_intimidate", 60, p -> {
            enemies(p, 8.0).forEach(e -> effect(e, MobEffects.WEAKNESS, 160, 0));
            sound(p, SoundEvents.RAVAGER_ROAR, 1.2F);
            return true;
        });
        art("race_orc_trophy", 180, p -> {
            Buffs.give(p, "race_trophy", 3600);
            return true;
        });
        // ---- Kobold ----
        art("race_kobold_skitter", 45, p -> {
            effect(p, MobEffects.MOVEMENT_SPEED, 160, 1);
            AttributeInstance step = p.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
            if (step != null && step.getModifier(SKITTER) == null) {
                step.addTransientModifier(new AttributeModifier(SKITTER, "mdvlcraft skitter", 0.5, AttributeModifier.Operation.ADDITION));
                SpecKit.later(p.serverLevel(), 160, () -> step.removeModifier(SKITTER));
            }
            return true;
        });
        art("race_kobold_pack_tactics", 90, p -> {
            Targets.allies(p, p.position(), 8.0).forEach(a -> Buffs.give(a, "race_pack_tactics", 200));
            sound(p, SoundEvents.WOLF_HOWL, 1.4F);
            return true;
        });
        // ---- Revenant ----
        art("race_revenant_raise_servant", 120, p -> {
            summon(p, EntityType.SKELETON, 1, 30, "race_servant", m -> m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)));
            sound(p, SoundEvents.SKELETON_AMBIENT, 0.8F);
            return true;
        });
        art("race_revenant_lantern_of_souls", 180, p -> {
            Buffs.give(p, LANTERN, 2400);
            sound(p, SoundEvents.SOUL_ESCAPE, 1.0F);
            return true;
        });
        // ---- Wolf-blooded ----
        art("race_wolf_blooded_call_pack", 180, p -> {
            summon(p, EntityType.WOLF, 2, 30, "race_pack", m -> {
            });
            sound(p, SoundEvents.WOLF_HOWL, 1.0F);
            return true;
        });
        art("race_wolf_blooded_moon_sight", 180, p -> {
            if (!p.level().isNight()) {
                return fail(p, "need_night");
            }
            effect(p, MobEffects.NIGHT_VISION, 2400, 0);
            return true;
        });
        // ---- Frostborn ----
        art("race_frostborn_whiteout", 120, p -> {
            for (int s = 0; s < 8; s++) {
                SpecKit.later(p.serverLevel(), s * 20, () -> {
                    p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY(1.0), p.getZ(), 120, 4.0, 1.5, 4.0, 0.05);
                    for (LivingEntity e : enemies(p, 6.0)) {
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                        if (e instanceof Mob mob && mob.getTarget() == p) {
                            SpecKit.blind(mob, 40);
                        }
                    }
                });
            }
            sound(p, SoundEvents.POWDER_SNOW_STEP, 0.6F);
            return true;
        });
        art("race_frostborn_frost_walk", 90, p -> {
            Buffs.give(p, "race_frost_walk", 600);
            return true;
        });
        // ---- Stormkin ----
        art("race_stormkin_thunderhead", 45, p -> {
            BlockPos spot = lookedBlock(p, 24.0);
            if (spot == null) {
                return fail(p, "no_target");
            }
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(p.level());
            if (bolt != null) {
                bolt.moveTo(Vec3.atBottomCenterOf(spot.above()));
                bolt.setCause(p);
                p.level().addFreshEntity(bolt);
            }
            return true;
        });
        art("race_stormkin_call_the_rain", 1200, p -> {
            if (p.level().isRaining()) {
                p.serverLevel().setWeatherParameters(6000, 0, false, false);
            } else {
                p.serverLevel().setWeatherParameters(0, 6000, true, false);
            }
            return true;
        });
        // ---- Dunekin ----
        art("race_dunekin_mirage", 90, p -> {
            SpecKit.decoy(p.serverLevel(), p, p.position(), 12.0, 120);
            effect(p, MobEffects.INVISIBILITY, 60, 0);
            burst(p.serverLevel(), p, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), 30, 0.5);
            return true;
        });
        art("race_dunekin_sandstorm", 90, p -> {
            for (int s = 0; s < 6; s++) {
                SpecKit.later(p.serverLevel(), s * 20, () -> {
                    p.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        p.getX(), p.getY(1.0), p.getZ(), 120, 3.0, 1.2, 3.0, 0.1);
                    for (LivingEntity e : enemies(p, 5.0)) {
                        effect(e, MobEffects.BLINDNESS, 40, 0);
                        Vec3 away = e.position().subtract(p.position()).multiply(1.0, 0.0, 1.0).normalize().scale(0.6);
                        e.push(away.x, 0.1, away.z);
                        e.hurtMarked = true;
                    }
                });
            }
            return true;
        });
        // ---- Sculkborn ----
        art("race_sculkborn_sculk_bloom", 300, p -> {
            BlockPos feet = p.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-4, -1, -4), feet.offset(4, -1, 4))) {
                BlockState state = p.level().getBlockState(pos);
                if (pos.distSqr(feet.below()) <= 16 && state.is(BlockTags.SCULK_REPLACEABLE) && p.level().getBlockState(pos.above()).isAir()) {
                    p.level().setBlockAndUpdate(pos, Blocks.SCULK.defaultBlockState());
                }
            }
            BLOOM.put(p.getUUID(), p.position());
            Buffs.give(p, "race_sculk_bloom", 1200);
            sound(p, SoundEvents.SCULK_CATALYST_BLOOM, 1.0F);
            return true;
        });
        art("race_sculkborn_echolocation", 30, p -> {
            p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24.0), e -> e != p)
                .forEach(e -> effect(e, MobEffects.GLOWING, 160, 0));
            List<BlockPos> chests = new ArrayList<>();
            int cx = p.chunkPosition().x;
            int cz = p.chunkPosition().z;
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    for (BlockEntity block : p.level().getChunk(x, z).getBlockEntities().values()) {
                        if (block instanceof RandomizableContainerBlockEntity && block.getBlockPos().distSqr(p.blockPosition()) <= 576) {
                            chests.add(block.getBlockPos());
                        }
                    }
                }
            }
            for (int s = 0; s < 8; s++) {
                SpecKit.later(p.serverLevel(), s * 20, () -> chests.forEach(pos -> p.serverLevel().sendParticles(p, ParticleTypes.SCULK_SOUL, true,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01)));
            }
            sound(p, SoundEvents.WARDEN_SONIC_CHARGE, 1.6F);
            return true;
        });
        // ---- Gloomkin ----
        art("race_gloomkin_lantern_swarm", 300, p -> {
            Buffs.give(p, LANTERN, 2400);
            Buffs.give(p, "race_moths", 2400);
            return true;
        });
        art("race_gloomkin_blinding_dust", 60, p -> {
            for (LivingEntity e : enemies(p, 4.0)) {
                effect(e, MobEffects.BLINDNESS, 80, 0);
                if (e instanceof Mob mob) {
                    SpecKit.blind(mob, 80);
                }
            }
            p.serverLevel().sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY(1.0), p.getZ(), 120, 2.0, 1.0, 2.0, 0.02);
            return true;
        });
        // ---- Faun ----
        art("race_faun_herd_speed", 120, p -> {
            Targets.allies(p, p.position(), 8.0).forEach(a -> effect(a, MobEffects.MOVEMENT_SPEED, 200, 1));
            return true;
        });
        art("race_faun_bound", 20, p -> {
            leap(p, 1.4, 0.8);
            return true;
        });
        // ---- Mimic ----
        art("race_mimic_chest_form", 30, p -> {
            Display.BlockDisplay chest = EntityType.BLOCK_DISPLAY.create(p.level());
            if (chest == null) {
                return false;
            }
            CompoundTag data = new CompoundTag();
            data.put("block_state", net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.CHEST.defaultBlockState()));
            chest.load(chest.saveWithoutId(new CompoundTag()).merge(data));
            chest.moveTo(Math.floor(p.getX()), p.getY(), Math.floor(p.getZ()));
            p.level().addFreshEntity(chest);
            CHESTS.put(p.getUUID(), chest);
            CHEST_AT.put(p.getUUID(), p.position());
            effect(p, MobEffects.INVISIBILITY, 12000, 0);
            Buffs.give(p, "race_chest_form", 12000);
            p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0), m -> m.getTarget() == p).forEach(m -> m.setTarget(null));
            return true;
        });
        art("race_mimic_copycat", 300, p -> {
            LivingEntity target = Targets.looked(p, 16.0F);
            if (!(target instanceof Mob)) {
                return fail(p, "no_target");
            }
            p.getPersistentData().putString("mdvlcraft_race_copycat", EntityType.getKey(target.getType()).toString());
            Buffs.give(p, "race_copycat", 1200);
            effect(p, MobEffects.INVISIBILITY, 1200, 0);
            p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0), m -> m.getType() == target.getType() && m.getTarget() == p)
                .forEach(m -> m.setTarget(null));
            burst(p.serverLevel(), p, ParticleTypes.WITCH, 20, 0.4);
            return true;
        });
        // ---- Illagerkin ----
        art("race_illagerkin_vex_scout", 120, p -> {
            List<net.minecraft.world.entity.monster.Vex> vexes = summon(p, EntityType.VEX, 1, 20, "race_vex_scout", m -> {
            });
            for (int s = 1; s <= 20; s++) {
                SpecKit.later(p.serverLevel(), s * 20, () -> vexes.stream().filter(Entity::isAlive).forEach(vex ->
                    p.level().getEntitiesOfClass(Mob.class, vex.getBoundingBox().inflate(12.0), m -> m != vex && !Summons.allied(p, m))
                        .forEach(m -> effect(m, MobEffects.GLOWING, 40, 0))));
            }
            return true;
        });
        art("race_illagerkin_raid_horn", 600, p -> {
            summon(p, EntityType.PILLAGER, 2, 60, "race_raid_horn", m -> m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW)));
            sound(p, SoundEvents.RAID_HORN.get(), 1.0F);
            return true;
        });
        // ---- Piglin-born ----
        art("race_piglinborn_hoglin_call", 900, p -> {
            summon(p, EntityType.HOGLIN, 1, 300, "race_hoglin", m -> {
                m.setPersistenceRequired();
                ((net.minecraft.world.entity.monster.hoglin.Hoglin)m).setImmuneToZombification(true);
            }).forEach(h -> p.startRiding(h, true));
            sound(p, SoundEvents.HOGLIN_AMBIENT, 1.0F);
            return true;
        });
        art("race_piglinborn_barter", 30, p -> {
            if (!consume(p, s -> s.is(Items.GOLD_INGOT))) {
                return fail(p, "need_gold");
            }
            LootParams params = new LootParams.Builder(p.serverLevel()).withParameter(LootContextParams.THIS_ENTITY, p)
                .create(LootContextParamSets.PIGLIN_BARTER);
            p.serverLevel().getServer().getLootData().getLootTable(BuiltInLootTables.PIGLIN_BARTERING).getRandomItems(params)
                .forEach(p.getInventory()::placeItemBackInInventory);
            sound(p, SoundEvents.PIGLIN_ADMIRING_ITEM, 1.2F);
            return true;
        });
        // ---- Kitsune ----
        art("race_kitsune_illusion", 90, p -> {
            Vec3 side = p.getLookAngle().cross(new Vec3(0.0, 1.0, 0.0)).normalize().scale(2.0);
            SpecKit.decoy(p.serverLevel(), p, p.position().add(side), 10.0, 100);
            SpecKit.decoy(p.serverLevel(), p, p.position().subtract(side), 10.0, 100);
            burst(p.serverLevel(), p, ParticleTypes.SOUL_FIRE_FLAME, 20, 0.6);
            return true;
        });
        art("race_kitsune_shapeshift", 180, p -> {
            ScaleTypes.BASE.getScaleData(p).setTargetScale(0.5F);
            effect(p, MobEffects.MOVEMENT_SPEED, 1200, 1);
            Buffs.give(p, "race_fox_form", 1200);
            SpecKit.later(p.serverLevel(), 1200, () -> ScaleTypes.BASE.getScaleData(p).setTargetScale(1.0F));
            burst(p.serverLevel(), p, ParticleTypes.POOF, 20, 0.4);
            return true;
        });
        // ---- Curse-Bonded ----
        art("race_curse_bonded_spirit_storage", 10, p -> {
            openStore(p, "mdvlcraft_race_spirit", 3, Component.translatable("ability.mdvlcraft.planned_race_curse_bonded_spirit_storage"));
            return true;
        });
        art("race_curse_bonded_hold_tight", 60, p -> {
            players(p, 8.0).forEach(o -> {
                Buffs.give(o, NO_FALL, 200);
                effect(o, MobEffects.SLOW_FALLING, 200, 0);
            });
            burst(p.serverLevel(), p, ParticleTypes.SOUL, 20, 0.5);
            return true;
        });
        // ---- Vessel ----
        art("race_vessel_shrug_off", 120, p -> {
            cleanse(p);
            p.heal(4.0F);
            return true;
        });
        art("race_vessel_superhuman_leap", 20, p -> {
            p.setDeltaMovement(p.getDeltaMovement().add(0.0, 1.25, 0.0));
            p.hurtMarked = true;
            Buffs.give(p, NO_FALL, 120);
            return true;
        });
        // ---- Umbral ----
        art("race_umbral_rabbit_escape", 90, p -> {
            p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0), m -> m.getTarget() == p).forEach(m -> {
                m.setTarget(null);
                SpecKit.blind(m, 60);
            });
            effect(p, MobEffects.INVISIBILITY, 60, 0);
            for (int i = 0; i < 3; i++) {
                var rabbit = EntityType.RABBIT.create(p.level());
                if (rabbit != null) {
                    rabbit.moveTo(p.getX(), p.getY(), p.getZ());
                    rabbit.setVariant(net.minecraft.world.entity.animal.Rabbit.Variant.BLACK);
                    p.level().addFreshEntity(rabbit);
                    SpecKit.later(p.serverLevel(), 60, rabbit::discard);
                }
            }
            return true;
        });
        art("race_umbral_divine_dogs", 180, p -> {
            summon(p, EntityType.WOLF, 2, 60, "race_divine_dogs", m -> m.setCustomName(Component.translatable("entity.mdvlcraft.race_divine_dog")));
            burst(p.serverLevel(), p, ParticleTypes.SQUID_INK, 30, 0.8);
            return true;
        });
        // ---- Shrinekin ----
        art("race_shrinekin_know_your_place", 45, p -> {
            LivingEntity target = Targets.looked(p, 16.0F);
            if (target == null) {
                return fail(p, "no_target");
            }
            int ticks = humanoid(target) ? 200 : 40;
            SpecKit.stun(target, ticks);
            if (target instanceof Mob mob) {
                mob.setPose(net.minecraft.world.entity.Pose.CROUCHING);
                SpecKit.later(p.serverLevel(), ticks, () -> mob.setPose(net.minecraft.world.entity.Pose.STANDING));
            }
            p.serverLevel().sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY(1.0), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            sound(p, SoundEvents.WARDEN_ROAR, 1.6F);
            return true;
        });
        art("race_shrinekin_call_uraume", 300, p -> {
            summon(p, EntityType.FOX, 1, 30, "race_uraume", fox -> {
                ((Fox)fox).setVariant(Fox.Type.SNOW);
                fox.setCustomName(Component.literal("Uraume"));
                fox.setCustomNameVisible(true);
            }).forEach(fox -> {
                for (int s = 2; s <= 30; s += 2) {
                    SpecKit.later(p.serverLevel(), s * 20, () -> uraumeCast(p, fox));
                }
            });
            burst(p.serverLevel(), p, ParticleTypes.SNOWFLAKE, 40, 0.8);
            return true;
        });
    }

    /** Uraume casts one of the four ice spells picked for it (cone of cold, icicle, frostwave, ray of frost) at the nearest enemy. */
    private static void uraumeCast(ServerPlayer owner, Mob fox) {
        if (!fox.isAlive() || !(fox.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = SpecKit.nearestEnemy(owner, fox.position(), 16.0, fox);
        if (target == null || target == owner) {
            return;
        }
        fox.getLookControl().setLookAt(target);
        Vec3 from = fox.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        switch (fox.getRandom().nextInt(4)) {
            case 0 -> {   // Icicle
                IcicleProjectile icicle = new IcicleProjectile(level, fox);
                icicle.setPos(from);
                icicle.shoot(to.subtract(from).normalize());
                icicle.setDamage(6.0F);
                level.addFreshEntity(icicle);
            }
            case 1 -> {   // Ray of Frost
                Vec3 step = to.subtract(from);
                for (int i = 0; i <= 10; i++) {
                    Vec3 at = from.add(step.scale(i / 10.0));
                    level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
                }
                frost(target, SpellDamageSource.source(fox, SpellRegistry.RAY_OF_FROST_SPELL.get()), 6.0F);
            }
            case 2 -> {   // Frostwave: every enemy within 4 of Uraume
                level.sendParticles(ParticleTypes.SNOWFLAKE, fox.getX(), fox.getY(0.5), fox.getZ(), 80, 2.0, 0.3, 2.0, 0.05);
                Targets.enemies(owner, fox.position(), 4.0).stream().filter(e -> e != fox)
                    .forEach(e -> frost(e, SpellDamageSource.source(fox, SpellRegistry.FROSTWAVE_SPELL.get()), 4.0F));
            }
            default -> {   // Cone of Cold: enemies in front within 6
                Vec3 dir = to.subtract(from).normalize();
                for (int i = 1; i <= 6; i++) {
                    Vec3 at = from.add(dir.scale(i));
                    level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 6, 0.12 * i, 0.12 * i, 0.12 * i, 0.01);
                }
                Targets.enemies(owner, fox.position(), 6.0).stream()
                    .filter(e -> e != fox && e.getBoundingBox().getCenter().subtract(from).normalize().dot(dir) > 0.75)
                    .forEach(e -> frost(e, SpellDamageSource.source(fox, SpellRegistry.CONE_OF_COLD_SPELL.get()), 5.0F));
            }
        }
        level.playSound(null, fox.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.6F, 1.6F);
    }

    private static void frost(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float damage) {
        target.invulnerableTime = 0;
        target.hurt(source, damage);
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 40));
    }

    // ---- what the arts leave running ----

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (Buffs.has(entity, NO_FALL)) {
            event.setCanceled(true);
        }
        if (Buffs.has(entity, "race_talon") && entity instanceof ServerPlayer player) {
            Buffs.clear(player, "race_talon");
            enemies(player, 3.0).forEach(e -> e.hurt(player.damageSources().playerAttack(player), 4.0F));
            player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 2, 0.5, 0.1, 0.5, 0.0);
        }
    }

    @SubscribeEvent
    public static void onSleepTime(SleepingTimeCheckEvent event) {
        if (Buffs.has(event.getEntity(), "race_lullaby")) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    /** Gulp: raw fish and meat give double food. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        ItemStack item = event.getItem();
        if (event.getEntity() instanceof ServerPlayer player && Buffs.has(player, "race_gulp") && item.isEdible()
            && (item.is(ItemTags.FISHES) || item.getFoodProperties(player) != null && item.getFoodProperties(player).isMeat())
            && !item.getDescriptionId().contains("cooked")) {
            var food = item.getFoodProperties(player);
            player.getFoodData().eat(food.getNutrition(), food.getSaturationModifier());
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && Buffs.has(attacker, "race_pack_tactics")) {
            event.setAmount(event.getAmount() * 1.1F);
        }
    }

    /** Trophy: the next kill drops its head (or a bone). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && Buffs.has(player, "race_trophy")) {
            Buffs.clear(player, "race_trophy");
            LivingEntity dead = event.getEntity();
            ItemStack head = dead.getType() == EntityType.ZOMBIE ? new ItemStack(Items.ZOMBIE_HEAD)
                : dead.getType() == EntityType.SKELETON ? new ItemStack(Items.SKELETON_SKULL)
                : dead.getType() == EntityType.CREEPER ? new ItemStack(Items.CREEPER_HEAD)
                : dead.getType() == EntityType.WITHER_SKELETON ? new ItemStack(Items.WITHER_SKELETON_SKULL)
                : dead.getType() == EntityType.PIGLIN ? new ItemStack(Items.PIGLIN_HEAD)
                : new ItemStack(Items.BONE);
            dead.spawnAtLocation(head);
        }
    }

    /** Sculk Bloom: for a minute, the experience of kills near the bloom goes to its sculkborn. */
    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        LivingEntity dead = event.getEntity();
        for (Map.Entry<UUID, Vec3> bloom : BLOOM.entrySet()) {
            if (dead.level().getPlayerByUUID(bloom.getKey()) instanceof ServerPlayer player && Buffs.has(player, "race_sculk_bloom")
                && dead.position().distanceToSqr(bloom.getValue()) <= 64.0) {
                player.giveExperiencePoints(event.getDroppedExperience());
                event.setDroppedExperience(0);
                return;
            }
        }
    }

    /** Chest Form and Copycat: the mobs they fool don't pick you as a target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer player)) {
            return;
        }
        if (Buffs.has(player, "race_chest_form")
            || Buffs.has(player, "race_copycat") && EntityType.getKey(event.getEntity().getType()).toString()
                .equals(player.getPersistentData().getString("mdvlcraft_race_copycat"))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (Buffs.has(player, "race_frost_walk") && player.onGround()) {
            FrostWalkerEnchantment.onEntityMoved(player, level, player.blockPosition(), 1);
        }
        // Chest Form ends when the player moves
        Vec3 at = CHEST_AT.get(player.getUUID());
        if (at != null && (at.distanceToSqr(player.position()) > 0.01 || !Buffs.has(player, "race_chest_form"))) {
            CHEST_AT.remove(player.getUUID());
            Display.BlockDisplay chest = CHESTS.remove(player.getUUID());
            if (chest != null) {
                chest.discard();
            }
            Buffs.clear(player, "race_chest_form");
            player.removeEffect(MobEffects.INVISIBILITY);
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        // Lantern of Souls and Lantern Swarm: a light block that follows the player
        BlockPos old = LIGHTS.get(player.getUUID());
        BlockPos here = BlockPos.containing(player.getEyePosition());
        if (old != null && (!old.equals(here) || !Buffs.has(player, LANTERN))) {
            if (level.getBlockState(old).is(Blocks.LIGHT)) {
                level.setBlockAndUpdate(old, Blocks.AIR.defaultBlockState());
            }
            LIGHTS.remove(player.getUUID());
        }
        if (Buffs.has(player, LANTERN) && level.getBlockState(here).isAir()) {
            level.setBlockAndUpdate(here, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14));
            LIGHTS.put(player.getUUID(), here);
            level.sendParticles(Buffs.has(player, "race_moths") ? ParticleTypes.WHITE_ASH : ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY(1.6), player.getZ(), 3, 0.6, 0.4, 0.6, 0.0);
        }
    }
}
