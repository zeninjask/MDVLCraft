package com.mdvlcraft.binder.ability;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pufferfish's Skills reward {@code mdvlcraft:ability}: {"ability": "irons_spellbooks:shield", "level": 2}.
 * While the skill holding it is unlocked, the player can put the ability on their wheel at that level.
 */
public final class AbilityReward implements Reward {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "ability");

    final Ability ability;
    final int level;

    private AbilityReward(Ability ability, int level) {
        this.ability = ability;
        this.level = level;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, AbilityReward::create);
    }

    private static Result<AbilityReward, Problem> create(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject).andThen(AbilityReward::parse);
    }

    private static Result<AbilityReward, Problem> parse(JsonObject data) {
        List<Problem> problems = new ArrayList<>();
        Optional<String> id = data.getString("ability").ifFailure(problems::add).getSuccess();
        Optional<Integer> level = data.getInt("level").ifFailure(problems::add).getSuccess();
        if (!problems.isEmpty()) {
            return Result.failure(Problem.combine(problems));
        }
        ResourceLocation abilityId = ResourceLocation.tryParse(id.orElseThrow());
        Optional<Ability> ability = abilityId == null ? Optional.empty() : Ability.byId(abilityId);
        if (ability.isEmpty()) {
            return Result.failure(Problem.message("Unknown ability `" + id.orElseThrow() + "`"));
        }
        int abilityLevel = level.orElseThrow();
        // Spell level caps come from Iron's spell config, which is not built yet while data packs load;
        // tools/generate_skill_trees.py checks them against Iron's defaults instead.
        int maxLevel = ability.get() instanceof Technique ? 1 : Integer.MAX_VALUE;
        if (abilityLevel < 1 || abilityLevel > maxLevel) {
            return Result.failure(Problem.message("Level " + abilityLevel + " of `" + abilityId + "` is not allowed"));
        }
        return Result.success(new AbilityReward(ability.get(), abilityLevel));
    }

    @Override
    public void update(RewardUpdateContext context) {
        AbilityGrants.set(context.getPlayer(), this, context.getCount() > 0);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        AbilityGrants.dispose(this);
    }
}
