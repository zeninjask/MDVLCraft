package com.mdvlcraft.binder.mixin.cursedfate;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cursed Fate's innate technique damage (Shrine: Dismantle, Cleave, the domain's cuts) adds attack damage / 1.5, so it
 * grew with the held weapon. DamageValuesRegisterProcedure reads attack damage in four branches, each for innate, then
 * strength, then weapon damage; the innate reads (ordinals 0, 3, 6 and 9) leave out the main-hand item's attack damage.
 */
@Mixin(targets = "cursedfate.procedures.DamageValuesRegisterProcedure", remap = false)
public abstract class InnateDamageNoWeaponMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;m_22135_()D", ordinal = 0), require = 1)
    private static double mdvlcraft$innate0(AttributeInstance attackDamage, LevelAccessor world, Entity entity) {
        return mdvlcraft$withoutWeapon(attackDamage, entity);
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;m_22135_()D", ordinal = 3), require = 1)
    private static double mdvlcraft$innate3(AttributeInstance attackDamage, LevelAccessor world, Entity entity) {
        return mdvlcraft$withoutWeapon(attackDamage, entity);
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;m_22135_()D", ordinal = 6), require = 1)
    private static double mdvlcraft$innate6(AttributeInstance attackDamage, LevelAccessor world, Entity entity) {
        return mdvlcraft$withoutWeapon(attackDamage, entity);
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;m_22135_()D", ordinal = 9), require = 1)
    private static double mdvlcraft$innate9(AttributeInstance attackDamage, LevelAccessor world, Entity entity) {
        return mdvlcraft$withoutWeapon(attackDamage, entity);
    }

    private static double mdvlcraft$withoutWeapon(AttributeInstance attackDamage, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return attackDamage.getValue();
        }
        List<AttributeModifier> weapon = new ArrayList<>();
        for (AttributeModifier modifier : living.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (attackDamage.hasModifier(modifier)) {
                attackDamage.removeModifier(modifier.getId());
                weapon.add(modifier);
            }
        }
        double bare = attackDamage.getValue();
        weapon.forEach(attackDamage::addTransientModifier);
        return bare;
    }
}
