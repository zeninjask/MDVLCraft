package com.mdvlcraft.binder.modified;

import cursedfate.entity.IncompleteShrineEntity;
import cursedfate.init.CursedfateModEntities;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Underworld Gate (Shinigami): real players only (not a fake player, such as the spell preview's). Raises a dark incomplete
 * Malevolent Shrine behind the caster for 10 s (only the model: it does not cut, see mixin/cursedfate/
 * IncompleteShrineOnTickProcedureMixin), holds every enemy within 24 blocks (party members and the caster's summons are
 * spared) in black root vines for 10 s and turns the caster invisible for 10 s. The shrine is scenery: it can't move or turn
 * (frozen in place by the shrine tick mixin) and can't be targeted or hit (mixin/cursedfate/IncompleteShrineEntityMixin).
 */
public class UnderworldGate extends ModifiedSpell {
    public static final int TICKS = 200;
    /** Tag of the gate's shrine; texture assets/cursedfate/textures/entities/underworld_shrine.png (tools/underworld_tex.py). */
    public static final String SHRINE_TAG = "mdvlcraft_underworld_gate";
    public static final String SHRINE_TEXTURE = "underworld_shrine";
    /** Where the shrine stands and faces (x, y, z, yaw), kept in its persistent data so it stays put. */
    public static final String SHRINE_SPOT = "mdvlcraft_gate_spot";

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
            for (LivingEntity enemy : Targets.enemies(player, player.position(), 24.0)) {
                if (!(enemy instanceof RootEntity)) {
                    BlackRoot.hold(player, enemy, TICKS);
                }
            }
            raiseShrine(server, player);   // after the roots, so the shrine is not taken for an enemy
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, TICKS, 0, false, false, true));
            GroundSpells.ring(server, player.position(), 24.0, ParticleTypes.SCULK_SOUL);
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        super.onCast(level, spellLevel, caster, source, data);
    }

    /** Where Cursed Fate raises its incomplete shrine: 9 blocks behind the caster, facing 12 blocks ahead of them. */
    private static void raiseShrine(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        BlockPos pos = BlockPos.containing(player.getX() - look.x * 9.0, player.getY(), player.getZ() - look.z * 9.0);
        IncompleteShrineEntity shrine = CursedfateModEntities.INCOMPLETE_SHRINE.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (shrine == null) {
            throw new IllegalStateException("Cursed Fate's incomplete shrine did not spawn at " + pos);
        }
        shrine.addTag(SHRINE_TAG);
        shrine.setTexture(SHRINE_TEXTURE);
        shrine.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(player.getX() + look.x * 12.0, player.getY(), player.getZ() + look.z * 12.0));
        shrine.setNoAi(true);
        shrine.setInvulnerable(true);
        shrine.setNoGravity(true);
        CompoundTag spot = new CompoundTag();
        spot.putDouble("x", shrine.getX());
        spot.putDouble("y", shrine.getY());
        spot.putDouble("z", shrine.getZ());
        spot.putFloat("yaw", shrine.getYRot());
        shrine.getPersistentData().put(SHRINE_SPOT, spot);
    }

    /** Keeps the shrine where it was raised, facing the same way (called every tick by the shrine tick mixin). */
    public static void hold(Entity shrine) {
        CompoundTag spot = shrine.getPersistentData().getCompound(SHRINE_SPOT);
        if (spot.isEmpty()) {
            return;
        }
        float yaw = spot.getFloat("yaw");
        shrine.setDeltaMovement(Vec3.ZERO);
        shrine.moveTo(spot.getDouble("x"), spot.getDouble("y"), spot.getDouble("z"), yaw, 0.0F);
        if (shrine instanceof LivingEntity living) {
            living.setYBodyRot(yaw);
            living.setYHeadRot(yaw);
            living.yBodyRotO = yaw;
            living.yHeadRotO = yaw;
        }
    }
}
