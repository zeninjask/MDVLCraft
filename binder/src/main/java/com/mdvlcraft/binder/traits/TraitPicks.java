package com.mdvlcraft.binder.traits;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.combat.Crits;
import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * The third batch of traits (Trait, from EARLY_RISER on), the ones picked from the trait picker: their attributes
 * (refresh, from TraitWorld), hits, kills, spells, guard and dodges. Unbreakable is in mixin/traits/DurabilityMixin and
 * Reflex Parry raises the parry window attribute that mixin/epicfight/ParryingSkillMixin reads.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TraitPicks {
    private static final UUID LISTENER = UUID.fromString("3f6c2d1a-9b7e-4c55-8a0f-5d2e7b1c9a64");
    private static final ResourceLocation CRIT_CHANCE = ResourceLocation.fromNamespaceAndPath("attributeslib", "crit_chance");
    private static final ResourceLocation STUN = ResourceLocation.fromNamespaceAndPath("cursedfate", "movement_stun");
    private static final DustParticleOptions VOID = new DustParticleOptions(new Vector3f(0.25F, 0.05F, 0.4F), 1.4F);
    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Light Fingers: the price rises on the offers a player has open, taken back when the window closes. */
    private static final Map<UUID, List<MerchantOffer>> MARKUPS = new HashMap<>();

    private TraitPicks() {
    }

    private static final class State {
        Vec3 lastPos = Vec3.ZERO;
        long stillSince;
        long lastDamaged;
        long lastHit;
        int meleeHits;
        int frenzy;
        long hitRunUntil;
        final Map<String, Long> schools = new HashMap<>();
        long domainReady;
        long domainUntil;
        Vec3 domainCentre = Vec3.ZERO;
        long eclipseReady;
        long mirrorReady;
        int mirrorHits;
        long mirrorUntil;
        long stormReady;
        long reflectReady;
        long fateReady;
        long decoyReady;
        long slamReady;
        long lastEat = -100000;
        long mercyUntil;
        long lightningReady;
        @Nullable Entity lastAttacker;
        float lastAttack;
    }

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    private static boolean has(Entity entity, Trait trait) {
        return Traits.has(entity, trait);
    }

    private static boolean night(LivingEntity entity) {
        long time = entity.level().getDayTime() % 24000;
        return time >= 13000 && time < 23000;
    }

    private static boolean inCombat(Player player) {
        int now = player.tickCount;
        return now - player.getLastHurtByMobTimestamp() < 100 || now - player.getLastHurtMobTimestamp() < 100;
    }

    private static boolean shield(Player player) {
        return player.getMainHandItem().getItem() instanceof ShieldItem || player.getOffhandItem().getItem() instanceof ShieldItem;
    }

    private static boolean boss(LivingEntity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES) || entity.getMaxHealth() >= 100.0F;
    }

    private static boolean stunned(LivingEntity entity) {
        MobEffect stun = ForgeRegistries.MOB_EFFECTS.getValue(STUN);
        if (stun != null && entity.hasEffect(stun)) {
            return true;
        }
        LivingEntityPatch<?> patch = SpecKit.patch(entity);
        return patch != null && (patch.getEntityState().hurt() || patch.getEntityState().knockDown());
    }

    private static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    // ---- attributes ----

    /** The fixed modifiers (from TraitWorld.refresh) and the ones that change with time and place (every second). */
    static void refresh(ServerPlayer player) {
        TraitWorld.modifier(player, Attributes.MAX_HEALTH, "swift_hands", has(player, Trait.SWIFT_HANDS) ? -1.0 : 0.0, Operation.ADDITION);
        boolean stout = has(player, Trait.STOUT_HEART);
        TraitWorld.modifier(player, Attributes.MAX_HEALTH, "stout_heart", stout ? 2.0 : 0.0, Operation.ADDITION);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "stout_heart_speed", stout ? -0.03 : 0.0, Operation.MULTIPLY_TOTAL);
        boolean hide = has(player, Trait.TOUGH_HIDE);
        TraitWorld.modifier(player, Attributes.ARMOR, "tough_hide", hide ? 1.0 : 0.0, Operation.ADDITION);
        TraitWorld.modifier(player, ForgeMod.SWIM_SPEED.get(), "tough_hide_swim", hide ? -0.05 : 0.0, Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, EpicFightAttributes.STUN_ARMOR.get(), "iron_will", has(player, Trait.IRON_WILL) ? 5.0 : 0.0, Operation.ADDITION);
        TraitWorld.modifier(player, ForgeRegistries.ATTRIBUTES.getValue(CRIT_CHANCE), "lucky_charm", has(player, Trait.LUCKY_CHARM) ? 0.03 : 0.0,
            Operation.ADDITION);
        TraitWorld.modifier(player, BinderAttributes.PARRY_WINDOW.get(), "reflex_parry", has(player, Trait.REFLEX_PARRY) ? 0.5 : 0.0, Operation.ADDITION);
        TraitWorld.modifier(player, EpicFightAttributes.IMPACT.get(), "shatter", has(player, Trait.SHATTER) ? 0.5 : 0.0, Operation.MULTIPLY_TOTAL);
        boolean brittle = has(player, Trait.BRITTLE_BONES);
        TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "brittle_bones_attack", brittle ? 0.25 : 0.0, Operation.MULTIPLY_BASE);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "brittle_bones_speed", brittle ? 0.15 : 0.0, Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "reckless", has(player, Trait.RECKLESS) ? 0.2 : 0.0, Operation.MULTIPLY_BASE);
        TraitWorld.modifier(player, Attributes.ARMOR, "vow_of_fasting", has(player, Trait.VOW_OF_FASTING) ? 3.0 : 0.0, Operation.ADDITION);
        everySecond(player);
    }

    private static void everySecond(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        State state = state(player);
        long time = level.getDayTime() % 24000;
        boolean morning = time < 6000 || time >= 23000;
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "early_riser", has(player, Trait.EARLY_RISER) && morning ? 0.1 : 0.0, Operation.MULTIPLY_TOTAL);
        boolean combat = inCombat(player);
        TraitWorld.modifier(player, EpicFightAttributes.STAMINA_REGEN.get(), "quick_recovery",
            has(player, Trait.QUICK_RECOVERY) ? (combat ? -0.05 : 0.15) : 0.0, Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "bulwark", has(player, Trait.BULWARK) && shield(player) ? -0.1 : 0.0, Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, AttributeRegistry.MANA_REGEN.get(), "mana_spring",
            has(player, Trait.MANA_SPRING) && now - state.stillSince >= 60 ? 1.0 : 0.0, Operation.MULTIPLY_BASE);
        int schools = 0;
        if (has(player, Trait.ELEMENTAL_ATTUNEMENT)) {
            state.schools.values().removeIf(at -> now - at > 400);
            schools = Math.min(4, state.schools.size());
        }
        TraitWorld.modifier(player, AttributeRegistry.SPELL_POWER.get(), "elemental_attunement", 0.05 * schools, Operation.MULTIPLY_BASE);
        boolean storm = level.isThundering() && level.canSeeSky(player.blockPosition());
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "lightning_rod", has(player, Trait.LIGHTNING_ROD) && storm ? 0.5 : 0.0, Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "vow_of_the_night", has(player, Trait.VOW_OF_THE_NIGHT) && night(player) ? 0.25 : 0.0,
            Operation.MULTIPLY_TOTAL);
        boolean poverty = has(player, Trait.VOW_OF_POVERTY);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "vow_of_poverty", poverty ? 0.15 : 0.0, Operation.MULTIPLY_TOTAL);
        boolean haste = has(player, Trait.VOW_OF_HASTE);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "vow_of_haste", haste ? 0.25 : 0.0, Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, Attributes.ATTACK_SPEED, "vow_of_haste_attack", haste ? 0.25 : 0.0, Operation.MULTIPLY_BASE);

        // Vows that take armour off: Poverty (enchanted pieces), Haste (heavier than leather on the chest and legs)
        if (poverty || haste) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack piece = player.getItemBySlot(slot);
                boolean heavy = haste && (slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS) && !piece.isEmpty()
                    && !(piece.getItem() instanceof ArmorItem armour && armour.getMaterial() == ArmorMaterials.LEATHER);
                if (!piece.isEmpty() && (poverty && piece.isEnchanted() || heavy)) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    if (!player.getInventory().add(piece)) {
                        player.drop(piece, false);
                    }
                    say(player, poverty && piece.isEnchanted() ? "trait.mdvlcraft.vow_of_poverty.broken" : "trait.mdvlcraft.vow_of_haste.broken");
                }
            }
        }

        // Sharp Eyes: hostiles near a sneaking player glow
        if (has(player, Trait.SHARP_EYES) && player.isShiftKeyDown()) {
            for (LivingEntity e : SpecKit.enemies(player, player.position(), 12.0)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
            }
        }
        // Iron Skin: 4 absorption for every 10 s without taking damage, up to 8
        if (has(player, Trait.IRON_SKIN) && now - state.lastDamaged >= 200 && (now - state.lastDamaged) % 200 < 20) {
            player.setAbsorptionAmount(Math.min(8.0F, player.getAbsorptionAmount() + 4.0F));
        }
        // Shadow Pact: invisible while sneaking in the dark
        if (has(player, Trait.SHADOW_PACT) && player.isShiftKeyDown() && level.getMaxLocalRawBrightness(player.blockPosition()) <= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
        }
        // Lightning Rod: a storm can strike you once a minute
        if (has(player, Trait.LIGHTNING_ROD) && storm && now >= state.lightningReady && player.getRandom().nextInt(60) == 0) {
            state.lightningReady = now + 1200;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(player.position());
                level.addFreshEntity(bolt);
            }
        }
        // Vow of the Guardian: allies within 8 blocks regenerate 1 health every 4 s
        if (has(player, Trait.VOW_OF_THE_GUARDIAN) && now % 80 < 20) {
            for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(8.0),
                p -> p != player && TraitCombat.allied(p, player))) {
                ally.heal(1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        State state = state(player);
        long now = player.level().getGameTime();
        if (player.position().distanceToSqr(state.lastPos) > 0.0025) {
            state.stillSince = now;
            state.lastPos = player.position();
        }
        if (player.tickCount % 20 == 0) {
            everySecond(player);
        }
        // Deep Lungs: air runs out a third slower; sprinting drains a little more hunger
        if (has(player, Trait.DEEP_LUNGS)) {
            if (player.isUnderWater() && player.tickCount % 3 == 0 && player.getAirSupply() < player.getMaxAirSupply()) {
                player.setAirSupply(player.getAirSupply() + 1);
            }
            if (player.isSprinting()) {
                player.causeFoodExhaustion(0.004F);
            }
        }
        // Warm Blood: freezing builds up half as fast
        if (has(player, Trait.WARM_BLOOD) && player.getTicksFrozen() > 0 && player.tickCount % 2 == 0) {
            player.setTicksFrozen(player.getTicksFrozen() - 1);
        }
        // Sharp Eyes: slower while sneaking (set every tick so it follows the sneak key)
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "sharp_eyes", has(player, Trait.SHARP_EYES) && player.isShiftKeyDown() ? -0.15 : 0.0,
            Operation.MULTIPLY_TOTAL);
        TraitWorld.modifier(player, Attributes.MOVEMENT_SPEED, "hit_and_run", has(player, Trait.HIT_AND_RUN) && now < state.hitRunUntil ? 0.15 : 0.0,
            Operation.MULTIPLY_TOTAL);
        // Domain Sense: the field's edge
        if (now < state.domainUntil && player.tickCount % 5 == 0) {
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24;
                player.serverLevel().sendParticles(VOID, state.domainCentre.x + Math.cos(a) * 6, state.domainCentre.y + 0.2,
                    state.domainCentre.z + Math.sin(a) * 6, 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- hits ----

    /** Domain Sense: inside the field every hit is a sure crit (before Crits rolls). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurtFirst(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && has(player, Trait.DOMAIN_SENSE)) {
            State state = state(player);
            long now = player.level().getGameTime();
            if (now >= state.domainReady && now >= state.domainUntil) {
                state.domainReady = now + 1200;
                state.domainUntil = now + 100;
                state.domainCentre = player.position();
                player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            if (now < state.domainUntil && player.position().distanceTo(state.domainCentre) <= 6.0) {
                Crits.sureUntil(player, now);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity source = event.getSource().getEntity();
        boolean melee = event.getSource().getDirectEntity() == source;
        float m = 1.0F;
        long now = victim.level().getGameTime();

        if (source instanceof ServerPlayer player) {
            State state = state(player);
            ItemStack hand = player.getMainHandItem();
            if (has(player, Trait.EARLY_RISER) && night(player)) {
                m *= 0.95F;
            }
            if (has(player, Trait.CAVE_DWELLER)) {
                m *= player.getY() < 40 ? 1.1F : (player.level().isDay() && player.level().canSeeSky(player.blockPosition()) ? 0.95F : 1.0F);
            }
            if (has(player, Trait.WOODSMAN) && melee) {
                m *= hand.getItem() instanceof AxeItem ? 1.1F : hand.getItem() instanceof SwordItem ? 0.9F : 1.0F;
            }
            if (has(player, Trait.ANGLERS_LUCK) && player.level().isRainingAt(player.blockPosition())) {
                m *= 0.95F;
            }
            if (has(player, Trait.TRACKERS_MARK)) {
                if (!victim.hasEffect(MobEffects.GLOWING)) {
                    m *= 0.95F;
                }
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            }
            if (has(player, Trait.OPPORTUNIST) && stunned(victim)) {
                m *= 1.25F;
            }
            if (has(player, Trait.HEADHUNTER)) {
                m *= boss(victim) ? 1.15F : 0.95F;
            }
            if (has(player, Trait.HIT_AND_RUN)) {
                state.hitRunUntil = now + 40;
            }
            if (melee) {
                state.meleeHits++;
                if (has(player, Trait.EMBER_STRIKE) && state.meleeHits % 5 == 0) {
                    victim.setSecondsOnFire(3);
                }
                if (has(player, Trait.FROST_TOUCH) && state.meleeHits % 5 == 0) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), player);
                }
                if (has(player, Trait.ARCANE_EDGE)) {
                    MagicData magic = MagicData.getPlayerMagicData(player);
                    if (magic.getMana() >= 5.0F) {
                        SpecKit.addMana(player, -5.0F);
                        event.setAmount(event.getAmount() + 4.0F);
                    }
                }
            }
            if (has(player, Trait.BLOOD_MOON_CHILD)) {
                m *= night(player) ? 1.3F : 0.8F;
            }
            if (has(player, Trait.FRENZY)) {
                state.frenzy = now - state.lastHit <= 40 ? Math.min(7, state.frenzy + 1) : 0;
                m *= 1.0F + 0.08F * state.frenzy;
            }
            if (has(player, Trait.PAPER_TIGER)) {
                String key = "mdvlcraft:paper_tiger_" + player.getUUID();
                boolean first = !victim.getPersistentData().getBoolean(key);
                victim.getPersistentData().putBoolean(key, true);
                m *= first ? 1.6F : 0.8F;
            }
            if (has(player, Trait.RECKLESS)) {
                m *= 1.35F;
            }
            if (has(player, Trait.LIGHTNING_ROD) && player.level().isThundering() && player.level().canSeeSky(player.blockPosition())) {
                m *= 1.5F;
            }
            if (has(player, Trait.SHADOW_PACT) && player.level().getMaxLocalRawBrightness(player.blockPosition()) > 12) {
                m *= 0.7F;
            }
            if (has(player, Trait.VOW_OF_THE_BLADE) && hand.getItem() instanceof SwordItem) {
                m *= 1.3F;
            }
            if (has(player, Trait.VOW_OF_POVERTY)) {
                m *= 1.25F;
            }
            if (has(player, Trait.VOW_OF_THE_GUARDIAN)) {
                m *= 0.75F;
            }
            if (has(player, Trait.VOW_OF_THE_NIGHT) && night(player)) {
                m *= 1.25F;
            }
            if (has(player, Trait.ECLIPSE) && now >= state.eclipseReady) {
                state.eclipseReady = now + 2400;
                eclipse(player);
            }
            if (has(player, Trait.STORM_LORD) && now >= state.stormReady) {
                state.stormReady = now + 160;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.level());
                if (bolt != null) {
                    bolt.moveTo(victim.position());
                    bolt.setVisualOnly(true);
                    player.level().addFreshEntity(bolt);
                }
                event.setAmount(event.getAmount() + 6.0F);
            }
            if (melee && has(player, Trait.MIRROR_DANCE) && state.mirrorHits > 0 && now <= state.mirrorUntil) {
                state.mirrorHits--;
                float echo = event.getAmount() * 0.5F;
                SpecKit.decoy(player.serverLevel(), player, player.position(), 0.0, 8);
                SpecKit.later(player.serverLevel(), 6, () -> {
                    if (victim.isAlive()) {
                        victim.invulnerableTime = 0;
                        victim.hurt(player.damageSources().playerAttack(player), echo);
                    }
                });
            }
            state.lastHit = now;
        }

        // Ricochet: a hit arrow bounces once to the nearest other enemy
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer shooter
            && has(shooter, Trait.RICOCHET) && !arrow.getPersistentData().getBoolean("mdvlcraft:ricochet")) {
            arrow.getPersistentData().putBoolean("mdvlcraft:ricochet", true);
            LivingEntity next = SpecKit.nearestEnemy(shooter, victim.position(), 5.0, victim);
            if (next != null) {
                float bounce = event.getAmount() * 0.5F;
                line(shooter.serverLevel(), victim.position().add(0, victim.getBbHeight() / 2, 0), next.position().add(0, next.getBbHeight() / 2, 0));
                next.hurt(shooter.damageSources().arrow(arrow, shooter), bounce);
            }
        }

        // Pack Leader: tamed animals hit harder and are hit harder
        if (source instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner && has(owner, Trait.PACK_LEADER)) {
            m *= 1.2F;
        }
        if (victim instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer owner && has(owner, Trait.PACK_LEADER)) {
            m *= 1.1F;
        }
        // Summoner's Pact and Unbound Summoner: summons
        LivingEntity summoner = source == null ? null : SpecKit.owner(source);
        if (summoner instanceof ServerPlayer owner) {
            if (has(owner, Trait.UNBOUND_SUMMONER)) {
                m *= 1.25F;
            }
            if (has(owner, Trait.SUMMONERS_PACT)) {
                owner.heal(event.getAmount() * m * 0.1F);
            }
            if (has(owner, Trait.HAUNTED) && source.getPersistentData().getBoolean("mdvlcraft:haunted") && owner.getRandom().nextFloat() < 0.1F) {
                event.setCanceled(true);
                owner.hurt(owner.damageSources().mobAttack((LivingEntity)source), event.getAmount());
                return;
            }
        }

        if (victim instanceof ServerPlayer player) {
            State state = state(player);
            Entity attacker = source;
            if (has(player, Trait.CLIMBER) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
                m *= 1.1F;
            }
            if (has(player, Trait.WARM_BLOOD) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
                m *= 1.1F;
            }
            if (has(player, Trait.REARGUARD) && attacker != null) {
                Vec3 to = attacker.position().subtract(player.position()).multiply(1, 0, 1).normalize();
                Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
                if (to.dot(look) < -0.3) {
                    m *= 0.7F;
                }
            }
            if (has(player, Trait.ARCANE_SHIELD)) {
                MagicData magic = MagicData.getPlayerMagicData(player);
                float max = SpecKit.maxMana(player);
                if (magic.getMana() > max * 0.5F) {
                    float absorbed = event.getAmount() * m * 0.2F;
                    SpecKit.addMana(player, -Math.min(magic.getMana(), absorbed * 5.0F));
                    m *= 0.8F;
                }
            }
            if (has(player, Trait.VOW_OF_THE_NIGHT) && !night(player)) {
                m *= 1.15F;
            }
            if (attacker instanceof LivingEntity living && has(player, Trait.HEXED_BLOOD)) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
            }
            state.lastAttacker = attacker;
            state.lastAttack = event.getAmount() * m;
        }
        // Shared Burden: a party member near the holder hands 15% of the hit to them
        if (victim instanceof ServerPlayer hit) {
            for (ServerPlayer holder : hit.serverLevel().getEntitiesOfClass(ServerPlayer.class, hit.getBoundingBox().inflate(8.0),
                p -> p != hit && has(p, Trait.SHARED_BURDEN) && TraitCombat.allied(p, hit))) {
                float moved = event.getAmount() * m * 0.15F;
                m *= 0.85F;
                holder.hurt(holder.damageSources().magic(), moved);
                break;
            }
        }
        event.setAmount(event.getAmount() * m);
    }

    /** Arc Fists: an unarmed crit arcs to 2 nearby enemies (after Crits has decided). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurtLast(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer player && has(player, Trait.ARC_FISTS)
            && event.getSource().getDirectEntity() == player && player.getMainHandItem().isEmpty() && Crits.critical(victim)) {
            int arcs = 0;
            for (LivingEntity e : SpecKit.enemies(player, victim.position(), 5.0)) {
                if (e != victim && arcs < 2) {
                    arcs++;
                    line(player.serverLevel(), victim.position().add(0, 1, 0), e.position().add(0, 1, 0));
                    e.hurt(player.damageSources().indirectMagic(player, player), 4.0F);
                }
            }
        }
    }

    private static void line(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from).scale(1.0 / 8);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = from.add(step.scale(i));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Eclipse: enemies within 10 blocks are blinded and slowed for 15 s. */
    private static void eclipse(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY(1.0), player.getZ(), 80, 4.0, 1.5, 4.0, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.5F);
        for (LivingEntity e : SpecKit.enemies(player, player.position(), 10.0)) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300, 0), player);
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0), player);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1), player);
            if (e instanceof Mob mob) {
                SpecKit.blind(mob, 300);
            }
        }
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        // Vow of Mercy: villagers and passive animals can't be harmed
        if (event.getSource().getEntity() instanceof ServerPlayer player && has(player, Trait.VOW_OF_MERCY)
            && (event.getEntity() instanceof AbstractVillager || event.getEntity() instanceof Animal && !(event.getEntity() instanceof Enemy))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State state = state(player);
        long now = player.level().getGameTime();
        state.lastDamaged = now;
        // Fate Weaver: a hit of more than 30% of max health takes 10% instead, every 30 s
        if (has(player, Trait.FATE_WEAVER) && now >= state.fateReady && event.getAmount() > player.getMaxHealth() * 0.3F) {
            state.fateReady = now + 600;
            event.setAmount(player.getMaxHealth() * 0.1F);
            player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || dead == player) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (has(player, Trait.GRAVITY_WELL)) {
            for (LivingEntity e : SpecKit.enemies(player, dead.position(), 4.0)) {
                if (e != dead) {
                    Vec3 pull = dead.position().subtract(e.position()).normalize().scale(0.6);
                    e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0.15, pull.z));
                    e.hurtMarked = true;
                }
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, dead.getX(), dead.getY(0.5), dead.getZ(), 30, 1.5, 0.5, 1.5, 0.05);
        }
        if (has(player, Trait.WAR_DRUMMER)) {
            for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(12.0),
                p -> p == player || TraitCombat.allied(p, player))) {
                ally.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, false, true));
            }
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && has(player, Trait.LIGHT_FINGERS) && !event.getDrops().isEmpty()
            && player.getRandom().nextFloat() < 0.1F) {
            var drops = new ArrayList<>(event.getDrops());
            var pick = drops.get(player.getRandom().nextInt(drops.size()));
            if (!pick.getItem().isDamageableItem()) {
                ItemStack one = pick.getItem().copyWithCount(1);
                event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(pick.level(), pick.getX(), pick.getY(), pick.getZ(), one));
            }
        }
    }

    // ---- movement ----

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (has(event.getEntity(), Trait.STEADY_FOOTING)) {
            event.setStrength(event.getStrength() * 0.6F);
        }
        if (has(event.getEntity(), Trait.BRITTLE_BONES)) {
            event.setStrength(event.getStrength() * 2.0F);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (has(entity, Trait.CLIMBER)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.75F);
        }
        if (has(entity, Trait.BRITTLE_BONES)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 2.0F);
        }
        // Meteor Drop: a fall of 6+ blocks lands on the enemies below for the damage you would take; you take half
        if (entity instanceof ServerPlayer player && has(player, Trait.METEOR_DROP) && event.getDistance() >= 6.0F) {
            float damage = (event.getDistance() - 3.0F) * event.getDamageMultiplier();
            List<LivingEntity> hit = SpecKit.enemies(player, player.position(), 3.0);
            if (!hit.isEmpty()) {
                for (LivingEntity e : hit) {
                    e.hurt(player.damageSources().playerAttack(player), damage);
                }
                event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
                player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 3, 1.0, 0.2, 1.0, 0.0);
                player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
        }
    }

    // ---- items, food, fishing, trade ----

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        float speed = event.getNewSpeed();
        if (has(player, Trait.EARLY_RISER)) {
            long time = player.level().getDayTime() % 24000;
            if (time < 6000 || time >= 23000) {
                speed *= 1.1F;
            }
        }
        if (has(player, Trait.WOODSMAN) && player.getMainHandItem().getItem() instanceof AxeItem) {
            speed *= 1.3F;
        }
        event.setNewSpeed(speed);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem();
        UseAnim anim = stack.getUseAnimation();
        if (has(player, Trait.SWIFT_HANDS) && (anim == UseAnim.EAT || anim == UseAnim.DRINK)) {
            event.setDuration(Math.max(1, event.getDuration() * 3 / 4));
        }
        if (has(player, Trait.VOW_OF_THE_BLADE) && (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer server) {
                say(server, "trait.mdvlcraft.vow_of_the_blade.no_bow");
            }
        }
        if (has(player, Trait.VOW_OF_FASTING) && stack.isEdible() && player instanceof ServerPlayer server) {
            if (player.level().getGameTime() - state(player).lastEat < 2400) {
                event.setCanceled(true);
                say(server, "trait.mdvlcraft.vow_of_fasting.wait");
            }
        }
    }

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        // Steady Breath: bows draw a fifth faster while you stand still
        if (event.getEntity() instanceof Player player && has(player, Trait.STEADY_BREATH) && event.getItem().getItem() instanceof BowItem
            && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 && event.getDuration() % 5 == 0) {
            event.setDuration(event.getDuration() - 1);
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack.isEdible()) {
            state(player).lastEat = player.level().getGameTime();
        }
        if (stack.getItem() instanceof PotionItem) {
            List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
            // Hexed Blood: potions last half as long
            if (has(player, Trait.HEXED_BLOOD)) {
                for (MobEffectInstance effect : effects) {
                    MobEffectInstance active = player.getEffect(effect.getEffect());
                    if (active != null && !effect.getEffect().isInstantenous()) {
                        player.removeEffect(effect.getEffect());
                        player.addEffect(new MobEffectInstance(active.getEffect(), active.getDuration() / 2, active.getAmplifier(), active.isAmbient(),
                            active.isVisible(), active.showIcon()));
                    }
                }
            }
            // Rotting Flesh: healing potions hurt, as they do the undead
            if (has(player, Trait.ROTTING_FLESH)) {
                for (MobEffectInstance effect : effects) {
                    if (effect.getEffect() == MobEffects.HEAL) {
                        float healed = 4 << effect.getAmplifier();
                        player.setHealth(Math.max(1.0F, player.getHealth() - healed));
                        player.hurt(player.damageSources().magic(), 6 << effect.getAmplifier());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && has(event.getEntity(), Trait.ROTTING_FLESH)
            && (effect.getEffect() == MobEffects.POISON || effect.getEffect() == MobEffects.HUNGER)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        // Iron Will: stuns wear off 30% sooner
        MobEffectInstance effect = event.getEffectInstance();
        MobEffect stun = ForgeRegistries.MOB_EFFECTS.getValue(STUN);
        if (event.getEntity() instanceof ServerPlayer player && has(player, Trait.IRON_WILL) && stun != null && effect.getEffect() == stun) {
            int cut = effect.getDuration() * 7 / 10;
            SpecKit.later(player.serverLevel(), Math.max(1, cut), () -> player.removeEffect(stun));
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            if (has(player, Trait.VOW_OF_FASTING)) {
                event.setAmount(event.getAmount() * 1.2F);
            }
            // Blood Bank: healing past full is kept as absorption, up to 6
            if (has(player, Trait.BLOOD_BANK)) {
                float over = player.getHealth() + event.getAmount() - player.getMaxHealth();
                if (over > 0.0F) {
                    player.setAbsorptionAmount(Math.min(6.0F, player.getAbsorptionAmount() + over));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        // Angler's Luck: half again as much treasure (one more treasure roll, a third of the time)
        if (event.getEntity() instanceof ServerPlayer player && has(player, Trait.ANGLERS_LUCK) && player.getRandom().nextFloat() < 0.07F) {
            FishingHook hook = event.getHookEntity();
            LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.FISHING);
            for (ItemStack stack : player.server.getLootData().getLootTable(BuiltInLootTables.FISHING_TREASURE).getRandomItems(params)) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide) {
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
            if (attack.getResult() != AttackResult.ResultType.BLOCKED || attack.isParried()) {
                return;
            }
            State state = state(player);
            long now = player.level().getGameTime();
            Entity attacker = attack.getDamageSource().getEntity();
            if (has(player, Trait.SHIELD_SLAM) && now >= state.slamReady && attacker instanceof LivingEntity living) {
                state.slamReady = now + 100;
                SpecKit.stun(living, 10);
            }
            if (has(player, Trait.INVINCIBLE_GUARD) && now >= state.reflectReady && attacker instanceof LivingEntity living) {
                state.reflectReady = now + 200;
                living.hurt(player.damageSources().thorns(player), attack.getDamage());
                player.serverLevel().sendParticles(ParticleTypes.CRIT, living.getX(), living.getY(0.6), living.getZ(), 15, 0.3, 0.4, 0.3, 0.3);
            }
        });
        patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, LISTENER, consume -> {
            if (consume.getSkill() instanceof GuardSkill) {
                if (has(player, Trait.BULWARK) && shield(player)) {
                    consume.setAmount(consume.getAmount() * 0.85F);
                }
                // Frenzy and Reckless: no guarding (the block costs more than there is)
                boolean frenzied = has(player, Trait.FRENZY) && state(player).frenzy > 0 && player.level().getGameTime() - state(player).lastHit <= 40;
                if (frenzied || has(player, Trait.RECKLESS)) {
                    consume.setAmount(Math.max(consume.getAmount(), patch.getMaxStamina() * 2.0F));
                }
                // Invincible Guard: never breaks
                if (has(player, Trait.INVINCIBLE_GUARD)) {
                    consume.setAmount(Math.max(0.0F, Math.min(consume.getAmount(), patch.getStamina() - 0.05F)));
                }
            } else if (consume.getSkill().getCategory() == SkillCategories.DODGE && has(player, Trait.STEADY_FOOTING)) {
                consume.setAmount(consume.getAmount() * 1.1F);
            }
        });
        patch.getEventListener().addEventListener(EventType.SKILL_CAST_EVENT, LISTENER, cast -> {
            var skill = cast.getSkillContainer().getSkill();
            if (skill == null || !cast.isExecutable() || skill.getCategory() != SkillCategories.DODGE) {
                return;
            }
            State state = state(player);
            long now = player.level().getGameTime();
            if (has(player, Trait.DECOY_DODGE) && now >= state.decoyReady) {
                state.decoyReady = now + 240;
                SpecKit.decoy(player.serverLevel(), player, player.position(), 8.0, 40);
            }
            if (has(player, Trait.MIRROR_DANCE) && now >= state.mirrorReady) {
                state.mirrorReady = now + 500;
                state.mirrorHits = 3;
                state.mirrorUntil = now + 200;
                player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
            }
        });
    }

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        // Light Fingers: villagers charge 5% more
        if (!(event.getContainer() instanceof MerchantMenu menu) || !has(event.getEntity(), Trait.LIGHT_FINGERS)) {
            return;
        }
        List<MerchantOffer> raised = new ArrayList<>();
        for (MerchantOffer offer : menu.getOffers()) {
            int up = Math.round(offer.getBaseCostA().getCount() * 0.05F);
            if (up > 0) {
                offer.addToSpecialPriceDiff(up);
                raised.add(offer);
            }
        }
        MARKUPS.put(event.getEntity().getUUID(), raised);
    }

    @SubscribeEvent
    public static void onClose(PlayerContainerEvent.Close event) {
        List<MerchantOffer> raised = MARKUPS.remove(event.getEntity().getUUID());
        if (raised != null) {
            for (MerchantOffer offer : raised) {
                offer.addToSpecialPriceDiff(-Math.round(offer.getBaseCostA().getCount() * 0.05F));
            }
        }
    }

    // ---- spells ----

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && has(player, Trait.VOW_OF_THE_BLADE) && event.isCancelable()) {
            event.setCanceled(true);
            say(player, "trait.mdvlcraft.vow_of_the_blade.no_spell");
        }
    }

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getCastSource() == CastSource.COMMAND) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        State state = state(player);
        long now = player.level().getGameTime();
        if (has(player, Trait.APPRENTICE)) {
            SpellRarity rarity = spell.getRarity(event.getSpellLevel());
            if (rarity == SpellRarity.COMMON || rarity == SpellRarity.UNCOMMON) {
                event.setManaCost(Math.round(event.getManaCost() * 0.85F));
            }
        }
        if (has(player, Trait.OVERCHARGE)) {
            event.setManaCost(Math.round(event.getManaCost() * 1.5F));
        }
        if (has(player, Trait.ELEMENTAL_ATTUNEMENT)) {
            state.schools.put(event.getSchoolType().getId().toString(), now);
            everySecond(player);
        }
        if (has(player, Trait.VOW_OF_MERCY)) {
            state.mercyUntil = now + 60;
        }
        if (has(player, Trait.VOLATILE_MANA)) {
            for (LivingEntity e : SpecKit.enemies(player, player.position(), 3.0)) {
                e.hurt(player.damageSources().indirectMagic(player, player), 5.0F);
            }
            player.hurt(player.damageSources().magic(), 2.0F);
            player.serverLevel().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY(1.0), player.getZ(), 25, 1.2, 0.6, 1.2, 0.1);
        }
        if (has(player, Trait.INFINITE_VOID) && player.getRandom().nextFloat() < 0.1F) {
            event.setManaCost(0);
            String id = event.getSpellId();
            SpecKit.later(player.serverLevel(), 1, () -> {
                MagicData magic = MagicData.getPlayerMagicData(player);
                magic.getPlayerCooldowns().removeCooldown(id);
                magic.getPlayerCooldowns().syncToPlayer(player);
            });
            player.serverLevel().sendParticles(VOID, player.getX(), player.getY(1.0), player.getZ(), 30, 0.5, 0.7, 0.5, 0.0);
        }
        // Unbound Summoner: a summoning spell summons twice
        if (has(player, Trait.UNBOUND_SUMMONER) && event.getSpellId().contains("summon")) {
            int level = event.getSpellLevel();
            SpecKit.later(player.serverLevel(), 4, () -> {
                if (player.isAlive()) {
                    spell.onCast(player.level(), level, player, CastSource.COMMAND, MagicData.getPlayerMagicData(player));
                }
            });
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getSpellDamageSource().getEntity() instanceof ServerPlayer player && has(player, Trait.OVERCHARGE)) {
            event.setAmount(event.getAmount() * 1.5F);
        }
    }

    @SubscribeEvent
    public static void onSpellHeal(SpellHealEvent event) {
        // Vow of Mercy: a spell you cast heals others twice as much
        if (event.getEntity() instanceof ServerPlayer caster && has(caster, Trait.VOW_OF_MERCY) && event.getTargetEntity() != caster) {
            event.getTargetEntity().heal(event.getHealAmount());
        }
    }

    @SubscribeEvent
    public static void onSummon(SpellSummonEvent<?> event) {
        if (event.getCaster() instanceof ServerPlayer player && has(player, Trait.SUMMONERS_PACT)) {
            LivingEntity creature = event.getCreature();
            TraitWorld.modifier(creature, Attributes.MAX_HEALTH, "summoners_pact", 0.3, Operation.MULTIPLY_TOTAL);
            creature.setHealth(creature.getMaxHealth());
        }
    }

    // ---- Haunted: a ghost in the gates ----

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 40 != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            boolean gate = com.mdvlcraft.binder.gate.GateDimensions.isGate(player.level().dimension());
            String key = "mdvlcraft:haunted_ghost";
            UUID ghostId = player.getPersistentData().hasUUID(key) ? player.getPersistentData().getUUID(key) : null;
            Entity ghost = ghostId == null ? null : player.serverLevel().getEntity(ghostId);
            if (has(player, Trait.HAUNTED) && gate && player.isAlive()) {
                if (ghost == null || !ghost.isAlive()) {
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "summoned_vex"));
                    Entity spawned = type == null ? null : type.create(player.serverLevel());
                    if (spawned instanceof io.redspace.ironsspellbooks.entity.mobs.SummonedVex mob) {
                        mob.moveTo(player.getX(), player.getY() + 1.5, player.getZ());
                        mob.setSummoner(player);
                        mob.getPersistentData().putBoolean("mdvlcraft:haunted", true);
                        player.serverLevel().addFreshEntity(mob);
                        player.getPersistentData().putUUID(key, mob.getUUID());
                    }
                }
            } else if (ghost != null) {
                ghost.discard();
                player.getPersistentData().remove(key);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
        MARKUPS.remove(event.getEntity().getUUID());
    }
}
