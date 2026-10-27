package com.mdvlcraft.binder.superchef;

import com.p1nero.invincible.api.skill.ComboNode;
import com.p1nero.invincible.conditions.JumpCondition;
import com.p1nero.invincible.conditions.SprintingCondition;
import com.p1nero.invincible.conditions.StackCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

/**
 * Epic Fight - Super Chef in the binder: weapon type mdvlcraft:skillet (given to Farmer's Delight's skillet by
 * kubejs/data/farmersdelight/capabilities/weapons/skillet.json), its animations (ChefAnimations) and its innate skill
 * mdvlcraft:chef_combo, Invincible's key combo: key 1 swings the spatula, key 2 the skillet, key 3 finishes.
 */
public final class SuperChefSetup {
    public static Skill CHEF_COMBO;

    private SuperChefSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ChefAnimations::onRegistry);
        modBus.addListener(SuperChefSetup::onSkillBuild);
        modBus.addListener(SuperChefSetup::onWeaponPresets);
    }

    private static void onSkillBuild(SkillBuildEvent event) {
        ComboNode root = ComboNode.create();
        ComboNode a = ComboNode.createNode(ChefAnimations.A);
        ComboNode ja = ComboNode.createNode(ChefAnimations.Y_A).addCondition(new JumpCondition()).setPriority(1);
        ComboNode da = ComboNode.createNode(ChefAnimations.Y_A).addCondition(new SprintingCondition()).setPriority(2);
        ComboNode b = ComboNode.createNode(ChefAnimations.B);
        ComboNode jb = ComboNode.createNode(ChefAnimations.Y_B).addCondition(new JumpCondition()).setPriority(1);
        ComboNode db = ComboNode.createNode(ChefAnimations.Y_B).addCondition(new SprintingCondition()).setPriority(2);
        ComboNode aB = ComboNode.createNode(ChefAnimations.A_B);
        ComboNode aBA = ComboNode.createNode(ChefAnimations.A_B_A);
        ComboNode aBB = ComboNode.createNode(ChefAnimations.A_B_B);
        ComboNode aBC = ComboNode.create()
            .addConditionNode(ComboNode.createNode(ChefAnimations.A_B_C).setPriority(1))
            .addConditionNode(ComboNode.createNode(ChefAnimations.Y_C).setPriority(2).addCondition(new StackCondition(1, 2)));
        ComboNode aA = ComboNode.createNode(ChefAnimations.A_A);
        ComboNode aAA = ComboNode.createNode(ChefAnimations.A_A_A);
        ComboNode aAB = ComboNode.createNode(ChefAnimations.A_A_B);
        ComboNode aAC = ComboNode.createNode(ChefAnimations.A_A_C);
        ComboNode bB = ComboNode.createNode(ChefAnimations.B_B);
        ComboNode bBA = ComboNode.createNode(ChefAnimations.B_B_A);
        ComboNode bBB = ComboNode.createNode(ChefAnimations.B_B_B);
        ComboNode bBC = ComboNode.createNode(ChefAnimations.B_B_C);
        ComboNode bA = ComboNode.createNode(ChefAnimations.B_A);
        ComboNode bAA = ComboNode.createNode(ChefAnimations.B_A_A);
        ComboNode bAB = ComboNode.createNode(ChefAnimations.B_A_B);
        ComboNode bAC = ComboNode.createNode(ChefAnimations.B_A_C);
        ComboNode yA = ComboNode.createNode(ChefAnimations.Y_A);
        ComboNode yB = ComboNode.createNode(ChefAnimations.Y_B);

        root.key1(ComboNode.create().addConditionNode(a).addConditionNode(ja).addConditionNode(da));
        root.key2(ComboNode.create().addConditionNode(b).addConditionNode(jb).addConditionNode(db));
        a.key1(ComboNode.create().addConditionNode(aA).addConditionNode(ja).addConditionNode(da)).key2(aB);
        b.key1(bA).key2(ComboNode.create().addConditionNode(bB).addConditionNode(jb).addConditionNode(db));
        for (ComboNode leap : new ComboNode[] {ja, da, yA}) {
            leap.key1(aA).key2(aB);
        }
        for (ComboNode leap : new ComboNode[] {jb, db, yB}) {
            leap.key1(bA).key2(bB);
        }
        aA.key1(aAA).key2(aAB).key3(aAC);
        aB.key1(aBA).key2(aBB).key3(aBC);
        bA.key1(bAA).key2(bAB).key3(bAC);
        bB.key1(bBA).key2(bBB).key3(bBC);
        // after any three-hit chain, either key leaps into Whirling Blade
        for (ComboNode third : new ComboNode[] {aAA, aAB, aBA, aBB, bAA, bAB, bBA, bBB}) {
            third.key1(yA).key2(yB);
        }
        CHEF_COMBO = event.createRegistryWorker("mdvlcraft").build("chef_combo", ChefComboAttack::new,
            ChefComboAttack.createComboBasicAttack().setCombo(root).setShouldDrawGui(true));
    }

    private static void onWeaponPresets(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry().put(ResourceLocation.fromNamespaceAndPath("mdvlcraft", "skillet"), item -> WeaponCapability.builder()
            .category(WeaponCategories.LONGSWORD)
            .styleProvider(patch -> Styles.TWO_HAND)
            .collider(ChefAnimations.SKILLET)
            .swingSound(EpicFightSounds.WHOOSH_BIG.get())
            .hitSound(EpicFightSounds.BLADE_HIT.get())
            .hitParticle(EpicFightParticles.HIT_BLADE.get())
            .canBePlacedOffhand(false)
            .newStyleCombo(Styles.TWO_HAND, Animations.LONGSWORD_DASH)
            .innateSkill(Styles.TWO_HAND, stack -> CHEF_COMBO)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.IDLE, ChefAnimations.IDLE)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.WALK, ChefAnimations.WALK)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.RUN, ChefAnimations.RUN)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.CHASE, ChefAnimations.RUN)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.KNEEL, ChefAnimations.KNEE)
            .livingMotionModifier(Styles.TWO_HAND, LivingMotions.SNEAK, ChefAnimations.SNEAK)
            .comboCancel(style -> false));
    }
}
