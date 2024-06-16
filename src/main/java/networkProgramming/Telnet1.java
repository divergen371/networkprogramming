package networkProgramming;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Telnet1 {
    private OutputStream        serverOutput;
    private BufferedInputStream serverInput;
    // prepare for server socket
    private Socket              serverSocket;

    /**
     * メインメソッド。コマンドライン引数を使用してサーバーに接続します。
     *
     * @param args コマンドライン引数。args[0]にはホスト名またはIPアドレス、args[1]にはポート番号が含まれます。
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Telnet1 <host> <port>");
            System.exit(1);
        }

        try {
            var t = new Telnet1();
            t.openConnection(args[0], Integer.parseInt(args[1]));
            t.mainProc();
        } catch (Exception e) {
            System.err.println("エラー: " + e);
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
    }

    /**
     * ソケットと標準入出力ストリームを接続し、それぞれのデータの転送を行うスレッドを作成および開始します。
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    public void mainProc() throws IOException {
        try (var serverOutput = this.serverOutput; var serverInput = this.serverInput) {
            var stdinToSocket = new StreamConnector(System.in, serverOutput);
            var socketToStdout = new StreamConnector(serverInput, System.out);

            // Thread create and start using lambdas for brevity
            var inputThread = new Thread(stdinToSocket);
            var outputThread = new Thread(socketToStdout);

            inputThread.start();
            outputThread.start();
        } catch (Exception e) {
            System.err.println("エラー: " + e);
            System.exit(1);
        }
    }
}

/**
 * 入力ストリームから出力ストリームへデータを転送するクラスです。
 * Runnableインターフェースを実装し、別スレッドでの実行をサポートします。
 */
@SuppressWarnings("InfiniteLoopStatement")
class StreamConnector implements Runnable {
    private final InputStream  src;
    private final OutputStream dest;

    /**
     * 指定された入力ストリームと出力ストリームでStreamConnectorを初期化します。
     *
     * @param src  データを読み取る入力ストリーム
     * @param dest データを書き込む出力ストリーム
     */
    public StreamConnector(InputStream src, OutputStream dest) {
        this.src = src;
        this.dest = dest;
    }

    /**
     * 入力ストリームからバッファを介してデータを読み取り、出力ストリームに書き込みます。
     * エラーが発生した場合は、スタックトレースを出力し、システムを終了します。
     */
    @Override
    public void run() {
        var buffer = new byte[1024];
        try (var src = this.src; var dest = this.dest) {
            while (true) {
                int n = src.read(buffer);
                if (n > 0) {
                    dest.write(buffer, 0, n);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("エラー: " + e);
            System.exit(1);
        }
    }
}
