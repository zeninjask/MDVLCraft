package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.PulseGoal;
import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import java.util.Objects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The Modified mounts (built from Summon Horse): tamed and saddled to the caster for 5 minutes. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class MountSpells {
    static final int MOUNT_TICKS = 6000;
    public static final String SPECTRAL_BOAT = "spell.mdvlcraft.spectral_boat";

    private MountSpells() {
    }

    /** A mount: a horse-like creature tamed to the caster and saddled, `speed` and `health` set by setup. */
    public abstract static class Mount<T extends AbstractHorse> extends SummonSpell {
        private final EntityType<T> type;
        private final double speed;
        private final double health;

        protected Mount(String key, EntityType<T> type, double speed, double health) {
            super(key, 1);
            this.type = type;
            this.speed = speed;
            this.health = health;
        }

        @Override
        protected int duration() {
            return MOUNT_TICKS;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            return caster instanceof Player;
        }

        protected void setup(T mount, Player rider) {
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, this.type, 1, castData, mount -> {
                Player rider = (Player)caster;
                mount.tameWithName(rider);
                mount.equipSaddle(null);
                Objects.requireNonNull(mount.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(this.speed);
                Objects.requireNonNull(mount.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(this.health);
                mount.setHealth(mount.getMaxHealth());
                this.setup(mount, rider);
            });
        }
    }

    /** Bone Steed: a skeleton horse that never tires and does not fear the dark. */
    public static class BoneSteed extends Mount<AbstractHorse> {
        @SuppressWarnings("unchecked")
        public BoneSteed() {
            super("bone_steed", (EntityType<AbstractHorse>)(EntityType<?>)EntityType.SKELETON_HORSE, 0.3, 30.0);
        }
    }

    /** Summon Camel: a camel that two players can ride. */
    public static class Camel extends Mount<AbstractHorse> {
        @SuppressWarnings("unchecked")
        public Camel() {
            super("camel", (EntityType<AbstractHorse>)(EntityType<?>)EntityType.CAMEL, 0.12, 40.0);
        }
    }

    /** Storm Steed: a white horse of lightning, very fast, that shocks what it runs through. */
    public static class StormSteed extends Mount<Horse> {
        public StormSteed() {
            super("storm_steed", EntityType.HORSE, 0.375, 34.0);
        }

        @Override
        protected void setup(Horse horse, Player rider) {
            horse.setVariant(Variant.WHITE);
            horse.equipArmor(rider, new ItemStack(Items.GOLDEN_HORSE_ARMOR));
        }
    }

    /** Void Strider: a black horse of the End that blinks 4 blocks forward every 2 s while ridden at speed. */
    public static class VoidStrider extends Mount<Horse> {
        public VoidStrider() {
            super("void_strider", EntityType.HORSE, 0.3, 30.0);
        }

        @Override
        protected void setup(Horse horse, Player rider) {
            horse.setVariant(Variant.BLACK);
            horse.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
    }

    /** Spectral Boat: a ghostly glowing boat for 5 minutes that is faster than a normal one (client/SpectralBoatClient). */
    public static class SpectralBoat extends ModifiedSpell {
        public SpectralBoat() {
            super("spectral_boat", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = caster.position().add(caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(2.0));
                Boat boat = new Boat(level, at.x, at.y + 0.2, at.z);
                boat.setVariant(Boat.Type.BIRCH);
                boat.setYRot(caster.getYRot());
                boat.setCustomName(Component.translatable(SPECTRAL_BOAT));
                boat.setGlowingTag(true);
                level.addFreshEntity(boat);
                Zones.start(server, at, caster, 0.0F, MOUNT_TICKS, 20, zone -> server.sendParticles(ParticleTypes.SOUL,
                    boat.getX(), boat.getY() + 0.5, boat.getZ(), 3, 0.5, 0.2, 0.5, 0.01), () -> {
                }, boat);
                server.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.5, at.z, 30, 0.6, 0.3, 0.6, 0.03);
                level.playSound(null, caster.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        switch (Summons.kind(mob)) {
            case "storm_steed" -> mob.goalSelector.addGoal(0, new PulseGoal(mob, 10, MountSpells::shock));
            case "void_strider" -> mob.goalSelector.addGoal(0, new PulseGoal(mob, 40, MountSpells::blink));
            default -> {
            }
        }
    }

    private static boolean galloping(Mob mount) {
        return mount.isVehicle() && new Vec3(mount.getX() - mount.xo, 0.0, mount.getZ() - mount.zo).lengthSqr() > 0.04;
    }

    private static void shock(Mob steed) {
        ServerLevel level = (ServerLevel)steed.level();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, steed.getX(), steed.getY(0.6), steed.getZ(), 6, 0.5, 0.5, 0.5, 0.05);
        if (!galloping(steed) || !(Summons.owner(steed) instanceof LivingEntity owner)) {
            return;
        }
        for (LivingEntity enemy : Targets.enemies(owner, steed.position(), 2.0)) {
            if (enemy != steed && !steed.hasPassenger(enemy)) {
                enemy.hurt(steed.damageSources().lightningBolt(), 3.0F + SummonSpells.power(steed));
                SummonSpells.spark(level, steed.getEyePosition(), enemy.getEyePosition());
            }
        }
    }

    /** The rider steers the mount from their client, so the blink carries both: off, teleport, back on. */
    private static void blink(Mob strider) {
        Entity rider = strider.getControllingPassenger();
        if (!galloping(strider) || rider == null) {
            return;
        }
        ServerLevel level = (ServerLevel)strider.level();
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot()).scale(4.0);
        Vec3 to = strider.position().add(forward);
        AABB box = strider.getBoundingBox().move(forward);
        if (!level.noCollision(strider, box) || !level.noCollision(rider, rider.getBoundingBox().move(forward))) {
            return;
        }
        level.sendParticles(ParticleTypes.PORTAL, strider.getX(), strider.getY(0.5), strider.getZ(), 40, 0.6, 0.6, 0.6, 0.3);
        rider.stopRiding();
        strider.teleportTo(to.x, to.y, to.z);
        rider.teleportTo(to.x, to.y, to.z);
        rider.startRiding(strider, true);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 0.8, to.z, 40, 0.6, 0.6, 0.6, 0.1);
        level.playSound(null, strider.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.2F);
    }
}
