package io.debuggerx.transport.handler;

import io.debuggerx.bridge.service.BridgeForwardingService;
import io.debuggerx.protocol.packet.JdwpPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * debugger(client) -> proxy(jvm) 的镜像转发器。
 *
 * <p>放置在 debugger 入站解码后、{@link io.debuggerx.transport.handler.DebugProxyHandler} 之前，
 * 直接从连接（client channel）获取 clientIp，并将解码后的 JDWP 数据包转发给 bridge。</p>
 */
public class BridgeForwardClientToBridgeHandler extends ChannelInboundHandlerAdapter {

    private final BridgeForwardingService forwardingService = BridgeForwardingService.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String clientIp = BridgeClientIpResolver.resolve(ctx.channel());
        if (msg instanceof JdwpPacket) {
            JdwpPacket packet = (JdwpPacket) msg;
            forwardingService.forwardServerToJvm(packet.toBytes(), clientIp);
        }
        // 握手协议与其它入站消息都不转发，仅透传。
        ctx.fireChannelRead(msg);
    }
}

