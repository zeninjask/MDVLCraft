package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Modified summons of the dead (built from Raise Dead and Summon Drowned) and Soul Reaper. Undead summons wear an
 * unbreakable cap so the sun does not burn them; the Burning Dead burn on purpose and fire does not hurt them.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class UndeadSummons {
    private static final Set<String> FIREPROOF = Set.of("burning_dead");
    private static final int DURATION_FIRE = 20000;

    private UndeadSummons() {
    }

    /** An unbreakable leather cap (dyed `colour`, or plain when negative): keeps an undead summon out of the sun's fire. */
    static void cap(Mob mob, int colour) {
        ItemStack cap = new ItemStack(Items.LEATHER_HELMET);
        cap.getOrCreateTag().putBoolean("Unbreakable", true);
        if (colour >= 0) {
            ((DyeableLeatherItem)Items.LEATHER_HELMET).setColor(cap, colour);
        }
        equip(mob, EquipmentSlot.HEAD, cap);
    }

    static void equip(Mob mob, EquipmentSlot slot, ItemStack stack) {
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.0F);
    }

    static void stats(Mob mob, double health, double damage) {
        Objects.requireNonNull(mob.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(health);
        if (damage > 0.0) {
            Objects.requireNonNull(mob.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(damage);
        }
        mob.setHealth(mob.getMaxHealth());
    }

    /** Summon Skeleton Archers: two skeletons with bows that shoot from range. */
    public static class SkeletonArchers extends SummonSpell {
        public SkeletonArchers() {
            super("skeleton_archers", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.SKELETON, 2, castData, skeleton -> {
                equip(skeleton, EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                cap(skeleton, 0x4A3B2A);
                skeleton.reassessWeaponGoal();
            });
        }
    }

    /** Bone Bulwark: two skeleton shieldbearers in iron with sword and shield that stay close and fight beside you. */
    public static class BoneGuard extends SummonSpell {
        public BoneGuard() {
            super("bone_guard", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.SKELETON, 2, castData, skeleton -> {
                equip(skeleton, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                equip(skeleton, EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                ItemStack helmet = new ItemStack(Items.IRON_HELMET);
                helmet.getOrCreateTag().putBoolean("Unbreakable", true);
                equip(skeleton, EquipmentSlot.HEAD, helmet);
                equip(skeleton, EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                equip(skeleton, EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                equip(skeleton, EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                stats(skeleton, 30.0, 0.0);
                skeleton.reassessWeaponGoal();
            });
        }
    }

    /** Burning Dead: three zombies wreathed in fire (it does not hurt them) that burst into flame when they die. */
    public static class BurningDead extends SummonSpell {
        public BurningDead() {
            super("burning_dead", 5);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.ZOMBIE, 3, castData, zombie -> {
                zombie.setBaby(false);
                zombie.setRemainingFireTicks(DURATION_FIRE);
            });
        }
    }

    /** Galvanise: three zombies raised with lightning, fast, whose hits spark to a second enemy. */
    public static class Reanimate extends SummonSpell {
        public Reanimate() {
            super("reanimate", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.ZOMBIE, 3, castData, zombie -> {
                zombie.setBaby(false);
                cap(zombie, 0xF2E14C);
                zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 1, false, false));
            });
        }
    }

    /** Summon Strays: two strays, whose arrows slow. */
    public static class Strays extends SummonSpell {
        public Strays() {
            super("strays", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.STRAY, 2, castData, stray -> {
                equip(stray, EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                cap(stray, 0xD8EEF5);
                stray.reassessWeaponGoal();
            });
        }
    }

    /** Drowned Legion: three drowned with tridents, which they throw. */
    public static class DrownedLegion extends SummonSpell {
        public DrownedLegion() {
            super("drowned_legion", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.DROWNED, 3, castData, drowned -> {
                drowned.setBaby(false);
                equip(drowned, EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
                cap(drowned, 0x2F7F8F);
            });
        }
    }

    /** Pirate Crew: three drowned pirates with swords and red bandanas. */
    public static class PirateCrew extends SummonSpell {
        public PirateCrew() {
            super("pirate_crew", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.DROWNED, 3, castData, drowned -> {
                drowned.setBaby(false);
                equip(drowned, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                cap(drowned, 0xB02E26);
                stats(drowned, 26.0, 4.0 + power * 0.5);
            });
        }
    }

    /** Drowned Horde: five weak drowned rise around the creature looked at (16 blocks) and go for it. */
    public static class DrownedHorde extends SummonSpell {
        public DrownedHorde() {
            super("drowned_dead", 3);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (Targets.looked(caster, 16.0F) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        @Override
        protected int duration() {
            return 900;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            LivingEntity target = Targets.looked(caster, 16.0F);
            int[] index = {0};
            this.spawn(level, caster, power, EntityType.DROWNED, 5, castData, drowned -> {
                drowned.setBaby(false);
                cap(drowned, 0x2F7F8F);
                stats(drowned, 10.0, 0.0);
                if (target != null) {
                    float angle = Mth.TWO_PI * index[0]++ / 5;
                    drowned.moveTo(target.getX() + 2.0 * Mth.cos(angle), target.getY(), target.getZ() + 2.0 * Mth.sin(angle), 0.0F, 0.0F);
                    drowned.setTarget(target);
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, drowned.getX(), drowned.getY(), drowned.getZ(), 15, 0.3, 0.1, 0.3, 0.1);
                }
            });
        }
    }

    /**
     * Soul Reaper: a wide blood slash (6 blocks, 120 degrees ahead) for the spell's power; each creature it kills rises as a
     * weakened skeleton that fights for the caster for 30 s.
     */
    public static class SoulReaper extends ModifiedSpell {
        public SoulReaper() {
            super("soul_reaper", 7);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float damage = this.getSpellPower(spellLevel, caster);
                Vec3 look = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                int raised = 0;
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                    Vec3 to = enemy.position().subtract(caster.position()).multiply(1.0, 0.0, 1.0);
                    if (to.lengthSqr() > 0.01 && to.normalize().dot(look) < 0.5) {
                        continue;
                    }
                    DamageSources.applyDamage(enemy, damage, this.getDamageSource(caster));
                    if (!enemy.isAlive() && raised < 3) {
                        raised++;
                        Vec3 at = enemy.position();
                        Summons.summon(server, caster, EntityType.SKELETON, 1, 600, "soul_reaper", new SummonedEntitiesCastData(), skeleton -> {
                            skeleton.moveTo(at.x, at.y, at.z, enemy.getYRot(), 0.0F);
                            skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                            cap(skeleton, 0x7A1B1B);
                            stats(skeleton, 10.0, 0.0);
                            skeleton.reassessWeaponGoal();
                        });
                        server.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 20, 0.3, 0.6, 0.3, 0.05);
                    }
                }
                for (int i = -6; i <= 6; i++) {
                    Vec3 dir = look.yRot(i * 10.0F * Mth.DEG_TO_RAD);
                    Vec3 at = caster.position().add(dir.scale(3.0)).add(0.0, 1.0, 0.0);
                    server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.0);
                    server.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** The Burning Dead stay alight (a zombie on fire sets what it hits alight). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && Summons.kind(mob).equals("burning_dead")) {
            mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 20, m -> m.setRemainingFireTicks(DURATION_FIRE)));
        }
    }

    @SubscribeEvent
    public static void onFire(LivingAttackEvent event) {
        if (FIREPROOF.contains(Summons.kind(event.getEntity())) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }

    /** A Burning Dead bursts into flame when it dies. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!Summons.kind(dead).equals("burning_dead") || !(dead.level() instanceof ServerLevel level)
            || !(Summons.owner(dead) instanceof LivingEntity owner)) {
            return;
        }
        float power = SummonSpells.power(dead);
        for (LivingEntity enemy : Targets.enemies(owner, dead.position(), 3.0)) {
            enemy.hurt(dead.damageSources().inFire(), power);
            enemy.setSecondsOnFire(5);
        }
        level.sendParticles(ParticleTypes.FLAME, dead.getX(), dead.getY(0.5), dead.getZ(), 60, 1.2, 0.6, 1.2, 0.08);
        level.playSound(null, dead.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.2F, 0.6F);
    }

    /** What the undead summons' hits do on top of their damage. */
    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !(Summons.owner(attacker) instanceof LivingEntity owner)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (Summons.kind(attacker).equals("reanimate")) {
            Targets.enemies(owner, victim.position(), 4.0).stream().filter(e -> e != victim).findFirst().ifPresent(next -> {
                next.hurt(attacker.damageSources().lightningBolt(), SummonSpells.power(attacker) * 0.5F);
                SummonSpells.spark((ServerLevel)attacker.level(), victim.getEyePosition(), next.getEyePosition());
            });
        }
    }
}
