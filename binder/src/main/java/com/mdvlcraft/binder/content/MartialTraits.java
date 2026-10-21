package com.mdvlcraft.binder.content;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/** The behaviour behind the Martial (combat) classes' marker powers (data/mdvlcraft/powers). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MartialTraits {
    private static final ResourceLocation SINGLE_COMBAT = power("duelist/single_combat");
    private static final ResourceLocation RIPOSTE_RHYTHM = power("duelist/riposte_rhythm");
    private static final ResourceLocation SHIELD_WALL = power("guardian/shield_wall");
    private static final ResourceLocation BULWARK = power("guardian/bulwark");
    private static final ResourceLocation LIGHT_FEET = power("skirmisher/light_feet");
    private static final ResourceLocation QUICK_RELOAD = power("crossbowman/quick_reload");
    private static final ResourceLocation PIERCING_BOLTS = power("crossbowman/piercing_bolts");
    private static final ResourceLocation LONG_REACH = power("spearman/long_reach");
    private static final ResourceLocation CHARGE = power("spearman/charge");
    private static final ResourceLocation TROPHIES = power("monster_hunter/trophies");
    private static final ResourceLocation BOUNTY = power("bounty_hunter/bounty");
    private static final ResourceLocation SPELLBLADE_RHYTHM = power("battle_mage/spellblade_rhythm");
    private static final ResourceLocation BOARDING_FURY = power("corsair/boarding_fury");

    private static final UUID EPIC_FIGHT_LISTENER = UUID.fromString("8a6f2c14-3d5e-4b7a-9c1f-0e2d4b6a8c10");
    private static final UUID SPEAR_REACH = UUID.fromString("8a6f2c14-3d5e-4b7a-9c1f-0e2d4b6a8c11");
    private static final UUID SPELLBLADE_POWER = UUID.fromString("8a6f2c14-3d5e-4b7a-9c1f-0e2d4b6a8c12");
    private static final long SPELLBLADE_TICKS = 60L;
    private static final Map<UUID, Long> LAST_MELEE = new HashMap<>();

    private MartialTraits() {
    }

    private static ResourceLocation power(String path) {
        return ResourceLocation.fromNamespaceAndPath("mdvlcraft", path);
    }

    private static boolean has(Entity entity, ResourceLocation power) {
        return OriginTraits.has(entity, power);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null) {
                throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
            }

            patch.getEventListener().addEventListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, EPIC_FIGHT_LISTENER, attack -> {
                if (attack.isParried() && has(player, RIPOSTE_RHYTHM)) {
                    patch.setStamina(Math.min(patch.getMaxStamina(), patch.getStamina() + patch.getMaxStamina() * 0.05F));
                }
            });
            patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, EPIC_FIGHT_LISTENER, consume -> {
                if (consume.getSkill() instanceof GuardSkill && has(player, SHIELD_WALL)) {
                    consume.setAmount(consume.getAmount() * 0.85F);
                } else if (consume.getSkill() instanceof DodgeSkill && has(player, LIGHT_FEET)) {
                    consume.setAmount(consume.getAmount() * 0.9F);
                }
            });
        } else if (!event.getLevel().isClientSide() && entity instanceof AbstractArrow arrow && arrow.shotFromCrossbow()
            && arrow.getOwner() instanceof Player owner && has(owner, PIERCING_BOLTS)) {
            arrow.setPierceLevel((byte)(arrow.getPierceLevel() + 1));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        LAST_MELEE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        float multiplier = 1.0F;
        if (event.getSource().getEntity() instanceof Player attacker) {
            boolean melee = event.getSource().getDirectEntity() == attacker;
            if (melee) {
                LAST_MELEE.put(attacker.getUUID(), attacker.level().getGameTime());
            }

            if (has(attacker, SINGLE_COMBAT) && alone(target)) {
                multiplier *= 1.1F;
            }

            if (melee && attacker.isSprinting() && holdingSpear(attacker) && has(attacker, CHARGE)) {
                multiplier *= 1.1F;
            }

            if (melee && has(attacker, SPELLBLADE_RHYTHM)) {
                AttributeInstance power = attacker.getAttribute(AttributeRegistry.SPELL_POWER.get());
                if (power != null && power.getModifier(SPELLBLADE_POWER) == null) {
                    power.addTransientModifier(new AttributeModifier(SPELLBLADE_POWER, "Spellblade Rhythm", 0.05, Operation.MULTIPLY_BASE));
                }
            }

            if ((onVessel(attacker) || nearWater(attacker)) && has(attacker, BOARDING_FURY)) {
                multiplier *= 1.15F;
            }
        }

        if (target instanceof Player victim && guarded(victim)) {
            multiplier *= 0.95F;
        }

        event.setAmount(event.getAmount() * multiplier);
    }

    /** No other monster stands within 6 blocks of the target. */
    private static boolean alone(LivingEntity target) {
        return target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(6.0),
            other -> other != target && other instanceof Enemy && other.isAlive()).isEmpty();
    }

    /** Another player with Bulwark stands within 4 blocks. */
    private static boolean guarded(Player victim) {
        return !victim.level().getEntitiesOfClass(Player.class, victim.getBoundingBox().inflate(4.0),
            other -> other != victim && has(other, BULWARK)).isEmpty();
    }

    private static boolean holdingSpear(Player player) {
        WeaponCategory category = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem()).getWeaponCategory();
        return category == WeaponCategories.SPEAR || category == WeaponCategories.TRIDENT;
    }

    private static boolean onVessel(Player player) {
        Entity vehicle = player.getRootVehicle();
        return vehicle != player && (vehicle instanceof Boat || ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType()).getNamespace().equals("alekiships"));
    }

    /** Water (still or flowing) within 8 blocks. */
    private static boolean nearWater(Player player) {
        return BlockPos.findClosestMatch(player.blockPosition(), 8, 8, pos -> player.level().getFluidState(pos).is(FluidTags.WATER)).isPresent();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase == Phase.END && !player.level().isClientSide() && player.tickCount % 10 == 0) {
            AttributeInstance spellPower = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
            Long last = LAST_MELEE.get(player.getUUID());
            if (spellPower != null && spellPower.getModifier(SPELLBLADE_POWER) != null
                && (last == null || player.level().getGameTime() - last > SPELLBLADE_TICKS || !has(player, SPELLBLADE_RHYTHM))) {
                spellPower.removeModifier(SPELLBLADE_POWER);
            }

            AttributeInstance reach = player.getAttribute(ForgeMod.ENTITY_REACH.get());
            boolean extended = holdingSpear(player) && has(player, LONG_REACH);
            if (extended && reach.getModifier(SPEAR_REACH) == null) {
                reach.addTransientModifier(new AttributeModifier(SPEAR_REACH, "Long Reach", 0.5, Operation.ADDITION));
            } else if (!extended && reach.getModifier(SPEAR_REACH) != null) {
                reach.removeModifier(SPEAR_REACH);
            }
        }
    }

    /** Every fourth tick of loading skips one tick ahead, so a crossbow loads in 80% of the time. */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        ItemStack item = event.getItem();
        if (item.getItem() instanceof CrossbowItem && !CrossbowItem.isCharged(item) && event.getDuration() % 4 == 0
            && has(event.getEntity(), QUICK_RELOAD)) {
            event.setDuration(event.getDuration() - 1);
        }
    }

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        Player player = event.getEntity();
        Long last = LAST_MELEE.get(player.getUUID());
        if (last != null && player.level().getGameTime() - last <= SPELLBLADE_TICKS && has(player, SPELLBLADE_RHYTHM)) {
            event.setManaCost(Math.round(event.getManaCost() * 0.9F));
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }

        if ((dead.getMobType() == MobType.UNDEAD || dead.getMobType() == MobType.ARTHROPOD) && !event.getDrops().isEmpty()
            && has(killer, TROPHIES) && dead.getRandom().nextFloat() < 0.3F) {
            ItemEntity pick = event.getDrops().stream().skip(dead.getRandom().nextInt(event.getDrops().size())).findFirst().orElseThrow();
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), pick.getItem().copy()));
        }

        if (dead instanceof PatrollingMonster captain && captain.isPatrolLeader() && has(killer, BOUNTY)) {
            ItemStack emeralds = new ItemStack(Items.EMERALD, 3 + dead.getRandom().nextInt(4));
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), emeralds));
        }
    }
}
