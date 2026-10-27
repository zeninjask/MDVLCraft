package com.mdvlcraft.binder.absorbed;

import com.mdvlcraft.binder.mixin.absorbed.LookControlAccessor;
import com.mdvlcraft.binder.mixin.absorbed.MobLookControlAccessor;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * From AI Improvements (BuiltBroken, MIT), the one part the pack used: a mob with the plain vanilla look control gets
 * this one, which works out where to look with a lookup-table atan2 instead of Math.atan2.
 */
@EventBusSubscriber(modid = "mdvlcraft")
public final class FastLookControl extends LookControl {
    private static final int DIM = 256;
    /** atan2(j, i) / DIM for i, j in [0, DIM), at [j * DIM + i]. */
    private static final float[] ATAN2 = new float[DIM * DIM];

    static {
        for (int i = 0; i < DIM; i++) {
            for (int j = 0; j < DIM; j++) {
                ATAN2[j * DIM + i] = (float)Math.atan2((float)j / DIM, (float)i / DIM);
            }
        }
    }

    private FastLookControl(Mob mob, LookControl old) {
        super(mob);
        LookControlAccessor state = (LookControlAccessor)old;
        this.wantedX = old.getWantedX();
        this.wantedY = old.getWantedY();
        this.wantedZ = old.getWantedZ();
        this.lookAtCooldown = state.mdvlcraft$lookAtCooldown();
        this.xMaxRotAngle = state.mdvlcraft$xMaxRotAngle();
        this.yMaxRotSpeed = state.mdvlcraft$yMaxRotSpeed();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && mob.getLookControl().getClass() == LookControl.class) {
            ((MobLookControlAccessor)mob).mdvlcraft$setLookControl(new FastLookControl(mob, mob.getLookControl()));
        }
    }

    @Override
    protected Optional<Float> getXRotD() {
        double dx = this.wantedX - this.mob.getX();
        double dy = this.wantedY - this.mob.getEyeY();
        double dz = this.wantedZ - this.mob.getZ();
        return Optional.of((float)(-(atan2(dy, Mth.sqrt((float)(dx * dx + dz * dz))) * 180.0F / (float)Math.PI)));
    }

    @Override
    protected Optional<Float> getYRotD() {
        double dx = this.wantedX - this.mob.getX();
        double dz = this.wantedZ - this.mob.getZ();
        return Optional.of((float)(atan2(dz, dx) * 180.0F / (float)Math.PI) - 90.0F);
    }

    private static float atan2(double y, double x) {
        float add;
        float mul;
        if (x < 0.0) {
            x = -x;
            if (y < 0.0) {
                y = -y;
                mul = 1.0F;
            } else {
                mul = -1.0F;
            }
            add = (float)-Math.PI;
        } else {
            if (y < 0.0) {
                y = -y;
                mul = -1.0F;
            } else {
                mul = 1.0F;
            }
            add = 0.0F;
        }
        double scale = (DIM - 1) / Math.max(x, y);
        return (ATAN2[(int)(y * scale) * DIM + (int)(x * scale)] + add) * mul;
    }
}
