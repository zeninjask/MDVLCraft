package com.mdvlcraft.binder.modified;

import com.mdvlcraft.binder.ability.ShrineGate;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Underworld Gate (Shinigami): real players only (a fake player, such as the spell preview's, has no Cursed Fate data to
 * open the shrine with). Opens the open Malevolent Shrine for 10 s (it does not cut), holds every enemy within
 * 12 blocks (party members and the caster's summons are spared) in black root vines for 10 s and turns the caster
 * invisible for 10 s.
 */
public class UnderworldGate extends ModifiedSpell {
    private static final int TICKS = 200;

    public UnderworldGate() {
        super("underworld_gate", 0);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return caster instanceof ServerPlayer && !(caster instanceof FakePlayer);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof ServerPlayer player && !(player instanceof FakePlayer) && level instanceof ServerLevel server) {
            if (ShrineGate.open(player)) {
                Zones.start(server, player.position(), player, 0.0F, TICKS, 1000, zone -> {
                }, () -> ShrineGate.close(player), null);
            }
            for (LivingEntity enemy : Targets.enemies(player, player.position(), 12.0)) {
                if (!(enemy instanceof RootEntity)) {
                    BlackRoot.hold(player, enemy, TICKS);
                }
            }
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, TICKS, 0, false, false, true));
            GroundSpells.ring(server, player.position(), 12.0, ParticleTypes.SCULK_SOUL);
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}
