package com.mdvlcraft.binder.wukong;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * The Ruyi Jingu Bang (item mdvlcraft:ruyi_jingu_bang, weapon type mdvlcraft:wukong_staff in
 * data/mdvlcraft/capabilities/weapons/), its animations (WukongAnimations), its charged smash (mdvlcraft:wukong_smash)
 * and the four-way Wukong dodge (mdvlcraft:wukong_dodge, taught by the Wukong vocation's tree).
 */
public final class WukongStaffSetup {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> RUYI_JINGU_BANG = ITEMS.register("ruyi_jingu_bang",
        () -> new RuyiJinguBang(Tiers.NETHERITE, 3, -3.0F, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static Skill SMASH;
    public static Skill DODGE;

    private WukongStaffSetup() {
    }

    public static void register(IEventBus modBus) {
        WeaponCategory.ENUM_MANAGER.registerEnumCls("mdvlcraft", WukongCategories.class);
        ITEMS.register(modBus);
        modBus.addListener(WukongAnimations::onRegistry);
        modBus.addListener(WukongStaffSetup::onSkillBuild);
        modBus.addListener(WukongStaffSetup::onWeaponPresets);
    }

    private static void onSkillBuild(SkillBuildEvent event) {
        SkillBuildEvent.ModRegistryWorker worker = event.createRegistryWorker("mdvlcraft");
        SMASH = worker.build("wukong_smash", WukongSmashSkill::new, WeaponInnateSkill.createWeaponInnateBuilder().setActivateType(Skill.ActivateType.HELD));
        DODGE = worker.build("wukong_dodge", DodgeSkill::new, DodgeSkill.createDodgeBuilder().setAnimations(
            WukongAnimations.DODGE_FORWARD, WukongAnimations.DODGE_BACKWARD, WukongAnimations.DODGE_LEFT, WukongAnimations.DODGE_RIGHT));
    }

    private static void onWeaponPresets(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "wukong_staff"), item -> WeaponCapability.builder()
            .category(WukongCategories.WK_STAFF)
            .styleProvider(patch -> Styles.TWO_HAND)
            .collider(WukongAnimations.STAFF)
            .swingSound(EpicFightSounds.WHOOSH_ROD.get())
            .hitSound(EpicFightSounds.BLUNT_HIT.get())
            .hitParticle(EpicFightParticles.HIT_BLUNT.get())
            .canBePlacedOffhand(false)
            .newStyleCombo(Styles.TWO_HAND, WukongAnimations.AUTO1, WukongAnimations.AUTO2, WukongAnimations.AUTO3, WukongAnimations.AUTO4,
                WukongAnimations.AUTO5, WukongAnimations.DASH_ATTACK, WukongAnimations.JUMP_ATTACK)
            .innateSkill(Styles.TWO_HAND, stack -> SMASH)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.IDLE, WukongAnimations.IDLE)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.WALK, WukongAnimations.WALK)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.CHASE, WukongAnimations.DASH)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.RUN, WukongAnimations.RUN)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.JUMP, WukongAnimations.JUMP_MOTION)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.FALL, WukongAnimations.FALL));
    }
}
