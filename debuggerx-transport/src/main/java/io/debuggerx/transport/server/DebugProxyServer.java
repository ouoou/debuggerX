package io.debuggerx.transport.server;

import io.debuggerx.common.config.DebuggerConfig;
import io.debuggerx.common.enums.ConnectionType;
import io.debuggerx.common.utils.AssertUtils;
import io.debuggerx.transport.codec.JdwpPacketDecoder;
import io.debuggerx.transport.codec.JdwpPacketEncoder;
import io.debuggerx.transport.handler.BridgeForwardClientToBridgeHandler;
import io.debuggerx.transport.handler.BridgeForwardJvmToBridgeHandler;
import io.debuggerx.transport.handler.DebugProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * debug代理service
 *
 * @author ouwu
 */
@Slf4j
public class DebugProxyServer {
    private final DebuggerConfig config;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final NioEventLoopGroup jvmClientEventGroup;
    private final DebugProxyHandler debugProxyHandler;
    
    public DebugProxyServer(DebuggerConfig config) {
        this.config = config;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.jvmClientEventGroup = new NioEventLoopGroup();
        this.debugProxyHandler = new DebugProxyHandler(ConnectionType.DEBUGGER_PROXY);
    }

    public void start() throws InterruptedException {
        // 连接JVM
        ChannelFuture jvmClientFuture = this.startJvmClient();

        // 启动debug代理服务
        this.startDebuggerProxyServer();

        try {
            AssertUtils.notNull(jvmClientFuture, "JvmClient is null");
            jvmClientFuture.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }


    public ChannelFuture startJvmClient() throws InterruptedException {
        // 连接JVM
        Bootstrap jvmClient = createClientBootstrap(jvmClientEventGroup);
        ChannelFuture jvmClientFuture = jvmClient.connect(config.getJvmServerHost(), config.getJvmServerPort()).sync();

        // 只监听被调试程序的连接状态
        jvmClientFuture.channel().closeFuture().addListener(future -> log.info("[JvmClient] disconnected, shutting down jvm client..."));
         return jvmClientFuture;
    }

    public void startDebuggerProxyServer() throws InterruptedException {
        // 启动调试器监听服务器
        ServerBootstrap debuggerServer = createServerBootstrap();
        ChannelFuture debuggerFuture = debuggerServer.bind(config.getDebuggerProxyPort()).sync();

        // 只监听被调试程序的连接状态
        debuggerFuture.channel().closeFuture().addListener(future -> log.info("[DebuggerProxy] debugger disconnected, shutting down proxy server..."));
    }
    
    private ServerBootstrap createServerBootstrap() {
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new JdwpPacketDecoder())
                                .addLast(new BridgeForwardClientToBridgeHandler())
                                .addLast(new JdwpPacketEncoder())
                                .addLast(debugProxyHandler);
                    }
                });
    }
    
    private Bootstrap createClientBootstrap(NioEventLoopGroup jvmClientEventGroup) {
        return new Bootstrap()
                .group(jvmClientEventGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_LINGER, 0)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new JdwpPacketDecoder())
                                .addLast(new JdwpPacketEncoder())
                                .addLast(new DebugProxyHandler(ConnectionType.JVM_SERVER))
                                .addLast(new BridgeForwardJvmToBridgeHandler());
                    }
                });
    }
    
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        jvmClientEventGroup.shutdownGracefully();
        log.info("[DebugProxyServer]Debug proxy server shutdown");
    }
} 