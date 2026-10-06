package com.mdvlcraft.binder.attribute;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** What the binder's skill-tree attributes do. */
@Mod.EventBusSubscriber(modid = MDVLBinder.MOD_ID)
public final class AttributeEffects {
    private static final UUID EPIC_FIGHT_LISTENER = UUID.fromString("6f1c3e0a-4d2b-4a8e-9a51-2b7d8f0c9e11");
    private static final int RIPOSTE_WINDOW_TICKS = 60;
    private static final int NATURE_REGEN_INTERVAL_TICKS = 100;
    /** Game time until which each Phantom's next melee hit gets riposte damage. */
    private static final Map<UUID, Long> RIPOSTE_UNTIL = new HashMap<>();

    private AttributeEffects() {
    }

    private static double value(Player player, RegistryObject<Attribute> attribute) {
        return player.getAttributeValue(attribute.get());
    }

    private static boolean isMelee(DamageSource source, Player attacker) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == attacker;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker && isMelee(source, attacker)) {
            event.setAmount(event.getAmount() * (float) (1 + meleeBonus(attacker, target)));
        }
    }

    /** Sum of the weapon, backstab and riposte bonuses that apply to this melee hit. */
    private static double meleeBonus(ServerPlayer attacker, LivingEntity target) {
        CapabilityItem weapon = EpicFightCapabilities.getItemStackCapability(attacker.getMainHandItem());
        WeaponCategory category = weapon.getWeaponCategory();
        double bonus = 0;
        if (category == CapabilityItem.WeaponCategories.GREATSWORD) {
            bonus += value(attacker, BinderAttributes.GREATSWORD_DAMAGE);
        } else if (category == CapabilityItem.WeaponCategories.UCHIGATANA || category == CapabilityItem.WeaponCategories.TACHI) {
            bonus += value(attacker, BinderAttributes.KATANA_DAMAGE);
        } else if (category == CapabilityItem.WeaponCategories.DAGGER) {
            bonus += value(attacker, BinderAttributes.DAGGER_DAMAGE);
        } else if (category == CapabilityItem.WeaponCategories.SWORD && attacker.getOffhandItem().isEmpty()) {
            bonus += value(attacker, BinderAttributes.RAPIER_DAMAGE);
        }
        Vec3 facing = Vec3.directionFromRotation(0, target.getYRot());
        Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        if (facing.dot(toAttacker) < -0.5) {
            bonus += value(attacker, BinderAttributes.BACKSTAB_DAMAGE);
        }
        Long riposteUntil = RIPOSTE_UNTIL.remove(attacker.getUUID());
        if (riposteUntil != null && riposteUntil >= attacker.level().getGameTime()) {
            bonus += value(attacker, BinderAttributes.RIPOSTE_DAMAGE);
        }
        return bonus;
    }

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FoodProperties food = event.getItem().getFoodProperties(player);
        double bonus = value(player, BinderAttributes.SATURATION_BONUS);
        if (food != null && bonus > 0) {
            FoodData foodData = player.getFoodData();
            float extra = (float) (food.getNutrition() * food.getSaturationModifier() * 2 * bonus);
            foodData.setSaturation(Math.min(foodData.getSaturationLevel() + extra, foodData.getFoodLevel()));
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() instanceof Player || event.getDrops().isEmpty()) {
            return;
        }
        if (player.getRandom().nextDouble() < value(player, BinderAttributes.PLUNDER)) {
            List<ItemEntity> drops = List.copyOf(event.getDrops());
            ItemEntity original = drops.get(player.getRandom().nextInt(drops.size()));
            event.getDrops().add(new ItemEntity(original.level(), original.getX(), original.getY(), original.getZ(), original.getItem().copy()));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % NATURE_REGEN_INTERVAL_TICKS != 0) {
            return;
        }
        double regen = value(player, BinderAttributes.NATURE_REGEN);
        if (regen > 0 && player.onGround() && player.level().getBlockState(player.getOnPos()).is(BlockTags.DIRT)
                && player.level().canSeeSky(player.blockPosition())) {
            player.heal((float) regen);
        }
    }

    /** Epic Fight parry, dodge and skill-cost hooks are only available through its per-player event listener. */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null) {
            throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
        }
        // Default priority (-1) runs after Epic Fight's guard skills have decided whether the hit was parried.
        patch.getEventListener().addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EPIC_FIGHT_LISTENER, attack -> {
            double restore = value(player, BinderAttributes.PARRY_STAMINA);
            if (attack.isParried() && restore > 0) {
                patch.setStamina((float) Math.min(patch.getMaxStamina(), patch.getStamina() + patch.getMaxStamina() * restore));
            }
        });
        patch.getEventListener().addEventListener(PlayerEventListener.EventType.SKILL_CONSUME_EVENT, EPIC_FIGHT_LISTENER, consume -> {
            if (consume.getSkill() instanceof DodgeSkill) {
                consume.setAmount(consume.getAmount() * (float) (1 - value(player, BinderAttributes.DODGE_EFFICIENCY)));
            }
        });
        patch.getEventListener().addEventListener(PlayerEventListener.EventType.DODGE_SUCCESS_EVENT, EPIC_FIGHT_LISTENER, dodge -> {
            if (value(player, BinderAttributes.RIPOSTE_DAMAGE) > 0) {
                RIPOSTE_UNTIL.put(player.getUUID(), player.level().getGameTime() + RIPOSTE_WINDOW_TICKS);
            }
        });
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RIPOSTE_UNTIL.remove(event.getEntity().getUUID());
    }
}
