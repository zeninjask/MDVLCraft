package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The Modified abilities that change the caster for a while (wings, forms, wards, marks) and the marks it puts on others.
 * Each gives a binder effect (BinderEffects); what the effect does is in the event handlers below.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class FormSpells {
    private static final String MARKED_BY = "mdvlcraft_reaper_by";
    private static final String HAUNTING = "mdvlcraft_haunting";

    private FormSpells() {
    }

    static MobEffect angelWings() {
        return BrandSpells.external("irons_spellbooks:angel_wings");
    }

    /** A spell that gives the caster `effect` (and anything `extra` adds) for `seconds`. */
    public static class SelfBuff extends ModifiedSpell {
        private final Supplier<MobEffect> effect;
        private final int seconds;

        SelfBuff(String key, int power, Supplier<MobEffect> effect, int seconds) {
            super(key, power);
            this.effect = effect;
            this.seconds = seconds;
        }

        void extra(ServerLevel level, LivingEntity caster, float power) {
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(this.effect.get(), this.seconds * 20, 0, false, true, true));
            if (level instanceof ServerLevel server) {
                this.extra(server, caster, this.getSpellPower(spellLevel, caster));
                server.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Avenging Wings: Angel Wing's flight for 20 s, and every melee hit calls down a sunbeam for a third more holy damage. */
    public static class HolyAvenger extends SelfBuff {
        HolyAvenger() {
            super("holy_avenger", 0, BinderEffects.AVENGING_WINGS, 20);
        }

        @Override
        void extra(ServerLevel level, LivingEntity caster, float power) {
            caster.addEffect(new MobEffectInstance(angelWings(), 400, 0, false, false, true));
        }
    }

    /** Phoenix Wings: Angel Wing's flight for 15 s; enemies within 3 blocks below and around the caster catch fire. */
    public static class PhoenixWings extends ModifiedSpell {
        PhoenixWings() {
            super("phoenix_wings", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            caster.addEffect(new MobEffectInstance(angelWings(), 300, 0, false, false, true));
            if (level instanceof ServerLevel server) {
                Zones.start(server, caster.position(), caster, 0.0F, 300, 10, zone -> {
                    Vec3 at = caster.position();
                    server.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.0, at.z, 12, 0.6, 0.4, 0.6, 0.02);
                    for (LivingEntity enemy : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3.0, 4.0, 3.0).move(0.0, -2.0, 0.0),
                        e -> e != caster && e.isAlive() && !Summons.allied(caster, e))) {
                        enemy.setSecondsOnFire(4);
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Heat Mirage: 10 s in which mobs lose and can't find the caster, who takes half fire damage. */
    public static class HeatMirage extends SelfBuff {
        HeatMirage() {
            super("heat_mirage", 0, BinderEffects.HEAT_MIRAGE, 10);
        }

        @Override
        void extra(ServerLevel level, LivingEntity caster, float power) {
            forget(caster, 16.0);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.5, 0.8, 0.5, 0.01);
        }
    }

    /** Fox Glamour: 30 s invisible, ignored by mobs until the caster attacks. */
    public static class FoxGlamour extends SelfBuff {
        FoxGlamour() {
            super("fox_glamour", 0, BinderEffects.FOX_GLAMOUR, 30);
        }

        @Override
        void extra(ServerLevel level, LivingEntity caster, float power) {
            caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, false, false, true));
            forget(caster, 16.0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        }
    }

    static void forget(LivingEntity caster, double radius) {
        for (Mob mob : caster.level().getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(radius), m -> m.getTarget() == caster)) {
            mob.setTarget(null);
        }
    }

    /** Lich Form: 30 s in which the caster can't be healed, its spells cost health instead of mana and deal 30% more. */
    public static class LichForm extends SelfBuff {
        LichForm() {
            super("lich_form", 0, BinderEffects.LICH, 30);
        }

        @Override
        void extra(ServerLevel level, LivingEntity caster, float power) {
            level.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
        }
    }

    /** Spirit Aegis: 10 s in which projectiles break on the caster and whatever strikes it in melee is soulburned. */
    public static class SpiritAegis extends SelfBuff {
        SpiritAegis() {
            super("spirit_aegis", 0, BinderEffects.SPIRIT_AEGIS, 10);
        }
    }

    /** Spirit Ward: 20 s of 8 absorption hearts' worth from circling spirits; whatever hits the caster while they last is soulburned. */
    public static class SpiritWard extends SelfBuff {
        SpiritWard() {
            super("spirit_ward", 0, BinderEffects.SPIRIT_GUARD, 20);
        }

        @Override
        void extra(ServerLevel level, LivingEntity caster, float power) {
            caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1, false, true, true));
        }
    }

    /** Lightning Rod: allies within 8 blocks get 4 absorption hearts and, for 20 s, take no lightning damage. */
    public static class LightningRod extends ModifiedSpell {
        LightningRod() {
            super("lightning_rod", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity ally : Targets.allies(caster, caster.position(), 8.0)) {
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 0, false, true, true), caster);
                ally.addEffect(new MobEffectInstance(BinderEffects.LIGHTNING_ROD.get(), 400, 0, false, true, true), caster);
            }
            if (level instanceof ServerLevel server) {
                GroundSpells.ring(server, caster.position(), 8.0, ParticleTypes.ELECTRIC_SPARK);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Warcry: every mob within 12 blocks turns on the caster, who has Strength I for as long (up to 15 s) as one targets it. */
    public static class Warcry extends ModifiedSpell {
        Warcry() {
            super("warcry", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 12.0)) {
                    if (enemy instanceof Mob mob) {
                        mob.setTarget(caster);
                    }
                }
                Zones.start(server, caster.position(), caster, 0.0F, 300, 20, zone -> {
                    if (!server.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(24.0), m -> m.getTarget() == caster).isEmpty()) {
                        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, true, true));
                    }
                }, () -> {
                }, null);
                server.sendParticles(ParticleTypes.ANGRY_VILLAGER, caster.getX(), caster.getY() + 2.2, caster.getZ(), 8, 0.6, 0.2, 0.6, 0.0);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Haunting: players only; a ghostly copy of the caster stays where it stood for 10 s and draws enemies near it; a mob
     * that strikes it flees for 3 s.
     */
    public static class Haunting extends ModifiedSpell {
        Haunting() {
            super("haunting", 0);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            return CloneSpells.player(caster);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster instanceof ServerPlayer player) {
                DoppelgangerEntity copy = DoppelgangerEntity.spawnCopy(player, player.position(), 1, 200, true,
                    c -> CloneSpells.puff((ServerLevel)c.level(), c.position()));
                copy.getPersistentData().putBoolean(HAUNTING, true);
                copy.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false, false));
                CloneSpells.taunt(player, copy, 12.0);
                CloneSpells.puff((ServerLevel)level, player.position());
                level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Spirit Communion: a channel (up to 10 s); every pulse the caster crouches still it gets back 4% mana and 8% stamina
     * and its spirit spells deal 20% more for 20 s.
     */
    public static class SpiritCommunion extends ModifiedSpell {
        SpiritCommunion() {
            super("spirit_communion", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (caster.isCrouching() && caster.getDeltaMovement().horizontalDistanceSqr() < 0.0004) {
                data.addMana((float)(caster.getAttributeValue(AttributeRegistry.MAX_MANA.get()) * 0.04));
                BuffSpells.restoreStamina(caster, 0.08F);
                caster.addEffect(new MobEffectInstance(BinderEffects.SPIRIT_COMMUNION.get(), 400, 0, false, true, true));
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY() + 1.0, caster.getZ(), 10, 0.8, 0.6, 0.8, 0.02);
                }
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hungry Ghosts: 15 s in which every kill sends a ghost at the nearest enemy (soulburn) and adds 3 s. */
    public static class HungryGhost extends SelfBuff {
        HungryGhost() {
            super("hungry_ghost", 0, BinderEffects.HUNGRY_GHOSTS, 15);
        }
    }

    /** A mark on the creature looked at within 24 blocks. */
    abstract static class Mark extends ModifiedSpell {
        Mark(String key, int power) {
            super(key, power);
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (Targets.looked(caster, 24.0F) == null) {
                MartialSpells.refuse(caster, "no_target");
                return false;
            }
            return true;
        }

        abstract void mark(LivingEntity caster, LivingEntity target, float power);

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 24.0F);
            if (target != null) {
                this.mark(caster, target, this.getSpellPower(spellLevel, caster));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0), caster);
                level.playSound(null, target.blockPosition(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.5F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Reaper's Mark: 20 s; if the marked creature dies, its soul bursts, hurting and soulburning the marker's enemies within 5 blocks. */
    public static class ReaperMark extends Mark {
        ReaperMark() {
            super("reaper_mark", 8);
        }

        @Override
        void mark(LivingEntity caster, LivingEntity target, float power) {
            target.addEffect(new MobEffectInstance(BinderEffects.REAPER_MARK.get(), 400, 0), caster);
            target.getPersistentData().putUUID(MARKED_BY, caster.getUUID());
            target.getPersistentData().putFloat(MARKED_BY + "_power", power);
        }
    }

    /** Bounty Mark: 60 s; if the marked creature dies, it drops its loot twice and three times the experience. */
    public static class Bounty extends Mark {
        Bounty() {
            super("bounty", 0);
        }

        @Override
        void mark(LivingEntity caster, LivingEntity target, float power) {
            target.addEffect(new MobEffectInstance(BinderEffects.BOUNTY.get(), 1200, 0), caster);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0), caster);
        }
    }

    /** Tracking: every creature within 32 blocks glows for 20 s and leaves a trail of particles where it walks. */
    public static class Tracking extends ModifiedSpell {
        Tracking() {
            super("tracking", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                List<LivingEntity> tracked = new ArrayList<>(server.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(32.0),
                    e -> e != caster && e.isAlive()));
                tracked.forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0), caster));
                Zones.start(server, caster.position(), caster, 0.0F, 400, 5, zone -> {
                    for (LivingEntity e : tracked) {
                        if (e.isAlive()) {
                            server.sendParticles(ParticleTypes.WAX_ON, e.getX(), e.getY() + 0.1, e.getZ(), 1, 0.1, 0.0, 0.1, 0.0);
                        }
                    }
                }, () -> {
                }, null);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(new HolyAvenger(), new PhoenixWings(), new HeatMirage(), new FoxGlamour(), new LichForm(), new SpiritAegis(),
            new SpiritWard(), new LightningRod(), new Warcry(), new Haunting(), new SpiritCommunion(), new HungryGhost(), new ReaperMark(),
            new Bounty(), new Tracking());
    }

    // --- what the effects do ---

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity hitter && hitter.hasEffect(BinderEffects.AVENGING_WINGS.get()) && BuffSpells.melee(event, hitter)
            && victim.level() instanceof ServerLevel level) {
            float extra = event.getAmount() / 3.0F;
            DamageSources.ignoreNextKnockback(victim);
            DamageSources.applyDamage(victim, extra, SpellRegistry.getSpell(new ResourceLocation("mdvlcraft", "holy_avenger")).getDamageSource(hitter));
            for (int y = 0; y < 8; y++) {
                level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + y, victim.getZ(), 3, 0.15, 0.3, 0.15, 0.0);
            }
        }
        if (victim.hasEffect(BinderEffects.HEAT_MIRAGE.get()) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        if (attacker instanceof LivingEntity hitter && hitter != victim && victim.hasEffect(BinderEffects.SPIRIT_GUARD.get())
            && victim.getAbsorptionAmount() > 0.0F) {
            BoltSpells.soulburn(victim, hitter, 60);
        }
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (victim.hasEffect(BinderEffects.SPIRIT_AEGIS.get())) {
            if (direct instanceof Projectile projectile) {
                event.setCanceled(true);
                projectile.discard();
                if (victim.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.SOUL, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.1, 0.1, 0.1, 0.02);
                }
                return;
            }
            if (direct instanceof LivingEntity hitter && hitter != victim) {
                BoltSpells.soulburn(victim, hitter, 80);
            }
        }
        if (victim.hasEffect(BinderEffects.LIGHTNING_ROD.get()) && (event.getSource().is(DamageTypeTags.IS_LIGHTNING)
            || event.getSource() instanceof SpellDamageSource spell && spell.spell().getSchoolType().getId().getPath().equals("lightning"))) {
            event.setCanceled(true);
        }
        // a fox-glamoured caster that attacks shows itself
        if (event.getSource().getEntity() instanceof LivingEntity hitter && hitter.hasEffect(BinderEffects.FOX_GLAMOUR.get())) {
            hitter.removeEffect(BinderEffects.FOX_GLAMOUR.get());
            hitter.removeEffect(MobEffects.INVISIBILITY);
        }
        // a mob that strikes a haunting flees from it for 3 s
        if (victim.getPersistentData().getBoolean(HAUNTING) && event.getSource().getEntity() instanceof PathfinderMob mob) {
            Vec3 away = mob.position().subtract(victim.position()).multiply(1.0, 0.0, 1.0).normalize().scale(10.0);
            mob.setTarget(null);
            mob.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.5);
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            victim.level().playSound(null, victim.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 1.0F, 1.4F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && (target.hasEffect(BinderEffects.HEAT_MIRAGE.get()) || target.hasEffect(BinderEffects.FOX_GLAMOUR.get()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(BinderEffects.LICH.get())) {
            event.setCanceled(true);
        }
    }

    /** A lich pays for spells with health: a tenth of the mana as damage, the mana kept. */
    @SubscribeEvent
    public static void onMana(ChangeManaEvent event) {
        LivingEntity player = event.getEntity();
        float spent = event.getOldMana() - event.getNewMana();
        if (spent > 0.0F && player.hasEffect(BinderEffects.LICH.get())) {
            event.setNewMana(event.getOldMana());
            player.hurt(player.damageSources().magic(), Math.min(spent * 0.1F, player.getHealth() - 1.0F));
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (!(event.getSpellDamageSource().getEntity() instanceof LivingEntity caster)) {
            return;
        }
        if (caster.hasEffect(BinderEffects.LICH.get())) {
            event.setAmount(event.getAmount() * 1.3F);
        }
        if (caster.hasEffect(BinderEffects.SPIRIT_COMMUNION.get())
            && event.getSpellDamageSource().spell().getSchoolType().getId().toString().equals("iss_magicfromtheeast:spirit")) {
            event.setAmount(event.getAmount() * 1.2F);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getSource().getEntity() instanceof LivingEntity killer && killer.hasEffect(BinderEffects.HUNGRY_GHOSTS.get())) {
            MobEffectInstance ghosts = killer.getEffect(BinderEffects.HUNGRY_GHOSTS.get());
            killer.addEffect(new MobEffectInstance(BinderEffects.HUNGRY_GHOSTS.get(), ghosts.getDuration() + 60, 0, false, true, true));
            Targets.enemies(killer, dead.position(), 12.0).stream().filter(e -> e != dead && e != killer)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(dead))).ifPresent(next -> {
                    SmiteSpells.beam(level, dead.position().add(0.0, 1.0, 0.0), next.getEyePosition(), ParticleTypes.SOUL);
                    BoltSpells.soulburn(killer, next, 100);
                });
        }
        if (dead.hasEffect(BinderEffects.REAPER_MARK.get()) && dead.getPersistentData().hasUUID(MARKED_BY)
            && level.getEntity(dead.getPersistentData().getUUID(MARKED_BY)) instanceof LivingEntity marker) {
            float power = dead.getPersistentData().getFloat(MARKED_BY + "_power");
            for (LivingEntity enemy : Targets.enemies(marker, dead.position(), 5.0)) {
                if (enemy != dead) {
                    DamageSources.applyDamage(enemy, power, SpellRegistry.getSpell(new ResourceLocation("mdvlcraft", "reaper_mark")).getDamageSource(marker));
                    BoltSpells.soulburn(marker, enemy, 100);
                }
            }
            level.sendParticles(ParticleTypes.SOUL, dead.getX(), dead.getY(0.5), dead.getZ(), 60, 1.5, 1.0, 1.5, 0.1);
            level.playSound(null, dead.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 2.0F, 0.6F);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().hasEffect(BinderEffects.BOUNTY.get())) {
            List<ItemEntity> extra = event.getDrops().stream()
                .map(drop -> new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy())).toList();
            event.getDrops().addAll(extra);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getEntity().hasEffect(BinderEffects.BOUNTY.get())) {
            event.setDroppedExperience(event.getDroppedExperience() * 3);
        }
    }
}
