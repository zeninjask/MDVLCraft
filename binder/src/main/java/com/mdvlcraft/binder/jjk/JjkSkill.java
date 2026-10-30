package com.mdvlcraft.binder.jjk;

import com.mdvlcraft.binder.epicskill.WeaponArtSkill;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;

/**
 * The Jujutsu Kaisen weapons' innate skills (Cursed Fate and the Projection Sorcery add-on). Each plays one of Epic
 * Fight's own attack animations and spends weapon charge (skill_parameters/jjk_*.json); JjkArts does the rest: what the
 * animation's own hits do differently, and what happens at its last hit. WeaponArtInnateMixin hands the skill out.
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = Bus.MOD)
public class JjkSkill extends WeaponInnateSkill {
    public enum Kind {
        SPLINTERED_SOUL("jjk_splintered_soul", () -> Animations.RUSHING_TEMPO1, "cursedfate:split_soul_katana"),
        THROUGH_INFINITY("jjk_through_infinity", () -> Animations.RUSHING_TEMPO1, "cursedfate:inverted_spearof_heaven"),
        STORED_IMPACT("jjk_stored_impact", () -> Animations.THE_GUILLOTINE, "cursedfate:dragon_bone"),
        HEMORRHAGE("jjk_hemorrhage", () -> Animations.SHARP_STAB, "cursedfate:slaughter_demon"),
        THUNDERCLAP("jjk_thunderclap", () -> Animations.WRATHFUL_LIGHTING, "cursedfate:kamutoke"),
        HEIAN_SKY_DANCE("jjk_heian_sky_dance", () -> Animations.DANCING_EDGE, "cursedfate:hiten"),
        INTO_THE_ZONE("jjk_into_the_zone", () -> Animations.LONGSWORD_DASH, "cursedfate:jet_black_blade"),
        FULL_MOON_SWEEP("jjk_full_moon_sweep", () -> Animations.STEEL_WHIRLWIND, "cursedfate:naginata"),
        RATIO("jjk_ratio", () -> Animations.SWEEPING_EDGE, "cursedfate:nanamis_cleaver"),
        BIRD_STRIKE("jjk_bird_strike", () -> Animations.AXE_AUTO2, "cursedfate:crow_sorcerer_axe"),
        RIKAS_ARM("jjk_rikas_arm", () -> Animations.BATTOJUTSU, "cursedfate:mutual_love_katana_weapon"),
        JACOBS_LADDER("jjk_jacobs_ladder", () -> Animations.WRATHFUL_LIGHTING, "cursedfate:mutual_love_katana_jacobs_ladder_weapon"),
        FPS_24("jjk_24_fps", () -> Animations.RUSHING_TEMPO1, "naoyaaddon:tanto"),
        // JujutsuCraft weapons (orca/OrcaWeapons)
        CLOUD_CRUSHER("jjk_cloud_crusher", () -> reascer.wom.gameasset.WOMAnimations.TORMENT_BERSERK_AIRSLAM, "mdvlcraft:playful_cloud"),
        DISPEL_FIELD("jjk_dispel_field", () -> Animations.STEEL_WHIRLWIND, "mdvlcraft:black_rope"),
        POSITIVE_FLAME("jjk_positive_flame", () -> Animations.SWEEPING_EDGE, "mdvlcraft:sword_of_extermination"),
        SIMPLE_DOMAIN("jjk_simple_domain", null, "mdvlcraft:sword_miwa_kasumi"),
        EVENING_MOON("jjk_evening_moon", () -> Animations.BATTOJUTSU, "mdvlcraft:sword_kusakabe"),
        PILLAR("jjk_pillar", () -> com.mdvlcraft.binder.wukong.WukongAnimations.SMASHES.get(1), "mdvlcraft:nyoi_staff"),
        INKED("jjk_inked", () -> Animations.TRIDENT_AUTO1, "mdvlcraft:g_staff"),
        TSUKKOMI("jjk_tsukkomi", () -> Animations.AXE_AUTO2, "mdvlcraft:harisen"),
        BLAZING_DRAW("jjk_blazing_draw", () -> Animations.BATTOJUTSU, "mdvlcraft:blazing_courage"),
        BLIGHT("jjk_blight", () -> Animations.STEEL_WHIRLWIND, "mdvlcraft:fester_life_blade");

        final String name;
        final Supplier<AnimationAccessor<? extends AttackAnimation>> animation;
        final List<ResourceLocation> items;

        Kind(String name, Supplier<AnimationAccessor<? extends AttackAnimation>> animation, String... items) {
            this.name = name;
            this.animation = animation;
            this.items = java.util.Arrays.stream(items).map(ResourceLocation::parse).toList();
        }

        /** Ticks from the start of the animation to its last hit (1 for a skill with no opening animation). */
        int contactTicks() {
            if (this.animation == null) {
                return 1;
            }
            AttackAnimation animation = this.animation.get().get();
            AttackAnimation.Phase last = animation.phases[animation.phases.length - 1];
            return Math.max(1, Math.round(last.contact * 20.0F));
        }
    }

    private static final Map<ResourceLocation, Kind> BY_ITEM = new HashMap<>();
    private static final Map<Kind, Skill> SKILLS = new HashMap<>();
    private final Kind kind;

    static {
        for (Kind kind : Kind.values()) {
            kind.items.forEach(item -> BY_ITEM.put(item, kind));
        }
    }

    public JjkSkill(SkillBuilder<? extends WeaponInnateSkill> builder, Kind kind) {
        super(builder);
        this.kind = kind;
    }

    @SubscribeEvent
    public static void build(SkillBuildEvent event) {
        SkillBuildEvent.ModRegistryWorker worker = event.createRegistryWorker("mdvlcraft");
        for (Kind kind : Kind.values()) {
            SKILLS.put(kind, worker.build(kind.name, builder -> new JjkSkill(builder, kind),
                new SkillBuilder<JjkSkill>().setCategory(SkillCategories.WEAPON_INNATE).setResource(Skill.Resource.WEAPON_CHARGE)));
        }
    }

    /** The innate skill of a JJK weapon, or null. */
    @Nullable
    public static Skill of(ItemStack stack) {
        Kind kind = stack.isEmpty() ? null : BY_ITEM.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        return kind == null ? null : SKILLS.get(kind);
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        if (this.kind.animation != null) {
            container.getExecutor().playAnimationSynchronized(this.kind.animation.get(), 0.0F);
        }
        super.executeOnServer(container, args);
        JjkArts.start((ServerPlayer)container.getExecutor().getOriginal(), this.kind);
    }

    @Override
    public ResourceLocation getSkillTexture() {
        return WeaponArtSkill.icon(this.kind.animation == null ? "battojutsu" : this.kind.animation.get().registryName().getPath());
    }
}
