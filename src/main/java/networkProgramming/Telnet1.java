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
    private static final Logger logger = Logger.getLogger(Telnet1.class.getName());

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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "エラー: ", e);
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
    public void openConnection(String host, int port) throws IOException, UnknownHostException {
        try (Socket serverSocket = new Socket(host, port);
                OutputStream serverOutput = serverSocket.getOutputStream();
                BufferedInputStream serverInput = new BufferedInputStream(serverSocket.getInputStream())) {

            logger.info("Connected to server: " + host + " on port: " + port);

            // Transfer data between System.in and serverOutput, and between serverInput and System.out
            mainProc(serverOutput, serverInput);
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Unknown host: " + host, e);
            throw e;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error while connecting to host: " + host, e);
            throw e;
        }
    }

    /**
     * ソケットと標準入出力ストリームを接続し、それぞれのデータの転送を行うスレッドを作成および開始します。
     *
     * @param serverOutput サーバーへの出力ストリーム
     * @param serverInput サーバーからの入力ストリーム
     */
    public void mainProc(OutputStream serverOutput, InputStream serverInput) {
        try {
            // Thread create and start using lambdas for brevity
            var inputThread = new Thread(() -> transferData(System.in, serverOutput));
            var outputThread = new Thread(() -> transferData(serverInput, System.out));

            inputThread.start();
            outputThread.start();
            logger.info("Data transfer threads started");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "エラー: ", e);
            System.exit(1);
        }
    }

    /**
     * 入力ストリームから出力ストリームへデータを転送します。
     *
     * @param src  データを読み取る入力ストリーム
     * @param dest データを書き込む出力ストリーム
     */
    private void transferData(InputStream src, OutputStream dest) {
        var buffer = new byte[1024];
        try (src; dest) {
            while (true) {
                int n = src.read(buffer);
                if (n > 0) {
                    dest.write(buffer, 0, n);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Data transfer error", e);
            System.exit(1);
        }
    }
}
