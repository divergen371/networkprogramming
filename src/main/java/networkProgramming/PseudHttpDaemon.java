package networkProgramming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PseudHttpDaemon クラスは簡易的なHTTPサーバーを実装します。
 * 指定されたファイルをクライアントに送信します。
 */
public class PseudHttpDaemon {

    private static final Logger logger = Logger.getLogger(PseudHttpDaemon.class.getName());

    /**
     * メインメソッド。サーバーを開始し、指定されたファイルをクライアントに送信します。
     *
     * @param args コマンドライン引数。送信するファイルのパスを指定します。
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            logger.severe("使用法: java PseudHttpDaemon <ファイル名>");
            System.exit(1);
        }

        String filePath = args[0];
        Path file = Paths.get(filePath);
        if (! Files.exists(file) || ! Files.isRegularFile(file)) {
            logger.severe("ファイルが見つかりません: " + filePath);
            System.exit(1);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(8080, 300)) {
                logger.info("サーバーがポート8080で開始しました");

                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        executorService.submit(() -> handleClient(
                                socket,
                                file));
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "クライアント接続エラー", e);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "サーバーエラー。再起動します...", e);
                // 例外が発生した場合、数秒待ってから再試行
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * クライアントとの通信を処理します。
     *
     * @param socket クライアントソケット
     * @param file   送信するファイルのパス
     */
    private static void handleClient(Socket socket, Path file) {
        logger.info("接続要求: " + socket.getInetAddress().getHostName());

        try (OutputStream out = socket.getOutputStream();
                InputStream infile = Files.newInputStream(file)
        ) {

            // HTTP/1.1のレスポンスを作成
            String httpResponse = String.join(
                    "\r\n",
                    "HTTP/1.1 200 OK",
                    "Content-Type: " + Files.probeContentType(file),
                    "Content-Length: " + Files.size(file),
                    "Connection: close",
                    "",
                    ""
            );
            out.write(httpResponse.getBytes());

            // ファイルの内容を送信
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = infile.read(buf)) != - 1) {
                out.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "ファイル送信エラー", e);
        }
    }
}
