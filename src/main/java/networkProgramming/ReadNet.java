package networkProgramming;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * ReadNetクラスは指定されたホストとポートに接続し、ソケットからデータを読み取り、標準出力に書き込みます。
 */
public class ReadNet {

    /**
     * ネットワーク読み取り操作を実行するメインメソッド。
     *
     * @param args コマンドライン引数。args[0]はホスト名、args[1]はポート番号です。
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("使用方法: java ReadNet <ホスト名> <ポート>");
            System.exit(1);
        }

        String hostname = args[0];
        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("無効なポート番号: " + args[1]);
            System.exit(1);
            return; // このreturnは一部のIDEでの到達可能性の警告を回避するための冗長なものです
        }

        byte[] buffer = new byte[1024];

        try (Socket socket = new Socket(hostname, port);
                InputStream inputStream = socket.getInputStream()
        ) {

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != - 1) {
                System.out.write(buffer, 0, bytesRead);
            }

        } catch (UnknownHostException e) {
            System.err.println("未知のホスト: " + hostname);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ネットワークエラー: " + e.getMessage());
            System.exit(1);
        }
    }
}
