package com.mdvlcraft.binder.modified;

import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Who a Modified ability reaches: the creature looked at, and the allies or enemies around a point (Summons.allied). */
public final class Targets {
    private Targets() {
    }

    /** The living creature the caster looks at within `reach` blocks (blocks stop the look), or null. */
    @Nullable
    public static LivingEntity looked(LivingEntity caster, float reach) {
        HitResult hit = Utils.raycastForEntity(caster.level(), caster, reach, true, 0.35F);
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && target.isAlive()
            ? target : null;
    }

    /** The caster's allies (the caster included) within `radius` of `center`. */
    public static List<LivingEntity> allies(LivingEntity caster, Vec3 center, double radius) {
        return around(caster.level(), center, radius).stream().filter(e -> Summons.allied(caster, e)).toList();
    }

    /** Everything within `radius` of `center` that is not on the caster's side. */
    public static List<LivingEntity> enemies(LivingEntity caster, Vec3 center, double radius) {
        return around(caster.level(), center, radius).stream().filter(e -> !Summons.allied(caster, e)).toList();
    }

    private static List<LivingEntity> around(Level level, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
            e -> e.isAlive() && !(e instanceof ArmorStand) && e.distanceToSqr(center) <= radius * radius);
    }
}
