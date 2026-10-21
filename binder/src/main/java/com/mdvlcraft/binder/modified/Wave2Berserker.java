package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.combat.BinderEffects;
import com.mdvlcraft.binder.modified.BoltSpells.Bolt;
import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The second wave's berserker abilities (one pair per berserker vocation), plus the spirit, thief and stalker pairs of the
 * same group: flurries and rages, thrown axes, flasks and kegs, howls and pulls, timed states kept with Buffs (what they do
 * lives in the event handlers at the end), conjured food and a few summons.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class Wave2Berserker {
    private static final String HAMSTRING_ARROW = "mdvlcraft_hamstring_shot";
    private static final String ANCESTOR_ARROW = "mdvlcraft_ancestors_call";
    private static final String QUICKDRAW_ARROW = "mdvlcraft_quickdraw";
    private static final String PERCH_TOP = "mdvlcraft_lookout_perch";
    private static final String GROG = "berserker_grog";
    private static final String MUTTON = "berserker_mutton";
    private static final String ONIGIRI = "berserker_onigiri";
    private static final List<Item> BINDLE_FOOD = List.of(Items.BREAD, Items.APPLE, Items.CARROT, Items.BAKED_POTATO, Items.COOKED_COD,
        Items.COOKIE);
    /** Set while Twin Tails copies a hit, so the copy isn't copied again. */
    private static boolean copying;

    private Wave2Berserker() {
    }

    // ---- shared helpers ----

    static AbstractSpell spell(String key) {
        return SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("mdvlcraft", key));
    }

    /** Hurts `target` with the damage source of the Modified ability `key`, cast by `caster` (for event handlers). */
    static boolean hurt(String key, LivingEntity caster, LivingEntity target, float amount) {
        return DamageSources.applyDamage(target, amount, spell(key).getDamageSource(caster));
    }

    /** The ability `key`'s spell power for `caster` (its base number, scaled by the caster's school power). */
    static float power(String key, LivingEntity caster) {
        return spell(key).getSpellPower(1, caster);
    }

    /** Whoever dealt a hit by weapon or by shot (not a spell, nor thorns), or null. */
    @Nullable
    static LivingEntity hitter(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && !(source instanceof SpellDamageSource) && !source.is(DamageTypes.THORNS)
            ? attacker : null;
    }

    /** The attacker of a melee hit (not a spell, a projectile or thorns), or null. */
    @Nullable
    static LivingEntity meleeAttacker(DamageSource source) {
        LivingEntity attacker = hitter(source);
        return attacker != null && source.getDirectEntity() == attacker && !source.is(DamageTypeTags.IS_PROJECTILE) ? attacker : null;
    }

    /** The shooter of a projectile hit (an arrow, a trident; not a spell), or null. */
    @Nullable
    static LivingEntity shooter(DamageSource source) {
        LivingEntity attacker = hitter(source);
        return attacker != null && source.getDirectEntity() instanceof Projectile ? attacker : null;
    }

    static void bleed(LivingEntity caster, LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(BrandSpells.external("attributeslib:bleeding"), ticks, 0), caster);
    }

    static void freeze(LivingEntity caster, LivingEntity target, int ticks) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), caster);
    }

    /** Pulls `who` toward `to`, about `distance` blocks (never past it). */
    static void pull(LivingEntity who, Vec3 to, double distance) {
        Vec3 in = to.subtract(who.position()).multiply(1.0, 0.0, 1.0);
        double length = Math.min(distance, Math.max(0.0, in.length() - 1.2));
        if (length > 0.0) {
            who.setDeltaMovement(in.normalize().scale(length * 0.18).add(0.0, 0.3, 0.0));
            who.hurtMarked = true;
        }
    }

    /** Gives `entity` `amount` absorption health on top of what it has, up to `cap`. */
    static void absorb(LivingEntity entity, float amount, float cap) {
        if (entity.getAbsorptionAmount() < cap) {
            entity.setAbsorptionAmount(Math.min(cap, entity.getAbsorptionAmount() + amount));
        }
    }

    /** Raises `count` allied mobs of `type` at `at` for `ticks`, as the summon kind `kind`. */
    static <T extends Mob> void raise(ServerLevel level, LivingEntity owner, EntityType<T> type, int count, int ticks, String kind, Vec3 at) {
        Summons.summon(level, owner, type, count, ticks, kind, new SummonedEntitiesCastData(), mob ->
            mob.moveTo(at.x + (mob.getRandom().nextDouble() - 0.5), at.y, at.z + (mob.getRandom().nextDouble() - 0.5), owner.getYRot(), 0.0F));
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 20 * count, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, BlockPos.containing(at), SoundEvents.VEX_CHARGE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** A timed state on the caster (Buffs, keyed by the spell); what it does is in the event handlers. */
    static class State extends ModifiedSpell {
        private final int ticks;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        State(String key, int ticks, ParticleOptions particle, SoundEvent sound) {
            super(key, 0);
            this.ticks = ticks;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Buffs.give(caster, this.config.key(), this.ticks);
            if (level instanceof ServerLevel server) {
                server.sendParticles(this.particle, caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, 0.5, 0.6, 0.5, 0.03);
            }
            level.playSound(null, caster.blockPosition(), this.sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Brawler ----

    /** Hundred Fists: 8 punches over 2 s on the creature looked at (5 blocks), each for the spell's power (2). */
    public static class HundredFists extends SmiteSpells.Smite {
        HundredFists() {
            super("hundred_fists", 2, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Zones.start(level, target.position(), caster, power, 40, 5, zone -> {
                if (!target.isAlive()) {
                    zone.stop();
                    return;
                }
                target.invulnerableTime = 0;
                this.hurt(caster, target, zone.power);
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            }, () -> {
            }, null);
        }
    }

    // ---- Hellraiser ----

    /** Firebrand Flask: a thrown flask; where it breaks the ground burns for 4 s, setting enemies within 3 blocks alight and hurting them each second. */
    static Bolt firebrandFlask() {
        return new Bolt("firebrand_flask", 2, Items.SPLASH_POTION, ParticleTypes.FLAME, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Zones.start(level, at, caster, power, 80, 10, zone -> {
                GroundSpells.ring(level, at, 3.0, ParticleTypes.FLAME);
                level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 3, 1.5, 0.1, 1.5, 0.0);
                if (zone.age() % 20 == 0) {
                    for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                        enemy.setSecondsOnFire(3);
                        bolt.hurt(caster, enemy, zone.power);
                    }
                }
            }, () -> {
            }, null);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }).speed(1.0F).gravity(0.05).sound(SoundEvents.SPLASH_POTION_THROW);
    }

    /** Pyre Rage: for 10 s creatures the caster kills explode, the spell's power (5) to enemies within 3 blocks and alight for 3 s. */
    public static class PyreRage extends State {
        PyreRage() {
            super("pyre_rage", 200, ParticleTypes.FLAME, SoundEvents.BLAZE_AMBIENT);
        }
    }

    // ---- Draugr ----

    /** Hurled Rimeaxe: a thrown frozen axe; the creature hit is hurt and frozen for 1.5 s. */
    static Bolt hurledRimeaxe() {
        return new Bolt("hurled_rimeaxe", 6, Items.IRON_AXE, ParticleTypes.SNOWFLAKE, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, power);
            freeze(caster, target, 30);
            MartialSpells.root(target, 30);
            ServerLevel level = (ServerLevel)target.level();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), target.getX(), target.getY(0.6),
                target.getZ(), 30, 0.4, 0.5, 0.4, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }, null).speed(1.6F).sound(SoundEvents.TRIDENT_THROW);
    }

    /** Barrow Howl: every enemy in a 6-block cone ahead gets Weakness II and is frozen for 2 s (no damage). */
    public static class BarrowHowl extends ModifiedSpell {
        BarrowHowl() {
            super("barrow_howl", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 eye = caster.getEyePosition();
            Vec3 look = caster.getLookAngle();
            double cone = Math.cos(Math.toRadians(45.0));
            for (LivingEntity enemy : Targets.enemies(caster, eye, 6.0)) {
                if (enemy != caster && enemy.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) >= cone) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1), caster);
                    freeze(caster, enemy, 40);
                }
            }
            if (level instanceof ServerLevel server) {
                for (int i = 1; i <= 6; i++) {
                    Vec3 at = eye.add(look.scale(i));
                    server.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y - 0.2, at.z, 4 + i * 2, 0.15 * i, 0.12 * i, 0.15 * i, 0.02);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.5F, 0.6F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Maelstrom ----

    /** Undertow: every enemy within 6 blocks is pulled to the caster; half a second later those within 2 blocks take the spell's power (5). */
    public static class Undertow extends ModifiedSpell {
        Undertow() {
            super("undertow", 5);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 6.0)) {
                    pull(enemy, caster.position(), 6.0);
                }
                float power = this.getSpellPower(spellLevel, caster);
                Zones.start(server, caster.position(), caster, power, 10, 100, zone -> {
                }, () -> {
                    for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 2.5)) {
                        DamageSources.applyDamage(enemy, power, this.getDamageSource(caster));
                    }
                    server.sendParticles(ParticleTypes.SPLASH, caster.getX(), caster.getY() + 0.3, caster.getZ(), 60, 1.5, 0.2, 1.5, 0.2);
                    server.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2F, 0.7F);
                }, null);
                GroundSpells.ring(server, caster.position(), 6.0, ParticleTypes.BUBBLE_POP);
                server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, caster.getX(), caster.getY(), caster.getZ(), 40, 2.5, 0.3, 2.5, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Brine Frenzy: for 10 s each of the caster's hits gives 1 heart of absorption (2 in water or rain), up to 4. */
    public static class BrineFrenzy extends State {
        BrineFrenzy() {
            super("brine_frenzy", 200, ParticleTypes.SPLASH, SoundEvents.DOLPHIN_PLAY);
        }
    }

    // ---- Stormbreaker ----

    /** Thunderhide: for 10 s whoever hits the caster in melee is zapped for the spell's power (4). */
    public static class Thunderhide extends State {
        Thunderhide() {
            super("thunderhide", 200, ParticleTypes.ELECTRIC_SPARK, SoundEvents.BEACON_POWER_SELECT);
        }
    }

    /** Bolt Drop: a 6-block leap ahead; on landing, enemies within 4 blocks take the spell's power (6) and are stunned for 1 s. */
    public static class BoltDrop extends ModifiedSpell {
        BoltDrop() {
            super("bolt_drop", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Vec3 ahead = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
            caster.setDeltaMovement(ahead.scale(0.65).add(0.0, 0.8, 0.0));
            caster.hurtMarked = true;
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Zones.start(server, caster.position(), caster, power, 80, 1, zone -> {
                    caster.resetFallDistance();
                    if (zone.age() >= 4 && caster.onGround()) {
                        this.land(server, caster, zone.power);
                        zone.stop();
                    }
                }, () -> {
                }, null);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 0.2, caster.getZ(), 30, 0.5, 0.2, 0.5, 0.2);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0F, 1.2F);
            super.onCast(level, spellLevel, caster, source, data);
        }

        void land(ServerLevel level, LivingEntity caster, float power) {
            for (LivingEntity enemy : Targets.enemies(caster, caster.position(), 4.0)) {
                DamageSources.applyDamage(enemy, power, this.getDamageSource(caster));
                MartialSpells.stun(enemy, StunType.LONG, 1.0F);
                MartialSpells.root(enemy, 20);
            }
            SmiteSpells.flash(level, caster.position());
            GroundSpells.ring(level, caster.position(), 4.0, ParticleTypes.ELECTRIC_SPARK);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, caster.getBlockStateOn()), caster.getX(), caster.getY() + 0.1,
                caster.getZ(), 60, 2.0, 0.1, 2.0, 0.2);
            level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    // ---- Zealous Blade ----

    /** Martyr's Zeal: for 10 s each hit the caster takes heals allies within 6 blocks 2 health and gives the caster Strength I (to the end). */
    public static class MartyrsZeal extends State {
        MartyrsZeal() {
            super("martyrs_zeal", 200, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE);
        }
    }

    /** Penitent Brand: marks the creature looked at (24 blocks) for 15 s; if it dies the caster heals 6 and allies within 6 blocks get Regeneration II for 5 s. */
    public static class PenitentBrand extends SmiteSpells.Smite {
        PenitentBrand() {
            super("penitent_brand", 0, 24.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            Buffs.mark(target, "penitent_brand", caster, 300);
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.WAX_ON);
            level.sendParticles(ParticleTypes.WAX_ON, target.getX(), target.getY(1.0), target.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    // ---- Eldritch Fury ----

    /** Rift Hook: strikes the creature looked at (16 blocks) for the spell's power and pulls every enemy within 5 blocks of it toward it. */
    public static class RiftHook extends SmiteSpells.Smite {
        RiftHook() {
            super("rift_hook", 5, 16.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            this.hurt(caster, target, power);
            for (LivingEntity enemy : Targets.enemies(caster, target.position(), 5.0)) {
                if (enemy != target) {
                    pull(enemy, target.position(), 5.0);
                }
            }
            SmiteSpells.beam(level, caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.REVERSE_PORTAL);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY(0.5), target.getZ(), 60, 2.5, 0.5, 2.5, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    // ---- Beastmaster ----

    /** Boar Rush: an 8-block dash; every enemy in its path is launched into the air. */
    public static class BoarRush extends ModifiedSpell {
        BoarRush() {
            super("boar_rush", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (LivingEntity enemy : MartialSpells.dash(caster, 8.0)) {
                enemy.setDeltaMovement(enemy.getDeltaMovement().multiply(0.3, 0.0, 0.3).add(0.0, 1.1, 0.0));
                enemy.hurtMarked = true;
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CLOUD, enemy.getX(), enemy.getY() + 0.2, enemy.getZ(), 10, 0.3, 0.1, 0.3, 0.05);
                }
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.HOGLIN_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hornet Swarm: four angry bees for 15 s whose stings poison (II) for 5 s and who can sting again. */
    public static class HornetSwarm extends SummonSpell {
        HornetSwarm() {
            super("hornet_swarm", 2);
        }

        @Override
        protected int duration() {
            return 300;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.BEE, 4, castData, bee -> {
            });
        }
    }

    // ---- Kitsune Fury ----

    /** Jade Rupture: strikes the creature looked at (5 blocks) for the spell's power plus its armour value and shatters its armour (II) for 6 s. */
    public static class JadeRupture extends SmiteSpells.Smite {
        JadeRupture() {
            super("jade_rupture", 4, 5.0F);
        }

        @Override
        void strike(ServerLevel level, LivingEntity caster, LivingEntity target, float power, MagicData data) {
            float armour = target.getArmorValue();
            target.addEffect(new MobEffectInstance(BinderEffects.SHATTERED.get(), 120, 1), caster);
            this.hurt(caster, target, power + armour);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), target.getX(), target.getY(0.6),
                target.getZ(), 40, 0.4, 0.5, 0.4, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.5F, 0.7F);
        }
    }

    /** Twin Tails: for 10 s each of the caster's hits is copied onto the nearest other enemy within 5 blocks of what it hit. */
    public static class TwinTails extends State {
        TwinTails() {
            super("twin_tails", 200, ParticleTypes.HAPPY_VILLAGER, SoundEvents.FOX_SCREECH);
        }
    }

    // ---- Hungry Ghost ----

    /** Soul Gorge: for 10 s each kill restores the caster 2 hunger and 2 health. */
    public static class SoulGorge extends State {
        SoulGorge() {
            super("soul_gorge", 200, ParticleTypes.SOUL, SoundEvents.GENERIC_EAT);
        }
    }

    // ---- Corsair ----

    /** Powder Keg: a thrown keg; a second after it lands it blows up for the spell's power (8) within 3 blocks, breaking no blocks. */
    static Bolt powderKeg() {
        return new Bolt("powder_keg", 8, Items.TNT, ParticleTypes.SMOKE, (bolt, caster, target, power) -> {
        }, (bolt, level, caster, at, power) -> {
            Zones.start(level, at, caster, power, 20, 4, zone -> {
                level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 1.0, at.z, 4, 0.1, 0.1, 0.1, 0.02);
            }, () -> {
                for (LivingEntity enemy : Targets.enemies(caster, at, 3.0)) {
                    bolt.hurt(caster, enemy, power);
                }
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z, 30, 1.5, 0.8, 1.5, 0.05);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 1.0F);
            }, GroundSpells.marker(level, at.subtract(0.0, 1.4, 0.0), Items.TNT));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0F, 1.0F);
        }).speed(0.9F).gravity(0.05).scale(1.2F).sound(SoundEvents.WITCH_THROW);
    }

    // ---- Skirmisher ----

    /** Hamstring Shot: an arrow where the caster looks; the creature hit gets Slowness III and bleeds for 5 s. */
    public static class HamstringShot extends ModifiedSpell {
        HamstringShot() {
            super("hamstring_shot", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            Arrow arrow = new Arrow(level, caster);
            arrow.setBaseDamage(this.getSpellPower(spellLevel, caster) / 3.0);
            arrow.setCritArrow(true);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putBoolean(HAMSTRING_ARROW, true);
            arrow.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 3.0F, 0.5F);
            level.addFreshEntity(arrow);
            level.playSound(null, caster.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Hit and Run: for 10 s the caster's melee hits give Speed II and its shots Strength I, each for 2 s. */
    public static class HitAndRun extends State {
        HitAndRun() {
            super("hit_and_run", 200, ParticleTypes.CLOUD, SoundEvents.ARMOR_EQUIP_LEATHER);
        }
    }

    // ---- Viking ----

    /** Bearded Axe: a thrown axe that hits for the spell's power and bounces on to up to 2 more creatures, each within 5 blocks of the last. */
    static Bolt beardedAxe() {
        return new Bolt("bearded_axe", 5, Items.IRON_AXE, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            ServerLevel level = (ServerLevel)target.level();
            List<LivingEntity> struck = new ArrayList<>();
            LivingEntity next = target;
            while (next != null && struck.size() < 3) {
                if (!struck.isEmpty()) {
                    SmiteSpells.beam(level, struck.get(struck.size() - 1).getEyePosition(), next.getEyePosition(), ParticleTypes.CRIT);
                }
                bolt.hurt(caster, next, power);
                struck.add(next);
                LivingEntity at = next;
                next = Targets.enemies(caster, at.position(), 5.0).stream().filter(e -> e != caster && !struck.contains(e))
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(at))).orElse(null);
            }
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
        }, null).speed(1.6F).sound(SoundEvents.TRIDENT_THROW);
    }

    // ---- Poltergeist ----

    /** Haunted Crockery: a thrown spectral pot; the creature hit takes the spell's power (4) and Nausea for 5 s. */
    static Bolt hauntedCrockery() {
        return new Bolt("haunted_crockery", 4, Items.FLOWER_POT, ParticleTypes.SOUL, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, power);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), caster);
            ServerLevel level = (ServerLevel)target.level();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TERRACOTTA.defaultBlockState()), target.getX(), target.getY(1.0),
                target.getZ(), 30, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, target.blockPosition(), SoundEvents.DECORATED_POT_SHATTER, SoundSource.PLAYERS, 1.0F, 1.0F);
        }, null).speed(1.2F).sound(SoundEvents.VEX_AMBIENT);
    }

    // ---- Spirit Hunter ----

    /** Ancestor's Call: the caster's next arrow (within 30 s) calls up an allied ghost (vex) where it lands, for 15 s. */
    public static class AncestorsCall extends State {
        AncestorsCall() {
            super("ancestors_call", 600, ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE);
        }
    }

    /** Ancestral Sight: every mob within 24 blocks glows for 12 s and the caster gets Speed I for 12 s. */
    public static class AncestralSight extends ModifiedSpell {
        AncestralSight() {
            super("ancestral_sight", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(24.0),
                    m -> m != caster && m.isAlive() && m.distanceToSqr(caster) <= 576.0)) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0), caster);
            }
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SOUL, caster.getX(), caster.getY() + 1.6, caster.getZ(), 25, 0.4, 0.3, 0.4, 0.03);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.7F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    // ---- Outlaw ----

    /** Lasso Throw: a thrown lasso; the creature hit is pulled up to 6 blocks toward the caster and slowed (II) for 3 s. */
    static Bolt lassoThrow() {
        return new Bolt("lasso_throw", 0, Items.LEAD, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            pull(target, caster.position(), 6.0);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), caster);
            SmiteSpells.beam((ServerLevel)target.level(), caster.getEyePosition().subtract(0.0, 0.3, 0.0), target.getEyePosition(), ParticleTypes.CRIT);
            target.level().playSound(null, target.blockPosition(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }, null).speed(1.5F).gravity(0.02).sound(SoundEvents.FISHING_BOBBER_THROW);
    }

    /** Quickdraw: for 6 s a drawn bow fires at once at full charge, and the caster's arrows deal 2 more damage. */
    public static class Quickdraw extends State {
        Quickdraw() {
            super("quickdraw", 120, ParticleTypes.CRIT, SoundEvents.CROSSBOW_LOADING_END);
        }
    }

    // ---- Vagabond ----

    /** Bindle Bash: a thrown bindle; the creature hit takes the spell's power (2), is stunned for 2 s and drops a random food. */
    static Bolt bindleBash() {
        return new Bolt("bindle_bash", 2, Items.STICK, ParticleTypes.CRIT, (bolt, caster, target, power) -> {
            bolt.hurt(caster, target, power);
            MartialSpells.stun(target, StunType.LONG, 2.0F);
            MartialSpells.root(target, 40);
            target.spawnAtLocation(new ItemStack(BINDLE_FOOD.get(target.getRandom().nextInt(BINDLE_FOOD.size()))));
            target.level().playSound(null, target.blockPosition(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }, null).speed(1.3F).gravity(0.03).scale(1.2F).sound(SoundEvents.WITCH_THROW);
    }

    /** Lucky Penny: for 15 s each of the caster's hits has a 15% chance to give it Luck and 2 hearts of Absorption for 30 s. */
    public static class LuckyPenny extends State {
        LuckyPenny() {
            super("lucky_penny", 300, ParticleTypes.WAX_OFF, SoundEvents.CHAIN_PLACE);
        }
    }

    // ---- Ranger ----

    /** Lookout Perch: a 4-block dirt pillar rises under the caster for 10 s; while it stands on it, its shots deal 30% more damage. */
    public static class LookoutPerch extends ModifiedSpell {
        LookoutPerch() {
            super("lookout_perch", 0);
        }

        static boolean room(Level level, BlockPos base) {
            for (int y = 0; y < 6; y++) {
                if (!level.getBlockState(base.above(y)).canBeReplaced() || !level.getFluidState(base.above(y)).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            if (!room(level, caster.blockPosition())) {
                MartialSpells.refuse(caster, "no_room");
                return false;
            }
            return true;
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            BlockPos base = caster.blockPosition();
            if (level instanceof ServerLevel server && room(server, base)) {
                caster.teleportTo(caster.getX(), base.getY() + 4.0, caster.getZ());
                caster.resetFallDistance();
                BlockState dirt = Blocks.COARSE_DIRT.defaultBlockState();
                List<BlockPos> placed = WallSpells.place(server, List.of(base, base.above(), base.above(2), base.above(3)), dirt);
                BlockPos top = base.above(3);
                Buffs.give(caster, "lookout_perch", 200);
                caster.getPersistentData().putLong(PERCH_TOP, top.asLong());
                Zones.start(server, Vec3.atCenterOf(top), caster, 0.0F, 200, 200, zone -> {
                }, () -> WallSpells.clear(server, placed, dirt), null);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dirt), caster.getX(), base.getY() + 1.0, caster.getZ(), 60, 0.5, 2.0,
                    0.5, 0.1);
            }
            level.playSound(null, caster.blockPosition(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.5F, 0.8F);
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Tracker Hound: a wolf for 30 s whose bites make the creature glow and slow it (I) for 5 s. */
    public static class TrackerHound extends SummonSpell {
        TrackerHound() {
            super("tracker_hound", 3);
        }

        @Override
        protected int duration() {
            return 600;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.WOLF, 1, castData, wolf -> SummonSpells.tame(wolf, caster));
        }
    }

    public static List<ModifiedSpell> all() {
        return List.of(
            new HundredFists(),
            // Iron Gut: for 12 s the caster takes 20% less damage and each hit by a creature gives it Strength I for 3 s
            new State("iron_gut", 240, ParticleTypes.CRIT, SoundEvents.ARMOR_EQUIP_IRON),
            firebrandFlask(),
            new PyreRage(),
            hurledRimeaxe(),
            new BarrowHowl(),
            new Undertow(),
            new BrineFrenzy(),
            new Thunderhide(),
            new BoltDrop(),
            new MartyrsZeal(),
            new PenitentBrand(),
            new RiftHook(),
            // Madness Ring: 5 s of pulses within 4 blocks of the caster: the spell's power (2) to enemies each second, with Nausea and Blindness
            new GroundSpells.Area("madness_ring", 2, 4.0, 5, 20, false, ParticleTypes.SQUID_INK, SoundEvents.ELDER_GUARDIAN_CURSE,
                (area, zone, allies, enemies) -> {
                    enemies.forEach(e -> area.hurt(zone.caster, e, zone.power));
                    GroundSpells.effect(enemies, zone.caster, MobEffects.CONFUSION, 80, 0);
                    GroundSpells.effect(enemies, zone.caster, MobEffects.BLINDNESS, 40, 0);
                }),
            new BoarRush(),
            new HornetSwarm(),
            new JadeRupture(),
            new TwinTails(),
            // Gaki Banquet: 5 s of pulses within 5 blocks of the caster: drains the spell's power (1) from each enemy into the caster and soulburns it
            new GroundSpells.Area("gaki_banquet", 1, 5.0, 5, 20, false, ParticleTypes.SOUL, SoundEvents.GENERIC_EAT,
                (area, zone, allies, enemies) -> {
                    for (LivingEntity enemy : enemies) {
                        float before = enemy.getHealth();
                        area.hurt(zone.caster, enemy, zone.power);
                        zone.caster.heal(Math.max(0.0F, before - enemy.getHealth()));
                        BoltSpells.soulburn(zone.caster, enemy, 40);
                        SmiteSpells.beam(zone.level, enemy.getEyePosition(), zone.caster.getEyePosition().subtract(0.0, 0.3, 0.0), ParticleTypes.SOUL);
                    }
                }),
            new SoulGorge(),
            powderKeg(),
            // Grog Ration: two bottles of grog: Strength II and Resistance I for 30 s, Nausea for 5 s
            new BoonSpells.Food("grog_ration", Items.POTION, GROG, 2, false),
            new HamstringShot(),
            new HitAndRun(),
            beardedAxe(),
            // Raider's Feast: three roasted mutton: 6 health and Strength I for 20 s
            new BoonSpells.Food("raiders_feast", Items.COOKED_MUTTON, MUTTON, 3, false),
            hauntedCrockery(),
            // Rattling Chains: 5 s within 5 blocks of the caster: enemies get Slowness III and glow
            new GroundSpells.Area("rattling_chains", 0, 5.0, 5, 10, false, ParticleTypes.SOUL, SoundEvents.CHAIN_BREAK,
                (area, zone, allies, enemies) -> {
                    GroundSpells.effect(enemies, zone.caster, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                    GroundSpells.effect(enemies, zone.caster, MobEffects.GLOWING, 30, 0);
                    if (zone.age() % 20 == 0) {
                        zone.level.playSound(null, BlockPos.containing(zone.center), SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 1.0F, 0.6F);
                    }
                }),
            new AncestorsCall(),
            new AncestralSight(),
            // Incense Circle: 6 s within 5 blocks of the caster: allies get Regeneration I and Saturation
            new GroundSpells.Area("incense_circle", 0, 5.0, 6, 20, false, ParticleTypes.CAMPFIRE_COSY_SMOKE, SoundEvents.CAMPFIRE_CRACKLE,
                (area, zone, allies, enemies) -> {
                    GroundSpells.effect(allies, zone.caster, MobEffects.REGENERATION, 50, 0);
                    GroundSpells.effect(allies, zone.caster, MobEffects.SATURATION, 1, 0);
                }),
            // Offering Onigiri: three rice balls: 6 hunger, 2 hearts of Absorption and Speed I for 60 s
            new BoonSpells.Food("offering_onigiri", Items.BREAD, ONIGIRI, 3, false),
            lassoThrow(),
            new Quickdraw(),
            bindleBash(),
            new LuckyPenny(),
            new LookoutPerch(),
            new TrackerHound());
    }

    // ---- event handlers ----

    /** What the victim's own states do (Iron Gut, Thunderhide, Martyr's Zeal) and what the attacker's add to its hit. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity by = source.getEntity();

        if (Buffs.has(victim, "iron_gut")) {
            event.setAmount(event.getAmount() * 0.8F);
            if (by instanceof LivingEntity && by != victim) {
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0));
            }
        }
        LivingEntity melee = meleeAttacker(source);
        if (melee != null && melee != victim && Buffs.has(victim, "thunderhide")) {
            hurt("thunderhide", victim, melee, power("thunderhide", victim));
            SummonSpells.spark(level, victim.getEyePosition(), melee.getEyePosition());
            level.playSound(null, melee.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.8F);
        }
        if (by instanceof LivingEntity && by != victim && Buffs.has(victim, "martyrs_zeal")) {
            for (LivingEntity ally : Targets.allies(victim, victim.position(), 6.0)) {
                if (ally != victim) {
                    ally.heal(2.0F);
                }
            }
            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Math.max(20, Buffs.remaining(victim, "martyrs_zeal")), 0));
            level.sendParticles(ParticleTypes.HEART, victim.getX(), victim.getY() + 1.0, victim.getZ(), 6, 2.0, 0.5, 2.0, 0.0);
        }

        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            if (arrow.getPersistentData().getBoolean(HAMSTRING_ARROW)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), arrow.getOwner() instanceof LivingEntity o ? o : null);
                bleed(arrow.getOwner() instanceof LivingEntity o ? o : victim, victim, 100);
            }
            if (arrow.getPersistentData().getBoolean(QUICKDRAW_ARROW)) {
                event.setAmount(event.getAmount() + 2.0F);
            }
        }

        LivingEntity attacker = hitter(source);
        if (attacker == null || attacker == victim) {
            return;
        }
        if (Buffs.has(attacker, "brine_frenzy")) {
            absorb(attacker, GroundSpells.wet(attacker) ? 4.0F : 2.0F, 8.0F);
            level.sendParticles(ParticleTypes.SPLASH, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
        }
        if (Buffs.has(attacker, "hit_and_run")) {
            if (melee == attacker) {
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
            } else if (shooter(source) == attacker) {
                attacker.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0));
            }
        }
        if (Buffs.has(attacker, "lucky_penny") && attacker.getRandom().nextFloat() < 0.15F) {
            attacker.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 0));
            attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
            level.sendParticles(ParticleTypes.WAX_OFF, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, attacker.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
        if (shooter(source) == attacker && Buffs.has(attacker, "lookout_perch")
            && attacker.getOnPos().asLong() == attacker.getPersistentData().getLong(PERCH_TOP)) {
            event.setAmount(event.getAmount() * 1.3F);
        }
        if (!copying && Buffs.has(attacker, "twin_tails")) {
            Targets.enemies(attacker, victim.position(), 5.0).stream().filter(e -> e != victim && e != attacker)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).ifPresent(other -> {
                    copying = true;
                    try {
                        hurt("twin_tails", attacker, other, event.getAmount());
                    } finally {
                        copying = false;
                    }
                    SmiteSpells.beam(level, victim.getEyePosition(), other.getEyePosition(), ParticleTypes.HAPPY_VILLAGER);
                });
        }
    }

    /** What the berserker summons' hits do on top of their damage. */
    @SubscribeEvent
    public static void onSummonHit(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !(Summons.owner(attacker) instanceof LivingEntity owner)) {
            return;
        }
        switch (Summons.kind(attacker)) {
            case "hornet_swarm" -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), owner);
            case "tracker_hound" -> {
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), owner);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), owner);
            }
            default -> {
            }
        }
    }

    /** Pyre Rage and Soul Gorge pay off on the caster's kills; Penitent Brand when the marked creature dies. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = dead.position();
        LivingEntity branded = Buffs.markedBy(dead, "penitent_brand");
        if (branded != null) {
            Buffs.clear(dead, "penitent_brand");
            branded.heal(6.0F);
            for (LivingEntity ally : Targets.allies(branded, branded.position(), 6.0)) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), branded);
            }
            level.sendParticles(ParticleTypes.END_ROD, branded.getX(), branded.getY() + 1.0, branded.getZ(), 30, 2.0, 0.6, 2.0, 0.02);
            level.playSound(null, branded.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity killer) || killer == dead || Summons.allied(killer, dead)) {
            return;
        }
        if (Buffs.has(killer, "pyre_rage")) {
            float power = power("pyre_rage", killer);
            for (LivingEntity enemy : Targets.enemies(killer, at, 3.0)) {
                if (enemy != dead) {
                    hurt("pyre_rage", killer, enemy, power);
                    enemy.setSecondsOnFire(3);
                }
            }
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 60, 1.2, 0.6, 1.2, 0.1);
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.5, 0.3, 0.5, 0.0);
            level.playSound(null, dead.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        if (Buffs.has(killer, "soul_gorge")) {
            if (killer instanceof Player player) {
                player.getFoodData().eat(2, 0.2F);
            }
            killer.heal(2.0F);
            SmiteSpells.beam(level, at.add(0.0, 1.0, 0.0), killer.getEyePosition().subtract(0.0, 0.3, 0.0), ParticleTypes.SOUL);
        }
    }

    /** Arrows loosed under Ancestor's Call (the next one only) and Quickdraw are tagged. */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        if (Buffs.has(owner, "ancestors_call") && !arrow.getPersistentData().getBoolean(HAMSTRING_ARROW)) {
            Buffs.clear(owner, "ancestors_call");
            arrow.getPersistentData().putBoolean(ANCESTOR_ARROW, true);
        }
        if (Buffs.has(owner, "quickdraw")) {
            arrow.getPersistentData().putBoolean(QUICKDRAW_ARROW, true);
        }
    }

    /** Ancestor's Call: where the tagged arrow lands, a vex fights for its shooter for 15 s. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !arrow.getPersistentData().getBoolean(ANCESTOR_ARROW)
            || !(arrow.level() instanceof ServerLevel level) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        arrow.getPersistentData().remove(ANCESTOR_ARROW);
        raise(level, owner, EntityType.VEX, 1, 300, "ancestors_call", event.getRayTraceResult().getLocation().add(0.0, 0.5, 0.0));
    }

    /** Quickdraw: drawing a bow looses a fully charged shot at once. */
    @SubscribeEvent
    public static void onDraw(LivingEntityUseItemEvent.Start event) {
        LivingEntity archer = event.getEntity();
        ItemStack stack = event.getItem();
        if (archer.level().isClientSide() || !(stack.getItem() instanceof BowItem bow) || !Buffs.has(archer, "quickdraw")) {
            return;
        }
        event.setCanceled(true);
        bow.releaseUsing(stack, archer.level(), archer, bow.getUseDuration(stack) - 20);
    }

    /** Hornet Swarm: the bees stay angry at their target and can sting again. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Bee bee) || !"hornet_swarm".equals(Summons.kind(bee))) {
            return;
        }
        bee.goalSelector.addGoal(0, new SummonSpells.PulseGoal(bee, 5, mob -> {
            Bee b = (Bee)mob;
            if (b.hasStung()) {
                CompoundTag tag = new CompoundTag();
                b.addAdditionalSaveData(tag);
                tag.putBoolean("HasStung", false);
                b.readAdditionalSaveData(tag);
            }
            LivingEntity target = b.getTarget();
            if (target != null && target.isAlive()) {
                b.setRemainingPersistentAngerTime(100);
                b.setPersistentAngerTarget(target.getUUID());
            }
        }));
    }

    /** The berserker group's boon foods. */
    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        LivingEntity eater = event.getEntity();
        switch (BoonSpells.kindOf(event.getItem())) {
            case GROG -> {
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            }
            case MUTTON -> {
                eater.heal(6.0F);
                eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
            }
            case ONIGIRI -> {
                if (eater instanceof Player player) {
                    player.getFoodData().eat(1, 0.1F);   // bread gives 5
                }
                eater.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 0));
                eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0));
            }
            default -> {
            }
        }
    }
}
