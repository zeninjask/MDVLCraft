package com.mdvlcraft.binder.superchef;

import com.p1nero.invincible.api.skill.ComboNode;
import com.p1nero.invincible.conditions.JumpCondition;
import com.p1nero.invincible.conditions.SprintingCondition;
import com.p1nero.invincible.conditions.StackCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
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
 * Epic Fight - Super Chef in the binder: the Frying Pan & Spatula (item mdvlcraft:chef_wok, drawn as a spatula in the right
 * hand and a pan in the left by client/ChefWokRenderer) with weapon type mdvlcraft:skillet
 * (data/mdvlcraft/capabilities/weapons/chef_wok.json), its animations (ChefAnimations) and its innate skill
 * mdvlcraft:chef_combo, Invincible's key combo: key 1 swings the spatula, key 2 the pan, key 3 finishes. Its own item, as
 * Epic Delight gives Farmer's Delight's skillet a moveset of its own.
 */
public final class SuperChefSetup {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> CHEF_WOK = ITEMS.register("chef_wok",
        () -> new SwordItem(Tiers.NETHERITE, 3, -2.4F, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)));
    /** The two halves ChefWokRenderer draws in the hands; not obtainable. */
    public static final RegistryObject<Item> CHEF_WOK_PAN = ITEMS.register("chef_wok_pan", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHEF_WOK_SPATULA = ITEMS.register("chef_wok_spatula", () -> new Item(new Item.Properties().stacksTo(1)));
    public static Skill CHEF_COMBO;
    private static final float CONVERT_TIME = 0.1F;

    private SuperChefSetup() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(SuperChefSetup::onSkillBuild);
        modBus.addListener(SuperChefSetup::onWeaponPresets);
    }

    private static void onSkillBuild(SkillBuildEvent event) {
        ComboNode root = ComboNode.create();
        ComboNode a = node(ChefAnimations.A);
        ComboNode ja = node(ChefAnimations.Y_A).addCondition(new JumpCondition()).setPriority(1);
        ComboNode da = node(ChefAnimations.Y_A).addCondition(new SprintingCondition()).setPriority(2);
        ComboNode b = node(ChefAnimations.B);
        ComboNode jb = node(ChefAnimations.Y_B).addCondition(new JumpCondition()).setPriority(1);
        ComboNode db = node(ChefAnimations.Y_B).addCondition(new SprintingCondition()).setPriority(2);
        ComboNode aB = node(ChefAnimations.A_B);
        ComboNode aBA = node(ChefAnimations.A_B_A);
        ComboNode aBB = node(ChefAnimations.A_B_B);
        ComboNode aBC = ComboNode.create()
            .addConditionNode(node(ChefAnimations.A_B_C).setPriority(1))
            .addConditionNode(node(ChefAnimations.Y_C).setPriority(2).addCondition(new StackCondition(1, 2)));
        ComboNode aA = node(ChefAnimations.A_A);
        ComboNode aAA = node(ChefAnimations.A_A_A);
        ComboNode aAB = node(ChefAnimations.A_A_B);
        ComboNode aAC = node(ChefAnimations.A_A_C);
        ComboNode bB = node(ChefAnimations.B_B);
        ComboNode bBA = node(ChefAnimations.B_B_A);
        ComboNode bBB = node(ChefAnimations.B_B_B);
        ComboNode bBC = node(ChefAnimations.B_B_C);
        ComboNode bA = node(ChefAnimations.B_A);
        ComboNode bAA = node(ChefAnimations.B_A_A);
        ComboNode bAB = node(ChefAnimations.B_A_B);
        ComboNode bAC = node(ChefAnimations.B_A_C);
        ComboNode yA = node(ChefAnimations.Y_A);
        ComboNode yB = node(ChefAnimations.Y_B);

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
        // after any two-hit chain, both buttons together (Invincible's KEY_1_2) finish it
        aA.key1(aAA).key2(aAB).key1_2(aAC);
        aB.key1(aBA).key2(aBB).key1_2(aBC);
        bA.key1(bAA).key2(bAB).key1_2(bAC);
        bB.key1(bBA).key2(bBB).key1_2(bBC);
        // after any three-hit chain, either key leaps into Whirling Blade
        for (ComboNode third : new ComboNode[] {aAA, aAB, aBA, aBB, bAA, bAB, bBA, bBB}) {
            third.key1(yA).key2(yB);
        }
        CHEF_COMBO = event.createRegistryWorker("mdvlcraft").build("chef_combo", ChefComboAttack::new,
            ChefComboAttack.createComboBasicAttack().setCombo(root).setShouldDrawGui(true));
    }

    /**
     * A combo step. Invincible plays each step with its node's convert time, 0 unless set, and a 0-length link animation
     * leaves the server's animator on NaN time: the step then only plays on the client and never hits.
     */
    private static ComboNode node(AnimationManager.AnimationAccessor<? extends StaticAnimation> animation) {
        return ComboNode.createNode(animation).setConvertTime(CONVERT_TIME);
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
            // Invincible runs this weapon's combos. Epic Fight's default counter handler reads the last two entries of the
            // combo list above (it has one), threw on every attack's begin and so stopped every attack from playing
            .comboCounterHandler((capability, causal, patch, next, counter) -> 0)
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
