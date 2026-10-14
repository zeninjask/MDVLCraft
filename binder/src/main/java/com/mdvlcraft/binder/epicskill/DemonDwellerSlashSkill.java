package com.mdvlcraft.binder.epicskill;

import com.mdvlcraft.binder.content.DemonSwordArts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;

/**
 * The Demon Dweller's innate skill: a Sweeping Edge swing, then the sword's Anti-Magic Slash leaves the blade. It runs on
 * weapon charge built by landing hits, like every innate (skill_parameters/anti_magic_slash.json sets the cost).
 * DemonDwellerInnateMixin gives it to the Demon Dweller in place of the longsword's own innate.
 */
@EventBusSubscriber(modid = "mdvlcraft", bus = Bus.MOD)
public class DemonDwellerSlashSkill extends WeaponInnateSkill {
    /** Ticks from the start of the swing to the slash, around the swing's hit. */
    private static final int SLASH_DELAY_TICKS = 8;
    public static Skill ANTI_MAGIC_SLASH;

    public DemonDwellerSlashSkill(SkillBuilder<? extends WeaponInnateSkill> builder) {
        super(builder);
    }

    @SubscribeEvent
    public static void build(SkillBuildEvent event) {
        ANTI_MAGIC_SLASH = event.createRegistryWorker("mdvlcraft").build("anti_magic_slash", DemonDwellerSlashSkill::new,
            new SkillBuilder<DemonDwellerSlashSkill>().setCategory(SkillCategories.WEAPON_INNATE).setResource(Skill.Resource.WEAPON_CHARGE));
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        container.getExecutor().playAnimationSynchronized(Animations.SWEEPING_EDGE, 0.0F);
        super.executeOnServer(container, args);
        DemonSwordArts.slashAfter((ServerPlayer)container.getExecutor().getOriginal(), SLASH_DELAY_TICKS);
    }
}
