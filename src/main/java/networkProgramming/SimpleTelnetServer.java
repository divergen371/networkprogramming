package networkProgramming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SimpleTelnetServer クラスはシンプルなTelnetサーバーを実装します。
 * クライアントからの接続を受け入れ、ネゴシエーションを行い、データの送受信を処理します。
 */
public class SimpleTelnetServer {
    static final byte IAC  = (byte) 255;
    static final byte DONT = (byte) 254;
    static final byte DO   = (byte) 253;
    static final byte WONT = (byte) 252;
    static final byte WILL = (byte) 251;

    private static final Logger logger = Logger.getLogger(SimpleTelnetServer.class.getName());

    /**
     * メインメソッド。サーバーソケットを作成し、クライアント接続を待ち受けます。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(23)) {
            logger.info("Telnet server started on port 23");
            //noinspection InfiniteLoopStatement
            while (true) {
                handleClientConnection(serverSocket);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server error: ", e);
        }
    }

    /**
     * クライアント接続を処理します。
     *
     * @param serverSocket サーバーソケット
     */
    private static void handleClientConnection(ServerSocket serverSocket) {
        try (Socket clientSocket = serverSocket.accept();
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream()
        ) {

            negotiateOptions(out);
            sendWelcomeMessage(out);
            transferData(in, out);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Client connection error: ", e);
        }
    }

    /**
     * クライアントとのオプションネゴシエーションを行います。
     *
     * @param out クライアントへの出力ストリーム
     * @throws IOException 入出力エラーが発生した場合
     */
    private static void negotiateOptions(OutputStream out) throws IOException {
        negotiateOption(out, WILL, (byte) 1); // エコーオプション (例)
        negotiateOption(out, DO, (byte) 3);   // サプレッサーゴアヘッド (例)
    }

    /**
     * 単一のオプションネゴシエーションを行います。
     *
     * @param out           クライアントへの出力ストリーム
     * @param optionCommand オプションコマンド (WILL, DO)
     * @param optionCode    オプションコード
     * @throws IOException 入出力エラーが発生した場合
     */
    private static void negotiateOption(OutputStream out, byte optionCommand, byte optionCode)
            throws IOException {
        out.write(new byte[]{IAC, optionCommand, optionCode});
        logNegotiation(optionCommand, optionCode);
        out.flush();
    }

    /**
     * クライアントにウェルカムメッセージを送信します。
     *
     * @param out クライアントへの出力ストリーム
     * @throws IOException 入出力エラーが発生した場合
     */
    private static void sendWelcomeMessage(OutputStream out)
            throws IOException {
        out.write("Welcome to Simple Telnet Server\r\n".getBytes());
        out.flush();
    }

    /**
     * クライアントからのデータを受信し、処理します。
     *
     * @param in  クライアントからの入力ストリーム
     * @param out クライアントへの出力ストリーム
     * @throws IOException 入出力エラーが発生した場合
     */
    private static void transferData(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != - 1) {
            handleData(out, buffer, bytesRead);
        }
    }

    /**
     * 受信したデータを処理し、必要に応じてIACシーケンスを処理します。
     *
     * @param out       クライアントへの出力ストリーム
     * @param buffer    受信したデータのバッファ
     * @param bytesRead 読み取ったバイト数
     * @throws IOException 入出力エラーが発生した場合
     */
    private static void handleData(OutputStream out, byte[] buffer, int bytesRead)
            throws IOException {
        int i = 0;
        while (i < bytesRead) {
            if (buffer[i] == IAC) {
                i = handleIAC(out, buffer, bytesRead, i);
            } else {
                out.write(buffer[i]);
                i++;
            }
        }
        out.flush();
    }

    /**
     * IAC (Interpret As Command) シーケンスを処理します。
     *
     * @param out       クライアントへの出力ストリーム
     * @param buffer    受信したデータのバッファ
     * @param bytesRead 読み取ったバイト数
     * @param i         現在のバッファ位置
     * @return 処理後のバッファ位置
     * @throws IOException 入出力エラーが発生した場合
     */
    private static int handleIAC(OutputStream out, byte[] buffer, int bytesRead, int i)
            throws IOException {
        if (i + 2 < bytesRead) {
            byte optionCommand = buffer[i + 1];
            byte optionCode = buffer[i + 2];
            logNegotiation(optionCommand, optionCode);
            if (optionCommand == DO) {
                out.write(new byte[]{IAC, WONT, optionCode});
            } else if (optionCommand == WILL) {
                out.write(new byte[]{IAC, DONT, optionCode});
            }
            i += 3;
        }
        return i;
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
        logger.info("Received negotiation command: " + commandName + " " + optionCode);
    }
}
