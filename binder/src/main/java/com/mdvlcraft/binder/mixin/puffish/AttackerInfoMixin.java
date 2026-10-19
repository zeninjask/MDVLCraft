package com.mdvlcraft.binder.mixin.puffish;

import com.mdvlcraft.binder.doppelganger.DoppelgangerEntity;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.puffish.skillsmod.util.AttackerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A summon's hits and kills count as its summoner's own for skill experience. Pufferfish only credits a player, or a tamed
 * animal's owner (which the trees' kill sources exclude); Iron's summons and the Modified abilities' summons (both in
 * Iron's SummonManager) and the doppelganger credit their owner instead.
 */
@Mixin(value = AttackerInfo.class, remap = false)
public abstract class AttackerInfoMixin {
    @Inject(method = "detect", at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$creditSummoner(Entity entity, Consumer<AttackerInfo> consumer, CallbackInfo ci) {
        if (entity == null || entity instanceof ServerPlayer || entity.level().isClientSide()) {
            return;
        }
        Entity owner = entity instanceof DoppelgangerEntity doppelganger ? doppelganger.getOwner() : SummonManager.getOwner(entity);
        if (owner instanceof ServerPlayer player) {
            consumer.accept(new AttackerInfo(player, false));
            ci.cancel();
        }
    }
}
