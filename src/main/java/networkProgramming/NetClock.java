package networkProgramming;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Logger;

public class NetClock {
    private static final Logger logger = Logger.getLogger(NetClock.class.getName());

    public static void main(String[] args) {
        try (ServerSocket servSock = new ServerSocket(6000, 300)) {
            //noinspection InfiniteLoopStatement
            while (true) {
                try (Socket sock = servSock.accept();
                        OutputStream out = sock.getOutputStream()) {

                    Date date = new Date();
                    String outstr = "\n" + "Hello, this is NetClock Server." + "\n" + date + "\n" + "Thank you." + "\n";
                    out.write(outstr.getBytes());
                    out.write('\n');
                    out.flush();

                    // Log the request and response
                    logger.info("Received a connection from " + sock.getInetAddress() + ":" + sock.getPort());
                    logger.info("Sent response: " + outstr);

                } catch (IOException e) {
                    logger.severe("クライアント接続の処理中にエラーが発生しました: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("サーバーの起動中にエラーが発生しました: " + e.getMessage());
            System.exit(1);
        }
    }
}