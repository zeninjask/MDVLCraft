package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.util.ModTags;
import java.util.Objects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Iron's Root vines, drawn black (client/BlackRootRenderer): Underworld Gate's roots. */
public class BlackRoot extends RootEntity {
    private static final float HEALTH = 40.0F;   // Iron's Root at power 1

    public BlackRoot(EntityType<? extends RootEntity> type, Level level) {
        super(type, level);
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
        root.moveTo(target.position());
        Objects.requireNonNull(root.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(HEALTH);
        root.setHealth(HEALTH);
        caster.level().addFreshEntity(root);
        target.startRiding(root, true);
    }
}
