package io.debuggerx.common.config;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/**
 * debug代理配置
 *
 * @author ouwu
 */
@Data
@Builder
@FieldNameConstants
public class DebuggerConfig {
    /**
     * 提供调试功能jvm服务地址
     */
    private String jvmServerHost;
    /**
     * 提供调试功能jvm服务端口
     */
    private int jvmServerPort;
    /**
     * 调试器代理端口
     */
    private int debuggerProxyPort;
    /**
     * Bridge 服务监听端口
     */
    private int bridgeServerPort;
    
    public static DebuggerConfig getDefault() {
        return DebuggerConfig.builder()
                .jvmServerHost("localhost")
                .jvmServerPort(5005)
                .debuggerProxyPort(55005)
                .bridgeServerPort(56005)
                .build();
    }

    @Override
    public String toString() {
        return "DebuggerConfig{" + "jvmServerPort=" + jvmServerPort +
                ", debuggerProxyPort=" + debuggerProxyPort +
                ", bridgeServerPort=" + bridgeServerPort +
                '}';
    }
}