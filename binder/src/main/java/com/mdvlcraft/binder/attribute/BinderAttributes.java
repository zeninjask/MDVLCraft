package com.mdvlcraft.binder.attribute;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Player attributes granted by the archetype skill trees that neither vanilla, Pufferfish's Attributes, Epic Fight nor
 * Iron's Spells provide (each one belongs to exactly one archetype). All default to 0; fractions are percentages
 * (0.1 = 10%). Their effects live in {@link AttributeEffects}.
 */
public final class BinderAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, MDVLBinder.MOD_ID);

    /** Knight: melee damage with greatswords. */
    public static final RegistryObject<Attribute> GREATSWORD_DAMAGE = fraction("greatsword_damage", 5);
    /** Samurai: melee damage with uchigatanas and tachis. */
    public static final RegistryObject<Attribute> KATANA_DAMAGE = fraction("katana_damage", 5);
    /** Samurai: share of max stamina restored by a successful parry. */
    public static final RegistryObject<Attribute> PARRY_STAMINA = fraction("parry_stamina", 1);
    /** Assassin: melee damage with daggers. */
    public static final RegistryObject<Attribute> DAGGER_DAMAGE = fraction("dagger_damage", 5);
    /** Assassin: melee damage when hitting a target from behind. */
    public static final RegistryObject<Attribute> BACKSTAB_DAMAGE = fraction("backstab_damage", 5);
    /** Phantom: melee damage with a sword and an empty off hand (rapier style). */
    public static final RegistryObject<Attribute> RAPIER_DAMAGE = fraction("rapier_damage", 5);
    /** Phantom: bonus damage of the first melee hit within 3 seconds of a successful dodge. */
    public static final RegistryObject<Attribute> RIPOSTE_DAMAGE = fraction("riposte_damage", 5);
    /** Phantom: stamina cost reduction of Epic Fight dodges. */
    public static final RegistryObject<Attribute> DODGE_EFFICIENCY = fraction("dodge_efficiency", 0.9);
    /** Wanderer: extra saturation from food. */
    public static final RegistryObject<Attribute> SATURATION_BONUS = fraction("saturation_bonus", 5);
    /** Thief: chance that a killed mob drops one of its items twice. */
    public static final RegistryObject<Attribute> PLUNDER = fraction("plunder", 1);
    /** Druid: health restored every 5 seconds while standing on natural ground under open sky. */
    public static final RegistryObject<Attribute> NATURE_REGEN = ATTRIBUTES.register("nature_regen",
            () -> new RangedAttribute("attribute.mdvlcraft.nature_regen", 0, 0, 10).setSyncable(true));
    /** Cursed: mana cost reduction of cursed techniques. */
    public static final RegistryObject<Attribute> TECHNIQUE_EFFICIENCY = fraction("technique_efficiency", 0.9);

    private BinderAttributes() {
    }

    private static RegistryObject<Attribute> fraction(String name, double max) {
        return ATTRIBUTES.register(name, () -> new RangedAttribute("attribute.mdvlcraft." + name, 0, 0, max).setSyncable(true));
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(BinderAttributes::onAttributeModification);
    }

    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        ATTRIBUTES.getEntries().forEach(attribute -> event.add(EntityType.PLAYER, attribute.get()));
    }
}
