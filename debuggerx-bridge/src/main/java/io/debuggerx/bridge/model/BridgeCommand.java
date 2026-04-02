package io.debuggerx.bridge.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Bridge 入站命令帧解码结果。
 *
 * <p>该类用于服务端接收 admin 发来的握手/心跳等命令。
 *
 * <p>注意：该类不参与 {@code BridgeOutboundMessage} 统一出站模型，避免与
 * 你要求的“删除 command 命令类型”产生耦合。</p>
 *
 * @author ouwu
 */
@Data
@AllArgsConstructor
public class BridgeCommand {

    /**
     * 命令类型。
     */
    private byte commandType;

    /**
     * 命令负载。
     */
    private byte[] payload;
}

