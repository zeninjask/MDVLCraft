package com.mdvlcraft.binder.combat;

import com.mdvlcraft.binder.ability.Ability;
import com.mdvlcraft.binder.ability.MonkArts;
import com.mdvlcraft.binder.ability.SlashPierce;
import com.mdvlcraft.binder.attribute.BinderAttributes;
import com.mdvlcraft.binder.config.BinderConfig;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "mdvlcraft")
public final class TechniqueScaling {
    private TechniqueScaling() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer caster
            && event.getSource().typeHolder().unwrapKey().map(key -> key.location().getNamespace().equals("cursedfate")).orElse(false)
) {
            boolean slash = SlashPierce.slashHit(event.getSource());
            boolean sear = SlashPierce.searingNow();
            // Shrine's techniques (its slashes and Sear) don't scale with the held weapon, but with half of fire spell power;
            // the slashes also with Shrine Damage
            float factor = MonkArts.artActive(caster) || slash || sear ? 1.0F : factor(caster, event);
            float shrine = slash ? SlashPierce.SHRINE_DAMAGE * (1.0F + (float)caster.getAttributeValue(BinderAttributes.SHRINE_DAMAGE.get())) : 1.0F;
            if (slash || sear) {   // fire spell power counts half: +25% of it is +12.5% here
                shrine *= 1.0F + ((float)caster.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER.get()) - 1.0F) * 0.5F;
            }
            event.setAmount(event.getAmount() * factor * shrine * Ability.DAMAGE_SCALE);
        }
    }

    private static float factor(ServerPlayer caster, LivingHurtEvent event) {
        double reference = BinderConfig.techniqueReferenceDamage();
        if (reference <= 0.0) {
            return 1.0F;
        }

        double damage = caster.getAttributeValue(Attributes.ATTACK_DAMAGE)
            + EnchantmentHelper.getDamageBonus(caster.getMainHandItem(), event.getEntity().getMobType());
        return (float)Math.max(BinderConfig.techniqueMinimumFactor(), damage / reference);
    }
}
