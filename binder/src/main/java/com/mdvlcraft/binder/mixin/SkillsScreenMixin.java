package com.mdvlcraft.binder.mixin;

import com.mdvlcraft.binder.client.DqGui;
import com.mdvlcraft.binder.client.SpellPreviews;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.Skill.State;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SkillsScreen.class, remap = false)
public abstract class SkillsScreenMixin {
    @Unique
    private static final Vector4fc mdvlcraft$LOCKED = new Vector4f(0.36F, 0.42F, 0.62F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$AVAILABLE = new Vector4f(0.66F, 0.72F, 0.88F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$AFFORDABLE = new Vector4f(0.94F, 0.95F, 1.0F, 1.0F);
    @Unique
    private static final Vector4fc mdvlcraft$EXCLUDED = new Vector4f(0.24F, 0.26F, 0.4F, 1.0F);
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
    @Unique
    @Nullable
    private State mdvlcraft$frameState;
    @Unique
    @Nullable
    private State mdvlcraft$iconState;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void mdvlcraft$slateTextures(CallbackInfo ci) {
        WINDOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/window.png");
        TABS_TEXTURE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/tabs.png");
    }

    @Inject(method = "drawFrame", at = @At("HEAD"))
    private void mdvlcraft$rememberState(
        GuiGraphics context, TextureBatchedRenderer renderer, ClientFrameConfig frame, float sizeScale, int x, int y, State state, CallbackInfo ci
    ) {
        this.mdvlcraft$frameState = state;
    }

    @Inject(method = "drawIcon", at = @At("HEAD"))
    private void mdvlcraft$takeState(CallbackInfo ci) {
        this.mdvlcraft$iconState = this.mdvlcraft$frameState;
        this.mdvlcraft$frameState = null;
    }

    @ModifyArg(
        method = "drawIcon",
        at = @At(
            value = "INVOKE",
            target = "Lnet/puffish/skillsmod/client/rendering/TextureBatchedRenderer;emitTexture(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIILorg/joml/Vector4fc;)V"
        ),
        index = 6
    )
    private Vector4fc mdvlcraft$tintIcon(Vector4fc colour) {
        if (this.mdvlcraft$iconState == null) {
            return colour;
        }

        return switch (this.mdvlcraft$iconState) {
            case LOCKED -> mdvlcraft$LOCKED;
            case AVAILABLE -> mdvlcraft$AVAILABLE;
            case AFFORDABLE -> mdvlcraft$AFFORDABLE;
            case UNLOCKED -> colour;
            case EXCLUDED -> mdvlcraft$EXCLUDED;
            default -> throw new IncompatibleClassChangeError();
        };
    }

    @Inject(method = "isInsideSkill", at = @At("RETURN"))
    private void mdvlcraft$reportHover(Vector2i mouse, ClientSkillConfig skill, ClientSkillDefinitionConfig definition, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            this.optActiveCategoryData.ifPresent(category -> SpellPreviews.hoverSkill(category.getConfig().id(), skill.id()));
        }
    }

    @ModifyConstant(method = {"drawWindow", "drawWindowWithCategory"}, constant = @Constant(intValue = -12566464))
    private int mdvlcraft$titleText(int colour) {
        return DqGui.TEXT;
    }

    /** A spell node's tooltip ends with how to preview the spell (client/SpellPreviews). */
    @Redirect(
        method = "lambda$drawContentWithCategory$21",
        at = @At(
            value = "INVOKE",
            target = "Lnet/puffish/skillsmod/client/config/skill/ClientSkillDefinitionConfig;description()Lnet/minecraft/network/chat/Component;"
        ),
        require = 0
    )
    private Component mdvlcraft$descriptionWithPreview(ClientSkillDefinitionConfig definition) {
        Component description = definition.description();
        boolean spell = this.optActiveCategoryData.map(category -> SpellPreviews.isSpellDefinition(category.getConfig().id(), definition.id())).orElse(false);
        Component shown = spell
            ? Component.empty().append(description).append("\n").append(SpellPreviews.prompt().copy().withStyle(ChatFormatting.DARK_GRAY))
            : description;
        // nodes with more to say show it while shift is held (Pufferfish's extra description): say so
        if (!net.minecraft.client.gui.screens.Screen.hasShiftDown() && !definition.extraDescription().getString().isBlank()) {
            shown = Component.empty().append(shown).append("\n").append(Component.translatable("screen.mdvlcraft.skills.shift_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        return shown;
    }

    @Redirect(
        method = "lambda$drawContentWithCategory$21",
        at = @At(
            value = "INVOKE",
            target = "Lnet/puffish/skillsmod/client/config/skill/ClientSkillDefinitionConfig;title()Lnet/minecraft/network/chat/Component;"
        ),
        require = 0
    )
    private Component mdvlcraft$titleWithCost(ClientSkillDefinitionConfig definition) {
        int cost = definition.cost();
        String key = cost == 0 ? "screen.mdvlcraft.skills.cost_free" : (cost == 1 ? "screen.mdvlcraft.skills.cost_one" : "screen.mdvlcraft.skills.cost");
        return Component.empty().append(definition.title()).append(Component.translatable(key, cost).withStyle(ChatFormatting.GOLD));
    }

    /** Exclusive links (the three specialisations of a base, one may be taken) are always drawn, not only on hover. */
    @Redirect(
        method = "drawContentWithCategory",
        at = @At(value = "INVOKE", target = "Lnet/puffish/skillsmod/client/config/ClientCategoryConfig;normalConnections()Ljava/util/Collection;"),
        require = 0
    )
    private java.util.Collection<net.puffish.skillsmod.client.config.skill.ClientSkillConnectionConfig> mdvlcraft$withExclusive(
            net.puffish.skillsmod.client.config.ClientCategoryConfig category) {
        java.util.List<net.puffish.skillsmod.client.config.skill.ClientSkillConnectionConfig> all = new java.util.ArrayList<>(category.normalConnections());
        all.addAll(category.exclusiveConnections());
        return all;
    }

    /** The node's id under its tooltip (Pufferfish shows it with advanced tooltips, F3+H) is never shown. */
    @Redirect(
        method = "lambda$drawContentWithCategory$21",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;advancedItemTooltips:Z", remap = true),
        require = 0
    )
    private boolean mdvlcraft$noSkillId(net.minecraft.client.Options options) {
        return false;
    }
}
