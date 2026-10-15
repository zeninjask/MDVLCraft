package com.mdvlcraft.binder.mixin;

import java.util.List;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The server list pings on a worker thread and listed the ping connection before giving it a listener; a server that
 * closed the connection in between made the menu's tick call handleDisconnection with no listener and crash
 * (NullPointerException in Connection.handleDisconnection). The connection is now listed once its listener is set.
 */
@Mixin(ServerStatusPinger.class)
public abstract class ServerStatusPingerMixin {
    @Shadow @Final private List<Connection> connections;

    @Redirect(method = "pingServer", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean mdvlcraft$listLater(List<Connection> list, Object connection) {
        return true;
    }

    @Redirect(method = "pingServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setListener(Lnet/minecraft/network/PacketListener;)V"))
    private void mdvlcraft$listWithListener(Connection connection, PacketListener listener) {
        connection.setListener(listener);
        synchronized (this.connections) {
            this.connections.add(connection);
        }
    }
}
