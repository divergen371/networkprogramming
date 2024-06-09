package networkProgramming;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NetClockサーバーは現在の日付と時刻をクライアントに送信するシンプルなサーバーです。
 *
 * <p>
 * このサーバーはポート6000でリッスンし、クライアントが接続すると現在の日時を返します。
 * 各クライアント接続は並列に処理され、スレッドプールによって管理されます。
 * </p>
 */
public class NetClock {
    private static final Logger logger           = Logger.getLogger(NetClock.class.getName());
    private static final int    PORT             = 6000;
    private static final int    BACKLOG          = 300;
    private static final int    THREAD_POOL_SIZE = 10;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(
                THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT, BACKLOG)) {
            // 無限ループでクライアント接続を待機
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            logger.log(
                    Level.SEVERE,
                    "サーバーの起動中にエラーが発生しました",
                    e);
        } finally {
            shutdownExecutorService(executorService);
        }
    }

    /**
     * ExecutorServiceをシャットダウンします。
     *
     * @param executorService シャットダウンするExecutorService
     */
    private static void shutdownExecutorService(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (! executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (! executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.severe("ExecutorService did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * クライアント接続を処理するためのクラス。
 */
class ClientHandler implements Runnable {
    private static final Logger logger            = Logger.getLogger(
            ClientHandler.class.getName());
    private static final String RESPONSE_TEMPLATE = "\nHello, this is NetClock Server.\n%s\nThank you.\n";
    private final        Socket clientSocket;

    /**
     * クライアントハンドラを初期化します。
     *
     * @param clientSocket クライアントソケット
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (OutputStream outputStream = clientSocket.getOutputStream()) {

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
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.log(
                        Level.SEVERE,
                        "クライアントソケットのクローズ中にエラーが発生しました",
                        e);
            }
        }
    }
}
