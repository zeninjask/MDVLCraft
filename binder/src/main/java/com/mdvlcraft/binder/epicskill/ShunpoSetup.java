package com.mdvlcraft.binder.epicskill;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Registers Shunpo (ShunpoSkill) and keeps its owner from raising a shield. */
public final class ShunpoSetup {
    public static Skill SHUNPO;

    private ShunpoSetup() {
    }

    @EventBusSubscriber(modid = "mdvlcraft", bus = Bus.MOD)
    public static final class Build {
        private Build() {
        }

        @SubscribeEvent
        public static void build(SkillBuildEvent event) {
            SHUNPO = event.createRegistryWorker("mdvlcraft").build("shunpo", ShunpoSkill::new, GuardSkill.createGuardBuilder());
        }
    }

    @EventBusSubscriber(modid = "mdvlcraft")
    public static final class NoShield {
        private NoShield() {
        }

        @SubscribeEvent
        public static void onUse(LivingEntityUseItemEvent.Start event) {
            if (event.getEntity() instanceof Player player && event.getItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
                PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
                SkillContainer guard = patch == null ? null : patch.getSkill(SkillSlots.GUARD);
                if (guard != null && guard.getSkill() == SHUNPO) {
                    event.setCanceled(true);
                }
            }
        }
    }
}
