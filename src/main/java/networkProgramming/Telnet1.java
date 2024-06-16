package networkProgramming;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Telnet1 {
    public    OutputStream        serverOutput;
    public    BufferedInputStream serverInput;
    // prepare for server socket
    protected Socket              serverSocket;

    /**
     * メインメソッド。コマンドライン引数を使用してサーバーに接続します。
     *
     * @param args コマンドライン引数。args[0]にはホスト名またはIPアドレス、args[1]にはポート番号が含まれます。
     */
    public static void main(String[] args) {
        try {
            Telnet1 t = new Telnet1();
            t.openConnection(args[0], Integer.parseInt(args[1]));
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
    public void main_proc() throws IOException {
        try {
            StreamConnector stdin_to_socket = new StreamConnector(
                    System.in,
                    serverOutput);
            StreamConnector socket_to_stdout = new StreamConnector(
                    serverInput,
                    System.out);

            // Thread create
            Thread inputThread = new Thread(stdin_to_socket);
            Thread outputThread = new Thread(socket_to_stdout);

            // Thread start
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
class StreamConnector implements Runnable {
    private InputStream  src  = null;
    private OutputStream dest = null;

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
        byte[] buff = new byte[1024];
        while (true) {
            try {
                int n = src.read(buff);
                if (n > 0) {
                    dest.write(buff, 0, n);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("エラー: " + e);
                System.exit(1);
            }
        }
    }
}