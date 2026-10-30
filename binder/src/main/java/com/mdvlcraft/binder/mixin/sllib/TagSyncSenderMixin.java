package com.mdvlcraft.binder.mixin.sllib;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SL Library's SAVE_AND_STC and STC syncs send a value to the player's client on every call, changed or not; Extended
 * Datapacks' charged attack sends its cast flag every tick for every player (20 packets a second each). A SAVE_AND_STC
 * value equal to the stored one isn't sent again; an unchanged STC value goes out at most once a second.
 */
@Mixin(targets = "sleys.sl.library.network.sync.TagSyncSender", remap = false)
public abstract class TagSyncSenderMixin {
    /** STC values last sent, by player and key: {value, game time}. */
    private static final Map<UUID, Map<String, Object[]>> mdvlcraft$sent = new ConcurrentHashMap<>();

    @Shadow
    private static ServerPlayer validateAndGetServerPlayer(Object target, String key) {
        throw new AssertionError();
    }

    @Inject(method = "processSync(Lsleys/sl/library/network/sync/TagSyncSender$SyncMethod;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Lsleys/sl/library/network/sync/TagSyncSender$Getter;Lsleys/sl/library/network/sync/TagSyncSender$Setter;Lsleys/sl/library/network/sync/TagSyncSender$STCSender;Lsleys/sl/library/network/sync/TagSyncSender$CTSSender;Ljava/util/function/BiPredicate;)V",
        at = @At("HEAD"), cancellable = true)
    private static void mdvlcraft$unchanged(@Coerce Object method, Object target, String key, Object value, @Coerce Object getter, @Coerce Object setter,
                                            @Coerce Object stc, @Coerce Object cts, java.util.function.BiPredicate<?, ?> equal, CallbackInfo ci) {
        String kind = ((Enum<?>)method).name();
        if (key == null || !kind.equals("SAVE_AND_STC") && !kind.equals("STC")) {
            return;
        }
        ServerPlayer player = validateAndGetServerPlayer(target, key);
        if (player == null) {
            return;
        }
        if (kind.equals("SAVE_AND_STC")) {
            Tag stored = player.getPersistentData().get(key);
            if (stored != null && stored.equals(mdvlcraft$tag(value))) {
                ci.cancel();
            }
            return;
        }
        // STC keeps nothing server-side, and the client may change its copy itself: an unchanged value still goes out
        // once a second, a changed one at once
        Map<String, Object[]> sent = mdvlcraft$sent.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Object[] last = sent.get(key);
        long now = player.level().getGameTime();
        if (last != null && Objects.equals(last[0], value) && now - (Long)last[1] < 20L) {
            ci.cancel();
            return;
        }
        sent.put(key, new Object[] {value, now});
    }

    @Nullable
    private static Tag mdvlcraft$tag(Object value) {
        if (value instanceof Boolean b) {
            return ByteTag.valueOf(b);
        } else if (value instanceof Integer i) {
            return IntTag.valueOf(i);
        } else if (value instanceof Float f) {
            return FloatTag.valueOf(f);
        } else if (value instanceof Double d) {
            return DoubleTag.valueOf(d);
        } else if (value instanceof Long l) {
            return LongTag.valueOf(l);
        } else if (value instanceof Byte b) {
            return ByteTag.valueOf(b);
        } else if (value instanceof String s) {
            return StringTag.valueOf(s);
        }
        return null;
    }
}
