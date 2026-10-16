package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The elemental arrows (built from Fire Arrow): a charged arrow shot where the caster looks, hitting for the spell's
 * power, with the element's effect on what it hits. Jade Arrow (StrikeSpells) is the spirit one.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class ArrowSpells {
    private static final String TAG = "mdvlcraft_element_arrow";
    private static final String MARKED_BY = "mdvlcraft_radiant_by";
    /** Arrows homing in on a Radiant Arrow mark: arrow to the marked creature. */
    private static final Map<AbstractArrow, LivingEntity> GUIDED = new HashMap<>();

    private ArrowSpells() {
    }

    public static class ElementArrow extends ModifiedSpell {
        private final String element;

        public ElementArrow(String element) {
            super("arrow_" + element, 4);
            this.element = element;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Arrow arrow = new Arrow(level, caster);
            arrow.setBaseDamage(this.getSpellPower(spellLevel, caster) / 3.0);
            arrow.setCritArrow(true);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putString(TAG, this.element);
            arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 3.0F, 0.5F);
            level.addFreshEntity(arrow);
            level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ElementArrow> all() {
        return List.of("ice", "lightning", "holy", "eldritch", "water", "blood").stream().map(ElementArrow::new).toList();
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.hasEffect(BinderEffects.RADIANT_MARK.get()) && owner.getStringUUID().equals(victim.getPersistentData().getString(MARKED_BY))) {
            event.setAmount(event.getAmount() * 1.5F);
        }
        String element = arrow.getPersistentData().getString(TAG);
        switch (element) {
            case "" -> {
            }
            case "ice" -> {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), owner);
                victim.setTicksFrozen(victim.getTicksRequiredToFreeze() + 140);
            }
            case "lightning" -> {
                AbstractSpell spell = SpellRegistry.getSpell(new ResourceLocation("mdvlcraft", "arrow_lightning"));
                Targets.enemies(owner, victim.position(), 6.0).stream().filter(e -> e != victim)
                    .min((a, b) -> Double.compare(a.distanceToSqr(victim), b.distanceToSqr(victim))).ifPresent(next -> {
                        DamageSources.applyDamage(next, event.getAmount(), spell.getDamageSource(owner));
                        SummonSpells.spark((ServerLevel)victim.level(), victim.getEyePosition(), next.getEyePosition());
                    });
            }
            case "holy" -> {
                victim.addEffect(new MobEffectInstance(BinderEffects.RADIANT_MARK.get(), 160, 0), owner);
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0), owner);
                victim.getPersistentData().putString(MARKED_BY, owner.getStringUUID());
            }
            case "eldritch" -> {
            }
            case "water" -> {
                victim.clearFire();
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), owner);
                victim.addEffect(new MobEffectInstance(BrandSpells.external("traveloptics:wet"), 160, 0), owner);
            }
            case "blood" -> victim.addEffect(new MobEffectInstance(BrandSpells.external("attributeslib:bleeding"), 100, 1), owner);
            default -> throw new IllegalStateException("Unknown arrow " + element);
        }
    }

    /** Void Arrow: where it lands, enemies within 5 blocks are pulled to it. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !"eldritch".equals(arrow.getPersistentData().getString(TAG))
            || !(arrow.level() instanceof ServerLevel level) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        arrow.getPersistentData().remove(TAG);
        Vec3 at = event.getRayTraceResult().getLocation();
        for (LivingEntity enemy : Targets.enemies(owner, at, 5.0)) {
            Vec3 in = at.subtract(enemy.position());
            enemy.setDeltaMovement(in.scale(0.3).add(0.0, 0.2, 0.0));
            enemy.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 60, 1.5, 0.8, 1.5, 0.2);
        level.playSound(null, arrow.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** Arrows shot by someone who has a Radiant Arrow mark out home in on the marked creature. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(event.getLevel() instanceof ServerLevel level)
            || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        String id = owner.getStringUUID();
        level.getEntitiesOfClass(LivingEntity.class, new AABB(owner.blockPosition()).inflate(48.0),
                e -> e.isAlive() && e.hasEffect(BinderEffects.RADIANT_MARK.get()) && id.equals(e.getPersistentData().getString(MARKED_BY)))
            .stream().findFirst().ifPresent(target -> GUIDED.put(arrow, target));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent event) {
        if (event.phase != Phase.END || GUIDED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<AbstractArrow, LivingEntity>> it = GUIDED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AbstractArrow, LivingEntity> guided = it.next();
            AbstractArrow arrow = guided.getKey();
            LivingEntity target = guided.getValue();
            Vec3 motion = arrow.getDeltaMovement();
            if (arrow.isRemoved() || !target.isAlive() || arrow.tickCount > 80 || motion.lengthSqr() < 0.04) {
                it.remove();
                continue;
            }
            Vec3 to = target.getBoundingBox().getCenter().subtract(arrow.position()).normalize().scale(motion.length());
            arrow.setDeltaMovement(motion.lerp(to, 0.2));
            arrow.hurtMarked = true;
        }
    }
}
