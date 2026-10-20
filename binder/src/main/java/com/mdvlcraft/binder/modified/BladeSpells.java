package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.KatanaWeapons;
import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The Modified blade strikes: Soul Rend, Phantom Draw and Leap of Faith; spell damage plus the weapon's. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class BladeSpells {
    /** Creatures that take no fall damage until the game time given. */
    private static final Map<UUID, Long> FALL_SAFE = new HashMap<>();
    private static final Map<UUID, Leap> LEAPS = new HashMap<>();

    private record Leap(LivingEntity caster, LivingEntity target, float damage, long until, Supplier<DamageSource> source) {
    }

    private BladeSpells() {
    }

    static void fallSafe(LivingEntity entity, int ticks) {
        FALL_SAFE.put(entity.getUUID(), entity.level().getGameTime() + ticks);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        Long until = FALL_SAFE.get(event.getEntity().getUUID());
        if (until != null) {
            if (event.getEntity().level().getGameTime() <= until) {
                event.setCanceled(true);
            }
            FALL_SAFE.remove(event.getEntity().getUUID());
        }
    }

    /** Soul Rend: Twin Fang's dash with both weapons' damage; it soulburns and gives back 3% mana for each creature hit. */
    public static class SoulRend extends ModifiedSpell {
        public SoulRend() {
            super("soul_rend", 3);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            float damage = this.getSpellPower(spellLevel, caster) + MartialSpells.attack(caster) + MartialSpells.TwinFang.offhandDamage(caster);
            for (LivingEntity target : MartialSpells.dash(caster, 6.0)) {
                if (DamageSources.applyDamage(target, damage, this.getDamageSource(caster))) {
                    BoltSpells.soulburn(caster, target, 100);
                    data.addMana((float)(caster.getAttributeValue(AttributeRegistry.MAX_MANA.get()) * 0.03));
                }
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                    server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Phantom Draw: katana only; Iaido's dashing cut, and a phantom of the caster left where it started repeats the cut
     * 0.75 s later for 60% (a player's phantom is a copy of them; anyone else's is a trail of souls).
     */
    public static class PhantomDraw extends ModifiedSpell {
        public PhantomDraw() {
            super("phantom_draw", 3);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            ItemStack held = caster.getMainHandItem();
            if (!KatanaWeapons.isKatana(MartialSpells.category(held), held)) {
                MartialSpells.refuse(caster, "need_katana");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 start = caster.position();
                float damage = this.getSpellPower(spellLevel, caster) + MartialSpells.attack(caster) * 1.6F;
                List<LivingEntity> cut = MartialSpells.dash(caster, 7.0);
                for (LivingEntity target : cut) {
                    DamageSources.applyDamage(target, damage, this.getDamageSource(caster));
                }
                Runnable echo = () -> {
                    for (LivingEntity target : cut) {
                        if (target.isAlive()) {
                            DamageSources.applyDamage(target, damage * 0.6F, this.getDamageSource(caster));
                            SmiteSpells.beam(server, start.add(0.0, 1.0, 0.0), target.getEyePosition(), ParticleTypes.SOUL);
                            server.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                    server.sendParticles(ParticleTypes.SOUL, start.x, start.y + 1.0, start.z, 20, 0.3, 0.6, 0.3, 0.05);
                    server.playSound(null, start.x, start.y, start.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
                };
                if (caster instanceof ServerPlayer player) {
                    DoppelgangerEntity.spawnCopy(player, start, 15, true, copy -> echo.run());
                } else {
                    Zones.start(server, start, caster, damage, 15, 1000, zone -> {
                    }, echo, null);
                }
                level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Leap of Faith: needs a creature looked at within 24 blocks and at least 3 blocks below; the caster dives at it, takes
     * no fall damage, and on landing strikes it for the spell's power plus three times its attack.
     */
    public static class LeapOfFaith extends ModifiedSpell {
        public LeapOfFaith() {
            super("leap_of_faith", 6);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            LivingEntity target = Targets.looked(caster, 24.0F);
            if (target == null || caster.getY() - target.getY() < 3.0) {
                MartialSpells.refuse(caster, "need_below");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 24.0F);
            if (target != null && level instanceof ServerLevel server) {
                Vec3 to = target.position().subtract(caster.position());
                Vec3 flat = to.multiply(1.0, 0.0, 1.0);
                caster.setDeltaMovement(flat.scale(0.12).add(0.0, 0.25, 0.0));
                caster.hurtMarked = true;
                fallSafe(caster, 100);
                float damage = this.getSpellPower(spellLevel, caster) + MartialSpells.attack(caster) * 3.0F;
                LEAPS.put(caster.getUUID(), new Leap(caster, target, damage, server.getGameTime() + 80, () -> this.getDamageSource(caster)));
                server.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY(), caster.getZ(), 20, 0.4, 0.2, 0.4, 0.05);
                level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** A diving caster lands its strike once it touches the ground near its target (or reaches it in the air). */
    @SubscribeEvent
    public static void onTick(ServerTickEvent event) {
        if (event.phase != Phase.END || LEAPS.isEmpty()) {
            return;
        }
        Iterator<Leap> it = LEAPS.values().iterator();
        while (it.hasNext()) {
            Leap leap = it.next();
            LivingEntity caster = leap.caster();
            LivingEntity target = leap.target();
            if (!caster.isAlive() || !target.isAlive() || caster.level() != target.level() || caster.level().getGameTime() > leap.until()) {
                it.remove();
                continue;
            }
            // steer the fall onto the target
            if (caster.tickCount % 4 == 0 && !caster.onGround()) {
                Vec3 flat = target.position().subtract(caster.position()).multiply(1.0, 0.0, 1.0);
                Vec3 motion = caster.getDeltaMovement();
                caster.setDeltaMovement(flat.scale(0.15).add(0.0, Math.min(motion.y, -0.3), 0.0));
                caster.hurtMarked = true;
            }
            if (caster.distanceTo(target) <= 2.5 || caster.onGround() && caster.distanceTo(target) <= 4.0) {
                DamageSources.applyDamage(target, leap.damage(), leap.source().get());
                ServerLevel level = (ServerLevel)caster.level();
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 30, 0.4, 0.5, 0.4, 0.3);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 0.7F);
                it.remove();
            } else if (caster.onGround() && caster.level().getGameTime() > leap.until() - 70) {
                it.remove();
            }
        }
    }
}
