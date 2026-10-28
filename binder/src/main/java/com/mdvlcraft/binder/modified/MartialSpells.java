package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.KatanaWeapons;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/** The martial Modified abilities: weapon strikes, dashes and stomps, scaled by the caster's attack damage. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MartialSpells {
    private static final UUID PARRY_LISTENER = UUID.fromString("5c1d7e2a-8b4f-4f0e-9a63-2d7b8e1f4c20");
    private static final Map<UUID, Long> LAST_PARRY = new HashMap<>();

    private MartialSpells() {
    }

    static void refuse(LivingEntity caster, String key) {
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("ability.mdvlcraft.modified." + key).withStyle(ChatFormatting.RED), true);
        }
    }

    static DamageSource melee(LivingEntity caster) {
        return caster instanceof Player player ? caster.damageSources().playerAttack(player) : caster.damageSources().mobAttack(caster);
    }

    static float attack(LivingEntity caster) {
        return (float)caster.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    static WeaponCategory category(ItemStack stack) {
        return EpicFightCapabilities.getItemStackCapability(stack).getWeaponCategory();
    }

    /** Epic Fight's stun on a creature that has a patch. */
    public static void stun(LivingEntity target, StunType type, float seconds) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        if (patch != null) {
            patch.applyStun(type, seconds);
        }
    }

    static void faceTowards(LivingEntity entity, LivingEntity other) {
        float yaw = (float)(Math.atan2(other.getZ() - entity.getZ(), other.getX() - entity.getX()) * (180.0 / Math.PI)) - 90.0F;
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }

    public static void root(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false));
    }

    /** Throws the caster forward and returns everything not on its side along the way (up to `distance` blocks, blocks stop it). */
    static List<LivingEntity> dash(LivingEntity caster, double distance) {
        Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        Vec3 start = caster.position().add(0.0, 0.5, 0.0);
        Vec3 end = caster.level().clip(new ClipContext(start, start.add(look.scale(distance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster))
            .getLocation();
        AABB swept = new AABB(start, end).inflate(1.2, 1.0, 1.2);
        List<LivingEntity> hit = caster.level().getEntitiesOfClass(LivingEntity.class, swept,
            e -> e != caster && e.isAlive() && !Summons.allied(caster, e));
        caster.setDeltaMovement(look.scale(Math.min(distance, end.distanceTo(start)) * 0.45).add(0.0, 0.12, 0.0));
        caster.hurtMarked = true;
        if (caster.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, (start.x + end.x) / 2, start.y + 0.5, (start.z + end.z) / 2, 4, 1.0, 0.2, 1.0, 0.0);
        }
        return hit;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null) {
                throw new IllegalStateException("Epic Fight patch missing on " + player.getGameProfile().getName());
            }
            patch.getEventListener().addEventListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, PARRY_LISTENER, attack -> {
                if (attack.isParried()) {
                    LAST_PARRY.put(player.getUUID(), player.level().getGameTime());
                }
            });
        }
    }

    static boolean parriedWithin(LivingEntity caster, int ticks) {
        Long last = LAST_PARRY.get(caster.getUUID());
        return last != null && caster.level().getGameTime() - last <= ticks;
    }

    /** Shield Bash: needs a shield in the off hand; slams the creature looked at, stunning and knocking it back. */
    public static class ShieldBash extends ModifiedSpell {
        public ShieldBash() {
            super("shield_bash", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!caster.getOffhandItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
                refuse(caster, "need_shield");
                return false;
            }
            if (Targets.looked(caster, 4.0F) == null) {
                refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 4.0F);
            if (target != null) {
                faceTowards(target, caster);   // Epic Fight's stun throws the target backwards from where it faces
                target.hurt(melee(caster), attack(caster) * 0.6F + 4.0F);
                stun(target, StunType.LONG, 1.5F);
                Vec3 away = target.position().subtract(caster.position()).normalize();
                target.knockback(1.2, -away.x, -away.z);
                level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.7F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Provoke: every mob within 12 blocks turns on the caster, who gains Resistance I for 10 s. */
    public static class Provoke extends ModifiedSpell {
        public Provoke() {
            super("provoke", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 12.0)) {
                if (enemy instanceof Mob mob) {
                    mob.setTarget(caster);
                }
            }
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ANGRY_VILLAGER, caster.getX(), caster.getY() + 2.2, caster.getZ(), 6, 0.5, 0.2, 0.5, 0.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8F, 1.3F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Javelin Hurl: needs a spear in the main hand; its flight pierces one creature and comes back (JavelinProjectile). */
    public static class Javelin extends ModifiedSpell {
        public Javelin() {
            super("javelin", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (category(caster.getMainHandItem()) != WeaponCategories.SPEAR) {
                refuse(caster, "need_spear");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            JavelinProjectile javelin = new JavelinProjectile(level, caster.getMainHandItem().copy());
            javelin.setOwner(caster);
            javelin.setPos(caster.getEyePosition().subtract(0.0, javelin.getBoundingBox().getYsize() * 0.5, 0.0));
            javelin.shoot(caster.getLookAngle());
            javelin.setDamage(attack(caster) * 1.4F);
            level.addFreshEntity(javelin);
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Iaido Draw: katana only; a dashing draw cut, doubled if the caster parried in the last 2 s. */
    public static class Iaido extends ModifiedSpell {
        public Iaido() {
            super("iaido", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            ItemStack held = caster.getMainHandItem();
            if (!KatanaWeapons.isKatana(category(held), held)) {
                refuse(caster, "need_katana");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float damage = attack(caster) * 1.6F * (parriedWithin(caster, 40) ? 2.0F : 1.0F);
            for (LivingEntity target : dash(caster, 7.0)) {
                target.hurt(melee(caster), damage);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Twin Fang: a dash that strikes once with each hand, with both weapons' damage. */
    public static class TwinFang extends ModifiedSpell {
        public TwinFang() {
            super("twin_fang", 0);
        }

        static float offhandDamage(LivingEntity caster) {
            ItemStack off = caster.getOffhandItem();
            double damage = 0.0;
            for (AttributeModifier modifier : off.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    damage += modifier.getAmount();
                }
            }
            return (float)damage;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float damage = attack(caster) + offhandDamage(caster);
            for (LivingEntity target : dash(caster, 6.0)) {
                target.hurt(melee(caster), damage);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.1F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Seismic Slam: a stomp hurting and rooting (2 s) everything within 5 blocks; damage scales with spell power. */
    public static class QuakeSlam extends ModifiedSpell {
        public QuakeSlam() {
            super("quake_slam", 7);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float damage = this.getSpellPower(spellLevel, caster);
            for (LivingEntity target : Targets.enemies(caster, caster.position(), 5.0)) {
                target.hurt(this.getDamageSource(caster), damage);
                target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.35, 0.0));
                target.hurtMarked = true;
                root(target, 40);
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, caster.getBlockStateOn()), caster.getX(), caster.getY() + 0.1,
                    caster.getZ(), 80, 2.5, 0.1, 2.5, 0.2);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }
}
