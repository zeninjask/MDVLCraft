package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.combat.EnchantmentScaling;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A weapon's hit damage with its enchantments, each against the target it is meant for: Sharpness on anything, Smite on
 * undead, Bane of Arthropods on arthropods, Impaling on water mobs, Misanthropy on villagers and illagers, Backstabbing
 * from behind, Death Wish at the edge of death and Massacre on an execution. The best target wins.
 */
final class EnchantedDamage {
    private static final List<MobType> TARGETS = List.of(MobType.UNDEFINED, MobType.UNDEAD, MobType.ARTHROPOD, MobType.ILLAGER, MobType.WATER);
    /** Misanthropy hits "humans": villagers (UNDEFINED) and illagers. */
    private static final List<MobType> HUMANS = List.of(MobType.UNDEFINED, MobType.ILLAGER);
    private static final ResourceLocation MISANTHROPY = ResourceLocation.fromNamespaceAndPath("majruszsenchantments", "misanthropy");
    private static final ResourceLocation DEATH_WISH = ResourceLocation.fromNamespaceAndPath("majruszsenchantments", "death_wish");
    private static final ResourceLocation BACKSTABBING = ResourceLocation.fromNamespaceAndPath("farmersdelight", "backstabbing");
    private static final ResourceLocation MASSACRE = ResourceLocation.fromNamespaceAndPath("combat_evolution", "massacre");
    /** config/majruszsenchantments.json: misanthropy damage_bonus_per_level, death_wish damage_multiplier_range max. */
    private static final float MISANTHROPY_PER_LEVEL = 2.0F;
    private static final float DEATH_WISH_MAX = 1.8F;
    /** config/combat_evolution-common.toml massacreEnchantment. */
    private static final float MASSACRE_PER_LEVEL = 0.128F;

    private EnchantedDamage() {
    }

    static double bestCase(ItemStack weapon, double damage) {
        int misanthropy = level(MISANTHROPY, weapon);
        double best = 0.0;
        for (MobType target : TARGETS) {
            double flat = EnchantmentHelper.getDamageBonus(weapon, target) + (HUMANS.contains(target) ? misanthropy * MISANTHROPY_PER_LEVEL : 0.0);
            best = Math.max(best, damage + flat);
        }

        int backstabbing = level(BACKSTABBING, weapon);
        if (backstabbing > 0) {
            // Farmer's Delight multiplies by 1.2 + 0.2 a level; the pack keeps 80% of the extra (BackstabbingEnchantmentMixin).
            best *= 1.0 + (0.2 + backstabbing * 0.2) * EnchantmentScaling.SCALE;
        }

        if (level(DEATH_WISH, weapon) > 0) {
            best *= DEATH_WISH_MAX;
        }

        best *= 1.0 + level(MASSACRE, weapon) * MASSACRE_PER_LEVEL;
        return best;
    }

    private static int level(ResourceLocation id, ItemStack weapon) {
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (enchantment == null) {
            throw new IllegalStateException("Enchantment " + id + " is not registered");
        }

        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, weapon);
    }
}
