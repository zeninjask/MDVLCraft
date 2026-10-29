package com.mdvlcraft.binder.traits;

import app.sedu.mc.parties._lytecore.api.team.AllianceAPI;
import com.mdvlcraft.binder.combat.Crits;
import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/** The traits that act on hits, dodges, kills and deaths. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TraitCombat {
    private static final UUID LISTENER = UUID.fromString("5d0f9a62-31c4-4f8e-b0c7-2a6e7d41c9f3");
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static boolean dealing;
    private static boolean halving;

    private TraitCombat() {
    }

    /** Per-player trackers. */
    private static final class State {
        @Nullable
        LivingEntity grudge;
        int combo;
        int stillTicks;
        Vec3 lastPos = Vec3.ZERO;
        int lastSlot = -1;
        long quickdrawUntil;
        int sprintTicks;
        int adrenaline;
        long adrenalineUntil;
        long secondWindReady;
        long secondShadowReady;
        long vowUntil;
        boolean vowUsed;
        boolean vowKilled;
    }

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    private static void deal(LivingEntity target, DamageSource source, float amount) {
        boolean was = dealing;
        dealing = true;
        try {
            target.hurt(source, amount);
        } finally {
            dealing = was;
        }
    }

    // ---- hits dealt and taken ----

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (dealing || event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        float multiplier = 1.0F;
        Entity attackerEntity = source.getEntity();

        if (attackerEntity instanceof ServerPlayer player && player != victim) {
            multiplier *= dealt(player, victim, source, event);
        } else if (attackerEntity instanceof LivingEntity pet && SpecKit.owner(pet) instanceof Player owner && Traits.has(owner, Trait.BEAST_BOND)) {
            multiplier *= 1.25F;
        } else if (attackerEntity instanceof OwnableEntity pet && pet.getOwner() instanceof Player owner && Traits.has(owner, Trait.BEAST_BOND)) {
            multiplier *= 1.25F;
        }

        if (victim instanceof ServerPlayer player) {
            multiplier *= taken(player, source, event.getAmount());
        } else if (owner(victim) instanceof Player owner && Traits.has(owner, Trait.BEAST_BOND)) {
            multiplier /= 1.5F;
        }
        event.setAmount(event.getAmount() * multiplier);
    }

    @Nullable
    private static Entity owner(LivingEntity entity) {
        LivingEntity summoner = SpecKit.owner(entity);
        if (summoner != null) {
            return summoner;
        }
        return entity instanceof OwnableEntity ownable ? ownable.getOwner() : null;
    }

    /** The player's damage multiplier for this hit. */
    private static float dealt(ServerPlayer player, LivingEntity victim, DamageSource source, LivingHurtEvent event) {
        State state = state(player);
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        boolean melee = source.getDirectEntity() == player && !(source instanceof SpellDamageSource);
        boolean ownShot = melee || source.getDirectEntity() instanceof Projectile;
        float m = 1.0F;

        if (Traits.has(player, Trait.GRUDGE) && state.grudge != null && state.grudge.isAlive()) {
            m *= victim == state.grudge ? 1.3F : 0.9F;
        }
        if (Traits.has(player, Trait.DUELIST)) {
            int near = TraitWorld.hostiles(player, 12.0, 10);
            m *= near == 1 ? 1.2F : near >= 3 ? 0.85F : 1.0F;
        }
        if (Traits.has(player, Trait.CROWD_BRAWLER)) {
            m *= 1.0F + 0.05F * TraitWorld.hostiles(player, 8.0, 5);
        }
        if (Traits.has(player, Trait.FIRST_BLOOD) && victim.getHealth() >= victim.getMaxHealth()) {
            m *= 1.5F;
        }
        if (Traits.has(player, Trait.EXECUTIONERS_EYE) && victim.getHealth() < victim.getMaxHealth() * 0.2F) {
            m *= 1.35F;
        }
        if (Traits.has(player, Trait.HUNTERS_PATIENCE) && ownShot && state.stillTicks >= 40) {
            m *= 1.6F;
            state.stillTicks = 0;
        }
        if (Traits.has(player, Trait.QUICKDRAW) && melee && now <= state.quickdrawUntil) {
            m *= 1.4F;
            state.quickdrawUntil = 0;
        }
        if (Traits.has(player, Trait.MOMENTUM) && melee && state.sprintTicks >= 20) {
            m *= 1.0F + 0.1F * Math.min(4, state.sprintTicks / 20);
            state.sprintTicks = 0;
        }
        if (Traits.has(player, Trait.ADRENALINE_JUNKIE) && now <= state.adrenalineUntil) {
            m *= 1.0F + 0.06F * state.adrenaline;
        }
        if (Traits.has(player, Trait.BERSERKER_BLOOD)) {
            float missing = 1.0F - player.getHealth() / player.getMaxHealth();
            m *= 1.0F + 0.45F * Math.min(1.0F, missing / 0.9F);
        }
        if (Traits.has(player, Trait.GLASS_SOUL)) {
            m *= 1.35F;
        }
        if (Traits.has(player, Trait.PACIFISTS_GRACE)) {
            m *= 0.8F;
        }
        if (Traits.has(player, Trait.LONE_WOLF) && !inParty(player)) {
            m *= 1.12F;
        }
        if (Traits.has(player, Trait.NIGHT_OWL) && TraitWorld.night(level)) {
            m *= 1.15F;
        }
        if (Traits.has(player, Trait.SUN_BLESSED)) {
            if (TraitWorld.sunlit(player)) {
                m *= victim.getMobType() == MobType.UNDEAD ? 1.1F : 1.0F;
            } else if (TraitWorld.night(level) || !level.canSeeSky(player.blockPosition())) {
                m *= 0.9F;
            }
        }
        if (Traits.has(player, Trait.SHADOW_STEP) && level.getMaxLocalRawBrightness(player.blockPosition()) >= 13) {
            m *= 0.9F;
        }
        if (Traits.has(player, Trait.STORM_TOUCHED) && level.isThundering()) {
            m *= 1.3F;
            if (Crits.critical(victim)) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(victim.position());
                    bolt.setCause(player);
                    level.addFreshEntity(bolt);
                }
            }
        }
        if (Traits.has(player, Trait.PYROMANIAC) && player.isOnFire()) {
            m *= 1.3F;
        }
        if (Traits.has(player, Trait.RESTLESS) && TraitWorld.homeDistance(player) < 100.0) {
            m *= 0.9F;
        }
        if (Traits.has(player, Trait.HOMEBODY) && TraitWorld.homeDistance(player) <= 200.0) {
            m *= 1.15F;
        }
        int sleepless = TraitWorld.sleeplessStacks(player);
        if (sleepless > 0) {
            m *= 1.0F + 0.06F * sleepless;
        }
        if (Traits.has(player, Trait.BEAST_BOND) && source.getDirectEntity() == player) {
            m *= 0.9F;
        }
        if (Traits.has(player, Trait.GAMBLERS_HAND) && Crits.critical(victim)) {
            int a = player.getRandom().nextInt(6) + 1;
            int b = player.getRandom().nextInt(6) + 1;
            m *= (0.5F + (a + b - 2) * 0.25F) / Crits.DAMAGE;
            player.displayClientMessage(Component.translatable("trait.mdvlcraft.dice", a, b).withStyle(ChatFormatting.GOLD), true);
            if (a == 6 && b == 6) {
                ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
                if (patch != null) {
                    patch.setStamina(patch.getMaxStamina());
                }
                SpecKit.addMana(player, SpecKit.maxMana(player));
            }
        }
        if (Traits.has(player, Trait.COMBO_ARTIST) && melee && ++state.combo % 6 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY(0.5), victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            for (LivingEntity e : SpecKit.enemies(player, victim.position(), 3.0)) {
                if (e != victim) {
                    deal(e, player.damageSources().playerAttack(player), 5.0F);
                }
            }
        }
        if (Traits.has(player, Trait.SECOND_SHADOW) && melee && now >= state.secondShadowReady) {
            state.secondShadowReady = now + 400;
            float echo = event.getAmount() * m * 0.5F;
            SpecKit.later(level, 8, () -> {
                if (victim.isAlive()) {
                    level.sendParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY(0.6), victim.getZ(), 15, 0.3, 0.5, 0.3, 0.0);
                    victim.invulnerableTime = 0;
                    deal(victim, player.damageSources().playerAttack(player), echo);
                }
            });
        }
        return m;
    }

    /** The multiplier on a hit the player takes. */
    private static float taken(ServerPlayer player, DamageSource source, float amount) {
        State state = state(player);
        float m = 1.0F;
        state.combo = 0;
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player) {
            if (Traits.has(player, Trait.GRUDGE)) {
                state.grudge = attacker;
            }
            if (Traits.has(player, Trait.DUELIST) && TraitWorld.hostiles(player, 12.0, 10) == 1) {
                m *= 0.9F;
            }
            if (Traits.has(player, Trait.THORNED_HIDE) && source.getDirectEntity() == attacker && !(source instanceof SpellDamageSource)) {
                deal(attacker, player.damageSources().thorns(player), amount * 0.2F);
            }
        }
        if (Traits.has(player, Trait.THORNED_HIDE) && (source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source instanceof SpellDamageSource)) {
            m *= 1.1F;
        }
        if (Traits.has(player, Trait.PYROMANIAC) && source.is(DamageTypeTags.IS_FIRE)) {
            m *= 0.5F;
        }
        // Bodyguard: a party member within 6 blocks takes a quarter of the hit instead
        if (!dealing && source.getEntity() != null) {
            for (Player guard : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(6.0),
                p -> p != player && p.isAlive() && Traits.has(p, Trait.BODYGUARD) && allied(p, player))) {
                deal(guard, guard.damageSources().generic(), amount * m * 0.25F);
                m *= 0.75F;
                break;
            }
        }
        return m;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }
        if (Traits.has(player, Trait.GLASS_SOUL)) {
            event.setAmount(Math.max(4.0F, event.getAmount()));
        }
        State state = state(player);
        long now = player.level().getGameTime();
        float after = player.getHealth() - event.getAmount();
        if (Traits.has(player, Trait.SECOND_WIND) && after > 0.0F && after < player.getMaxHealth() * 0.25F && now >= state.secondWindReady) {
            state.secondWindReady = now + 2400;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        State state = STATES.get(player.getUUID());
        if (state != null && state.vowUntil > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        } else if (Traits.has(player, Trait.STORM_TOUCHED) && source.is(DamageTypeTags.IS_LIGHTNING)) {
            event.setCanceled(true);
        } else if (Traits.has(player, Trait.COLD_BLOODED) && source.is(DamageTypeTags.IS_FREEZING)
            && player.level().getBiome(player.blockPosition()).value().coldEnoughToSnow(player.blockPosition())) {
            event.setCanceled(true);
        }
    }

    // ---- kills and deaths ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != dead) {
            State state = STATES.get(killer.getUUID());
            if (state != null && state.vowUntil > 0) {
                state.vowKilled = true;
            }
            if (Traits.has(killer, Trait.RALLY_CRY) && dead instanceof Enemy) {
                for (Player ally : killer.level().getEntitiesOfClass(Player.class, killer.getBoundingBox().inflate(12.0),
                    p -> p.isAlive() && (p == killer || allied(p, killer)))) {
                    ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
                }
            }
        }
        if (dead instanceof ServerPlayer player) {
            State state = state(player);
            if (Traits.has(player, Trait.UNDYING_VOW) && !state.vowUsed) {
                event.setCanceled(true);
                state.vowUsed = true;
                state.vowKilled = false;
                state.vowUntil = player.level().getGameTime() + 120;
                player.setHealth(1.0F);
                player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 30, 0.4, 0.8, 0.4, 0.2);
                player.displayClientMessage(Component.translatable("trait.mdvlcraft.vow").withStyle(ChatFormatting.DARK_RED), true);
                return;
            }
            state.grudge = null;
            state.vowUntil = 0;
            TraitWorld.clearSleepless(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        State state = STATES.get(event.getEntity().getUUID());
        if (state != null) {
            state.vowUsed = false;
            state.vowUntil = 0;
        }
    }

    // ---- every tick: trackers, the vow ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        State state = state(player);
        long now = player.level().getGameTime();
        Vec3 pos = player.position();
        state.stillTicks = pos.distanceToSqr(state.lastPos) < 1.0E-4 ? state.stillTicks + 1 : 0;
        state.lastPos = pos;
        int slot = player.getInventory().selected;
        if (slot != state.lastSlot) {
            if (state.lastSlot >= 0 && !player.getMainHandItem().isEmpty()) {
                state.quickdrawUntil = now + 30;
            }
            state.lastSlot = slot;
        }
        state.sprintTicks = player.isSprinting() ? Math.min(80, state.sprintTicks + 1) : Math.max(0, state.sprintTicks - 2);
        if (now > state.adrenalineUntil) {
            state.adrenaline = 0;
        }
        if (state.vowUntil > 0 && now >= state.vowUntil) {
            state.vowUntil = 0;
            if (!state.vowKilled) {
                player.getInventory().dropAll();
                player.kill();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    // ---- Epic Fight: dodges, guard ----

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            return;
        }
        patch.getEventListener().addEventListener(EventType.DODGE_SUCCESS_EVENT, LISTENER, dodge -> {
            long now = player.level().getGameTime();
            if (Traits.has(player, Trait.ADRENALINE_JUNKIE)) {
                State state = state(player);
                state.adrenaline = Math.min(5, state.adrenaline + 1);
                state.adrenalineUntil = now + 120;
            }
            if (Traits.has(player, Trait.AFTERIMAGE)) {
                SpecKit.decoy(player.serverLevel(), player, player.position(), 8.0, 40);
            }
        });
        patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, LISTENER, consume -> {
            // Berserker Blood: below 30% health a guard can't hold (the block costs more stamina than there is)
            if (consume.getSkill() instanceof GuardSkill && Traits.has(player, Trait.BERSERKER_BLOOD)
                && player.getHealth() < player.getMaxHealth() * 0.3F) {
                consume.setAmount(Math.max(consume.getAmount(), patch.getMaxStamina() * 2.0F));
            }
        });
    }

    // ---- movement ----

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (Traits.has(event.getEntity(), Trait.FEATHERWEIGHT)) {
            event.setStrength(event.getStrength() * 1.5F);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (Traits.has(event.getEntity(), Trait.FEATHERWEIGHT) && event.getDistance() <= 8.0F) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (Traits.has(entity, Trait.FEATHERWEIGHT)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 1.1, 1.0));
        }
        if (Traits.has(entity, Trait.IMMOVABLE) && entity.isSprinting()) {
            // no sprint-jump boost: take back what the jump added along the facing
            float yaw = entity.getYRot() * ((float)Math.PI / 180F);
            entity.setDeltaMovement(entity.getDeltaMovement().add(Math.sin(yaw) * 0.2F, 0.0, -Math.cos(yaw) * 0.2F));
        }
    }

    // ---- projectiles, healing, food, loot ----

    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof ServerPlayer player
            && Traits.has(player, Trait.MIRROR_SKIN) && player.getRandom().nextFloat() < 0.12F) {
            Projectile projectile = event.getProjectile();
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.0));
            projectile.setOwner(player);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        boolean graced = Traits.has(entity, Trait.PACIFISTS_GRACE) || !entity.level().getEntitiesOfClass(Player.class,
            entity.getBoundingBox().inflate(8.0), p -> p != entity && Traits.has(p, Trait.PACIFISTS_GRACE) && allied(p, entity)).isEmpty();
        if (graced) {
            event.setAmount(event.getAmount() * 1.5F);
        }
    }

    /** Iron Stomach: poison lasts half as long (the effect is put back at half its time, once). */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance added = event.getEffectInstance();
        if (halving || added.getEffect() != MobEffects.POISON || !(event.getEntity() instanceof ServerPlayer player)
            || !Traits.has(player, Trait.IRON_STOMACH)) {
            return;
        }
        SpecKit.later(player.serverLevel(), 1, () -> {
            MobEffectInstance poison = player.getEffect(MobEffects.POISON);
            if (poison != null && poison.getDuration() > 1) {
                halving = true;
                try {
                    player.removeEffect(MobEffects.POISON);
                    player.addEffect(new MobEffectInstance(MobEffects.POISON, poison.getDuration() / 2, poison.getAmplifier(),
                        poison.isAmbient(), poison.isVisible(), poison.showIcon()));
                } finally {
                    halving = false;
                }
            }
        });
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!Traits.has(event.getEntity(), Trait.IRON_STOMACH)) {
            return;
        }
        ItemStack food = event.getItem();
        if (food.is(Items.ROTTEN_FLESH) || food.is(Items.SPIDER_EYE) || food.is(Items.CHICKEN) || food.is(Items.POISONOUS_POTATO)
            || food.is(Items.PUFFERFISH)) {
            event.getEntity().removeEffect(MobEffects.HUNGER);
            event.getEntity().removeEffect(MobEffects.POISON);
            event.getEntity().removeEffect(MobEffects.CONFUSION);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && !(event.getEntity() instanceof Player)
            && Traits.has(player, Trait.LUCKY_LOOTER) && player.getRandom().nextFloat() < 0.1F) {
            for (ItemEntity drop : new java.util.ArrayList<>(event.getDrops())) {
                ItemEntity copy = new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                copy.setDefaultPickUpDelay();
                event.getDrops().add(copy);
            }
        }
    }

    // ---- parties ----

    static boolean allied(Entity a, Entity b) {
        return a instanceof ServerPlayer pa && b instanceof ServerPlayer pb && AllianceAPI.areAllied(pa, pb);
    }

    static boolean inParty(ServerPlayer player) {
        return player.server.getPlayerList().getPlayers().stream().anyMatch(other -> other != player && AllianceAPI.areAllied(player, other));
    }
}
