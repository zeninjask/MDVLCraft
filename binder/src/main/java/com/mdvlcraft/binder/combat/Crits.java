package com.mdvlcraft.binder.combat;

import com.mdvlcraft.binder.traits.Trait;
import com.mdvlcraft.binder.traits.TraitMore;
import com.mdvlcraft.binder.traits.Traits;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The pack's one critical-hit system: every hit by a player or a hostile mob (their arrows and spells too) has a 1 in 20
 * chance to be critical, dealing 1.5 times the damage. Vanilla's jump crits, Apothic Attributes' own roll
 * (mixin/apoth/AttributeEventsMixin) and Weapons of Miracles' Critical Knowledge roll (mixin/wom/CriticalKnowledgeMixin)
 * are off; what they gave now feeds this roll:
 * - Apothic's crit chance and crit damage attributes above their defaults (gear, effects) add to the chance and damage;
 * - Lender's Delight's Crit Chance stat adds its percent;
 * - Critical Knowledge adds 20% (and 5% per Fire Protection level worn), and its crits deal 2x (+0.15x per Blast Protection);
 * - Rime Veil (Interlace Spellweaves) and the Riposte trait make the next melee hit a sure crit;
 * - the Black Flash trait turns every crit into a black flash (2.5 times the damage).
 * Effects that care about crits (Crimson Edge, Black Flash Instinct) ask critical(victim) during the same hit.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Crits {
    public static final float CHANCE = 0.05F;
    public static final float DAMAGE = 1.5F;
    private static final ResourceLocation CRIT_CHANCE = ResourceLocation.fromNamespaceAndPath("attributeslib", "crit_chance");
    private static final ResourceLocation CRIT_DAMAGE = ResourceLocation.fromNamespaceAndPath("attributeslib", "crit_damage");
    private static final ResourceLocation RIME_VEIL = ResourceLocation.fromNamespaceAndPath("iss_csw", "rime_veil");
    private static final ResourceLocation CRITICAL_KNOWLEDGE = ResourceLocation.fromNamespaceAndPath("wom", "critical_knowledge");
    /** Victims of a critical hit, by the game time of that hit. */
    private static final Map<LivingEntity, Long> CRIT = new WeakHashMap<>();
    /** Players whose vanilla swing (Player.attack) started under Rime Veil: the mod's mixin takes the effect off before the hit. */
    private static final Map<Player, Long> VEILED = new WeakHashMap<>();
    /** Players whose next melee hit is a sure crit until a game time (the Riposte trait). */
    private static final Map<Player, Long> SURE_UNTIL = new WeakHashMap<>();
    private static Object lendersCapability;
    private static Method lendersStat;
    private static Object lendersCritType;
    private static boolean lendersLooked;

    private Crits() {
    }

    /** Whether the hit `victim` is taking now is a critical hit. */
    public static boolean critical(LivingEntity victim) {
        Long at = CRIT.get(victim);
        return at != null && at == victim.level().getGameTime();
    }

    /** The player's next melee hit before game time `until` is a sure crit. */
    public static void sureUntil(Player player, long until) {
        SURE_UNTIL.put(player, until);
    }

    private static boolean rolls(LivingEntity attacker) {
        return attacker instanceof Player || attacker instanceof Enemy;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        MobEffect veil = ForgeRegistries.MOB_EFFECTS.getValue(RIME_VEIL);
        if (veil != null && event.getEntity().hasEffect(veil)) {
            VEILED.put(event.getEntity(), event.getEntity().level().getGameTime());
        }
    }

    /** Vanilla's jump crit (and the mods that force it) never applies: the roll in onHurt decides. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVanillaCrit(CriticalHitEvent event) {
        event.setResult(Event.Result.DENY);
        event.setDamageModifier(1.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity victim = event.getEntity();
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == victim || !rolls(attacker) || victim.level().isClientSide
            || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.THORNS)) {
            return;
        }
        long now = victim.level().getGameTime();
        double chance = CHANCE + attribute(attacker, CRIT_CHANCE);
        double damage = DAMAGE + attribute(attacker, CRIT_DAMAGE);
        boolean sure = false;
        if (attacker instanceof Player player) {
            chance += lendersChance(player);
            if (criticalKnowledge(player)) {
                int fire = 0;
                int blast = 0;
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    fire += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, player.getItemBySlot(slot));
                    blast += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, player.getItemBySlot(slot));
                }
                chance += 0.20 + 0.05 * fire;
                damage = Math.max(damage, 2.0 + 0.15 * blast);
            }
            MobEffect veil = ForgeRegistries.MOB_EFFECTS.getValue(RIME_VEIL);
            boolean melee = source.getDirectEntity() == player;
            Long veiled = VEILED.remove(player);
            Long riposte = SURE_UNTIL.get(player);
            if (melee && riposte != null && now <= riposte) {
                SURE_UNTIL.remove(player);
                sure = true;
            } else if (melee && veiled != null && veiled == now) {
                sure = true;
            } else if (melee && veil != null && player.hasEffect(veil)) {   // Epic Fight swings skip Player.attack, and the mod's mixin with it
                player.removeEffect(veil);
                sure = true;
            }
        }
        if (!sure && victim.getRandom().nextFloat() >= chance) {
            return;
        }
        CRIT.put(victim, now);
        if (attacker instanceof Player player && Traits.has(player, Trait.BLACK_FLASH)) {
            // Black Flash: every crit is a black flash instead, 2.5 times the damage
            event.setAmount((float)(event.getAmount() * Math.max(damage, 2.5)));
            TraitMore.blackFlash(player, victim);
            return;
        }
        event.setAmount((float)(event.getAmount() * damage));
        if (victim.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 14, 0.3, 0.4, 0.3, 0.3);
            level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** An Apothic Attributes attribute above its default (0 without the mod or the attribute). */
    private static double attribute(LivingEntity entity, ResourceLocation id) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
        return attribute == null || entity.getAttribute(attribute) == null ? 0.0
            : Math.max(0.0, entity.getAttributeValue(attribute) - attribute.getDefaultValue());
    }

    private static boolean criticalKnowledge(Player player) {
        PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        return patch != null && patch.getSkillCapability().listSkillContainers()
            .anyMatch(c -> c.getSkill() != null && CRITICAL_KNOWLEDGE.equals(c.getSkill().getRegistryName()));
    }

    /** Lender's Delight's Crit Chance stat (a percent), read by reflection: the mod is not on the binder's classpath. */
    private static double lendersChance(Player player) {
        try {
            if (!lendersLooked) {
                lendersLooked = true;
                Class<?> provider = Class.forName("net.chaolux.lendersdelight.coomon.stat.PlayerStatProvider");
                Field field = provider.getField("PLAYER_STAT");
                lendersCapability = field.get(null);
                Class<?> type = Class.forName("net.chaolux.lendersdelight.coomon.stat.StatType");
                lendersCritType = type.getField("CRIT_CHANCE").get(null);
                lendersStat = Class.forName("net.chaolux.lendersdelight.coomon.stat.IPlayerStat").getMethod("getStat", type);
            }
            if (lendersCapability == null) {
                return 0.0;
            }
            @SuppressWarnings("unchecked")
            var cap = player.getCapability((net.minecraftforge.common.capabilities.Capability<Object>)lendersCapability).resolve();
            return cap.isPresent() ? ((Number)lendersStat.invoke(cap.get(), lendersCritType)).doubleValue() / 100.0 : 0.0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            lendersCapability = null;
            return 0.0;
        }
    }
}
