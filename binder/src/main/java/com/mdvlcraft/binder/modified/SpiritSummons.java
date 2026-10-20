package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.modified.SummonSpells.SummonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.SummonedEntitiesCastData;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The spirit summons of the Spirit vocations: drowned ghosts, an honour guard, void yokai, a ghost hawk and a spectral steed. */
@EventBusSubscriber(modid = "mdvlcraft")
public final class SpiritSummons {
    private SpiritSummons() {
    }

    /** Funayurei: three drowned ghosts with ladles (wooden shovels) whose hits slow and drag down. */
    public static class Funayurei extends SummonSpell {
        Funayurei() {
            super("funayurei", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.DROWNED, 3, castData, drowned -> {
                drowned.setBaby(false);
                UndeadSummons.equip(drowned, EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SHOVEL));
                UndeadSummons.cap(drowned, 0xD8E8E8);
            });
        }
    }

    /** Honour Guard: a spirit samurai with a shield that draws enemies near you every 3 s and takes a third of the hits you take. */
    public static class HonorGuard extends SummonSpell {
        HonorGuard() {
            super("honor_guard", 5);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VINDICATOR, 1, castData, guard -> {
                UndeadSummons.equip(guard, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                UndeadSummons.equip(guard, EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                UndeadSummons.cap(guard, 0xB0182A);
                UndeadSummons.stats(guard, 60.0, 4.0 + power);
                Objects.requireNonNull(guard.getAttribute(Attributes.ARMOR)).setBaseValue(10.0);
            });
        }
    }

    /** Void Yokai: three small void spirits (vexes) that blink to their target every 3 s and weaken what they hit. */
    public static class VoidYokai extends SummonSpell {
        VoidYokai() {
            super("void_yokai", 3);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 3, castData, vex -> {
            });
        }
    }

    /** Spirit Hawk: a ghost hawk (a vex, so it flies through walls) that makes the nearest enemy glow and soulburns what it strikes. */
    public static class SpiritHawk extends SummonSpell {
        SpiritHawk() {
            super("spirit_hawk", 4);
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.VEX, 1, castData, vex -> {
            });
        }
    }

    /** Spirit Steed: players only; a fast spectral horse that freezes a path over water as it runs (like Frost Walker). */
    public static class SpiritSteed extends SummonSpell {
        SpiritSteed() {
            super("spirit_steed", 1);
        }

        @Override
        protected int duration() {
            return 6000;
        }

        @Override
        public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
            return caster instanceof Player;
        }

        @Override
        protected void summon(ServerLevel level, LivingEntity caster, float power, SummonedEntitiesCastData castData) {
            this.spawn(level, caster, power, EntityType.HORSE, 1, castData, (Horse horse) -> {
                horse.tameWithName((Player)caster);
                horse.equipSaddle(null);
                horse.setVariant(Variant.WHITE);
                horse.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 6000, 0, false, false));
                Objects.requireNonNull(horse.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.34);
                Objects.requireNonNull(horse.getAttribute(Attributes.JUMP_STRENGTH)).setBaseValue(0.8);
                Objects.requireNonNull(horse.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(30.0);
                horse.setHealth(horse.getMaxHealth());
            });
        }
    }

    public static List<SummonSpell> all() {
        return List.of(new Funayurei(), new HonorGuard(), new VoidYokai(), new SpiritHawk(), new SpiritSteed());
    }

    /** Freezes the water under and around a spirit steed into frosted ice, which melts as Frost Walker's does. */
    static void frostPath(Mob steed) {
        if (!(steed.level() instanceof ServerLevel level) || !steed.onGround() && !steed.isInWater()) {
            return;
        }
        BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();
        BlockPos under = steed.blockPosition().below();
        for (BlockPos pos : BlockPos.betweenClosed(under.offset(-2, 0, -2), under.offset(2, 0, 2))) {
            if (level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos).isSource() && level.isEmptyBlock(pos.above())) {
                level.setBlockAndUpdate(pos, frosted);
                level.scheduleTick(pos, Blocks.FROSTED_ICE, 60 + steed.getRandom().nextInt(60));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        switch (Summons.kind(mob)) {
            case "funayurei" -> mob.goalSelector.addGoal(2, new MeleeAttackGoal((PathfinderMob)mob, 1.0, false));
            case "spirit_steed" -> mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 2, SpiritSummons::frostPath));
            case "honor_guard" -> mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 60, guard -> {
                if (Summons.owner(guard) instanceof LivingEntity owner) {
                    CloneSpells.taunt(owner, guard, 8.0);
                }
            }));
            case "void_yokai" -> mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 60, yokai -> {
                LivingEntity target = yokai.getTarget();
                if (target != null && target.isAlive() && yokai.distanceTo(target) > 3.0 && yokai.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, yokai.getX(), yokai.getY(), yokai.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
                    Vec3 next = target.position().add(0.0, target.getBbHeight() + 0.5, 0.0);
                    yokai.teleportTo(next.x, next.y, next.z);
                    level.playSound(null, yokai.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.6F, 1.6F);
                }
            }));
            case "spirit_hawk" -> mob.goalSelector.addGoal(0, new SummonSpells.PulseGoal(mob, 40, hawk -> {
                if (Summons.owner(hawk) instanceof LivingEntity owner) {
                    Targets.enemies(owner, owner.position(), 16.0).stream().filter(e -> e != owner)
                        .min((a, b) -> Double.compare(a.distanceToSqr(owner), b.distanceToSqr(owner)))
                        .ifPresent(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0), owner));
                }
            }));
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof Mob attacker && Summons.owner(attacker) instanceof LivingEntity owner) {
            switch (Summons.kind(attacker)) {
                case "funayurei" -> {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), owner);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, -0.4, 0.0));
                    victim.hurtMarked = true;
                }
                case "void_yokai" -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), owner);
                case "spirit_hawk" -> BoltSpells.soulburn(owner, victim, 80);
                default -> {
                }
            }
        }
        // an honour guard within 8 blocks takes a third of what its owner takes
        if (victim.level() instanceof ServerLevel level && event.getAmount() > 0.0F) {
            level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(8.0),
                    m -> m.isAlive() && "honor_guard".equals(Summons.kind(m)) && Summons.owner(m) == victim)
                .stream().findFirst().ifPresent(guard -> {
                    float share = event.getAmount() / 3.0F;
                    event.setAmount(event.getAmount() - share);
                    guard.hurt(guard.damageSources().generic(), share);
                });
        }
    }
}
