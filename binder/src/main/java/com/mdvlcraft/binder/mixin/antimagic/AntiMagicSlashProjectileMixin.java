package com.mdvlcraft.binder.mixin.antimagic;

import com.tiago.antimagic.entity.AntiMagicSlashProjectile;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Every Anti-Magic Slash (the Demon Dweller's, the spell's and the Anti-Magic Demon's) passes through blocks without
 * breaking them and still hits what is behind them: its hit check looks for entities only.
 */
@Mixin(value = AntiMagicSlashProjectile.class, remap = false)
public abstract class AntiMagicSlashProjectileMixin {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;m_278158_(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"), require = 1)
    private HitResult mdvlcraft$throughBlocks(Entity slash, Predicate<Entity> canHit) {
        Vec3 from = slash.position();
        Vec3 to = from.add(slash.getDeltaMovement());
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(slash.level(), slash, from, to,
            slash.getBoundingBox().expandTowards(slash.getDeltaMovement()).inflate(1.0), canHit);
        return hit != null ? hit : BlockHitResult.miss(to, Direction.getNearest(to.x - from.x, to.y - from.y, to.z - from.z), BlockPos.containing(to));
    }
}
