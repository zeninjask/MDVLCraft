package com.mdvlcraft.binder.client;

/**
 * A Social Interactions row with the pack's party buttons (mixin/parties/SocialPlayerEntryPartyMixin). Clicks reach them
 * through mixin/parties/SocialEntryClickMixin rather than the row's children(), which Parties already overrides for its
 * invite button: two return-value hooks on children() cancel each other.
 */
public interface PartyRowButtons {
    /** True when the click hit a visible party button (it has then run). */
    boolean mdvlcraft$clickParty(double mouseX, double mouseY, int button);
}
