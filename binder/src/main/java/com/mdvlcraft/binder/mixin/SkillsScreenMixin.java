package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.MDVLBinder;
import com.mdvlcraft.binder.client.SpellPreviews;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.client.config.ClientFrameConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillConfig;
import net.puffish.skillsmod.client.config.skill.ClientSkillDefinitionConfig;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.gui.SkillsScreen;
import net.puffish.skillsmod.client.rendering.TextureBatchedRenderer;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Slate skill screen. Pufferfish's Skills borrows the vanilla advancement window and tabs; point it at the binder's
 * charcoal versions instead (same layout; drawn by tools/generate_textures.py). The vanilla advancements screen keeps
 * its own look. Node frames, lines and backgrounds are set in the skill tree data.
 * <p>
 * The white line icons are tinted by their skill's state (dim while locked, full white once unlocked), and the title
 * bar text is light so it reads on the dark title bar. The skill under the mouse is reported to SpellPreviews, so
 * holding the Ponder key over a spell skill previews the spell.
 */
@Mixin(value = SkillsScreen.class, remap = false)
public abstract class SkillsScreenMixin {
    @Unique
    private static final Vector4fc mdvlcraft$LOCKED = new Vector4f(0.40F, 0.42F, 0.46F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$AVAILABLE = new Vector4f(0.70F, 0.72F, 0.76F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$AFFORDABLE = new Vector4f(0.92F, 0.93F, 0.95F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$EXCLUDED = new Vector4f(0.26F, 0.28F, 0.32F, 1.0F);
    @Unique
    private static final int mdvlcraft$TITLE_TEXT = 0xFFE8ECF2;

    @Shadow
    @Final
    @Mutable
    private static ResourceLocation WINDOW_TEXTURE;

    @Shadow
    @Final
    @Mutable
    private static ResourceLocation TABS_TEXTURE;

    @Shadow
    private Optional<ClientCategoryData> optActiveCategoryData;

    /** State of the skill whose frame was just drawn; Pufferfish draws each skill's icon right after its frame. */
    @Unique
    @Nullable
    private Skill.State mdvlcraft$frameState;
    /** State the icon being drawn is tinted for; null (untinted) for tab icons. */
    @Unique
    @Nullable
    private Skill.State mdvlcraft$iconState;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void mdvlcraft$slateTextures(CallbackInfo ci) {
        WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "textures/gui/skills/window.png");
        TABS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MDVLBinder.MOD_ID, "textures/gui/skills/tabs.png");
    }

    @Inject(method = "drawFrame", at = @At("HEAD"))
    private void mdvlcraft$rememberState(GuiGraphics context, TextureBatchedRenderer renderer, ClientFrameConfig frame, float sizeScale,
                                         int x, int y, Skill.State state, CallbackInfo ci) {
        mdvlcraft$frameState = state;
    }

    @Inject(method = "drawIcon", at = @At("HEAD"))
    private void mdvlcraft$takeState(CallbackInfo ci) {
        mdvlcraft$iconState = mdvlcraft$frameState;
        mdvlcraft$frameState = null;
    }

    @ModifyArg(method = "drawIcon", at = @At(value = "INVOKE",
            target = "Lnet/puffish/skillsmod/client/rendering/TextureBatchedRenderer;emitTexture(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIILorg/joml/Vector4fc;)V"),
            index = 6)
    private Vector4fc mdvlcraft$tintIcon(Vector4fc colour) {
        if (mdvlcraft$iconState == null) {
            return colour;
        }
        return switch (mdvlcraft$iconState) {
            case LOCKED -> mdvlcraft$LOCKED;
            case AVAILABLE -> mdvlcraft$AVAILABLE;
            case AFFORDABLE -> mdvlcraft$AFFORDABLE;
            case UNLOCKED -> colour;
            case EXCLUDED -> mdvlcraft$EXCLUDED;
        };
    }

    /** Pufferfish asks this of the skill under the mouse when it draws the tooltip (and of skills on a click). */
    @Inject(method = "isInsideSkill", at = @At("RETURN"))
    private void mdvlcraft$reportHover(Vector2i mouse, ClientSkillConfig skill, ClientSkillDefinitionConfig definition,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            optActiveCategoryData.ifPresent(category -> SpellPreviews.hoverSkill(category.getConfig().id(), skill.id()));
        }
    }

    @ModifyConstant(method = {"drawWindow", "drawWindowWithCategory"}, constant = @Constant(intValue = -12566464))
    private int mdvlcraft$titleText(int colour) {
        return mdvlcraft$TITLE_TEXT;
    }
}
