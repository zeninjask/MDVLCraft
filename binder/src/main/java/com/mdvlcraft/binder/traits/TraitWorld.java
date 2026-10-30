package com.mdvlcraft.binder.traits;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

/**
 * Traits that change the player's attributes or react to where they are: fixed modifiers (refresh, on login, respawn
 * and trait changes) and ones checked once a second (the tick).
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TraitWorld {
    private static final ResourceLocation SKILL_EXPERIENCE = ResourceLocation.fromNamespaceAndPath("puffish_attributes", "experience");
    private static final String SLEEPLESS = "mdvlcraft:trait_sleepless";
    private static final String SLEEPLESS_DAY = "mdvlcraft:trait_sleepless_day";
    private static final Map<UUID, Long> CHIMED = new HashMap<>();

    private TraitWorld() {
    }

    private static UUID id(String name) {
        return UUID.nameUUIDFromBytes(("mdvlcraft:trait/" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Sets (amount != 0) or removes a trait's transient modifier. */
    static void modifier(LivingEntity entity, @Nullable Attribute attribute, String name, double amount, Operation operation) {
        AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        UUID uuid = id(name);
        AttributeModifier current = instance.getModifier(uuid);
        if (current != null && current.getAmount() == amount && current.getOperation() == operation) {
            return;
        }
        if (current != null) {
            instance.removeModifier(uuid);
        }
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(uuid, "Trait: " + name, amount, operation));
        }
    }

    /** The fixed modifiers, set to match the player's traits. */
    static void refresh(ServerPlayer player) {
        boolean heavy = Traits.has(player, Trait.HEAVY_HANDS);
        modifier(player, Attributes.ATTACK_SPEED, "heavy_hands_speed", heavy ? -0.15 : 0.0, Operation.MULTIPLY_TOTAL);
        modifier(player, EpicFightAttributes.IMPACT.get(), "heavy_hands_impact", heavy ? 1.0 : 0.0, Operation.MULTIPLY_TOTAL);
        boolean immovable = Traits.has(player, Trait.IMMOVABLE);
        modifier(player, Attributes.MOVEMENT_SPEED, "immovable_speed", immovable ? -0.1 : 0.0, Operation.MULTIPLY_TOTAL);
        modifier(player, Attributes.KNOCKBACK_RESISTANCE, "immovable_knockback", immovable ? 1.0 : 0.0, Operation.ADDITION);
        modifier(player, EpicFightAttributes.STUN_ARMOR.get(), "immovable_stun", immovable ? 15.0 : 0.0, Operation.ADDITION);
        boolean overflow = Traits.has(player, Trait.MANA_OVERFLOW);
        modifier(player, AttributeRegistry.MAX_MANA.get(), "mana_overflow", overflow ? 0.5 : 0.0, Operation.MULTIPLY_BASE);
        modifier(player, AttributeRegistry.MANA_REGEN.get(), "mana_overflow_regen", overflow ? -0.3 : 0.0, Operation.MULTIPLY_TOTAL);
        modifier(player, AttributeRegistry.MAX_MANA.get(), "blood_price", Traits.has(player, Trait.BLOOD_PRICE) ? -0.2 : 0.0, Operation.MULTIPLY_TOTAL);
        boolean overclocked = Traits.has(player, Trait.OVERCLOCKED);
        modifier(player, Attributes.ATTACK_SPEED, "overclocked_speed", overclocked ? 0.15 : 0.0, Operation.MULTIPLY_BASE);
        modifier(player, AttributeRegistry.CAST_TIME_REDUCTION.get(), "overclocked_cast", overclocked ? 0.1 : 0.0, Operation.ADDITION);
        modifier(player, EpicFightAttributes.STAMINA_REGEN.get(), "overclocked_stamina", overclocked ? -0.3 : 0.0, Operation.MULTIPLY_TOTAL);
        boolean cursedBody = Traits.has(player, Trait.CURSED_BODY);
        modifier(player, Attributes.ARMOR, "cursed_body_armour", cursedBody ? 8.0 : 0.0, Operation.ADDITION);
        modifier(player, Attributes.ARMOR_TOUGHNESS, "cursed_body_toughness", cursedBody ? 3.0 : 0.0, Operation.ADDITION);
        modifier(player, Attributes.MAX_HEALTH, "hollow_heart", Traits.has(player, Trait.HOLLOW_HEART) ? 10.0 : 0.0, Operation.ADDITION);
        modifier(player, Attributes.MAX_HEALTH, "quicksilver", Traits.has(player, Trait.QUICKSILVER) ? -2.0 : 0.0, Operation.ADDITION);
        TraitPicks.refresh(player);
        everySecond(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            everySecond(player);
        }
        if (Traits.has(player, Trait.TREASURE_SENSE) && player.tickCount % 60 == 0) {
            treasure(player);
        }
    }

    private static void everySecond(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        // Crowd Brawler: +1 armour for each hostile within 8 (max 5); its damage is in TraitCombat
        modifier(player, Attributes.ARMOR, "crowd_brawler", Traits.has(player, Trait.CROWD_BRAWLER) ? hostiles(player, 8.0, 5) : 0.0, Operation.ADDITION);

        // Pyromaniac: slowed in water or rain
        boolean wet = player.isInWaterRainOrBubble();
        modifier(player, Attributes.MOVEMENT_SPEED, "pyromaniac", Traits.has(player, Trait.PYROMANIAC) && wet ? -0.15 : 0.0, Operation.MULTIPLY_TOTAL);

        // Cold-Blooded: fast in the cold, slow in the heat
        double cold = 0.0;
        if (Traits.has(player, Trait.COLD_BLOODED)) {
            Biome biome = level.getBiome(pos).value();
            if (biome.coldEnoughToSnow(pos)) {
                cold = 0.15;
                player.setTicksFrozen(0);
            } else if (level.dimension() == Level.NETHER || biome.getBaseTemperature() >= 1.5F) {
                cold = -0.1;
            }
        }
        modifier(player, Attributes.MOVEMENT_SPEED, "cold_blooded", cold, Operation.MULTIPLY_TOTAL);

        // Restless and Homebody: distance from the player's bed (or the world spawn)
        double home = homeDistance(player);
        boolean restless = Traits.has(player, Trait.RESTLESS) && home > 800.0;
        modifier(player, Attributes.MOVEMENT_SPEED, "restless", restless ? 0.1 : 0.0, Operation.MULTIPLY_TOTAL);
        modifier(player, ForgeRegistries.ATTRIBUTES.getValue(SKILL_EXPERIENCE), "restless_experience", restless ? 0.15 : 0.0, Operation.ADDITION);
        boolean homebody = Traits.has(player, Trait.HOMEBODY);
        modifier(player, Attributes.ARMOR, "homebody", homebody && home <= 200.0 ? 2.0 : 0.0, Operation.ADDITION);
        modifier(player, Attributes.MAX_HEALTH, "homebody_far", homebody && home > 1000.0 ? -0.1 : 0.0, Operation.MULTIPLY_TOTAL);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        // Hungry Mind: spell power as hunger falls
        double hungry = Traits.has(player, Trait.HUNGRY_MIND) ? 0.2 * Math.min(1.0, (20 - player.getFoodData().getFoodLevel()) / 17.0) : 0.0;
        modifier(player, AttributeRegistry.SPELL_POWER.get(), "hungry_mind", Math.round(hungry * 100.0) / 100.0, Operation.MULTIPLY_BASE);

        // Sleepless: a stack for each night survived without sleep
        if (Traits.has(player, Trait.SLEEPLESS)) {
            sleepless(player);
        }
        int stacks = Traits.has(player, Trait.SLEEPLESS) ? Traits.data(player).getInt(SLEEPLESS) : 0;
        modifier(player, Attributes.MOVEMENT_SPEED, "sleepless", 0.04 * stacks, Operation.MULTIPLY_TOTAL);

        // Night Owl: night vision at night
        if (Traits.has(player, Trait.NIGHT_OWL) && night(level)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }

        // Sun-Blessed: health in direct sunlight
        if (Traits.has(player, Trait.SUN_BLESSED) && sunlit(player) && player.tickCount % 80 == 0 && player.isHurt()) {
            player.heal(1.0F);
        }

        // Shadow Step: hidden from mobs more than 6 blocks away while sneaking in the dark
        if (Traits.has(player, Trait.SHADOW_STEP) && hidden(player)) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0), m -> m.getTarget() == player)) {
                if (mob.distanceToSqr(player) > 36.0) {
                    mob.setTarget(null);
                }
            }
        }

        // Cursed Body: no chestplate
        if (Traits.has(player, Trait.CURSED_BODY)) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.isEmpty()) {
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                if (!player.getInventory().add(chest)) {
                    player.drop(chest, false);
                }
            }
        }
    }

    // ---- natural regeneration (mixin/traits/FoodRegenMixin) ----

    /** Ticks to add to the food regeneration timer this tick: Night Owl slows it by day, Lone Wolf speeds it up alone. */
    public static int regenShift(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 0;
        }
        int shift = 0;
        if (Traits.has(player, Trait.NIGHT_OWL) && player.level().dimensionType().hasSkyLight() && !night(player.level())
            && player.getRandom().nextFloat() < 0.25F) {
            shift--;
        }
        if (Traits.has(player, Trait.LONE_WOLF) && !TraitCombat.inParty(serverPlayer) && player.getRandom().nextFloat() < 0.25F) {
            shift++;
        }
        return shift;
    }

    // ---- Sleepless ----

    private static void sleepless(ServerPlayer player) {
        long day = player.level().getDayTime() / 24000L;
        long time = player.level().getDayTime() % 24000L;
        var data = Traits.data(player);
        if (time >= 0 && time < 1000 && data.getLong(SLEEPLESS_DAY) != day) {
            if (data.contains(SLEEPLESS_DAY)) {
                data.putInt(SLEEPLESS, Math.min(5, data.getInt(SLEEPLESS) + 1));
            }
            data.putLong(SLEEPLESS_DAY, day);
        }
        // phantoms come from the first night: the game counts the player as three days without rest
        int rest = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        if (rest < 72000) {
            player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), 72000);
        }
    }

    public static int sleeplessStacks(ServerPlayer player) {
        return Traits.has(player, Trait.SLEEPLESS) ? Traits.data(player).getInt(SLEEPLESS) : 0;
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Traits.data(player).putInt(SLEEPLESS, 0);
        }
    }

    /** Sleepless stacks end with death (TraitCombat calls this). */
    static void clearSleepless(ServerPlayer player) {
        Traits.data(player).putInt(SLEEPLESS, 0);
    }

    // ---- Shadow Step ----

    static boolean hidden(LivingEntity player) {
        return player.isShiftKeyDown() && player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 7;
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && Traits.has(target, Trait.SHADOW_STEP) && hidden(target) && event.getEntity().distanceToSqr(target) > 36.0) {
            event.setCanceled(true);
        }
    }

    // ---- Treasure Sense ----

    /** Unopened loot containers within 24 blocks glimmer, and the nearest chimes. */
    private static void treasure(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos nearest = null;
        double best = 24.0 * 24.0;
        int cx = player.chunkPosition().x;
        int cz = player.chunkPosition().z;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                if (!level.hasChunk(x, z)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(x, z);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!unopened(be, player)) {
                        continue;
                    }
                    double d = be.getBlockPos().distToCenterSqr(player.position());
                    if (d <= 24.0 * 24.0) {
                        level.sendParticles(player, ParticleTypes.WAX_ON, true, be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 1.1,
                            be.getBlockPos().getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
                        if (d < best) {
                            best = d;
                            nearest = be.getBlockPos();
                        }
                    }
                }
            }
        }
        long now = level.getGameTime();
        if (nearest != null && now - CHIMED.getOrDefault(player.getUUID(), 0L) >= 200) {
            CHIMED.put(player.getUUID(), now);
            player.connection.send(new ClientboundSoundPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.AMBIENT,
                nearest.getX() + 0.5, nearest.getY() + 0.5, nearest.getZ() + 0.5, 0.6F, 1.2F, level.random.nextLong()));
        }
    }

    /** A loot chest whose loot hasn't been rolled yet, or a Lootr container this player hasn't opened. */
    private static boolean unopened(BlockEntity be, ServerPlayer player) {
        if (be instanceof RandomizableContainerBlockEntity container && container.saveWithoutMetadata().contains("LootTable", 8)) {
            return true;
        }
        try {
            java.lang.reflect.Method openers = be.getClass().getMethod("getOpeners");
            Object set = openers.invoke(be);
            return set instanceof java.util.Set<?> s && !s.contains(player.getUUID());
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    // ---- shared checks ----

    static int hostiles(LivingEntity of, double radius, int max) {
        return Math.min(max, of.level().getEntitiesOfClass(LivingEntity.class, of.getBoundingBox().inflate(radius),
            e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(of) <= radius * radius).size());
    }

    static boolean night(Level level) {
        long time = level.getDayTime() % 24000L;
        return level.dimensionType().hasSkyLight() && time >= 13000 && time < 23000;
    }

    static boolean sunlit(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = BlockPos.containing(entity.getEyePosition());
        return !night(level) && level.dimensionType().hasSkyLight() && !level.isRaining() && level.canSeeSky(pos)
            && level.getBrightness(LightLayer.SKY, pos) >= 14;
    }

    /** Blocks from the player's bed (or the world spawn) in this dimension, or infinity elsewhere. */
    static double homeDistance(ServerPlayer player) {
        BlockPos bed = player.getRespawnPosition();
        boolean here = bed != null ? player.getRespawnDimension() == player.level().dimension() : player.level().dimension() == Level.OVERWORLD;
        if (!here) {
            return Double.MAX_VALUE;
        }
        BlockPos home = bed != null ? bed : player.serverLevel().getSharedSpawnPos();
        return Math.sqrt(home.distToCenterSqr(player.position()));
    }

    static Vec3 eyes(LivingEntity entity) {
        return entity.getEyePosition();
    }
}
