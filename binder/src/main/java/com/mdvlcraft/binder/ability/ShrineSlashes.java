package com.mdvlcraft.binder.ability;

import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

public final class ShrineSlashes {
    private static final String DOME_SLASH = "photon fx photon:cleavedomain entity @s 0 1 0 0 0 0 2.5 2.5 2.5 0 false true";
    private static final Supplier<SoundEvent> SWEEP =
        () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("cursedfate", "metalsweep"));

    private ShrineSlashes() {
    }

    @Nullable
    public static ServerPlayer owner(Entity domain, String key) {
        String uuid = domain.getPersistentData().getString(key);
        if (uuid.isEmpty()) {
            return null;
        } else {
            return domain.level().getPlayerByUUID(UUID.fromString(uuid)) instanceof ServerPlayer player ? player : null;
        }
    }

    public static boolean held(ServerPlayer owner) {
        return !TechniqueRunner.isSlashing(owner);
    }

    /**
     * One tick of the closed shrine's slashes filling its dome, drawn with the open shrine's own effect (Cursed Fate's
     * cleavedomain, two or three a tick, and its metal sweep now and then) in place of its per-target cleaves.
     */
    static void fillDome(Entity domain) {
        if (domain.getServer() == null) {
            return;
        }
        CommandSourceStack source = domain.createCommandSourceStack().withSuppressedOutput().withPermission(4);
        int slashes = 2 + domain.level().random.nextInt(2);
        for (int i = 0; i < slashes; i++) {
            domain.getServer().getCommands().performPrefixedCommand(source, DOME_SLASH);
        }
        if (domain.level().random.nextInt(3) == 0) {
            domain.level().playSound(null, domain.blockPosition(), SWEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
