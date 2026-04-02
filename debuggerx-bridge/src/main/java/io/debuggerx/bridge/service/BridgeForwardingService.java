package io.debuggerx.bridge.service;

import io.debuggerx.bridge.constants.BridgeProtocolConstants;
import io.debuggerx.bridge.model.BridgeForwardFrame;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * Bridge 转发服务。
 *
 * @author ouwu
 */
@Slf4j
public final class BridgeForwardingService {

    private static final BridgeForwardingService INSTANCE = new BridgeForwardingService();

    private final ChannelGroup adminChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private BridgeForwardingService() {
    }

    public static BridgeForwardingService getInstance() {
        return INSTANCE;
    }

    public void registerAdminChannel(Channel channel) {
        adminChannels.add(channel);
        log.info("[BridgeForward] Admin channel registered: {}", channel.remoteAddress());
    }

    public void unregisterAdminChannel(Channel channel) {
        adminChannels.remove(channel);
        log.info("[BridgeForward] Admin channel unregistered: {}", channel.remoteAddress());
    }

    public void forwardServerToJvm(byte[] payload, String clientIp) {
        forward(payload, BridgeProtocolConstants.DIRECTION_SERVER_TO_JVM, clientIp);
    }

    public void forwardJvmToServer(byte[] payload, String clientIp) {
        forward(payload, BridgeProtocolConstants.DIRECTION_JVM_TO_SERVER, clientIp);
    }

    private void forward(byte[] payload, byte direction, String clientIp) {
        if (payload == null || payload.length == 0 || adminChannels.isEmpty()) {
            return;
        }
        String rawClientIp = clientIp == null || clientIp.isEmpty() ? "unknown" : clientIp;
        BridgeForwardFrame frame = new BridgeForwardFrame(direction, rawClientIp, payload);
        adminChannels.writeAndFlush(frame);
    }
}
