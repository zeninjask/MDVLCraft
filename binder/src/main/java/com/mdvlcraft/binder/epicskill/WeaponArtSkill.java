package com.mdvlcraft.binder.epicskill;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;

/**
 * A weapon art (WeaponArts) as the weapon's innate skill: it plays the art's attack animation, spends weapon charge like
 * every innate, and sets the item's own ability off at the animation's hit. WeaponArtInnateMixin hands the skill to the
 * art's items. The icon is one of Epic Fight's own innate icons, picked to match the animation.
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = Bus.MOD)
public class WeaponArtSkill extends WeaponInnateSkill {
    private static final Map<WeaponArts.Art, Skill> SKILLS = new HashMap<>();
    private final WeaponArts.Art art;

    public WeaponArtSkill(SkillBuilder<? extends WeaponInnateSkill> builder, WeaponArts.Art art) {
        super(builder);
        this.art = art;
    }

    @SubscribeEvent
    public static void build(SkillBuildEvent event) {
        SkillBuildEvent.ModRegistryWorker worker = event.createRegistryWorker("mdvlcraft");
        for (WeaponArts.Art art : WeaponArts.ARTS) {
            SKILLS.put(art, worker.build(art.name(), builder -> new WeaponArtSkill(builder, art),
                new SkillBuilder<WeaponArtSkill>().setCategory(SkillCategories.WEAPON_INNATE).setResource(Skill.Resource.WEAPON_CHARGE)));
        }
    }

    /** The innate skill of a weapon with an art, or null. */
    @Nullable
    public static Skill of(ItemStack stack) {
        WeaponArts.Art art = WeaponArts.of(stack);
        return art == null ? null : SKILLS.get(art);
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        container.getExecutor().playAnimationSynchronized(this.art.animation().get(), 0.0F);
        super.executeOnServer(container, args);
        WeaponArts.start((ServerPlayer)container.getExecutor().getOriginal(), this.art);
    }

    @Override
    public ResourceLocation getSkillTexture() {
        return icon(this.art.animation().get().registryName().getPath());
    }

    /** One of Epic Fight's innate icons, picked by the path of the animation the skill plays. */
    public static ResourceLocation icon(String animation) {
        String icon = animation.contains("smash") || animation.contains("guillotine") ? "the_guillotine"
            : animation.contains("battojutsu") ? "battojutsu"
            : animation.contains("steel_whirlwind") ? "steel_whirlwind"
            : animation.contains("rushing_tempo") ? "rushing_tempo"
            : animation.contains("liechtenauer") || animation.contains("longsword") ? "liechtenauer"
            : animation.contains("wrathful") ? "wrathful_lighting"
            : animation.contains("spear") ? "everlasting_allegiance"
            : animation.contains("grasping") ? "grasping_spire"
            : animation.contains("relentless") ? "relentless_combo"
            : animation.contains("sharp_stab") ? "sharp_stab"
            : animation.contains("fist") ? "rushing_tempo"
            : animation.contains("axe") ? "blade_rush"
            : animation.contains("dash") ? "dancing_edge"
            : "sweeping_edge";
        return ResourceLocation.fromNamespaceAndPath("epicfight", "textures/gui/skills/weapon_innate/" + icon + ".png");
    }
}
