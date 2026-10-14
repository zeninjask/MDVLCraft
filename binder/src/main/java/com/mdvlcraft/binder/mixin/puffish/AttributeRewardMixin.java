package com.mdvlcraft.binder.mixin.puffish;

import com.mdvlcraft.binder.skills.SameClassHalving;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.reward.builtin.AttributeReward;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pufferfish's attribute reward, scaled by SameClassHalving: each unlocked copy adds value × scale, and a copy whose
 * modifier holds a different amount (the player's archetypes changed) is replaced.
 */
@Mixin(value = AttributeReward.class, remap = false)
public abstract class AttributeRewardMixin {
    @Shadow @Final private List<UUID> uuids;
    @Shadow @Final private Attribute attribute;
    @Shadow @Final private float value;
    @Shadow @Final private AttributeModifier.Operation operation;

    @Shadow
    private void createMissingUUIDs(int count) {
        throw new AssertionError();
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, require = 1)
    private void mdvlcraft$scaledUpdate(RewardUpdateContext context, CallbackInfo ci) {
        ci.cancel();
        ServerPlayer player = context.getPlayer();
        int count = context.getCount();
        AttributeInstance instance = Objects.requireNonNull(player.getAttribute(attribute));
        double amount = value * SameClassHalving.scale(player, (Reward)(Object)this);
        createMissingUUIDs(count);
        for (int i = 0; i < uuids.size(); i++) {
            UUID uuid = uuids.get(i);
            AttributeModifier current = instance.getModifier(uuid);
            boolean wanted = i < count;
            if (current != null && (!wanted || current.getAmount() != amount)) {
                instance.removeModifier(uuid);
                current = null;
            }
            if (wanted && current == null) {
                instance.addTransientModifier(new AttributeModifier(uuid, "", amount, operation));
            }
        }
    }
}
