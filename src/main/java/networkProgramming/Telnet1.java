package networkProgramming;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Telnet1 {
    private static final Logger              logger = Logger.getLogger(Telnet1.class.getName());
    private              OutputStream        serverOutput;
    private              BufferedInputStream serverInput;
    private              Socket              serverSocket;

    /**
     * メインメソッド。コマンドライン引数を使用してサーバーに接続します。
     *
     * @param args コマンドライン引数。args[0]にはホスト名またはIPアドレス、args[1]にはポート番号が含まれます。
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            logger.severe("Usage: java Telnet1 <host> <port>");
            System.exit(1);
        }

        try {
            var t = new Telnet1();
            t.openConnection(args[0], Integer.parseInt(args[1]));
            t.mainProc();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error: ", e);
            System.exit(1);
        }
    }

    /**
     * アドレスとポート番号からソケットとストリームを作成します。
     *
     * @param host 接続するホストのアドレス
     * @param port 接続するポート番号
     * @throws IOException          ソケットの作成に失敗した場合
     * @throws UnknownHostException ホストが見つからない場合
     */
    public void openConnection(String host, int port)
            throws IOException, UnknownHostException {
        serverSocket = new Socket(host, port);
        serverOutput = serverSocket.getOutputStream();
        serverInput = new BufferedInputStream(serverSocket.getInputStream());

        logger.info("Connected to server: " + host + " on port: " + port);
    }

    /**
     * ソケットと標準入出力ストリームを接続し、それぞれのデータの転送を行うスレッドを作成および開始します。
     */
    public void mainProc() {
        try {
            Thread inputThread = createAndStartThread(System.in, serverOutput);
            Thread outputThread = createAndStartThread(serverInput, System.out);

            logger.info("Data transfer threads started");

            // Wait for threads to finish
            inputThread.join();
            outputThread.join();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error: ", e);
            System.exit(1);
        } finally {
            closeResources();
        }
    }

    private Thread createAndStartThread(InputStream src, OutputStream dest) {
        Thread thread = new Thread(() -> transferData(src, dest));
        thread.start();
        return thread;
    }

    private void transferData(InputStream src, OutputStream dest) {
        byte[] buffer = new byte[1024];
        try {
            while (true) {
                int n = src.read(buffer);
                if (n > 0) {
                    dest.write(buffer, 0, n);
                } else if (n == - 1) {
                    break; // End of stream
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Data transfer error", e);
            System.exit(1);
        }
    }

    private void closeResources() {
        try {
            if (serverOutput != null) {
                serverOutput.close();
            }
            if (serverInput != null) {
                serverInput.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing resources: ", e);
        }
    }
}
