package io.debuggerx.transport.handler;

import io.debuggerx.common.constants.JdwpConstants;
import io.debuggerx.common.enums.ConnectionType;
import io.debuggerx.core.service.DebuggerService;
import io.debuggerx.core.strategy.ConnectionHandlerStrategy;
import io.debuggerx.core.strategy.impl.DebuggerProxyStrategy;
import io.debuggerx.core.strategy.impl.JvmServerStrategy;
import io.debuggerx.protocol.packet.JdwpPacket;
import io.debuggerx.protocol.packet.PacketSource;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;

import static io.debuggerx.common.enums.ConnectionType.DEBUGGER_PROXY;
import static io.debuggerx.common.enums.ConnectionType.JVM_SERVER;

/**
 * 数据包处理
 *
 * @author ouwu
 */
@Slf4j
@ChannelHandler.Sharable
public class DebugProxyHandler extends ChannelInboundHandlerAdapter {
    private final ConnectionType connectionType;
    private final DebuggerService debuggerService;

    private final EnumMap<ConnectionType, ConnectionHandlerStrategy> strategies = new EnumMap<>(ConnectionType.class);

    public DebugProxyHandler(ConnectionType connectionType) {
        this.connectionType = connectionType;
        this.debuggerService = DebuggerService.getInstance();

        strategies.put(connectionType, connectionType == ConnectionType.DEBUGGER_PROXY ? new DebuggerProxyStrategy() : new JvmServerStrategy());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 对于JVM客户端主动发送握手信息
        if (connectionType == JVM_SERVER) {
            handleHandShakeMsg(ctx);
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof byte[] && new String((byte[]) msg).equals(JdwpConstants.HANDSHAKE_STRING)) {
            handleHandshake(ctx);
            return;
        }
        
        if (msg instanceof JdwpPacket) {
            handlePacket(ctx, (JdwpPacket) msg);
        }
    }

    private void handleHandshake(ChannelHandlerContext ctx) {
        debuggerService.handleHandshake(ctx.channel(), connectionType);
        
        // debug客户端主动握手回复
        if (connectionType == DEBUGGER_PROXY) {
            handleHandShakeMsg(ctx);
        }
    }

    private void handleHandShakeMsg(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.wrappedBuffer(JdwpConstants.HANDSHAKE_PACKET))
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("[DebugProxyHandleMsg] Failed to send handshake: {}", ctx.channel());
                    }
                });
    }
    
    private void handlePacket(ChannelHandlerContext ctx, JdwpPacket packet) {
        // 前置处理
        execPreprocessor(ctx, packet);

        // 执行命令
        execPacket(ctx, packet);

    }

    private void execPreprocessor(ChannelHandlerContext ctx, JdwpPacket packet) {
        // 如果是事件请求 缓存requestId
        debuggerService.cacheRequestId(new PacketSource(connectionType, ctx.channel()), packet);
    }

    private void execPacket(ChannelHandlerContext ctx, JdwpPacket packet) {
        strategies.get(connectionType)
                .handle(ctx, packet, debuggerService);
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("[DebugProxyHandleInactive] {} channel inactive: {}", connectionType, ctx.channel());
        debuggerService.handleDisconnect(ctx.channel(), connectionType);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        log.error("Error", cause);
        ctx.close();
    }
}