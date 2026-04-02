package io.debuggerx.bridge.server;

import io.debuggerx.bridge.codec.BridgeServerDecoder;
import io.debuggerx.bridge.codec.BridgeServerEncoder;
import io.debuggerx.bridge.handler.BridgeServerHandler;
import io.debuggerx.common.config.DebuggerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * Bridge TCP Server。
 *
 * @author ouwu
 */
@Slf4j
public class BridgeServer {

    private final DebuggerConfig config;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final BridgeServerHandler bridgeServerHandler;

    public BridgeServer(DebuggerConfig config) {
        this.config = config;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.bridgeServerHandler = new BridgeServerHandler();
    }

    public void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new BridgeServerDecoder())
                                .addLast(new BridgeServerEncoder())
                                .addLast(bridgeServerHandler);
                    }
                });
        ChannelFuture future = bootstrap.bind(config.getBridgeServerPort()).sync();
        future.channel().closeFuture()
                .addListener(f -> log.info("[BridgeServer] bridge server closed, port={}", config.getBridgeServerPort()));
        log.info("[BridgeServer] started, listening on {}", config.getBridgeServerPort());
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        log.info("[BridgeServer] shutdown completed");
    }
}
