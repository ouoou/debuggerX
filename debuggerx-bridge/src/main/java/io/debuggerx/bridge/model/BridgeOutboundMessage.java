package io.debuggerx.bridge.model;

/**
 * Bridge 统一出站模型接口。
 *
 * @author ouwu
 */
public interface BridgeOutboundMessage {

    /**
     * 返回统一出站模型类型。
     *
     * @return 消息类型
     */
    BridgeOutboundMessageType getOutboundMessageType();
}
