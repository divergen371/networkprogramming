package networkProgramming;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NetClockサーバーは現在の日付と時刻をクライアントに送信するシンプルなサーバーです。
 *
 * <p>
 * このサーバーはポート6000でリッスンし、クライアントが接続すると現在の日時を返します。
 * </p>
 */
public class NetClock {
    private static final Logger logger            = Logger.getLogger(NetClock.class.getName());
    private static final int    PORT              = 6000;
    private static final int    BACKLOG           = 300;
    private static final String RESPONSE_TEMPLATE = "\nHello, this is NetClock Server.\n%s\nThank you.\n";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT, BACKLOG)) {
            // 無限ループでクライアント接続を待機
            //noinspection InfiniteLoopStatement
            while (true) {
                handleClientConnection(serverSocket);
            }
        } catch (IOException e) {
            logger.log(
                    Level.SEVERE,
                    "サーバーの起動中にエラーが発生しました",
                    e);
            System.exit(1);
        }
    }

    /**
     * クライアントの接続を処理します。
     *
     * @param serverSocket サーバーソケット
     */
    private static void handleClientConnection(ServerSocket serverSocket) {
        try (Socket clientSocket = serverSocket.accept();
                OutputStream outputStream = clientSocket.getOutputStream()
        ) {

            String response = String.format(RESPONSE_TEMPLATE, new Date());
            outputStream.write(response.getBytes());
            outputStream.write('\n');
            outputStream.flush();

            // リクエストとレスポンスをログに記録
            logger.info("Received a connection from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            logger.info("Sent response: " + response);

        } catch (IOException e) {
            logger.log(
                    Level.SEVERE,
                    "クライアント接続の処理中にエラーが発生しました",
                    e);
        }
    }
}
