package io.debuggerx.bridge.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.ByteBuffer;

/**
 * Bridge 转发帧对象。
 *
 * @author ouwu
 */
@Data
@AllArgsConstructor
public class BridgeForwardFrame implements BridgeOutboundMessage {
    /**
     * 传输方向。
     */
    private byte direction;
    /**
     * debugger 客户端 IP（host）。
     */
    private String clientIp;
    /**
     * 原始 payload。
     */
    private byte[] payload;

    /**
     * 将当前对象序列化为协议转发帧的字节数组（大端）。
     *
     * <pre>
     * totalLen(4) + direction(1) + clientLen(2) + clientIp(clientLen) + payloadLen(4) + payload(payloadLen)
     * </pre>
     *
     * 其中：`totalLen = 1 + 2 + clientIpLen + 4 + payloadLen = 7 + clientIpLen + payloadLen`。
     *
     * @return 协议字节数组
     */
    public byte[] toByteArray() {
        byte[] rawPayload = payload == null ? new byte[0] : payload;
        int payloadLen = rawPayload.length;

        String rawClientIp = clientIp == null || clientIp.isEmpty() ? "unknown" : clientIp;
        byte[] clientIpBytes = rawClientIp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int clientIpLen = clientIpBytes.length;
        if (clientIpLen > 65535) {
            throw new IllegalArgumentException("clientIp too long: " + clientIpLen);
        }

        // totalLen = direction(1) + clientLen(2) + clientIp(clientIpLen) + payloadLen(4) + payload(payloadLen)
        int totalLen = 1 + 2 + clientIpLen + 4 + payloadLen;

        ByteBuffer buffer = ByteBuffer.allocate(4 + totalLen);
        buffer.putInt(totalLen);
        buffer.put(direction);
        buffer.putShort((short) clientIpLen);
        if (clientIpLen > 0) {
            buffer.put(clientIpBytes);
        }
        buffer.putInt(payloadLen);
        if (payloadLen > 0) {
            buffer.put(rawPayload);
        }
        return buffer.array();
    }

    @Override
    public BridgeOutboundMessageType getOutboundMessageType() {
        return BridgeOutboundMessageType.FORWARD_FRAME;
    }
}
