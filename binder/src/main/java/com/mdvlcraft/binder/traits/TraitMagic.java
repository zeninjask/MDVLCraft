package com.mdvlcraft.binder.traits;

import com.mdvlcraft.binder.specs.SpecKit;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The spell traits: Echo Caster, Wild Magic and Blood Price (its cast check is mixin/traits/BloodPriceMixin). */
@EventBusSubscriber(modid = "mdvlcraft")
public final class TraitMagic {
    /** Wild Magic: the empowered spell and until when its damage counts. */
    private static final Map<UUID, Empowered> EMPOWERED = new HashMap<>();

    private TraitMagic() {
    }

    private record Empowered(String spell, long until) {
    }

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Traits.has(player, Trait.WILD_MAGIC) || !event.isCancelable()) {
            return;
        }
        if (player.getRandom().nextFloat() < 0.15F) {
            event.setCanceled(true);
            AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
            MagicData magic = MagicData.getPlayerMagicData(player);
            if (event.getCastSource().consumesMana()) {
                SpecKit.addMana(player, -Math.min(magic.getMana(), spell.getManaCost(event.getSpellLevel()) / 2.0F));
            }
            player.serverLevel().sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY(1.0), player.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
            player.displayClientMessage(Component.translatable("trait.mdvlcraft.fizzle").withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (Traits.has(player, Trait.WILD_MAGIC) && player.getRandom().nextFloat() < 0.2F) {
            EMPOWERED.put(player.getUUID(), new Empowered(event.getSpellId(), player.level().getGameTime() + 100));
            player.serverLevel().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
        }
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (Traits.has(player, Trait.ECHO_CASTER) && spell.getCastType() == CastType.INSTANT && event.getCastSource() != CastSource.COMMAND
            && player.getRandom().nextFloat() < 0.1F) {
            int level = Math.max(1, event.getSpellLevel() / 2);
            SpecKit.later(player.serverLevel(), 10, () -> {
                if (player.isAlive()) {
                    spell.onCast(player.level(), level, player, CastSource.COMMAND, MagicData.getPlayerMagicData(player));
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.0), player.getZ(), 30, 0.5, 0.6, 0.5, 0.5);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getSpellDamageSource().getEntity() instanceof ServerPlayer player) {
            Empowered empowered = EMPOWERED.get(player.getUUID());
            if (empowered != null && player.level().getGameTime() <= empowered.until
                && event.getSpellDamageSource().spell() != null && empowered.spell.equals(event.getSpellDamageSource().spell().getSpellId())) {
                event.setAmount(event.getAmount() * 1.6F);
            }
        }
    }

    /**
     * Blood Price, from BloodPriceMixin before a cast is checked: a player without enough mana pays the rest in health, 1
     * per 10 mana, if that leaves them above 1 health.
     */
    public static void bloodPrice(AbstractSpell spell, int level, CastSource source, MagicData magic, ServerPlayer player) {
        if (!Traits.has(player, Trait.BLOOD_PRICE) || !source.consumesMana() || magic.getPlayerCooldowns().isOnCooldown(spell)) {
            return;
        }
        float cost = spell.getManaCost(level);
        float missing = cost - magic.getMana();
        if (missing <= 0.0F) {
            return;
        }
        float health = missing / 10.0F;
        if (player.getHealth() - health > 1.0F) {
            player.setHealth(player.getHealth() - health);
            SpecKit.addMana(player, missing);
            player.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY(1.0), player.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        EMPOWERED.remove(event.getEntity().getUUID());
    }
}
