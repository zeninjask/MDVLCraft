package com.mdvlcraft.binder.gate;

import com.mdvlcraft.binder.gate.GateInstances.Instance;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Spawns gate mobs (plan 5.3). Mobs are made the way integration/BossArena does it, calling finalizeSpawn directly, so the
 * pack's content filter (which cancels natural/event spawns of many mob mods) doesn't stop them. Every gate mob is scaled
 * by its gate's rank and carries its instance id in persistent NBT ({@link #TAG}); rewards, clear checks and breaks find
 * gate mobs by that tag. Phase 3 adds theme mob pools and room triggers on top of this.
 */
public final class GateSpawner {
    /** Persistent NBT int on every gate mob: the id of its instance. */
    public static final String TAG = "mdvlcraft_gate";
    private static final UUID HEALTH = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0001");
    private static final UUID DAMAGE = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0002");
    private static final UUID ARMOUR = UUID.fromString("6f1c2a52-6b0e-4a39-9a51-7c3c1f0a0003");

    private GateSpawner() {
    }

    /** A mob of `type` at `pos`, scaled to the instance's rank and tagged with it; null if the type can't be made. */
    @Nullable
    public static Mob spawn(ServerLevel level, Instance instance, EntityType<?> type, BlockPos pos) {
        if (!(type.create(level) instanceof Mob mob)) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        scale(mob, instance.rank);
        mob.getPersistentData().putInt(TAG, instance.id);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    /** The instance id a gate mob belongs to, or -1 for any other entity. */
    public static int instanceOf(Entity entity) {
        return entity.getPersistentData().contains(TAG) ? entity.getPersistentData().getInt(TAG) : -1;
    }

    static void scale(Mob mob, GateRank rank) {
        modify(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH, "gate health", rank.health - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE, "gate damage", rank.damage - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(mob.getAttribute(Attributes.ARMOR), ARMOUR, "gate armour", rank.armour, AttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void modify(@Nullable AttributeInstance attribute, UUID id, String name, double amount, AttributeModifier.Operation operation) {
        if (attribute != null && amount != 0.0 && attribute.getModifier(id) == null) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
        }
    }
}
