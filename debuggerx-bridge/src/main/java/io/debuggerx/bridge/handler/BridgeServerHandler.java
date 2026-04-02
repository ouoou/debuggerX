package io.debuggerx.bridge.handler;

import io.debuggerx.bridge.constants.BridgeProtocolConstants;
import io.debuggerx.bridge.model.BridgeCommand;
import io.debuggerx.bridge.service.BridgeForwardingService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Bridge 入站处理器。
 *
 * @author ouwu
 */
@Slf4j
@ChannelHandler.Sharable
public class BridgeServerHandler extends ChannelInboundHandlerAdapter {

    private final BridgeForwardingService forwardingService = BridgeForwardingService.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof BridgeCommand)) {
            log.warn("[BridgeHandler] Unsupported inbound message type: {}", msg.getClass());
            return;
        }
        BridgeCommand command = (BridgeCommand) msg;
        switch (command.getCommandType()) {
            case BridgeProtocolConstants.CMD_HANDSHAKE:
                handleHandshake(ctx);
                break;
            default:
                // 当前协议仅保留握手和心跳命令，未知命令直接告警。
                log.warn("[BridgeHandler] Unknown command type {}, channel {}",
                        command.getCommandType(), ctx.channel());
                break;
        }
    }

    private void handleHandshake(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.wrappedBuffer(BridgeProtocolConstants.BRIDGE_HANDSHAKE_BYTES))
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("[BridgeHandler] Handshake response failed, channel {}", ctx.channel(), future.cause());
                        ctx.close();
                        return;
                    }
                    forwardingService.registerAdminChannel(ctx.channel());
                    log.info("[BridgeHandler] Handshake success, admin connected: {}", ctx.channel().remoteAddress());
                });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        forwardingService.unregisterAdminChannel(ctx.channel());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[BridgeHandler] exception on channel {}", ctx.channel(), cause);
        ctx.close();
    }
}
