package io.debuggerx.transport.handler;

import io.debuggerx.bridge.service.BridgeForwardingService;
import io.debuggerx.common.enums.ConnectionType;
import io.debuggerx.core.session.DebugSession;
import io.debuggerx.core.session.SessionManager;
import io.debuggerx.protocol.packet.JdwpPacket;
import io.debuggerx.protocol.packet.PacketSource;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * JVM(proxy) -> client 的镜像转发器（并附带 clientIp）。
 *
 * <p>放置在 {@link io.debuggerx.transport.handler.DebugProxyHandler} 之后，依赖
 * {@code DebugProxyHandler} 将解析完成后的入站消息继续向下传播。</p>
 */
public class BridgeForwardJvmToBridgeHandler extends ChannelInboundHandlerAdapter {

    private final BridgeForwardingService forwardingService = BridgeForwardingService.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof JdwpPacket) {
            JdwpPacket packet = (JdwpPacket) msg;
            forwardToBridge(packet);
        }
        // 握手协议与其它入站消息都不转发，仅透传。
        ctx.fireChannelRead(msg);
    }

    private void forwardToBridge(JdwpPacket packet) {
        DebugSession session = SessionManager.getInstance().findJvmServerSession();
        Set<PacketSource> packetSources = resolveClientPacketSources(session, packet);
        if (packetSources.isEmpty()) {
            return;
        }

        byte[] payload = packet.toBytes();
        for (PacketSource packetSource : packetSources) {
            if (packetSource.getSourceType() != ConnectionType.DEBUGGER_PROXY) {
                continue;
            }
            String clientIp = BridgeClientIpResolver.resolve(packetSource.getChannel());
            forwardingService.forwardJvmToServer(payload, clientIp);
        }
    }

    private Set<PacketSource> resolveClientPacketSources(DebugSession session, JdwpPacket packet) {
        Set<PacketSource> sources = new HashSet<>();

        // 仅使用 head.id 反查 origin PacketSource。
        // DebuggerService 会在回复包路径中把 header.id 恢复为 originId。
        int originId = packet.getHeader().getId();
        session.getPacketIdMap().values().forEach(pair -> {
            if (pair.getLeft() != null && pair.getLeft() == originId) {
                sources.add(pair.getRight());
            }
        });

        return sources;
    }
}

