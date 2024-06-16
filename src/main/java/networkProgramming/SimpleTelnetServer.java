package networkProgramming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleTelnetServer {
    static final byte IAC = (byte) 255;
    static final byte DONT = (byte) 254;
    static final byte DO = (byte) 253;
    static final byte WONT = (byte) 252;
    static final byte WILL = (byte) 251;

    private static final Logger logger = Logger.getLogger(SimpleTelnetServer.class.getName());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(23)) {
            logger.info("Telnet server started on port 23");
            //noinspection InfiniteLoopStatement
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                        InputStream in = clientSocket.getInputStream();
                        OutputStream out = clientSocket.getOutputStream()
                ) {

                    // 提案するオプションのネゴシエーション
                    negotiateOption(out, WILL, (byte) 1); // エコーオプション (例)
                    negotiateOption(out, DO, (byte) 3);   // サプレッサーゴアヘッド (例)

                    // 最初のメッセージを送信
                    out.write("Welcome to Simple Telnet Server\r\n".getBytes());
                    out.flush();

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != - 1) {
                        int i = 0;
                        while (i < bytesRead) {
                            if (buffer[i] == IAC) {
                                if (i + 2 < bytesRead) {
                                    byte optionCommand = buffer[i + 1];
                                    byte optionCode = buffer[i + 2];
                                    logNegotiation(optionCommand, optionCode);
                                    if (optionCommand == DO) {
                                        out.write(new byte[]{
                                                IAC,
                                                WONT,
                                                optionCode
                                        });
                                    } else if (optionCommand == WILL) {
                                        out.write(new byte[]{
                                                IAC,
                                                DONT,
                                                optionCode
                                        });
                                    }
                                    i += 3;
                                } else {
                                    break; // wait for more data
                                }
                            } else {
                                out.write(buffer[i]);
                                i++;
                            }
                        }
                        out.flush();
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Client connection error: ", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server error: ", e);
        }
    }

    /**
     * 提案するオプションのネゴシエーションを行います。
     *
     * @param out           出力ストリーム
     * @param optionCommand オプションコマンド (WILL, DO)
     * @param optionCode    オプションコード
     * @throws IOException 入出力エラーが発生した場合
     */
    private static void negotiateOption(OutputStream out, byte optionCommand, byte optionCode)
            throws IOException {
        out.write(new byte[]{IAC, optionCommand, optionCode});
        logNegotiation(optionCommand, optionCode);
        out.flush(); // 応答を即座に送信
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
