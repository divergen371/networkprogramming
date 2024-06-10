package networkProgramming;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.conscrypt.OpenSSLProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PseudHttp2Daemon {

    private static final Logger logger = Logger.getLogger(PseudHttp2Daemon.class.getName());

    public static void main(String[] args) {
        if (args.length != 1) {
            logger.severe("Usage: java PseudHttp2Daemon <filename>");
            System.exit(1);
        }

        String filePath = args[0];
        Path file = Paths.get(filePath);
        if (! Files.exists(file) || ! Files.isRegularFile(file)) {
            logger.severe("File not found: " + filePath);
            System.exit(1);
        }

        new PseudHttp2Daemon().startServer(
                8443,
                file); // 8443 is the default HTTPS port
    }

    private void startServer(int port, Path file) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // Conscryptセキュリティプロバイダの登録
            Security.addProvider(new OpenSSLProvider());

            SelfSignedCertificate ssc = new SelfSignedCertificate();
            SslContext sslCtx = SslContextBuilder.forServer(
                                                         ssc.certificate(),
                                                         ssc.privateKey())
                                                 .sslProvider(SslProvider.OPENSSL)
                                                 .ciphers(
                                                         Http2SecurityUtil.CIPHERS,
                                                         SupportedCipherSuiteFilter.INSTANCE)
                                                 .applicationProtocolConfig(new ApplicationProtocolConfig(
                                                         ApplicationProtocolConfig.Protocol.ALPN,
                                                         ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                                         ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                                         ApplicationProtocolNames.HTTP_2,
                                                         ApplicationProtocolNames.HTTP_1_1))
                                                 .build();

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                     ch.pipeline()
                       .addLast(new ApplicationProtocolNegotiationHandler(
                               ApplicationProtocolNames.HTTP_1_1) {
                           @Override
                           protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                               if (ApplicationProtocolNames.HTTP_2.equals(
                                       protocol)) {
                                   ctx.pipeline()
                                      .addLast(Http2FrameCodecBuilder.forServer()
                                                                     .build());
                                   ctx.pipeline()
                                      .addLast(new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                                          @Override
                                          protected void initChannel(Channel ch) {
                                              ch.pipeline()
                                                .addLast(new Http2ServerHandler(
                                                        file));
                                          }
                                      }));
                               } else {
                                   ctx.pipeline()
                                      .addLast(new HttpServerCodec());
                                   ctx.pipeline()
                                      .addLast(new HttpObjectAggregator(65536));
                                   ctx.pipeline()
                                      .addLast(new Http1ServerHandler(file));
                               }
                           }
                       });
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            logger.info("Server started on port: " + port);
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Server error", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static class Http2ServerHandler extends SimpleChannelInboundHandler<Http2HeadersFrame> {
        private final Path file;

        public Http2ServerHandler(Path file) {
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
        protected void channelRead0(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame) {
            logger.info("HTTP/2 request received: " + headersFrame.headers()
                                                                  .path());
            if (headersFrame.isEndStream()) {
                sendResponse(ctx);
            } else {
                ctx.pipeline()
                   .addLast(new SimpleChannelInboundHandler<Http2DataFrame>() {
                       @Override
                       protected void channelRead0(ChannelHandlerContext ctx, Http2DataFrame dataFrame) {
                           if (dataFrame.isEndStream()) {
                               sendResponse(ctx);
                           }
                       }

                       @Override
                       public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                           logger.log(Level.SEVERE, "Handler error", cause);
                           ctx.close();
                       }
                   });
            }
        }

        private void sendResponse(ChannelHandlerContext ctx) {
            try {
                byte[] content = Files.readAllBytes(file);
                Http2Headers headers = new DefaultHttp2Headers()
                        .status(HttpResponseStatus.OK.codeAsText())
                        .set(
                                HttpHeaderNames.CONTENT_TYPE,
                                Files.probeContentType(file))
                        .setInt(HttpHeaderNames.CONTENT_LENGTH, content.length);
                ctx.write(new DefaultHttp2HeadersFrame(headers));

                DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(
                        Unpooled.copiedBuffer(content),
                        true);
                ctx.writeAndFlush(dataFrame)
                   .addListener(ChannelFutureListener.CLOSE);
                logger.info("HTTP/2 response sent");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "File read error", e);
                Http2Headers headers = new DefaultHttp2Headers().status(
                        HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText());
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, true))
                   .addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.log(Level.SEVERE, "Handler error", cause);
            ctx.close();
        }
    }

    private static class Http1ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final Path file;

        public Http1ServerHandler(Path file) {
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
            logger.info("HTTP/1.1 request received: " + request.uri());
            try {
                byte[] content = Files.readAllBytes(file);
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                                        HttpResponseStatus.OK,
                                                                        Unpooled.copiedBuffer(
                                                                                content));
                response.headers()
                        .set(
                                HttpHeaderNames.CONTENT_TYPE,
                                Files.probeContentType(file));
                response.headers()
                        .set(
                                HttpHeaderNames.CONTENT_LENGTH,
                                response.content().readableBytes());
                response.headers()
                        .set(
                                HttpHeaderNames.CONNECTION,
                                HttpHeaderValues.KEEP_ALIVE);

                ctx.writeAndFlush(response)
                   .addListener(ChannelFutureListener.CLOSE);
                logger.info("HTTP/1.1 response sent for request: " + request.uri());
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
