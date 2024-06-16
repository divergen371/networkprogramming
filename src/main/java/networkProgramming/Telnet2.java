package networkProgramming;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Telnet2 {
    static final         int                 DEFAULT_TELNET_PORT = 23;
    static final         byte                IAC                 = (byte) 255;
    static final         byte                DONT                = (byte) 254;
    static final         byte                DO                  = (byte) 253;
    static final         byte                WONT                = (byte) 252;
    static final         byte                WILL                = (byte) 251;
    private static final Logger              logger              = Logger.getLogger(
            Telnet2.class.getName());
    private              OutputStream        serverOutput;
    private              BufferedInputStream serverInput;
    private              Socket              serverSocket;

    /**
     * Telnetオプションの交渉を行います。
     *
     * @param in  サーバーからの入力ストリーム
     * @param out サーバーへの出力ストリーム
     * @throws IOException 入出力エラーが発生した場合
     */
    static void negotiation(BufferedInputStream in, OutputStream out)
            throws IOException {
        byte[] buff = new byte[3];
        while (true) {
            in.mark(buff.length);
            if (in.available() >= buff.length) {
                int readBytes = in.read(buff); // 読み取ったバイト数を使用
                if (readBytes == - 1) {
                    break; // ストリームの終わりに達した場合
                }
                if (buff[0] != IAC) {
                    in.reset();
                    return;
                } else {
                    byte optionCommand = buff[1];
                    byte optionCode = buff[2];
                    logNegotiation(optionCommand, optionCode);
                    if (optionCommand == DO) {
                        out.write(new byte[]{IAC, WONT, optionCode});
                    } else if (optionCommand == WILL) {
                        out.write(new byte[]{IAC, DONT, optionCode});
                    }
                    out.flush(); // 応答をフラッシュして送信
                }
            }
        }
    }

    /**
     * メインメソッド。コマンドライン引数を使用してサーバーに接続します。
     *
     * @param args コマンドライン引数。args[0]にはホスト名またはIPアドレス、args[1]にはポート番号が含まれます。
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            logger.severe("Usage: java Telnet2 <host> <port>");
            System.exit(1);
        }

        try {
            var t = new Telnet2();
            t.openConnection(args[0], Integer.parseInt(args[1]));
            t.mainProc();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error: ", e);
            System.exit(1);
        }
    }

    /**
     * ネゴシエーションの内容をログに記録します。
     *
     * @param optionCommand オプションコマンド (DO, DONT, WILL, WONT)
     * @param optionCode    オプションコード
     */
    private static void logNegotiation(byte optionCommand, byte optionCode) {
        String commandName = switch (optionCommand) {
            case DO -> "DO";
            case DONT -> "DONT";
            case WILL -> "WILL";
            case WONT -> "WONT";
            default -> "UNKNOWN";
        };
        logger.info("Negotiation command: " + commandName + " " + optionCode);
    }

    /**
     * アドレスとポート番号からソケットとストリームを作成し、接続します。
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

        if (port == DEFAULT_TELNET_PORT) {
            negotiation(serverInput, serverOutput);
        }

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

            // スレッドの終了を待機
            inputThread.join();
            outputThread.join();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error: ", e);
            System.exit(1);
        } finally {
            closeResources();
        }
    }

    /**
     * 入力ストリームから出力ストリームへデータを転送するスレッドを作成し開始します。
     *
     * @param src  データを読み取る入力ストリーム
     * @param dest データを書き込む出力ストリーム
     * @return 作成されたスレッド
     */
    private Thread createAndStartThread(InputStream src, OutputStream dest) {
        Thread thread = new Thread(() -> transferData(src, dest));
        thread.start();
        return thread;
    }

    /**
     * 入力ストリームから出力ストリームへデータを転送します。
     *
     * @param src  データを読み取る入力ストリーム
     * @param dest データを書き込む出力ストリーム
     */
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

    /**
     * サーバーとの接続をクローズし、リソースを解放します。
     */
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
