package com.mdvlcraft.binder.mixin.parties;

import app.sedu.mc.parties.common.group.ClientGroupManager;
import app.sedu.mc.parties.common.group.GroupType;
import app.sedu.mc.parties.common.group.TrackedGroup;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.social.SocialInteractionsPlayerList;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A fourth tab on the Social Interactions screen, Party: only the online members of your party. The vanilla tabs are
 * rebuilt a quarter wide each to make room, and picking any of them leaves the Party tab.
 */
@Mixin(SocialInteractionsScreen.class)
public abstract class SocialScreenPartyTabMixin extends Screen {
    @Shadow
    @Final
    private static Component TAB_ALL;
    @Shadow
    @Final
    private static Component TAB_HIDDEN;
    @Shadow
    @Final
    private static Component TAB_BLOCKED;
    @Shadow
    SocialInteractionsPlayerList socialInteractionsPlayerList;
    @Shadow
    private SocialInteractionsScreen.Page page;
    @Shadow
    private Button allButton;
    @Shadow
    private Button hiddenButton;
    @Shadow
    private Button blockedButton;
    @Unique
    private boolean mdvlcraft$party;
    @Unique
    private Button mdvlcraft$partyButton;

    protected SocialScreenPartyTabMixin(Component title) {
        super(title);
    }

    @Shadow
    private void showPage(SocialInteractionsScreen.Page page) {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mdvlcraft$partyTab(CallbackInfo ci) {
        int left = this.socialInteractionsPlayerList.getRowLeft();
        int width = (this.socialInteractionsPlayerList.getRowRight() - left - 3) / 4;
        this.removeWidget(this.allButton);
        this.removeWidget(this.hiddenButton);
        this.removeWidget(this.blockedButton);
        this.allButton = this.addRenderableWidget(Button.builder(TAB_ALL, b -> this.mdvlcraft$show(false, SocialInteractionsScreen.Page.ALL))
            .bounds(left, 45, width, 20).build());
        this.hiddenButton = this.addRenderableWidget(Button.builder(TAB_HIDDEN, b -> this.mdvlcraft$show(false, SocialInteractionsScreen.Page.HIDDEN))
            .bounds(left + (width + 1), 45, width, 20).build());
        this.blockedButton = this.addRenderableWidget(Button.builder(TAB_BLOCKED, b -> this.mdvlcraft$show(false, SocialInteractionsScreen.Page.BLOCKED))
            .bounds(left + 2 * (width + 1), 45, width, 20).build());
        this.mdvlcraft$partyButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.mdvlcraft.party.tab"),
            b -> this.mdvlcraft$show(true, SocialInteractionsScreen.Page.ALL)).bounds(left + 3 * (width + 1), 45, width, 20).build());
        this.showPage(this.page);
    }

    @Unique
    private void mdvlcraft$show(boolean party, SocialInteractionsScreen.Page page) {
        this.mdvlcraft$party = party;
        this.showPage(page);
    }

    @Inject(method = "showPage", at = @At("TAIL"))
    private void mdvlcraft$showParty(SocialInteractionsScreen.Page page, CallbackInfo ci) {
        if (this.mdvlcraft$partyButton == null) {
            return;   // the first showPage of init runs before the Party tab exists
        }
        Component tab = Component.translatable("gui.mdvlcraft.party.tab");
        if (!this.mdvlcraft$party) {
            this.mdvlcraft$partyButton.setMessage(tab);
            return;
        }
        this.allButton.setMessage(TAB_ALL);
        this.mdvlcraft$partyButton.setMessage(tab.copy().withStyle(ChatFormatting.UNDERLINE));
        TrackedGroup party = ClientGroupManager.getGroup(GroupType.PARTY);
        Set<UUID> members = party == null ? Set.of() : this.minecraft.player.connection.getOnlinePlayerIds().stream()
            .filter(party::contains).collect(Collectors.toSet());
        this.socialInteractionsPlayerList.updatePlayerList(members, this.socialInteractionsPlayerList.getScrollAmount(), false);
    }
}
