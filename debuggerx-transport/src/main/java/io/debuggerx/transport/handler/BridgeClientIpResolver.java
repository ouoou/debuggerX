package io.debuggerx.transport.handler;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Bridge 转发场景下的客户端 IP 解析工具。
 */
public final class BridgeClientIpResolver {

    private BridgeClientIpResolver() {
    }

    /**
     * 从 channel 的 remoteAddress 中解析客户端 IP。
     *
     * @param channel netty channel
     * @return 客户端 IP（不可解析时返回 unknown）
     */
    public static String resolve(Channel channel) {
        SocketAddress remoteAddress = channel == null ? null : channel.remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
            if (inetSocketAddress.getAddress() != null) {
                return inetSocketAddress.getAddress().getHostAddress();
            }
            return inetSocketAddress.getHostString();
        }
        return remoteAddress == null ? "unknown" : remoteAddress.toString();
    }
}

