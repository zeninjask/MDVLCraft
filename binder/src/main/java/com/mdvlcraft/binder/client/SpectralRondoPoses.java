package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.mixin.PoseStackAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;

/**
 * Keeps Spectral Rondo's pose push and pop paired (mixin/somakespells/SpectralRondoPlayerRenderHandlerMixin). Somakespells
 * pushes on RenderPlayerEvent.Pre and pops on Post behind one on/off flag. When a Pre is cancelled after its push (ModernFix
 * then pops the push and no Post comes), a later Post popped a pose it never pushed and the frame ended with "Pose stack not
 * empty". Each push is remembered by player and pose, and Post pops only its own push while that pose is still on top.
 */
public final class SpectralRondoPoses {
    private static final Deque<Push> PUSHED = new ArrayDeque<>();

    private SpectralRondoPoses() {
    }

    public static void pushed(RenderPlayerEvent.Pre event) {
        PUSHED.push(new Push(event.getEntity(), event.getPoseStack().last()));
    }

    public static void popOwn(RenderPlayerEvent.Post event) {
        PoseStack stack = event.getPoseStack();
        Deque<PoseStack.Pose> poses = ((PoseStackAccessor)stack).mdvlcraft$poses();
        // A push whose pose is gone was popped by someone else; it is never popped again.
        PUSHED.removeIf(push -> !poses.contains(push.pose()));
        Push top = PUSHED.peek();
        if (top != null && top.player() == event.getEntity() && top.pose() == stack.last()) {
            PUSHED.pop();
            stack.popPose();
        }
    }

    private record Push(Player player, PoseStack.Pose pose) {
    }
}
