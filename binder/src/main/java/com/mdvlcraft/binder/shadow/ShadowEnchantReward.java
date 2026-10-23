package com.mdvlcraft.binder.shadow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

/**
 * mdvlcraft:shadow_enchant {enchantment, level}: the samurai's Shadow Arsenal nodes. A player's shadow weapons carry the
 * highest level of each enchantment among their unlocked nodes (ShadowArsenal applies them when one is summoned).
 */
public final class ShadowEnchantReward implements Reward {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "shadow_enchant");
    private static final Map<UUID, Set<ShadowEnchantReward>> ACTIVE = new HashMap<>();
    private final ResourceLocation enchantment;
    private final int level;

    private ShadowEnchantReward(ResourceLocation enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, ShadowEnchantReward::create);
    }

    private static Result<ShadowEnchantReward, Problem> create(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject).andThen(ShadowEnchantReward::parse);
    }

    private static Result<ShadowEnchantReward, Problem> parse(JsonObject data) {
        List<Problem> problems = new ArrayList<>();
        Optional<String> id = data.getString("enchantment").ifFailure(problems::add).getSuccess();
        Optional<Integer> level = data.getInt("level").ifFailure(problems::add).getSuccess();
        if (!problems.isEmpty()) {
            return Result.failure(Problem.combine(problems));
        }
        ResourceLocation enchantment = ResourceLocation.tryParse(id.orElseThrow());
        return enchantment == null || level.orElseThrow() < 1 ? Result.failure(Problem.message("Bad shadow enchantment " + id.orElseThrow()))
            : Result.success(new ShadowEnchantReward(enchantment, level.orElseThrow()));
    }

    /** The enchantments (highest unlocked level of each) the player's shadow weapons get; unknown enchantments are skipped. */
    public static Map<Enchantment, Integer> of(ServerPlayer player) {
        Map<Enchantment, Integer> levels = new HashMap<>();
        for (ShadowEnchantReward reward : ACTIVE.getOrDefault(player.getUUID(), Set.of())) {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(reward.enchantment);
            if (enchantment != null) {
                levels.merge(enchantment, reward.level, Math::max);
            }
        }
        return levels;
    }

    @Override
    public void update(RewardUpdateContext context) {
        Set<ShadowEnchantReward> rewards = ACTIVE.computeIfAbsent(context.getPlayer().getUUID(), uuid -> new HashSet<>());
        if (context.getCount() > 0) {
            rewards.add(this);
        } else {
            rewards.remove(this);
        }
        ShadowArsenal.refresh(context.getPlayer());   // a shadow weapon out now gets the new enchantments
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        ACTIVE.values().forEach(rewards -> rewards.remove(this));
    }
}
