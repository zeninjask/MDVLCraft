package com.mdvlcraft.binder.mixin.absorbed.client;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * From Fast IP Ping (Fallen_Breath, LGPL-3.0): a server typed as a bare IP address gets an InetAddress that already has
 * its host name, so pinging and connecting skip the slow reverse DNS lookup.
 */
@Mixin(value = ServerAddressResolver.class, priority = 200)
public interface ServerAddressResolverMixin {
    /**
     * @author Fallen_Breath (Fast IP Ping)
     * @reason no reverse lookup for IP-only addresses (m_171877_ is the SRG name of SYSTEM's lambda$static$0)
     */
    @Overwrite(remap = false)
    static Optional<ResolvedServerAddress> m_171877_(ServerAddress address) {
        try {
            InetAddress resolved = InetAddress.getByName(address.getHost());
            if (InetAddresses.isInetAddress(address.getHost())) {
                resolved = InetAddress.getByAddress(resolved.getHostAddress(), resolved.getAddress());
            }
            return Optional.of(ResolvedServerAddress.from(new InetSocketAddress(resolved, address.getPort())));
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }
}
