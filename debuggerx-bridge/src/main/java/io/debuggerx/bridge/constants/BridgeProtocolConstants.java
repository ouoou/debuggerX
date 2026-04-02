package io.debuggerx.bridge.constants;

import java.nio.charset.StandardCharsets;

/**
 * Bridge 协议常量定义。
 *
 * @author ouwu
 */
public final class BridgeProtocolConstants {

    private BridgeProtocolConstants() {
    }

    /**
     * admin-client 与 bridge 的握手字符串。
     */
    public static final String BRIDGE_HANDSHAKE = "debuggerX-bridge-Handshake";
    /**
     * 握手字节数组（ASCII）。
     */
    public static final byte[] BRIDGE_HANDSHAKE_BYTES = BRIDGE_HANDSHAKE.getBytes(StandardCharsets.US_ASCII);

    /**
     * 最大 payload 长度，16MB。
     */
    public static final int MAX_PAYLOAD_LENGTH = 16 * 1024 * 1024;
    /**
     * 最大来源地址长度，512 字节。
     */
    public static final int MAX_ADDRESS_LENGTH = 512;

    /**
     * 方向：服务端 -> JVM。
     */
    public static final byte DIRECTION_SERVER_TO_JVM = 0x01;
    /**
     * 方向：JVM -> 服务端。
     */
    public static final byte DIRECTION_JVM_TO_SERVER = 0x02;

    /**
     * 来源：debuggerX server 侧。
     */
    public static final byte SOURCE_PROXY_SERVER = 0x01;
    /**
     * 来源：目标 JVM 侧。
     */
    public static final byte SOURCE_TARGET_JVM = 0x02;

    /**
     * 命令：握手（仅内部对象使用，线协议握手为固定字符串）。
     */
    public static final byte CMD_HANDSHAKE = 0x01;
}
