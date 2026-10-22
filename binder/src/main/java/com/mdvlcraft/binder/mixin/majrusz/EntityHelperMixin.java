package com.mdvlcraft.binder.mixin.majrusz;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Majrusz's "human" check (Misanthropy's targets: villagers, wandering traders, players, witches, illagers) covers every
 * humanoid person in the pack: every villager type, Iron's and its addons' mages and other spell casters (not the undead
 * ones), Villager Recruits', Village Recruits' and Workers' people, the Cursed Fate characters (recruits, Gojo, Sukuna,
 * Naoya, Naobito) and the entity type tag mdvlcraft:humans (other mods' sorcerers, Herobrine and the like).
 */
@Pseudo
@Mixin(targets = "com.majruszlibrary.entity.EntityHelper", remap = false)
public abstract class EntityHelperMixin {
    private static final TagKey<EntityType<?>> HUMANS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("mdvlcraft", "humans"));

    @Inject(method = "isHuman", at = @At("RETURN"), cancellable = true)
    private static void mdvlcraft$moreHumans(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && entity != null) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entity instanceof AbstractVillager || entity.getType().is(HUMANS)
                || entity instanceof AbstractSpellCastingMob caster && caster.getMobType() != MobType.UNDEAD
                || id != null && (id.getNamespace().equals("recruits") || id.getNamespace().equals("village_recruits") || id.getNamespace().equals("workers")
                || id.getNamespace().equals("naoyaaddon") && id.getPath().endsWith("_npc")
                || id.getNamespace().equals("cursedfate") && id.getPath().startsWith("shinjuku_showdown_"))) {
                cir.setReturnValue(true);
            }
        }
    }
}
