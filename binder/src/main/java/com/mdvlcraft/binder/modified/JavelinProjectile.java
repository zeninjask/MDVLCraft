package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.entity.spells.thrown_item.ThrownItemProjectile;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Javelin Hurl's spear (drawn by Iron's thrown-item renderer): it pierces the first creature it hits, then on a second
 * creature or a block it turns and flies back to its thrower, passing through everything. The spear never leaves the
 * thrower's hand; this is its flight.
 */
public class JavelinProjectile extends ThrownItemProjectile {
    private static final int PIERCES = 1;
    private final Set<Integer> struck = new HashSet<>();
    private boolean returning;

    public JavelinProjectile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public JavelinProjectile(Level level, ItemStack spear) {
        this(ModifiedEntities.JAVELIN.get(), level);
        this.setThrownItem(spear);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !this.returning && !this.struck.contains(target.getId()) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.returning || this.level().isClientSide) {
            return;
        }

        Entity target = result.getEntity();
        this.struck.add(target.getId());
        DamageSource source = this.getOwner() instanceof Player player
            ? this.damageSources().thrown(this, player) : this.damageSources().thrown(this, this.getOwner());
        target.hurt(source, this.getDamage());
        if (this.struck.size() > PIERCES) {
            this.returning = true;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        this.returning = true;   // AbstractMagicProjectile.onHit already played the impact
    }

    @Override
    public boolean collidesWithBlocks() {
        return !this.returning;
    }

    @Override
    public void tick() {
        if (this.returning && !this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
                this.discard();
                return;
            }
            Vec3 toOwner = owner.getEyePosition().subtract(this.position());
            if (toOwner.length() < 1.5) {
                this.discard();
                return;
            }
            this.setDeltaMovement(toOwner.normalize().scale(1.6));
        }
        super.tick();
        if (this.returning) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.getDefaultGravity(), 0.0));
        }
    }
}
