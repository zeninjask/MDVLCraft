package com.mdvlcraft.binder.traits;

import com.mdvlcraft.binder.combat.Crits;
import com.mdvlcraft.binder.gate.GateDimensions;
import com.mdvlcraft.binder.mixin.traits.EntityFlagsAccessor;
import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * The second batch of traits (Trait, from SECOND_BREAKFAST on): their hits, kills, ticks and the rest. Black Flash's crit
 * is in combat/Crits, Ghost Walker's pushing in mixin/traits/GhostWalkerPushMixin, Lord of the Hunt's summon time in
 * mixin/traits/SummonDurationMixin, Second Breakfast's hunger in mixin/traits/ExhaustionMixin, Pathfinder's compass
 * range in command/QuestCompass, and Hollow Heart's and Quicksilver's health in TraitWorld.refresh.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TraitMore {
    private static final UUID LISTENER = UUID.fromString("8e3b0c57-2f6a-4d1e-9b84-6c1f0a7d3e52");
    private static final String PHOENIX = "mdvlcraft:trait_phoenix";
    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Haggler: the discounts on the offers a player has open, taken back when the window closes. */
    private static final Map<UUID, List<Discount>> DISCOUNTS = new HashMap<>();
    private static boolean carrying;
    private static boolean halving;

    private TraitMore() {
    }

    private static final class State {
        long lastCombat = -1000;
        long combatStart;
        long spiteUntil;
        int galeTicks;
        final Deque<Cast> casts = new ArrayDeque<>();
        @Nullable
        SchoolType lastSchool;
        long chainReady;
        long medicReady;
        long lastKill = -1000;
        int reaper;
        int lust;
        long lustUntil;
        long lustClock;
        boolean lustPenalty;
        int retreat;
        Vec3 retreatFrom = Vec3.ZERO;
        long retreatOffUntil;
        long timeSkipReady;
        long lastBlackFlash = -1;
    }

    private record Cast(String spell, long at) {
    }

    private record Discount(MerchantOffer offer, int amount) {
    }

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ---- hits ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (TraitCombat.dealing() || carrying || victim.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        long now = victim.level().getGameTime();
        float m = 1.0F;
        if (source.getEntity() instanceof ServerPlayer player && player != victim) {
            State state = state(player);
            combat(state, now);
            boolean melee = source.getDirectEntity() == player && !(source instanceof SpellDamageSource);
            if (Traits.has(player, Trait.BRAWLER) && melee) {
                ItemStack hand = player.getMainHandItem();
                CapabilityItem item = EpicFightCapabilities.getItemStackCapability(hand);
                if (hand.isEmpty() || item.getWeaponCategory() == CapabilityItem.WeaponCategories.FIST) {
                    event.setAmount(event.getAmount() + 3.0F);
                    SpecKit.stun(victim, 12);
                } else if (twoHanded(player)) {
                    m *= 0.9F;
                }
            }
            if (Traits.has(player, Trait.SPITEFUL) && now <= state.spiteUntil) {
                m *= 1.4F;
                state.spiteUntil = 0;
            }
            if (Traits.has(player, Trait.REAPERS_DUE) && now - state.lastKill <= 200) {
                m *= 1.0F + 0.05F * state.reaper;
            }
            if (Traits.has(player, Trait.VOW_OF_THE_SHIELD)) {
                m *= 0.7F;
            }
            if (Traits.has(player, Trait.VOW_OF_NO_RETREAT)) {
                m *= 1.0F + 0.04F * state.retreat;
            }
            if (Traits.has(player, Trait.BLACK_FLASH)) {
                if (state.lastBlackFlash < 0) {
                    state.lastBlackFlash = now;
                }
                if (now - state.lastBlackFlash > 400 && !Crits.critical(victim)) {
                    m *= 0.85F;
                }
            }
        }
        if (victim instanceof ServerPlayer player) {
            State state = state(player);
            if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
                combat(state, now);
                if (Traits.has(player, Trait.VOW_OF_NO_RETREAT) && now >= state.retreatOffUntil) {
                    if (state.retreat == 0) {
                        state.retreatFrom = player.position();
                    }
                    state.retreat = Math.min(10, state.retreat + 1);
                }
            }
            if (Traits.has(player, Trait.WARDED)) {
                if (source instanceof SpellDamageSource || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
                    m *= 0.85F;
                } else if (source.getDirectEntity() instanceof AbstractArrow) {
                    m *= 1.1F;
                }
            }
            if (Traits.has(player, Trait.BLOODLUST) && state.lustPenalty) {
                m *= 1.2F;
            }
            if (Traits.has(player, Trait.GALE_RUNNER)) {
                state.galeTicks = 0;
                MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
                if (speed != null && speed.getAmplifier() == 0 && speed.getDuration() <= 10) {
                    player.removeEffect(MobEffects.MOVEMENT_SPEED);
                }
            }
            if (source.getEntity() != null) {
                for (Player guard : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8.0),
                    p -> p != player && Traits.has(p, Trait.VOW_OF_THE_SHIELD) && TraitCombat.allied(p, player) && guarding(p))) {
                    m *= 0.7F;
                    break;
                }
            }
        }
        event.setAmount(event.getAmount() * m);
    }

    private static void combat(State state, long now) {
        if (now - state.lastCombat > 100) {
            state.combatStart = now;
        }
        state.lastCombat = now;
    }

    private static boolean inCombat(State state, long now) {
        return now - state.lastCombat <= 100;
    }

    /** Vow of the Shield: a shield in either hand, or Epic Fight's guard held. */
    private static boolean guarding(Player player) {
        if (player.getMainHandItem().getItem() instanceof ShieldItem || player.getOffhandItem().getItem() instanceof ShieldItem) {
            return true;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        return patch != null && patch.getHoldingSkill() instanceof GuardSkill;
    }

    private static boolean twoHanded(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        CapabilityItem item = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
        return patch != null && !item.isEmpty() && item.getStyle(patch) == CapabilityItem.Styles.TWO_HAND;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }
        if (victim instanceof ServerPlayer player && Traits.has(player, Trait.SPITEFUL)) {
            float after = player.getHealth() - event.getAmount();
            if (player.getHealth() >= player.getMaxHealth() * 0.5F && after > 0.0F && after < player.getMaxHealth() * 0.5F) {
                state(player).spiteUntil = player.level().getGameTime() + 80;
            }
        }
    }

    /** Overflowing Soul: a spell's damage past the kill goes on to the nearest enemy. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (carrying || !(event.getSource() instanceof SpellDamageSource) || !(event.getSource().getEntity() instanceof ServerPlayer player)
            || !Traits.has(player, Trait.OVERFLOWING_SOUL)) {
            return;
        }
        float overkill = event.getAmount() - victim.getHealth();
        if (overkill < 0.5F) {
            return;
        }
        LivingEntity next = SpecKit.nearestEnemy(player, victim.position(), 6.0, victim);
        if (next != null) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.SOUL, next.getX(), next.getY(0.6), next.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
            carrying = true;
            try {
                next.invulnerableTime = 0;
                next.hurt(player.damageSources().indirectMagic(player, player), overkill);
            } finally {
                carrying = false;
            }
        }
    }

    /** Black Flash (from Crits): a crit looks and sounds like Focus's Black Flash. */
    public static void blackFlash(Player player, LivingEntity victim) {
        state(player).lastBlackFlash = victim.level().getGameTime();
        if (player instanceof ServerPlayer serverPlayer) {
            com.mdvlcraft.binder.ability.MonkArts.blackFlashEffects(serverPlayer, victim);
        }
    }

    // ---- kills and deaths ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }
        long now = dead.level().getGameTime();
        if (dead instanceof ServerPlayer player && Traits.has(player, Trait.PHOENIX)) {
            var data = Traits.data(player);
            if (now >= data.getLong(PHOENIX) || data.getLong(PHOENIX) - now > 24000) {
                event.setCanceled(true);
                data.putLong(PHOENIX, now + 24000);
                player.setHealth(player.getMaxHealth() * 0.5F);
                ServerLevel level = player.serverLevel();
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(0.5), player.getZ(), 60, 0.8, 1.0, 0.8, 0.08);
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
                for (LivingEntity e : SpecKit.enemies(player, player.position(), 6.0)) {
                    e.setSecondsOnFire(5);
                }
                player.displayClientMessage(Component.translatable("trait.mdvlcraft.phoenix").withStyle(ChatFormatting.GOLD), true);
                return;
            }
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == dead) {
            return;
        }
        State state = state(killer);
        if (Traits.has(killer, Trait.REAPERS_DUE)) {
            state.reaper = now - state.lastKill <= 200 ? Math.min(6, state.reaper + 1) : 1;
        }
        state.lastKill = now;
        if (Traits.has(killer, Trait.LEECH_BLADE) && event.getSource().getDirectEntity() == killer) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(killer, ServerPlayerPatch.class);
            if (patch != null) {
                patch.setStamina(Math.min(patch.getMaxStamina(), patch.getStamina() + 2.0F));
            }
        }
        if (Traits.has(killer, Trait.BLOODLUST)) {
            state.lust = Math.min(5, state.lust + 1);
            state.lustUntil = now + 160;
            state.lustClock = now;
            state.lustPenalty = false;
            TraitWorld.modifier(killer, Attributes.ATTACK_SPEED, "bloodlust", 0.1 * state.lust, Operation.MULTIPLY_BASE);
        }
        if (Traits.has(killer, Trait.CHAIN_CASTER) && event.getSource() instanceof SpellDamageSource spell && spell.spell() != null
            && now >= state.chainReady) {
            MagicData magic = MagicData.getPlayerMagicData(killer);
            if (magic.getPlayerCooldowns().removeCooldown(spell.spell().getSpellId())) {
                magic.getPlayerCooldowns().syncToPlayer(killer);
                state.chainReady = now + 400;
                killer.serverLevel().sendParticles(ParticleTypes.ENCHANT, killer.getX(), killer.getY(1.0), killer.getZ(), 20, 0.4, 0.5, 0.4, 0.5);
            }
        }
    }

    /** Martyr: a death that went through (nothing saved the player) heals the party. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDied(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Traits.has(player, Trait.MARTYR)) {
            for (Player ally : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(16.0),
                p -> p != player && p.isAlive() && TraitCombat.allied(p, player))) {
                ally.setHealth(ally.getMaxHealth());
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
                player.serverLevel().sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0), ally.getZ(), 8, 0.4, 0.5, 0.4, 0.0);
            }
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() != null && Traits.has(event.getAttackingPlayer(), Trait.SCAVENGER) && !(event.getEntity() instanceof Player)) {
            event.setDroppedExperience(event.getDroppedExperience() + 3);
        }
    }

    // ---- every tick ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        State state = state(player);
        long now = player.level().getGameTime();
        if (Traits.has(player, Trait.GALE_RUNNER)) {
            state.galeTicks = player.isSprinting() ? state.galeTicks + 1 : 0;
            if (state.galeTicks >= 60) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 0, true, false));
            }
        }
        if (Traits.has(player, Trait.SWIMMER) && player.isUnderWater() && player.tickCount % 3 == 0
            && player.getAirSupply() < player.getMaxAirSupply()) {
            player.setAirSupply(player.getAirSupply() + 1);
        }
        if (player.tickCount % 20 == 0) {
            everySecond(player, state, now);
        }
        if (Traits.has(player, Trait.KEEN_EARS) && player.tickCount % 200 == 0) {
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0),
                e -> e instanceof Enemy && e.isAlive() && !player.hasLineOfSight(e))) {
                glow(player, e, 20);
            }
        }
    }

    private static void everySecond(ServerPlayer player, State state, long now) {
        ServerLevel level = player.serverLevel();
        boolean fighting = inCombat(state, now);

        // Pack Mule: fast with a light pack, armoured with a full one
        int used = 0;
        for (ItemStack stack : player.getInventory().items) {
            used += stack.isEmpty() ? 0 : 1;
        }
        boolean mule = Traits.has(player, Trait.PACK_MULE);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "pack_mule", mule && used < 10 ? 0.15 : 0.0, Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, Attributes.ARMOR, "pack_mule_armour", mule && used > 30 ? 2.0 : 0.0, Operation.ADDITION);

        // Swimmer: fast in water, slow on desert sand
        boolean swimmer = Traits.has(player, Trait.SWIMMER);
        TraitWorld.modifier(player, ForgeMod.SWIM_SPEED.get(), "swimmer", swimmer ? 0.3 : 0.0, Operation.ADDITION);
        boolean desert = swimmer && !player.isInWater() && level.getBiome(player.blockPosition()).is(Tags.Biomes.IS_DESERT);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "swimmer_desert", desert ? -0.05 : 0.0, Operation.MULTIPLY_TOTAL);

        // Fleet Foot, Battle Trance, Last Stand
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "fleet_foot", Traits.has(player, Trait.FLEET_FOOT) && !fighting ? 0.08 : 0.0,
            Operation.MULTIPLY_TOTAL);
        boolean trance = Traits.has(player, Trait.BATTLE_TRANCE) && fighting && now - state.combatStart >= 200;
        TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "battle_trance", trance ? 0.15 : 0.0, Operation.MULTIPLY_BASE);
        boolean lastStand = Traits.has(player, Trait.LAST_STAND) && player.getHealth() < player.getMaxHealth() * 0.2F;
        TraitWorld.modifier(player, Attributes.ARMOR, "last_stand", lastStand ? 4.0 : 0.0, Operation.ADDITION);

        // Counterweight: two-handers swing faster, one-handers reach further
        boolean counter = Traits.has(player, Trait.COUNTERWEIGHT) && !player.getMainHandItem().isEmpty();
        boolean twoHands = counter && twoHanded(player);
        boolean weapon = counter && !EpicFightCapabilities.getItemStackCapability(player.getMainHandItem()).isEmpty();
        TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "counterweight", twoHands ? 0.1 : 0.0, Operation.MULTIPLY_BASE);
        TraitWorld.modifier(player, ForgeMod.ENTITY_REACH.get(), "counterweight_reach", weapon && !twoHands ? 1.0 : 0.0, Operation.ADDITION);

        // Pathfinder: the party near a pathfinder moves faster
        boolean led = !level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(16.0),
            p -> Traits.has(p, Trait.PATHFINDER) && (p == player || TraitCombat.allied(p, player))).isEmpty();
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "pathfinder", led ? 0.1 : 0.0, Operation.MULTIPLY_TOTAL);

        // Bloodlust: the stacks run out; idle in a gate or a structure, hits hurt more
        if (Traits.has(player, Trait.BLOODLUST)) {
            if (now > state.lustUntil && state.lust > 0) {
                state.lust = 0;
                TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "bloodlust", 0.0, Operation.MULTIPLY_BASE);
            }
            if (!dungeon(player)) {
                state.lustClock = now;
                state.lustPenalty = false;
            } else if (now - state.lustClock > 1200 && !state.lustPenalty) {
                state.lustPenalty = true;
                player.displayClientMessage(Component.translatable("trait.mdvlcraft.bloodlust").withStyle(ChatFormatting.DARK_RED), true);
            }
        } else if (state.lust > 0 || state.lustPenalty) {
            state.lust = 0;
            state.lustPenalty = false;
            TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "bloodlust", 0.0, Operation.MULTIPLY_BASE);
        }

        // Vow of No Retreat: the fight ends after 10 quiet seconds; leaving its ground breaks the vow
        if (state.retreat > 0) {
            if (now - state.lastCombat > 200) {
                state.retreat = 0;
            } else if (player.position().distanceTo(state.retreatFrom) > 24.0) {
                state.retreat = 0;
                state.retreatOffUntil = now + 12000;
                player.displayClientMessage(Component.translatable("trait.mdvlcraft.vow_broken").withStyle(ChatFormatting.YELLOW), true);
            }
        }

        // Light Sleeper: a hostile close by wakes the player
        if (Traits.has(player, Trait.LIGHT_SLEEPER) && player.isSleeping() && TraitWorld.hostiles(player, 8.0, 1) > 0) {
            player.stopSleepInBed(true, true);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        }

        // Green Thumb: extra growth ticks for the crops around the player
        if (Traits.has(player, Trait.GREEN_THUMB)) {
            for (int i = 0; i < 10; i++) {
                BlockPos pos = player.blockPosition().offset(level.random.nextInt(17) - 8, level.random.nextInt(9) - 4, level.random.nextInt(17) - 8);
                BlockState block = level.getBlockState(pos);
                if (block.getBlock() instanceof BonemealableBlock && block.isRandomlyTicking()) {
                    block.randomTick(level, pos, level.random);
                }
            }
        }

        // Bloodhound: wounded creatures, and what the party is fighting, glow for this player
        if (Traits.has(player, Trait.BLOODHOUND)) {
            int shown = 0;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32.0),
                e -> !(e instanceof Player) && e.isAlive() && e.getHealth() < e.getMaxHealth() * 0.5F)) {
                if (shown++ < 30) {
                    glow(player, e, 22);
                }
            }
            for (Player ally : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(32.0), p -> p != player && TraitCombat.allied(p, player))) {
                LivingEntity target = ally.getLastHurtMob();
                if (target != null && target.isAlive() && ally.tickCount - ally.getLastHurtMobTimestamp() < 100 && target.distanceToSqr(player) < 32.0 * 32.0) {
                    glow(player, target, 22);
                }
            }
        }
    }

    /** In a gate, or inside a structure's piece (villages don't count). */
    private static boolean dungeon(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (GateDimensions.isGate(level.dimension())) {
            return true;
        }
        BlockPos pos = player.blockPosition();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            if (!registry.wrapAsHolder(structure).is(StructureTags.VILLAGE) && level.structureManager().getStructureWithPieceAt(pos, structure).isValid()) {
                return true;
            }
        }
        return false;
    }

    /** Makes `entity` glow for `viewer` only, for `ticks`, by sending the viewer its flags with the glowing bit set. */
    private static void glow(ServerPlayer viewer, LivingEntity entity, int ticks) {
        var flags = EntityFlagsAccessor.mdvlcraft$flags();
        byte real = entity.getEntityData().get(flags);
        viewer.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), List.of(SynchedEntityData.DataValue.create(flags, (byte)(real | 0x40)))));
        SpecKit.later(viewer.serverLevel(), ticks, () -> {
            if (!entity.isRemoved() && viewer.connection != null) {
                viewer.connection.send(new ClientboundSetEntityDataPacket(entity.getId(),
                    List.of(SynchedEntityData.DataValue.create(flags, entity.getEntityData().get(flags)))));
            }
        });
    }

    // ---- Epic Fight: parries, guard, dodges ----

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof AbstractArrow arrow && !event.loadedFromDisk() && !event.getLevel().isClientSide
            && arrow.getOwner() instanceof ServerPlayer shooter && shooter.isShiftKeyDown() && Traits.has(shooter, Trait.STEADY_AIM)) {
            // Steady Aim: half of the spread is taken out, and the arrow flies 25% faster
            Vec3 v = arrow.getDeltaMovement();
            Vec3 aim = v.normalize().add(shooter.getLookAngle()).normalize();
            arrow.setDeltaMovement(aim.scale(v.length() * 1.25));
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            return;
        }
        patch.getEventListener().addEventListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, LISTENER, attack -> {
            if (attack.isParried() && Traits.has(player, Trait.RIPOSTE)) {
                Crits.sureUntil(player, player.level().getGameTime() + 30);
            }
        });
        patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, LISTENER, consume -> {
            if (consume.getSkill() instanceof GuardSkill) {
                if (Traits.has(player, Trait.OVERGUARD) && patch.getStamina() >= patch.getMaxStamina() - 0.01F) {
                    consume.setAmount(0.0F);
                } else if (Traits.has(player, Trait.LAST_STAND) && player.getHealth() < player.getMaxHealth() * 0.2F) {
                    consume.setAmount(consume.getAmount() / 1.2F);
                }
            } else if (consume.getSkill().getCategory() == SkillCategories.DODGE && Traits.has(player, Trait.QUICKSILVER)) {
                consume.setAmount(consume.getAmount() * 0.7F);
            }
        });
        patch.getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, LISTENER, cast -> {
            var skill = cast.getSkillContainer().getSkill();
            if (skill != null && cast.isExecutable() && skill.getCategory() == SkillCategories.DODGE && Traits.has(player, Trait.TIME_SKIP)) {
                timeSkip(player);
            }
        });
    }

    /** Time Skip: every 30 s, a dodge jumps 5 blocks along its way and slows the enemies at both ends. */
    private static void timeSkip(ServerPlayer player) {
        State state = state(player);
        long now = player.level().getGameTime();
        if (now < state.timeSkipReady) {
            return;
        }
        state.timeSkipReady = now + 600;
        Vec3 move = player.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        Vec3 dir = move.lengthSqr() > 1.0E-4 ? move.normalize() : player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 from = player.position();
        Vec3 to = from;
        for (double d = 0.5; d <= 5.0; d += 0.5) {
            Vec3 spot = from.add(dir.scale(d));
            AABB box = player.getBoundingBox().move(spot.subtract(from));
            if (!player.level().noCollision(player, box)) {
                break;
            }
            to = spot;
        }
        ServerLevel level = player.serverLevel();
        for (Vec3 end : new Vec3[]{from, to}) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, end.x, end.y + 1.0, end.z, 25, 0.3, 0.6, 0.3, 0.05);
            for (LivingEntity e : SpecKit.enemies(player, end, 4.0)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
        }
        player.teleportTo(to.x, to.y, to.z);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    // ---- spells ----

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State state = state(player);
        long now = player.level().getGameTime();
        if (Traits.has(player, Trait.SPELL_WEAVER)) {
            if (state.lastSchool != null && state.lastSchool != event.getSchoolType()) {
                event.setManaCost(Math.round(event.getManaCost() * 0.85F));
            }
            state.lastSchool = event.getSchoolType();
        }
        if (Traits.has(player, Trait.ARCANE_RHYTHM)) {
            state.casts.removeIf(c -> now - c.at > 200 || c.spell.equals(event.getSpellId()));
            state.casts.addLast(new Cast(event.getSpellId(), now));
            if (state.casts.size() >= 3) {
                state.casts.clear();
                float refund = event.getManaCost();
                SpecKit.later(player.serverLevel(), 1, () -> SpecKit.addMana(player, refund));
                player.serverLevel().sendParticles(ParticleTypes.NOTE, player.getX(), player.getY(1.2), player.getZ(), 6, 0.4, 0.3, 0.4, 1.0);
            }
        }
    }

    /** Storm Caller: lightning spells jump to one more enemy close by. */
    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (carrying || !(event.getSpellDamageSource().getEntity() instanceof ServerPlayer player) || !Traits.has(player, Trait.STORM_CALLER)
            || event.getSpellDamageSource().spell() == null || event.getSpellDamageSource().spell().getSchoolType() != SchoolRegistry.LIGHTNING.get()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        LivingEntity next = SpecKit.nearestEnemy(player, victim.position(), 6.0, victim);
        if (next == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 a = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
        Vec3 b = next.position().add(0.0, next.getBbHeight() * 0.5, 0.0);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = a.lerp(b, i / 8.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        float amount = event.getAmount() * 0.6F;
        SpecKit.later(level, 2, () -> {
            if (next.isAlive()) {
                carrying = true;
                try {
                    next.invulnerableTime = 0;
                    next.hurt(event.getSpellDamageSource(), amount);
                } finally {
                    carrying = false;
                }
            }
        });
    }

    // ---- food, effects, healing ----

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && Traits.has(player, Trait.SECOND_BREAKFAST)) {
            FoodProperties food = event.getItem().getFoodProperties(player);
            if (food != null) {
                player.getFoodData().eat(Math.max(1, Math.round(food.getNutrition() * 0.2F)), food.getSaturationModifier());
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == MobEffects.CONFUSION && Traits.has(event.getEntity(), Trait.THICK_SKULL)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Thick Skull: blindness lasts half as long (put back at half its time, once). */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (halving || event.getEffectInstance().getEffect() != MobEffects.BLINDNESS || !(event.getEntity() instanceof ServerPlayer player)
            || !Traits.has(player, Trait.THICK_SKULL)) {
            return;
        }
        SpecKit.later(player.serverLevel(), 1, () -> {
            MobEffectInstance blind = player.getEffect(MobEffects.BLINDNESS);
            if (blind != null && blind.getDuration() > 1) {
                halving = true;
                try {
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blind.getDuration() / 2, blind.getAmplifier(),
                        blind.isAmbient(), blind.isVisible(), blind.showIcon()));
                } finally {
                    halving = false;
                }
            }
        });
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!event.getEntity().level().isClientSide && Traits.has(event.getEntity(), Trait.HOLLOW_HEART)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }

    // ---- the world: spawns, crops, chests, trades, broken tools ----

    /** Light Sleeper: no hostile spawns within 24 blocks of a sleeping light sleeper. */
    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL || !(event.getEntity() instanceof Enemy)) {
            return;
        }
        for (Player player : event.getLevel().getLevel().players()) {
            if (player.isSleeping() && Traits.has(player, Trait.LIGHT_SLEEPER) && player.distanceToSqr(event.getX(), event.getY(), event.getZ()) < 24.0 * 24.0) {
                event.setSpawnCancelled(true);
                return;
            }
        }
    }

    /** Green Thumb: a grown crop sometimes drops one more of its harvest. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (event.getLevel() instanceof ServerLevel level && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
            && Traits.has(event.getPlayer(), Trait.GREEN_THUMB) && level.random.nextFloat() < 0.15F) {
            List<ItemStack> drops = Block.getDrops(state, level, event.getPos(), null);
            if (!drops.isEmpty()) {
                Block.popResource(level, event.getPos(), drops.get(0).copyWithCount(1));
            }
        }
    }

    /** Ghost Walker: no chests while a hostile is within 16 blocks. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !Traits.has(player, Trait.GHOST_WALKER)) {
            return;
        }
        BlockEntity be = player.level().getBlockEntity(event.getPos());
        if (be instanceof Container && TraitWorld.hostiles(player, 16.0, 1) > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            player.displayClientMessage(Component.translatable("trait.mdvlcraft.ghost_walker.chest").withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    /** Field Medic: sneak-use an ally with an empty hand to heal them 6 health over 3 s. */
    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer medic) || event.getHand() != InteractionHand.MAIN_HAND || !medic.isShiftKeyDown()
            || !medic.getMainHandItem().isEmpty() || !(event.getTarget() instanceof ServerPlayer patient) || !Traits.has(medic, Trait.FIELD_MEDIC)
            || !TraitCombat.allied(medic, patient)) {
            return;
        }
        State state = state(medic);
        long now = medic.level().getGameTime();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (now < state.medicReady) {
            medic.displayClientMessage(Component.translatable("trait.mdvlcraft.field_medic.wait", (state.medicReady - now + 19) / 20)
                .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        state.medicReady = now + 600;
        for (int t = 0; t <= 40; t += 20) {
            SpecKit.later(medic.serverLevel(), t, () -> {
                if (patient.isAlive()) {
                    patient.heal(2.0F);
                    medic.serverLevel().sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY(1.0), patient.getZ(), 3, 0.3, 0.4, 0.3, 0.0);
                }
            });
        }
    }

    /** Haggler: 10% off every offer while the trade window is open (put back when it closes; villagers reset their own). */
    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        if (!(event.getContainer() instanceof MerchantMenu menu) || !Traits.has(event.getEntity(), Trait.HAGGLER)) {
            return;
        }
        List<Discount> given = new ArrayList<>();
        for (MerchantOffer offer : menu.getOffers()) {
            int off = Math.round(offer.getBaseCostA().getCount() * 0.1F);
            if (off > 0) {
                offer.addToSpecialPriceDiff(-off);
                given.add(new Discount(offer, off));
            }
        }
        DISCOUNTS.put(event.getEntity().getUUID(), given);
    }

    @SubscribeEvent
    public static void onClose(PlayerContainerEvent.Close event) {
        List<Discount> given = DISCOUNTS.remove(event.getEntity().getUUID());
        if (given != null) {
            for (Discount d : given) {
                if (d.offer.getSpecialPriceDiff() != 0) {
                    d.offer.addToSpecialPriceDiff(d.amount);
                }
            }
        }
    }

    /** Scavenger: a broken weapon, tool or armour piece leaves two of what it is repaired with. */
    @SubscribeEvent
    public static void onBroken(PlayerDestroyItemEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !Traits.has(player, Trait.SCAVENGER)) {
            return;
        }
        ItemStack broken = event.getOriginal();
        Ingredient repair = broken.getItem() instanceof TieredItem tiered ? tiered.getTier().getRepairIngredient()
            : broken.getItem() instanceof ArmorItem armour ? armour.getMaterial().getRepairIngredient() : Ingredient.EMPTY;
        ItemStack[] items = repair.getItems();
        if (items.length > 0) {
            ItemStack scrap = items[0].copyWithCount(2);
            if (!player.getInventory().add(scrap)) {
                player.drop(scrap, false);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
        DISCOUNTS.remove(event.getEntity().getUUID());
    }
}
