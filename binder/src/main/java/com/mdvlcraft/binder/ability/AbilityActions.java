package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.content.DemonicAssimilationDismiss;
import com.mdvlcraft.binder.content.SummonModes;
import com.mdvlcraft.binder.doppelganger.Doppelgangers;
import com.mdvlcraft.binder.gaunt.GauntForm;
import com.mdvlcraft.binder.gaunt.GauntLeap;
import com.mdvlcraft.binder.raiju.RaijuForm;
import com.mdvlcraft.binder.seraph.SeraphForm;
import com.tiago.antimagic.spell.DemonicAssimilationSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AbilityActions {
    private AbilityActions() {
    }

    public static boolean assign(ServerPlayer player, int slot, Optional<ResourceLocation> abilityId) {
        if (abilityId.isPresent() && abilityId.flatMap(Ability::byId).map(a -> AbilityGrants.level(player, a)).orElse(0) == 0) {
            return false;
        }

        Loadout.set(player, slot, abilityId);
        return true;
    }

    public static void press(ServerPlayer player, int slot) {
        if (com.mdvlcraft.binder.gate.GateCombat.downed(player)) {
            return;   // no skills while downed (combat/DownedNoSkills)
        }
        if (ShrineSign.signing(player)) {
            return;   // holding the shrine's hand sign (ShrineSign)
        }
        if (RaijuForm.in(player)) {
            // Yakusanoikazuchi's wheel: Transform Back only.
            GauntAbility.raijuSlot(slot).ifPresent(raiju -> castSpell(player, SpellRegistry.getSpell(ResourceLocation.parse(RaijuForm.SPELL)), 1));
            return;
        }
        if (GauntForm.in(player)) {
            // The form's own wheel: Transform Back, Leap, Horrify and Faceless, nothing else.
            GauntAbility.slot(slot).ifPresent(gaunt -> {
                if (gaunt == GauntAbility.REVERT) {
                    castSpell(player, SpellRegistry.getSpell(ResourceLocation.parse(GauntForm.SPELL)), 1);
                } else if (GauntForm.active(player)) {
                    switch (gaunt) {
                        case LEAP -> GauntLeap.press(player);
                        case HORRIFY -> GauntLeap.horrify(player);
                        case FACELESS -> GauntLeap.faceless(player);
                        default -> {
                        }
                    }
                    SkillCallout.announce(player, gaunt);
                }
            });
            return;
        }
        Optional<Ability> ability = Loadout.get(player).get(slot).flatMap(Ability::byId);
        if (!ability.isEmpty()) {
            int level = AbilityGrants.level(player, ability.get());
            if (level == 0) {
                player.connection
                    .send(
                        new ClientboundSetActionBarTextPacket(
                            Component.translatable("ability.mdvlcraft.not_granted", ability.get().displayName()).withStyle(ChatFormatting.RED)
                        )
                    );
            } else if (SeraphForm.in(player) && !(ability.get() instanceof SpellAbility)) {
                SeraphForm.refuse(player);
            } else if (ability.get() instanceof SpellAbility spellAbility) {
                castSpell(player, spellAbility.spell(), level);   // SkillCallout calls it out when Iron's casts it
            } else {
                cast(player, ability.get());
            }
        }
    }

    /** A wheel ability other than an Iron's spell; it is called out (SkillCallout) when the press spent mana or started a cooldown. */
    private static void cast(ServerPlayer player, Ability ability) {
        float mana = MagicData.getPlayerMagicData(player).getMana();
        long cooldown = ability instanceof Technique technique ? TechniqueCooldowns.remaining(player, technique) : 0L;
        if (ability instanceof Technique technique) {
            TechniqueRunner.press(player, technique);
        } else if (ability instanceof SlotSkillAbility slotSkill) {
            slotSkill.cast(player);
        } else if (ability instanceof StanceAbility) {
            Stances.press(player);
        } else if (ability instanceof DoppelgangerAbility) {
            Doppelgangers.press(player);
        } else if (ability instanceof SustenanceAbility sustenance) {
            sustenance.cast(player);
        } else if (ability instanceof FormlessSwordAbility) {
            com.mdvlcraft.binder.shadow.ShadowArsenal.press(player);
        } else if (ability instanceof PlannedAbility planned) {
            planned.cast(player);
            return;
        }
        boolean used = ability instanceof StanceAbility ? Stances.active(player).isPresent()
            : player.isCreative() || MagicData.getPlayerMagicData(player).getMana() < mana
                || ability instanceof Technique technique && TechniqueCooldowns.remaining(player, technique) > cooldown;
        if (used) {
            SkillCallout.announce(player, ability);
        }
    }

    public static void dropUngranted(ServerPlayer player) {
        List<Optional<ResourceLocation>> wheel = Loadout.get(player);

        for (int slot = 0; slot < wheel.size(); slot++) {
            Optional<ResourceLocation> id = wheel.get(slot);
            if (id.isPresent() && id.flatMap(Ability::byId).map(a -> AbilityGrants.level(player, a) == 0).orElse(true)) {
                Loadout.set(player, slot, Optional.empty());
            }
        }

        Doppelgangers.dismissIfUngranted(player);
    }

    public static void release(ServerPlayer player) {
        GauntLeap.release(player);
        TechniqueRunner.release(player);
        Doppelgangers.release(player);
    }

    private static void castSpell(ServerPlayer player, AbstractSpell spell, int level) {
        // Casting Demonic Assimilation again ends it; Iron's would only answer that the spell is on cooldown.
        if (spell instanceof DemonicAssimilationSpell && DemonicAssimilationDismiss.end(player)) {
            return;
        }

        MagicData magic = MagicData.getPlayerMagicData(player);
        // With its summons out, casting a summon spell again changes how they fight; sneaking casts the recast, which dismisses them.
        if (SummonModes.summonsOut(magic, spell) && !player.isShiftKeyDown()) {
            SummonModes.cycle(player);
            return;
        }

        if (magic.isCasting() && !magic.getCastingSpellId().equals(spell.getSpellId())) {
            Utils.serverSideCancelCast(player, magic.getCastType() != CastType.LONG);
        }

        spell.attemptInitiateCast(
            ItemStack.EMPTY, spell.getLevelFor(level, player), player.level(), player, CastSource.SPELLBOOK, true, SpellSelectionManager.MAINHAND
        );
    }
}
