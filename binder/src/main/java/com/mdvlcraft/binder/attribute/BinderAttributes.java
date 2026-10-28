package com.mdvlcraft.binder.attribute;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BinderAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "mdvlcraft");
    public static final RegistryObject<Attribute> GREATSWORD_DAMAGE = fraction("greatsword_damage", 5.0);
    public static final RegistryObject<Attribute> KATANA_DAMAGE = fraction("katana_damage", 5.0);
    public static final RegistryObject<Attribute> PARRY_STAMINA = fraction("parry_stamina", 1.0);
    public static final RegistryObject<Attribute> PARRY_WINDOW = fraction("parry_window", 3.0);
    public static final RegistryObject<Attribute> DODGE_DISTANCE = fraction("dodge_distance", 2.0);
    public static final RegistryObject<Attribute> DAGGER_DAMAGE = fraction("dagger_damage", 5.0);
    public static final RegistryObject<Attribute> STAFF_DAMAGE = fraction("staff_damage", 5.0);
    public static final RegistryObject<Attribute> SPEAR_DAMAGE = fraction("spear_damage", 5.0);
    public static final RegistryObject<Attribute> POLEARM_DAMAGE = fraction("polearm_damage", 5.0);
    public static final RegistryObject<Attribute> BACKSTAB_DAMAGE = fraction("backstab_damage", 5.0);
    public static final RegistryObject<Attribute> RAPIER_DAMAGE = fraction("rapier_damage", 5.0);
    public static final RegistryObject<Attribute> RIPOSTE_DAMAGE = fraction("riposte_damage", 5.0);
    public static final RegistryObject<Attribute> DODGE_EFFICIENCY = fraction("dodge_efficiency", 0.9);
    public static final RegistryObject<Attribute> SATURATION_BONUS = fraction("saturation_bonus", 5.0);
    public static final RegistryObject<Attribute> PLUNDER = fraction("plunder", 1.0);
    public static final RegistryObject<Attribute> NATURE_REGEN = ATTRIBUTES.register(
        "nature_regen", () -> new RangedAttribute("attribute.mdvlcraft.nature_regen", 0.0, 0.0, 10.0).setSyncable(true)
    );
    public static final RegistryObject<Attribute> TECHNIQUE_EFFICIENCY = fraction("technique_efficiency", 0.9);
    public static final RegistryObject<Attribute> ARROW_PENETRATION = fraction("arrow_penetration", 1.0);
    public static final RegistryObject<Attribute> UNARMED_DAMAGE = ATTRIBUTES.register(
        "unarmed_damage", () -> new RangedAttribute("attribute.mdvlcraft.unarmed_damage", 0.0, 0.0, 20.0).setSyncable(true)
    );
    public static final RegistryObject<Attribute> FIST_MASTERY = fraction("fist_mastery", 5.0);
    public static final RegistryObject<Attribute> UNARMED_PARRY = fraction("unarmed_parry", 1.0);
    public static final RegistryObject<Attribute> AFFLICTION_IMMUNITY = fraction("affliction_immunity", 1.0);
    /** Master Chef's Shrine Mastery nodes: extra damage on shrine techniques (ability/SlashPierce, combat/TechniqueScaling). */
    public static final RegistryObject<Attribute> SHRINE_DAMAGE = fraction("shrine_damage", 5.0);

    private BinderAttributes() {
    }

    private static RegistryObject<Attribute> fraction(String name, double max) {
        return ATTRIBUTES.register(name, () -> new RangedAttribute("attribute.mdvlcraft." + name, 0.0, 0.0, max).setSyncable(true));
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(BinderAttributes::onAttributeModification);
    }

    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        ATTRIBUTES.getEntries().forEach(attribute -> event.add(EntityType.PLAYER, attribute.get()));
        // Iron's spellcaster goals read every mob's entity reach, but some addon mobs built on Iron's classes (Traveloptics'
        // Enraged Dead King) never register it, so their first melee crashes the server. Every mob gets the default.
        event.getTypes().stream().filter(type -> !event.has(type, ForgeMod.ENTITY_REACH.get()))
            .forEach(type -> event.add(type, ForgeMod.ENTITY_REACH.get()));
    }
}
