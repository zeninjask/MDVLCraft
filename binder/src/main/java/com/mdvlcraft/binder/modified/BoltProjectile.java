package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.entity.spells.thrown_item.ThrownItemProjectile;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The flight of a bolt Modified ability (BoltSpells): an item drawn by Iron's thrown-item renderer, trailing the bolt's
 * particles. What it does on a hit comes from its Bolt, looked up by key; a bolt reloaded with its chunk has no caster and
 * is removed.
 */
public class BoltProjectile extends ThrownItemProjectile {
    private String key = "";

    public BoltProjectile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public BoltProjectile(Level level, BoltSpells.Bolt bolt, LivingEntity caster) {
        this(ModifiedEntities.BOLT.get(), level);
        this.key = bolt.key();
        this.setThrownItem(new ItemStack(bolt.item));
        this.setScale(bolt.scale);
        this.setOwner(caster);
    }

    @Nullable
    private BoltSpells.Bolt bolt() {
        return BoltSpells.BOLTS.get(this.key);
    }

    @Override
    public float getSpeed() {
        BoltSpells.Bolt bolt = this.bolt();
        return bolt == null ? 1.0F : bolt.speed;
    }

    @Override
    protected double getDefaultGravity() {
        BoltSpells.Bolt bolt = this.bolt();
        return bolt == null ? 0.0 : bolt.gravity;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        BoltSpells.Bolt bolt = this.bolt();
        if (this.level() instanceof ServerLevel level && bolt != null && this.getOwner() instanceof LivingEntity caster
            && result.getEntity() instanceof LivingEntity target) {
            bolt.hit.apply(bolt, caster, target, this.getDamage());
            if (bolt.land != null) {
                bolt.land.apply(bolt, level, caster, result.getLocation(), this.getDamage());
            }
            this.pierceOrDiscard();   // Iron's own entity hit does this; without it a bolt flew on and landed a second time
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        BoltSpells.Bolt bolt = this.bolt();
        if (this.level() instanceof ServerLevel level && bolt != null && bolt.land != null && this.getOwner() instanceof LivingEntity caster) {
            bolt.land.apply(bolt, level, caster, result.getLocation(), this.getDamage());
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            BoltSpells.Bolt bolt = this.bolt();
            if (bolt == null || !(this.getOwner() instanceof LivingEntity)) {
                this.discard();
                return;
            }
            Vec3 at = this.position();
            level.sendParticles(bolt.trail, at.x, at.y + this.getBbHeight() / 2, at.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Bolt", this.key);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.key = tag.getString("Bolt");
    }
}
