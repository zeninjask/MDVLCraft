package com.mdvlcraft.binder.specs;

import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.modified.BuffSpells;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/** Assassin, Thief, Stalker and Wanderer specialisations (Specs), and Epic Fight dodges for Surge Step. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class RogueSpecs {
    private static final UUID SKILL_LISTENER = UUID.fromString("9d3e6f20-4a1b-4c8d-b7e5-2f9a0c3d1e47");
    private static final UUID MOUNT_SPEED = UUID.fromString("e7b2d4a6-1c3f-4e58-9a0b-6d8f2c4e1a39");
    private static final TagKey<DamageType> EXECUTION = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("combat_evolution", "execution"));
    private static final ResourceLocation SHADOW_STEP = ResourceLocation.fromNamespaceAndPath("wom", "shadow_step");
    private static final Set<String> ESCAPE_SKILLS = Set.of("epicfight:emergency_escape", "wom:ender_step", "wom:shadow_step");
    private static final Set<String> ESCAPE_SPELLS = Set.of("mdvlcraft:smoke_bomb", "irons_spellbooks:evasion", "irons_spellbooks:invisibility",
        "traveloptics:blink");
    private static final List<ItemStack> FOOD = List.of(new ItemStack(Items.BREAD), new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.BAKED_POTATO),
        new ItemStack(Items.APPLE), new ItemStack(Items.COOKED_CHICKEN), new ItemStack(Items.COOKED_PORKCHOP));

    private RogueSpecs() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            return;
        }
        patch.getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, SKILL_LISTENER, cast -> {
            Skill skill = cast.getSkillContainer().getSkill();
            if (skill == null || !cast.isExecutable()) {
                return;
            }
            if (skill.getCategory() == SkillCategories.DODGE) {
                if (MageSpecs.surgeReady(player)) {
                    MageSpecs.surge(player, 8);
                }
                if (Specs.has(player, "pocket_sand")) {
                    for (int t = 0; t <= 10; t += 2) {
                        SpecKit.later(player.serverLevel(), t, () -> {
                            for (LivingEntity e : SpecKit.enemies(player, player.position(), 2.0)) {
                                if (e instanceof Mob mob && mob.getPersistentData().getLong("mdvlcraft_blind") <= player.level().getGameTime()) {
                                    SpecKit.blind(mob, 40);
                                    player.serverLevel().sendParticles(ParticleTypes.ASH,
                                        mob.getX(), mob.getEyeY(), mob.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
                                }
                            }
                        });
                    }
                }
            }
            if (ESCAPE_SKILLS.contains(String.valueOf(skill.getRegistryName())) && Specs.has(player, "smoke_and_mirrors")) {
                SpecKit.decoy(player.serverLevel(), player, player.position(), 10.0, 60);
            }
        });
    }

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ESCAPE_SPELLS.contains(event.getSpellId()) && Specs.has(player, "smoke_and_mirrors")) {
            SpecKit.decoy(player.serverLevel(), player, player.position(), 10.0, 60);
        }
    }

    // ---- hits ----

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (victim.getPersistentData().getLong("mdvlcraft_hunted") > victim.level().getGameTime()) {
            event.setAmount(event.getAmount() * 1.1F);
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (source.is(EXECUTION) && Specs.has(player, "vanish")) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            resetSkill(player, SHADOW_STEP);
        }
        if (SpecKit.melee(source, player)) {
            if (Specs.has(player, "venomblade") && SpecKit.dagger(player)) {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
            }
            if (victim instanceof Mob mob && Specs.has(player, "light_fingers") && !mob.getPersistentData().getBoolean("mdvlcraft_robbed")
                && level.random.nextFloat() < 0.05F) {
                mob.getPersistentData().putBoolean("mdvlcraft_robbed", true);
                steal(level, player, mob, source);
            }
        }
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            String mark = "mdvlcraft_marked_" + player.getUUID();
            if (Specs.has(player, "hunter_s_mark") && !victim.getPersistentData().getBoolean(mark)) {
                victim.getPersistentData().putBoolean(mark, true);
                victim.getPersistentData().putLong("mdvlcraft_hunted", level.getGameTime() + 160);
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0), player);
            }
            if (Specs.has(player, "ricochet") && !arrow.getPersistentData().getBoolean("mdvlcraft_bounced") && level.random.nextFloat() < 0.2F) {
                arrow.getPersistentData().putBoolean("mdvlcraft_bounced", true);
                LivingEntity next = SpecKit.nearestEnemy(player, victim.position(), 6.0, victim);
                if (next != null) {
                    float damage = event.getAmount() * 0.5F;
                    SpecKit.later(level, 2, () -> {
                        next.invulnerableTime = 0;
                        next.hurt(player.damageSources().arrow(arrow, player), damage);
                        for (int i = 1; i < 8; i++) {
                            double t = i / 8.0;
                            level.sendParticles(ParticleTypes.CRIT, victim.getX() + (next.getX() - victim.getX()) * t,
                                victim.getY(0.6) + (next.getY(0.6) - victim.getY(0.6)) * t, victim.getZ() + (next.getZ() - victim.getZ()) * t, 1, 0, 0, 0, 0);
                        }
                    });
                }
            }
        }
    }

    /** One random item from the creature's own drop table, knocked out in front of it (Thief, Light Fingers). */
    private static void steal(ServerLevel level, ServerPlayer player, Mob mob, DamageSource source) {
        LootTable table = level.getServer().getLootData().getLootTable(mob.getLootTable());
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, mob)
            .withParameter(LootContextParams.ORIGIN, mob.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, source)
            .withOptionalParameter(LootContextParams.KILLER_ENTITY, player)
            .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, player)
            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
            .withLuck(player.getLuck())
            .create(LootContextParamSets.ENTITY);
        List<ItemStack> loot = table.getRandomItems(params);
        if (!loot.isEmpty()) {
            mob.spawnAtLocation(loot.get(level.random.nextInt(loot.size())));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY(0.8), mob.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /** Readies an equipped Epic Fight skill at once (Assassin, Vanish). */
    private static void resetSkill(ServerPlayer player, ResourceLocation id) {
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            return;
        }
        for (SkillContainer container : patch.getSkillCapability().skillContainers) {
            if (container.getSkill() != null && id.equals(container.getSkill().getRegistryName())) {
                Skill.setSkillConsumptionSynchronize(container, container.getMaxResource());
            }
        }
    }

    // ---- deaths, drops ----

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !dead.hasEffect(BinderEffects.DEATH_MARK.get())) {
            return;
        }
        UUID by = BuffSpells.deathMarkedBy(dead);
        if (by != null && level.getPlayerByUUID(by) instanceof ServerPlayer player && Specs.has(player, "contagious_mark")) {
            int ticks = dead.getEffect(BinderEffects.DEATH_MARK.get()).getDuration();
            LivingEntity next = SpecKit.nearestEnemy(player, dead.position(), 8.0, dead);
            if (next != null && ticks > 20) {
                BuffSpells.deathMark(next, player, ticks);
                level.sendParticles(ParticleTypes.SOUL, next.getX(), next.getY(1.0), next.getZ(), 10, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() != null && Specs.has(event.getAttackingPlayer(), "scavenger")) {
            event.setDroppedExperience(Math.round(event.getDroppedExperience() * 1.15F));
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer player && dead instanceof Mob && Specs.has(player, "scavenger")
            && player.level().random.nextFloat() < 0.10F) {
            ItemStack food = FOOD.get(player.level().random.nextInt(FOOD.size())).copy();
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), food));
        }
    }

    // ---- arrows coming back (Retrieval) ----

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow && event.getRayTraceResult() instanceof EntityHitResult hit
            && hit.getEntity() instanceof LivingEntity && arrow.getOwner() instanceof ServerPlayer player
            && arrow.pickup == AbstractArrow.Pickup.ALLOWED && Specs.has(player, "retrieval") && player.level().random.nextFloat() < 0.4F) {
            ItemStack back = new ItemStack(arrow instanceof SpectralArrow ? Items.SPECTRAL_ARROW : Items.ARROW);
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
    }

    // ---- mounts and darkness (Wanderer) ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity mount) {
            AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                boolean has = Specs.has(player, "horse_whisperer");
                if (has && speed.getModifier(MOUNT_SPEED) == null) {
                    speed.addTransientModifier(new AttributeModifier(MOUNT_SPEED, "Horse Whisperer", 0.15, AttributeModifier.Operation.MULTIPLY_BASE));
                    player.getPersistentData().putUUID("mdvlcraft_whispered", mount.getUUID());
                } else if (!has) {
                    speed.removeModifier(MOUNT_SPEED);
                }
            }
        } else if (player.getPersistentData().hasUUID("mdvlcraft_whispered")) {
            if (player.serverLevel().getEntity(player.getPersistentData().getUUID("mdvlcraft_whispered")) instanceof LivingEntity old
                && old.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                old.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(MOUNT_SPEED);
            }
            player.getPersistentData().remove("mdvlcraft_whispered");
        }

        boolean dark = player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 4;
        if (dark && Specs.has(player, "night_eyes")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
            player.getPersistentData().putBoolean("mdvlcraft_night_eyes", true);
        } else if (player.getPersistentData().getBoolean("mdvlcraft_night_eyes")) {
            MobEffectInstance vision = player.getEffect(MobEffects.NIGHT_VISION);
            if (vision != null && vision.getDuration() <= 260 && vision.isAmbient()) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            player.getPersistentData().remove("mdvlcraft_night_eyes");
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        for (Entity rider : event.getEntity().getPassengers()) {
            if (rider instanceof Player player && Specs.has(player, "horse_whisperer")) {
                event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
                return;
            }
        }
    }
}
