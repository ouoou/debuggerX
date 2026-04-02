package io.debuggerx.bridge.codec;

import io.debuggerx.bridge.constants.BridgeProtocolConstants;
import io.debuggerx.bridge.model.BridgeCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Bridge 入站解码器。
 *
 * 握手前：接收固定握手字符串。
 * 握手后：接收 admin 命令帧（type + length + payload）。
 *
 * @author ouwu
 */
@Slf4j
public class BridgeServerDecoder extends ByteToMessageDecoder {

    private boolean handshakeCompleted;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!handshakeCompleted) {
            decodeHandshake(ctx, in, out);
            return;
        }
        decodeCommandFrame(ctx, in, out);
    }

    private void decodeHandshake(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < BridgeProtocolConstants.BRIDGE_HANDSHAKE_BYTES.length) {
            return;
        }
        byte[] handshake = new byte[BridgeProtocolConstants.BRIDGE_HANDSHAKE_BYTES.length];
        in.readBytes(handshake);
        if (!Arrays.equals(handshake, BridgeProtocolConstants.BRIDGE_HANDSHAKE_BYTES)) {
            log.warn("[BridgeDecoder] Invalid handshake from {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        handshakeCompleted = true;
        out.add(new BridgeCommand(BridgeProtocolConstants.CMD_HANDSHAKE, handshake));
    }

    private void decodeCommandFrame(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 5) {
            return;
        }
        in.markReaderIndex();
        byte type = in.readByte();
        int length = in.readInt();
        if (length < 0 || length > BridgeProtocolConstants.MAX_PAYLOAD_LENGTH) {
            log.warn("[BridgeDecoder] Invalid command length {}, channel {}", length, ctx.channel());
            ctx.close();
            return;
        }
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        byte[] payload = new byte[length];
        in.readBytes(payload);
        out.add(new BridgeCommand(type, payload));
    }
}
