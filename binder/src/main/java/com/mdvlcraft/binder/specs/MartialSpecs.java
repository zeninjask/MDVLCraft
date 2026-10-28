package com.mdvlcraft.binder.specs;

import com.mdvlcraft.binder.gate.GateSpawner;
import com.mdvlcraft.binder.modified.Buffs;
import com.mdvlcraft.binder.modified.OriginJobB;
import com.mdvlcraft.binder.modified.Wave2Guardian;
import com.mdvlcraft.binder.mixin.ThrownTridentAccessor;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/** Martial class specialisations (Specs) that aren't inside the class abilities themselves. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MartialSpecs {
    private static final UUID SKILL_LISTENER = UUID.fromString("5b2e8c1d-4f7a-4e3b-9d6c-1a8f3e2b7c90");
    private static final UUID BANNER = UUID.fromString("9c4d2e1f-7a3b-4c5d-8e6f-2b1a0c9d8e7f");
    private static final UUID SEA_LEGS = UUID.fromString("3f8a1b2c-6d4e-4f5a-9b7c-8e2d1a0f3c6b");
    /** The last melee attacker of each player this tick (Counterweight's Epic Fight guard has no source). */
    private static final Map<UUID, LivingEntity> LAST_ATTACKER = new HashMap<>();
    /** When each player last moved (Rooted). */
    private static final Map<UUID, Long> LAST_MOVE = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();

    private MartialSpecs() {
    }

    // ---- Epic Fight: Kite (free dodge), Counterweight (guard) ----

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch != null) {
                patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, SKILL_LISTENER, consume -> {
                    if (consume.getSkill() instanceof DodgeSkill && Specs.has(player, "kite") && Buffs.has(player, "kite")) {
                        consume.setAmount(0.0F);   // Kite: the speed boost's next dodge is free
                        Buffs.clear(player, "kite");
                    } else if (consume.getSkill() instanceof GuardSkill && Specs.has(player, "counterweight")) {
                        LivingEntity attacker = LAST_ATTACKER.get(player.getUUID());
                        if (attacker != null && attacker.isAlive()) {
                            Wave2Guardian.knock(player.position(), attacker, 3.0);
                        }
                    }
                });
            }
        } else if (entity instanceof ThrownTrident trident && !event.loadedFromDisk() && trident.getOwner() instanceof Player owner
            && Specs.has(owner, "return_to_hand")) {
            trident.getEntityData().set(ThrownTridentAccessor.mdvlcraft$loyalty(), (byte)3);   // Return to Hand
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().getEntity() instanceof LivingEntity attacker
            && event.getSource().getDirectEntity() == attacker) {
            LAST_ATTACKER.put(player.getUUID(), attacker);
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (event.getEntity() instanceof Player player && Specs.has(player, "counterweight")
            && event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker) {
            Wave2Guardian.knock(player.position(), attacker, 3.0);
        }
    }

    // ---- knockback: Hold the Gate, Rooted ----

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (Specs.has(player, "hold_the_gate")) {
            for (LivingEntity mob : SpecKit.enemies(player, player.position(), 16.0)) {
                if (Buffs.markedBy(mob, "challenge") == player) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (Specs.has(player, "rooted") && player.level().getGameTime() - LAST_MOVE.getOrDefault(player.getUUID(), 0L) >= 40) {
            event.setCanceled(true);
        }
    }

    // ---- hits ----

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        float amount = event.getAmount();
        if (source instanceof Player attacker) {
            if (Buffs.has(attacker, "ambusher") && Specs.has(attacker, "ambusher")) {
                amount *= 1.3F;   // Ambusher
                Buffs.clear(attacker, "ambusher");
            }
            if (Specs.has(attacker, "giant_slayer") && (victim.getType().is(Tags.EntityTypes.BOSSES) || GateSpawner.isBoss(victim))) {
                amount *= 1.1F;
            }
            if (direct instanceof AbstractArrow arrow && arrow.shotFromCrossbow() && Specs.has(attacker, "heavy_bolts")) {
                Wave2Guardian.knock(attacker.position(), victim, 2.0);
            }
        }
        if (victim instanceof Player player) {
            if (Specs.has(player, "honour_duel") && source instanceof LivingEntity enemy
                && SpecKit.enemies(player, enemy.position(), 8.0).stream().noneMatch(e -> e != enemy)) {
                amount *= 0.9F;
            }
            if (Specs.has(player, "mana_ward") && player instanceof ServerPlayer server) {
                float share = amount * 0.1F;
                if (MagicData.getPlayerMagicData(server).getMana() >= share) {
                    SpecKit.addMana(server, -share);
                    amount -= share;
                }
            }
        }
        LivingEntity rider = victim.getControllingPassenger();
        if (rider != null && Specs.has(rider, "barding")) {
            amount *= 0.7F;
        }
        event.setAmount(amount);
    }

    /** Bring Them In Alive: a hit that leaves an enemy under 20% health slows it for 2 s. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof Player attacker && Specs.has(attacker, "bring_them_in_alive")
            && victim.getHealth() - event.getAmount() < victim.getMaxHealth() * 0.2F && victim.getHealth() > event.getAmount()) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
    }

    // ---- kills ----

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level() instanceof ServerLevel level && Buffs.has(victim, "pyre_of_faith") && victim.isOnFire()) {
            LivingEntity marker = Buffs.markedBy(victim, "pyre_of_faith");   // Pyre of Faith: burning victims explode
            if (marker != null) {
                for (LivingEntity enemy : SpecKit.enemies(marker, victim.position(), 3.0)) {
                    if (enemy != victim) {
                        enemy.invulnerableTime = 0;
                        enemy.hurt(marker.damageSources().indirectMagic(victim, marker), 4.0F);
                    }
                }
                level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY(0.5), victim.getZ(), 30, 1.0, 0.6, 1.0, 0.05);
            }
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (Specs.has(player, "bloodied_banner")) {
            for (LivingEntity ally : SpecKit.allies(player, player.position(), 8.0)) {
                OriginJobB.modify(ally, Attributes.ATTACK_DAMAGE, BANNER, "bloodied_banner", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL, 120);
            }
            OriginJobB.modify(player, Attributes.ATTACK_DAMAGE, BANNER, "bloodied_banner", 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL, 120);
        }
        if (Specs.has(player, "quick_draw") && event.getSource().getDirectEntity() instanceof AbstractArrow && player.getMainHandItem().getItem() instanceof BowItem) {
            Buffs.give(player, "quick_draw", 200);
        }
        if (Specs.has(player, "scavenged_blades")) {
            MagicData magic = MagicData.getPlayerMagicData(player);
            if (magic.getPlayerCooldowns().removeCooldown("mdvlcraft:throwing_knives")) {
                magic.getPlayerCooldowns().syncToPlayer(player);
            }
        }
        if (Specs.has(player, "practised_hands")) {
            reload(player);
        }
    }

    /** Practised Hands: loads one arrow into an empty main-hand crossbow from the inventory. */
    private static void reload(ServerPlayer player) {
        ItemStack crossbow = player.getMainHandItem();
        if (!(crossbow.getItem() instanceof CrossbowItem) || CrossbowItem.isCharged(crossbow)) {
            return;
        }
        ItemStack ammo = player.getProjectile(crossbow);
        if (ammo.isEmpty()) {
            return;
        }
        ItemStack shot = ammo.copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            ammo.shrink(1);
        }
        ListTag loaded = new ListTag();
        loaded.add(shot.save(new CompoundTag()));
        crossbow.getOrCreateTag().put("ChargedProjectiles", loaded);
        CrossbowItem.setCharged(crossbow, true);
    }

    /** Skinning Knife: a monster's drops come twice, 10% of the time. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Enemy && event.getSource().getEntity() instanceof Player player && Specs.has(player, "skinning_knife")
            && player.getRandom().nextFloat() < 0.1F) {
            List<ItemEntity> copies = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                copies.add(new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
            event.getDrops().addAll(copies);
        }
    }

    // ---- items and spells ----

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getSpellId().equals("mdvlcraft:vanish") && Specs.has(player, "ambusher")) {
            Buffs.give(player, "ambusher", 220);
        } else if (event.getSpellId().equals("mdvlcraft:silver_oil") && Specs.has(player, "long_lasting_oil")) {
            SpecKit.later(level, 1, () -> Buffs.extend(player, "silver_oil", Buffs.remaining(player, "silver_oil")));
        }
    }

    /** Quick Draw: the bow draws 50% faster while the buff lasts. */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getItem().getItem() instanceof BowItem && Buffs.has(event.getEntity(), "quick_draw") && event.getDuration() % 2 == 0) {
            event.setDuration(event.getDuration() - 1);
        }
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getItem().getItem() instanceof BowItem) {
            Buffs.clear(event.getEntity(), "quick_draw");
        }
    }

    /** Shared Meal: eating heals your pets within 10 blocks for half the food's value. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Specs.has(player, "shared_meal")) {
            return;
        }
        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null) {
            return;
        }
        for (TamableAnimal pet : player.level().getEntitiesOfClass(TamableAnimal.class, player.getBoundingBox().inflate(10.0),
            pet -> pet.isTame() && pet.isOwnedBy(player))) {
            pet.heal(food.getNutrition() / 2.0F);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof AbstractVillager villager && player.isShiftKeyDown() && Specs.has(player, "pickpocket")) {
            pickpocket(player, villager);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else if (Specs.has(player, "whisperer")) {
            ItemStack held = event.getItemStack();
            if (target instanceof TamableAnimal animal && !animal.isTame() && animal.isFood(held)) {
                animal.tame(player);
                animal.level().broadcastEntityEvent(animal, (byte)7);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else if (target instanceof AbstractHorse horse && !horse.isTamed() && held.isEmpty()) {
                horse.tameWithName(player);
                horse.level().broadcastEntityEvent(horse, (byte)7);
            }
        }
    }

    /** Pickpocket: one random trade result, once a day per villager. */
    private static void pickpocket(Player player, AbstractVillager villager) {
        long day = player.level().getDayTime() / 24000L;
        CompoundTag data = villager.getPersistentData();
        String key = "mdvlcraft_pickpocketed_" + player.getUUID();
        if (data.contains(key) && data.getLong(key) == day) {
            return;
        }
        MerchantOffers offers = villager.getOffers();
        if (offers.isEmpty()) {
            return;
        }
        data.putLong(key, day);
        MerchantOffer offer = offers.get(player.getRandom().nextInt(offers.size()));
        ItemStack loot = offer.getResult().copy();
        if (!player.getInventory().add(loot)) {
            player.drop(loot, false);
        }
    }

    /** Whistle: sneak-using a lead calls the last horse ridden, if within 64 blocks. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || !event.getItemStack().is(Items.LEAD) || !player.isShiftKeyDown()
            || !Specs.has(player, "whistle") || !player.getPersistentData().hasUUID("mdvlcraft_last_horse")) {
            return;
        }
        Entity horse = level.getEntity(player.getPersistentData().getUUID("mdvlcraft_last_horse"));
        if (horse instanceof AbstractHorse && horse.isAlive() && horse.distanceTo(player) <= 64.0F) {
            Vec3 beside = player.position().add(player.getLookAngle().multiply(1.5, 0.0, 1.5));
            horse.teleportTo(beside.x, player.getY(), beside.z);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Remembers the horse ridden (Whistle); Fighting Retreat on being knocked off. */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Entity mount = event.getEntityBeingMounted();
        if (event.isMounting() && mount instanceof AbstractHorse) {
            player.getPersistentData().putUUID("mdvlcraft_last_horse", mount.getUUID());
        } else if (event.isDismounting() && Specs.has(player, "fighting_retreat")
            && (player.hurtTime > 0 || mount instanceof LivingEntity living && (living.hurtTime > 0 || !living.isAlive()))) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getDistance() <= 6.0F && Specs.has(event.getEntity(), "light_feet")) {
            event.setCanceled(true);
        }
    }

    // ---- every tick: Rooted's stillness, Second Guard, Sea Legs ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Vec3 last = LAST_POS.put(player.getUUID(), player.position());
        if (last == null || last.distanceToSqr(player.position()) > 0.0004) {
            LAST_MOVE.put(player.getUUID(), player.level().getGameTime());
        }
        if (player.tickCount % 5 == 0 && Specs.has(player, "second_guard")) {
            boolean cooling = player.getCooldowns().isOnCooldown(Items.SHIELD);
            CompoundTag data = player.getPersistentData();
            if (cooling && !data.getBoolean("mdvlcraft_second_guard")) {
                data.putBoolean("mdvlcraft_second_guard", true);
                int left = Math.round(player.getCooldowns().getCooldownPercent(Items.SHIELD, 0.0F) * 100.0F);
                player.getCooldowns().addCooldown(Items.SHIELD, Math.max(1, left / 2));
            } else if (!cooling) {
                data.remove("mdvlcraft_second_guard");
            }
        }
        if (player.tickCount % 10 == 0) {
            AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
            boolean wet = player.isInWater() && Specs.has(player, "sea_legs");
            if (speed != null && wet && speed.getModifier(SEA_LEGS) == null) {
                speed.addTransientModifier(new AttributeModifier(SEA_LEGS, "mdvlcraft sea_legs", 0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (speed != null && !wet && speed.getModifier(SEA_LEGS) != null) {
                speed.removeModifier(SEA_LEGS);
            }
        }
    }
}
