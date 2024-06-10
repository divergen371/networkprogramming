package networkProgramming;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PseudHttpDaemon {

    private static final Logger logger = Logger.getLogger(PseudHttpDaemon.class.getName());

    public static void main(String[] args) {
        if (args.length != 1) {
            logger.severe("Usage: java PseudHttpDaemon <filename>");
            System.exit(1);
        }

        String filePath = args[0];
        Path file = Paths.get(filePath);
        if (! Files.exists(file) || ! Files.isRegularFile(file)) {
            logger.severe("File not found: " + filePath);
            System.exit(1);
        }

        new PseudHttpDaemon().startServer(8080, file);
    }

    private void startServer(int port, Path file) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                     ch.pipeline().addLast(new HttpRequestDecoder());
                     ch.pipeline().addLast(new HttpObjectAggregator(65536));
                     ch.pipeline().addLast(new HttpResponseEncoder());
                     ch.pipeline().addLast(new ChunkedWriteHandler());
                     ch.pipeline().addLast(new HttpServerHandler(file));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            logger.info("Server started on port: " + port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Server interrupted", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final Path file;

        public HttpServerHandler(Path file) {
            this.file = file;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            logger.info("Client connected: " + ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.info("Client disconnected: " + ctx.channel()
                                                     .remoteAddress());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            logger.info("HTTP request received: " + request.uri());
            try {
                byte[] content = Files.readAllBytes(file);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(content));
                response.headers().set(
                        HttpHeaderNames.CONTENT_TYPE,
                        Files.probeContentType(file));
                response.headers().set(
                        HttpHeaderNames.CONTENT_LENGTH,
                        response.content().readableBytes());
                response.headers().set(
                        HttpHeaderNames.CONNECTION,
                        HttpHeaderValues.KEEP_ALIVE);

                ctx.writeAndFlush(response)
                   .addListener(ChannelFutureListener.CLOSE);
                logger.info("HTTP response sent for request: " + request.uri());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "File read error", e);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR);
                ctx.writeAndFlush(response)
                   .addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.log(Level.SEVERE, "Handler error", cause);
            ctx.close();
        }
    }
}
