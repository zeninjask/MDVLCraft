package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.util.ModTags;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Iron's Root vines, drawn black (client/BlackRootRenderer): Underworld Gate's roots. Nothing can target the vines, and
 * whoever they hold is out of the fight: it can't take a target (RootedMobs) and stays facing the way it faced when
 * rooted, so it can be assassinated (Combat Evolution: from behind, and the target must have no target of its own).
 */
public class BlackRoot extends RootEntity {
    private static final float HEALTH = 40.0F;   // Iron's Root at power 1

    public BlackRoot(EntityType<? extends RootEntity> type, Level level) {
        super(type, level);
    }

    /** Mobs don't take the vines for an enemy. */
    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    /** After the rider's own tick (its AI included), so whatever it tried to turn to is undone. */
    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction move) {
        super.positionRider(passenger, move);
        passenger.setYRot(this.getYRot());
        passenger.setXRot(0.0F);
        passenger.setYHeadRot(this.getYRot());
        if (passenger instanceof LivingEntity living) {
            living.yBodyRot = this.getYRot();
            living.yBodyRotO = this.getYRot();
            living.yHeadRotO = this.getYRot();
        }
    }

    /** Holds `target` in black vines for `ticks`, as Iron's Root spell does; creatures that can't be rooted are left alone. */
    static void hold(LivingEntity caster, LivingEntity target, int ticks) {
        if (target.getType().is(ModTags.CANT_ROOT)) {
            return;
        }

        BlackRoot root = new BlackRoot(ModifiedEntities.BLACK_ROOT.get(), caster.level());
        root.setOwner(caster);
        root.setDuration(ticks);
        root.setTarget(target);
        root.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0.0F);
        if (target instanceof Mob mob) {
            RootedMobs.calm(mob);
        }
        Objects.requireNonNull(root.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(HEALTH);
        root.setHealth(HEALTH);
        caster.level().addFreshEntity(root);
        target.startRiding(root, true);
    }
}
