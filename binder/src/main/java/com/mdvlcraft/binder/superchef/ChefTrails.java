package com.mdvlcraft.binder.superchef;

import java.util.Arrays;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.particle.EpicFightParticles;

/** The spatula (right hand) and skillet (left hand) swing trails; frames are at 60 per second. */
@OnlyIn(Dist.CLIENT)
final class ChefTrails {
    private ChefTrails() {
    }

    static void add(StaticAnimation animation, TrailInfo.Builder... trails) {
        animation.addProperty(ClientAnimationProperties.TRAIL_EFFECT, Arrays.stream(trails).map(TrailInfo.Builder::create).toList());
    }

    static TrailInfo.Builder spatula(float startFrame, float endFrame) {
        return trail(startFrame, endFrame, -1.25, ((HumanoidArmature)Armatures.BIPED.get()).toolR.getName(), "spatula_base");
    }

    static TrailInfo.Builder skillet(float startFrame, float endFrame) {
        return trail(startFrame, endFrame, -1.95, ((HumanoidArmature)Armatures.BIPED.get()).toolL.getName(), "skillet_base");
    }

    private static TrailInfo.Builder trail(float startFrame, float endFrame, double length, String joint, String texture) {
        return TrailInfo.builder().r(0.24313726F).g(0.24313726F).b(0.24313726F)
            .startPos(new Vec3(0.0, 0.1, -0.4)).endPos(new Vec3(0.0, -0.05, length))
            .time(startFrame / 60.0F, endFrame / 60.0F).lifetime(10).interpolations(24)
            .joint(joint).itemSkinHand(InteractionHand.MAIN_HAND)
            .texture("mdvlcraft:textures/particle/weapon_trail/" + texture + ".png")
            .type(EpicFightParticles.SWING_TRAIL.get());
    }
}
