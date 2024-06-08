package networkProgramming;

import java.io.InputStream;
import java.net.Socket;

public class ReadNet {
    public static void main(String[] args) {
        byte[] buff = new byte[1024];

        try (Socket readSock = new Socket(args[0], Integer.parseInt(args[1]));
                InputStream inputStr = readSock.getInputStream()
        ) {

            int n;
            while ((n = inputStr.read(buff)) != - 1) {
                System.out.write(buff, 0, n);
            }
        } catch (Exception e) {
            System.err.println("ネットワークエラー: " + e.getMessage());
            System.exit(1);
        }
    }
}