package com.mdvlcraft.binder.epicskill;

import com.mdvlcraft.binder.MDVLBinder;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;

/**
 * Pufferfish's Skills reward {@code mdvlcraft:epicfight_skill}: {"skill": "epicfight:berserker"}. While the skill tree
 * node holding it is unlocked, the player has learned that Epic Fight skill and can equip it in the abilities screen.
 */
public final class EpicSkillReward implements Reward {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "epicfight_skill");

    final Skill skill;

    private EpicSkillReward(Skill skill) {
        this.skill = skill;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, EpicSkillReward::create);
    }

    private static Result<EpicSkillReward, Problem> create(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject).andThen(EpicSkillReward::parse);
    }

    private static Result<EpicSkillReward, Problem> parse(JsonObject data) {
        Result<String, Problem> id = data.getString("skill");
        if (id.getFailure().isPresent()) {
            return Result.failure(id.getFailure().orElseThrow());
        }
        String name = id.getSuccess().orElseThrow();
        Skill skill = SkillManager.getSkill(name);
        if (skill == null) {
            return Result.failure(Problem.message("Unknown Epic Fight skill `" + name + "`"));
        }
        if (!skill.getCategory().learnable()) {
            return Result.failure(Problem.message("Epic Fight skill `" + name + "` cannot be learned"));
        }
        EpicSkillGrants.register(skill);
        return Result.success(new EpicSkillReward(skill));
    }

    @Override
    public void update(RewardUpdateContext context) {
        EpicSkillGrants.set(context.getPlayer(), this, context.getCount() > 0);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        EpicSkillGrants.dispose(this);
    }
}
