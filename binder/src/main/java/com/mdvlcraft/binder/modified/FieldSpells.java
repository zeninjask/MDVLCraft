package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import virtuoel.pehkui.api.ScaleTypes;

/** Modified abilities that set something down on the field: plants, turrets, a trader, and spirits that sweep past. */
public final class FieldSpells {
    private FieldSpells() {
    }

    static Vec3 ahead(LivingEntity caster, double distance) {
        return caster.position().add(caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(distance));
    }

    /**
     * Corpse Flower: a huge rotting flower 2 blocks ahead for 20 s; every 1.5 s it spits poison at the nearest enemy within
     * 8 blocks (Poison II and half the spell's power) and heals the caster's summons within 6 blocks.
     */
    public static class CorpseFlower extends ModifiedSpell {
        public CorpseFlower() {
            super("corpse_flower", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Vec3 at = ahead(caster, 2.0);
                ArmorStand flower = new ArmorStand(level, at.x, at.y - 1.2, at.z);
                flower.setInvisible(true);
                flower.setNoGravity(true);
                flower.setInvulnerable(true);
                flower.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.WITHER_ROSE));
                ScaleTypes.BASE.getScaleData(flower).setScale(3.0F);
                level.addFreshEntity(flower);
                Zones.start(server, at, caster, power, 400, 30, zone -> {
                    Targets.enemies(caster, at, 8.0).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(at))).ifPresent(enemy -> {
                        enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), caster);
                        DamageSources.applyDamage(enemy, power * 0.5F, this.getDamageSource(caster));
                        Vec3 from = at.add(0.0, 1.5, 0.0);
                        Vec3 step = enemy.getEyePosition().subtract(from).scale(0.1);
                        for (int i = 0; i <= 10; i++) {
                            Vec3 p = from.add(step.scale(i));
                            server.sendParticles(ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
                        }
                    });
                    for (LivingEntity ally : Targets.allies(caster, at, 6.0)) {
                        if (!Summons.kind(ally).isEmpty() && Summons.owner(ally) == caster) {
                            ally.heal(power * 0.5F);
                            server.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0) + 0.3, ally.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
                        }
                    }
                    server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 1.0, at.z, 12, 1.0, 0.8, 1.0, 0.0);
                }, () -> {
                }, flower);
                level.playSound(null, caster.blockPosition(), SoundEvents.BIG_DRIPLEAF_TILT_DOWN, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * Siege Ballista: a ballista 2 blocks ahead, facing the way the caster looks, for 15 s; every 1.5 s it shoots a heavy
     * piercing bolt at the nearest enemy within 24 blocks in front of it.
     */
    public static class Ballista extends ModifiedSpell {
        public Ballista() {
            super("ballista", 6);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Vec3 at = ahead(caster, 2.0);
                Vec3 facing = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                Direction direction = Direction.getNearest(facing.x, 0.0, facing.z);
                CompoundTag tag = new CompoundTag();
                tag.putString("id", "minecraft:block_display");
                CompoundTag state = new CompoundTag();
                state.putString("Name", "minecraft:dispenser");
                CompoundTag properties = new CompoundTag();
                properties.putString("facing", direction.getSerializedName());
                state.put("Properties", properties);
                tag.put("block_state", state);
                Entity turret = EntityType.loadEntityRecursive(tag, level, e -> {
                    e.moveTo(Math.floor(at.x), Math.floor(at.y), Math.floor(at.z), 0.0F, 0.0F);
                    return e;
                });
                if (turret == null) {
                    throw new IllegalStateException("Could not make the ballista's block display");
                }
                level.addFreshEntity(turret);
                Vec3 muzzle = new Vec3(Math.floor(at.x) + 0.5, Math.floor(at.y) + 0.5, Math.floor(at.z) + 0.5);
                Zones.start(server, muzzle, caster, power, 300, 30, zone -> Targets.enemies(caster, muzzle, 24.0).stream()
                    .filter(e -> e.getEyePosition().subtract(muzzle).normalize().dot(facing) > 0.5)
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(muzzle))).ifPresent(enemy -> {
                        Arrow bolt = new Arrow(level, caster);
                        bolt.setPos(muzzle.add(facing.scale(0.7)));
                        Vec3 aim = enemy.getEyePosition().subtract(bolt.position());
                        bolt.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.05, aim.z, 3.2F, 0.5F);
                        bolt.setBaseDamage(power / 3.0);
                        bolt.setCritArrow(true);
                        bolt.setPierceLevel((byte)2);
                        bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
                        level.addFreshEntity(bolt);
                        level.playSound(null, turret.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2F, 0.7F);
                    }), () -> {
                }, turret);
                level.playSound(null, caster.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6F, 1.2F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /** Fence: a travelling trader stands 2 blocks ahead for 60 s and buys common loot for emeralds. */
    public static class Fence extends ModifiedSpell {
        public Fence() {
            super("fence", 0);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                Vec3 at = ahead(caster, 2.0);
                WanderingTrader trader = EntityType.WANDERING_TRADER.create(server);
                if (trader == null) {
                    throw new IllegalStateException("Cannot create a wandering trader");
                }
                trader.moveTo(at.x, at.y, at.z, caster.getYRot() + 180.0F, 0.0F);
                trader.setNoAi(true);
                trader.setInvulnerable(true);
                trader.setDespawnDelay(1200);
                MerchantOffers offers = trader.getOffers();
                offers.clear();
                offers.add(buy(Items.ROTTEN_FLESH, 12));
                offers.add(buy(Items.BONE, 10));
                offers.add(buy(Items.STRING, 10));
                offers.add(buy(Items.SPIDER_EYE, 6));
                offers.add(buy(Items.GUNPOWDER, 6));
                offers.add(buy(Items.ARROW, 24));
                offers.add(buy(Items.LEATHER, 6));
                offers.add(buy(Items.IRON_INGOT, 3));
                offers.add(buy(Items.GOLD_INGOT, 3));
                level.addFreshEntity(trader);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1.0, at.z, 20, 0.4, 0.6, 0.4, 0.0);
                level.playSound(null, caster.blockPosition(), SoundEvents.WANDERING_TRADER_REAPPEARED, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }

        private static MerchantOffer buy(net.minecraft.world.item.Item item, int count) {
            return new MerchantOffer(new ItemStack(item, count), new ItemStack(Items.EMERALD), 64, 1, 0.0F);
        }
    }

    /**
     * Jade Serpent: a jade serpent spirit coils around the creature looked at (16 blocks) for 4 s, holding it in place and
     * crushing it for 40% of the spell's power every half second.
     */
    public static class JadeSerpent extends ModifiedSpell {
        public JadeSerpent() {
            super("jade_serpent", 6);
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
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            LivingEntity target = Targets.looked(caster, 16.0F);
            if (level instanceof ServerLevel server && target != null) {
                float power = this.getSpellPower(spellLevel, caster);
                Zones.start(server, target.position(), caster, power, 80, 10, zone -> {
                    if (!target.isAlive()) {
                        return;
                    }
                    MartialSpells.root(target, 15);
                    DamageSources.applyDamage(target, power * 0.4F, this.getDamageSource(caster));
                    float spin = server.getGameTime() * 0.4F;
                    for (int i = 0; i < 24; i++) {
                        float angle = spin + i * 0.5F;
                        double radius = target.getBbWidth() * 0.8 + 0.3;
                        server.sendParticles(ParticleTypes.GLOW, target.getX() + Mth.cos(angle) * radius, target.getY() + i * target.getBbHeight() / 24.0,
                            target.getZ() + Mth.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }, () -> {
                }, null);
                level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }

    /**
     * River Dragon: a jade river dragon sweeps 24 blocks ahead over 1.5 s; every ally it passes within 3 blocks is healed by
     * the spell's power and given Regeneration for 4 s.
     */
    public static class RiverDragon extends ModifiedSpell {
        public RiverDragon() {
            super("river_dragon", 4);
        }

        @Override
        public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
            if (level instanceof ServerLevel server) {
                float power = this.getSpellPower(spellLevel, caster);
                Vec3 start = caster.getEyePosition().subtract(0.0, 0.4, 0.0);
                Vec3 dir = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
                long began = server.getGameTime();
                Set<UUID> healed = new HashSet<>();
                Zones.start(server, start, caster, power, 30, 2, zone -> {
                    float progress = (server.getGameTime() - began) / 30.0F;
                    for (float back = 0.0F; back < 3.0F; back += 0.5F) {
                        float d = Math.max(0.0F, progress * 24.0F - back);
                        Vec3 body = start.add(dir.scale(d)).add(side.scale(Mth.sin(d * 0.6F) * 1.2));
                        server.sendParticles(back == 0.0F ? ParticleTypes.GLOW : ParticleTypes.SPLASH, body.x, body.y, body.z, 4, 0.2, 0.2, 0.2, 0.0);
                    }
                    Vec3 head = start.add(dir.scale(progress * 24.0F));
                    for (LivingEntity ally : Targets.allies(caster, head, 3.0)) {
                        if (healed.add(ally.getUUID())) {
                            ally.heal(power);
                            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0), caster);
                            server.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0) + 0.3, ally.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
                        }
                    }
                }, () -> {
                }, null);
                level.playSound(null, caster.blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            super.onCast(level, spellLevel, caster, source, data);
        }
    }
}
