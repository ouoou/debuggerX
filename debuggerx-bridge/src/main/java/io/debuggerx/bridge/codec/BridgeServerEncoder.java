package io.debuggerx.bridge.codec;

import io.debuggerx.bridge.model.BridgeForwardFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * Bridge 出站编码器。
 *
 * @author ouwu
 */
public class BridgeServerEncoder extends MessageToByteEncoder<BridgeForwardFrame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BridgeForwardFrame frame, ByteBuf out) {
        encodeForwardFrame(frame, out);
    }

    private void encodeForwardFrame(BridgeForwardFrame frame, ByteBuf out) {
        byte[] payload = frame.getPayload();
        int payloadLength = payload == null ? 0 : payload.length;

        String rawClientIp = frame.getClientIp();
        String clientIp = rawClientIp == null || rawClientIp.isEmpty() ? "unknown" : rawClientIp;
        byte[] clientIpBytes = clientIp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int clientLen = clientIpBytes.length;
        if (clientLen > 65535) {
            throw new IllegalArgumentException("clientIp too long: " + clientLen);
        }

        // totalLen = direction(1) + clientLen(2) + clientIp(clientLen) + payloadLen(4) + payload(payloadLen)
        int totalLen = 1 + 2 + clientLen + 4 + payloadLength;
        out.writeInt(totalLen);
        out.writeByte(frame.getDirection());
        out.writeShort(clientLen);
        if (clientLen > 0) {
            out.writeBytes(clientIpBytes);
        }
        out.writeInt(payloadLength);
        if (payloadLength > 0) {
            out.writeBytes(payload);
        }
    }
}
