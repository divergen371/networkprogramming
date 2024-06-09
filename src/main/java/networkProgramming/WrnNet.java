package networkProgramming;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Optional;

/**
 * WrnNetは、指定されたホストとポートに接続し、
 * HTTP GETリクエストを送信し、サーバーのレスポンスを表示するシンプルなネットワーククライアントです。
 */
public class WrnNet {
    private static final int    BUFFER_SIZE           = 1024;
    private static final String USAGE_MESSAGE         = """
            使用方法: java WrnNet <ホスト> <ポート>
            --help または -h でこのメッセージを表示
            """;
    private static final String NETWORK_ERROR_MESSAGE = "ネットワークエラー: ";

    /**
     * WrnNetクライアントを実行するメインメソッド。
     *
     * @param args コマンドライン引数。args[0]がホスト、args[1]がポートを示します。
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            if (args.length == 1 && (args[0].equals("--help") || args[0].equals(
                    "-h"))) {
                System.out.println(USAGE_MESSAGE);
                System.exit(0);
            } else {
                System.err.println(USAGE_MESSAGE);
                System.exit(1);
            }
        }

        var host = args[0];
        var portOpt = parsePort(args[1]);

        portOpt.ifPresentOrElse(port -> {
            try (var socket = createSocket(host, port);
                    var inputStr = socket.getInputStream();
                    var outputStr = socket.getOutputStream()
            ) {

                sendHttpRequest(outputStr, host);
                handleServerResponse(inputStr);

            } catch (IOException e) {
                System.err.println(NETWORK_ERROR_MESSAGE + e.getMessage());
                System.exit(1);
            }
        }, () -> System.exit(1));
    }

    /**
     * 文字列からポート番号を解析します。
     *
     * @param portStr 文字列で表現されたポート番号。
     * @return 有効な場合は解析されたポート番号を含むOptional、無効な場合は空のOptionalを返します。
     */
    private static Optional<Integer> parsePort(String portStr) {
        try {
            return Optional.of(Integer.parseInt(portStr));
        } catch (NumberFormatException e) {
            System.err.println("無効なポート番号: " + portStr);
            return Optional.empty();
        }
    }

    /**
     * 指定されたホストとポートに接続するソケットを作成します。
     * ポートが443の場合はSSLSocketを使用します。
     *
     * @param host 接続するホスト名。
     * @param port 接続するポート番号。
     * @return 作成されたソケット。
     * @throws IOException ソケットの作成時にI/Oエラーが発生した場合。
     */
    private static Socket createSocket(String host, int port)
            throws IOException {
        if (port == 443) {
            var sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            return sslSocketFactory.createSocket(host, port);
        } else {
            return new Socket(host, port);
        }
    }

    /**
     * 指定されたホストにHTTP GETリクエストを送信します。
     *
     * @param outputStr リクエストを書き込むOutputStream。
     * @param host      リクエストに含めるホスト名。
     * @throws IOException リクエストの送信時にI/Oエラーが発生した場合。
     */
    private static void sendHttpRequest(OutputStream outputStr, String host)
            throws IOException {
        var request = buildHttpRequest(host);
        outputStr.write(request.getBytes());
        outputStr.flush();
    }

    /**
     * 指定されたホストのためのHTTP GETリクエスト文字列を構築します。
     *
     * @param host リクエストに含めるホスト名。
     * @return HTTP GETリクエスト文字列。
     */
    private static String buildHttpRequest(String host) {
        return """
                GET / HTTP/1.1\r
                Host: %s\r
                Connection: close\r
                \r
                """.formatted(host);
    }

    /**
     * サーバーのレスポンスを処理し、InputStreamから読み取りコンソールに書き込みます。
     *
     * @param inputStr サーバーのレスポンスを読み取るInputStream。
     * @throws IOException レスポンスの読み取り時にI/Oエラーが発生した場合。
     */
    private static void handleServerResponse(InputStream inputStr)
            throws IOException {
        var buff = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStr.read(buff)) != - 1) {
            System.out.write(buff, 0, bytesRead);
        }
    }
}
