package com.mdvlcraft.binder.wukong;

import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;

/** The Monkey King identity (mdvlcraft:wukong): does nothing itself; equipped, it unlocks the Ruyi Jingu Bang's moveset (WukongGate). */
public class MonkeyKingSkill extends Skill {
    public MonkeyKingSkill(SkillBuilder<? extends Skill> builder) {
        super(builder);
    }
}
