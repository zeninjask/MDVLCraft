package com.mdvlcraft.binder.mixin.parties;

import app.sedu.mc.parties.common.group.ClientGroupManager;
import app.sedu.mc.parties.common.group.GroupType;
import app.sedu.mc.parties.common.group.TrackedGroup;
import com.mdvlcraft.binder.client.PartyLeader;
import com.mdvlcraft.binder.client.PartyRowButtons;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Party controls on the Social Interactions screen's rows (beside Parties' own invite button): the party leader's name
 * has a gold crown; the leader sees a red X on each other member's row to remove them (/party kick); a player in a party
 * sees Leave on their own row (/party leave). Clicks arrive through SocialEntryClickMixin.
 */
@Mixin(PlayerEntry.class)
public abstract class SocialPlayerEntryPartyMixin implements PartyRowButtons {
    @Shadow
    @Final
    private UUID id;
    @Shadow
    @Final
    private String playerName;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private Button mdvlcraft$kick;
    @Unique
    private Button mdvlcraft$leave;

    @Shadow
    private Component getStatusComponent() {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mdvlcraft$partyButtons(CallbackInfo ci) {
        this.mdvlcraft$kick = Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED),
                button -> this.minecraft.player.connection.sendCommand("party kick " + this.playerName))
            .size(20, 20).tooltip(Tooltip.create(Component.translatable("gui.mdvlcraft.party.kick"))).build();
        this.mdvlcraft$leave = Button.builder(Component.translatable("gui.mdvlcraft.party.leave"),
                button -> this.minecraft.player.connection.sendCommand("party leave"))
            .size(44, 20).build();
        this.mdvlcraft$kick.visible = false;
        this.mdvlcraft$leave.visible = false;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void mdvlcraft$renderParty(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
            boolean hovered, float partialTick, CallbackInfo ci) {
        TrackedGroup party = ClientGroupManager.getGroup(GroupType.PARTY);
        UUID self = this.minecraft.player.getUUID();
        boolean member = party != null && party.contains(this.id);
        this.mdvlcraft$kick.visible = member && !this.id.equals(self) && self.equals(party.getLeader());
        this.mdvlcraft$leave.visible = member && this.id.equals(self);
        if (member && this.id.equals(party.getLeader())) {
            int nameY = this.getStatusComponent() == CommonComponents.EMPTY ? top + (height - 9) / 2 : top + (height - 18) / 2;
            graphics.drawString(this.minecraft.font, PartyLeader.MARK, left + 32 + this.minecraft.font.width(this.playerName) + 3, nameY,
                PartyLeader.COLOUR, false);
        }
        int buttonY = top + (height - 20) / 2;
        if (this.mdvlcraft$kick.visible) {
            this.mdvlcraft$kick.setPosition(left + width - 96, buttonY);
            this.mdvlcraft$kick.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.mdvlcraft$leave.visible) {
            this.mdvlcraft$leave.setPosition(left + width - 4 - 44, buttonY);
            this.mdvlcraft$leave.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mdvlcraft$clickParty(double mouseX, double mouseY, int button) {
        return this.mdvlcraft$kick.visible && this.mdvlcraft$kick.mouseClicked(mouseX, mouseY, button)
            || this.mdvlcraft$leave.visible && this.mdvlcraft$leave.mouseClicked(mouseX, mouseY, button);
    }
}
