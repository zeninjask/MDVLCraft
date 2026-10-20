package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The arrow Modified abilities (vanilla arrows and tridents, tagged with the ability): volleys that rain on the spot looked
 * at, a silent shot and a grappling arrow. A volley's arrows and tridents vanish when they land.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class VolleySpells {
    private static final String TAG = "mdvlcraft_volley";
    private static final List<AbstractArrow> LANDED = new ArrayList<>();

    private VolleySpells() {
    }

    /** The block point looked at within `reach` (or the end of the look). */
    static Vec3 lookedPoint(LivingEntity caster, double reach) {
        Vec3 eye = caster.getEyePosition();
        return caster.level().clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(reach)), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, caster)).getLocation();
    }

    /** 14 arrows (or tridents) falling on the spot looked at: most on the enemies within 4 blocks of it, the rest around it. */
    public static class Volley extends ModifiedSpell {
        private final String kind;

        Volley(String key, String kind) {
            super(key, 6);
            this.kind = kind;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = lookedPoint(caster, 32.0);
                float power = this.getSpellPower(spellLevel, caster);
                List<LivingEntity> enemies = Targets.enemies(caster, at, 4.0);
                for (int i = 0; i < 14; i++) {
                    AbstractArrow arrow = this.kind.equals("trident")
                        ? new ThrownTrident(level, caster, new ItemStack(Items.TRIDENT)) : new Arrow(level, caster);
                    arrow.setBaseDamage(power / 3.0);
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    arrow.getPersistentData().putString(TAG, this.kind);
                    // most fall on the enemies there, the rest anywhere within 3 blocks
                    Vec3 aim = !enemies.isEmpty() && i % 3 != 2 ? enemies.get(i % enemies.size()).position()
                        : at.add((caster.getRandom().nextDouble() - 0.5) * 6.0, 0.0, (caster.getRandom().nextDouble() - 0.5) * 6.0);
                    Vec3 from = aim.add((caster.getRandom().nextDouble() - 0.5) * 2.0, 12.0 + caster.getRandom().nextDouble() * 4.0,
                        (caster.getRandom().nextDouble() - 0.5) * 2.0);
                    Vec3 down = aim.add(0.0, 0.5, 0.0).subtract(from);
                    arrow.moveTo(from.x, from.y, from.z);
                    arrow.shoot(down.x, down.y, down.z, 1.6F, 1.0F);
                    if (this.kind.equals("flaming")) {
                        arrow.setSecondsOnFire(100);
                    }
                    level.addFreshEntity(arrow);
                }
                server.sendParticles(this.kind.equals("thorn") ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.CLOUD,
                    at.x, at.y + 12.0, at.z, 40, 3.0, 0.5, 3.0, 0.02);
                level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Silent Shot: a silent arrow; shot invisible or sneaking it crits for half again as much. */
    public static class SilentShot extends ModifiedSpell {
        SilentShot() {
            super("silent_shot", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            boolean stealth = caster.isInvisible() || caster.isCrouching();
            Arrow arrow = new Arrow(level, caster);
            arrow.setBaseDamage(this.getSpellPower(spellLevel, caster) / 3.0 * (stealth ? 1.5 : 1.0));
            arrow.setCritArrow(stealth);
            arrow.setSilent(true);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putString(TAG, "silent");
            arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 3.5F, 0.2F);
            level.addFreshEntity(arrow);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Grapple Arrow: where the arrow lands, the caster is pulled to it (no fall damage for 3 s). */
    public static class GrappleArrow extends ModifiedSpell {
        GrappleArrow() {
            super("grapple_arrow", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Arrow arrow = new Arrow(level, caster);
            arrow.setBaseDamage(0.5);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putString(TAG, "grapple");
            arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 3.0F, 0.2F);
            level.addFreshEntity(arrow);
            level.playSound(null, caster.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(new Volley("flaming_volley", "flaming"), new Volley("thorn_volley", "thorn"), new Volley("trident_rain", "trident"),
            new SilentShot(), new GrappleArrow());
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        switch (arrow.getPersistentData().getString(TAG)) {
            case "flaming" -> victim.setSecondsOnFire(5);
            case "thorn" -> {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), owner);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), owner);
            }
            case "trident" -> {
                victim.addEffect(new MobEffectInstance(BrandSpells.external("traveloptics:wet"), 160, 0), owner);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), owner);
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) {
            return;
        }
        String kind = arrow.getPersistentData().getString(TAG);
        if (kind.equals("grapple") && arrow.getOwner() instanceof LivingEntity owner && owner.level() == level) {
            arrow.getPersistentData().remove(TAG);
            Vec3 to = event.getRayTraceResult().getLocation().subtract(owner.position());
            owner.setDeltaMovement(to.normalize().scale(Math.min(2.6, 0.6 + to.length() * 0.14)).add(0.0, 0.45, 0.0));
            owner.hurtMarked = true;
            BladeSpells.fallSafe(owner, 60);
            SmiteSpells.beam(level, owner.getEyePosition(), event.getRayTraceResult().getLocation(), ParticleTypes.CRIT);
            LANDED.add(arrow);
        } else if (!kind.isEmpty() && !kind.equals("silent") && event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
            LANDED.add(arrow);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent event) {
        if (event.phase == Phase.END && !LANDED.isEmpty()) {
            LANDED.forEach(AbstractArrow::discard);
            LANDED.clear();
        }
    }
}
