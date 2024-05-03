package networkProgramming;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteFile {
    public static void main(String[] args) {
        byte[] buff = new byte[1024];
        boolean cont = true;
        FileOutputStream outfile = null;
        int periodAscii =  '.';

        try {
            outfile = new FileOutputStream(args[0]);
        } catch (FileNotFoundException e) {
            System.err.println("ファイルがありません");
            System.exit(1);
        }

        while (cont) {
            try {
                int n = System.in.read(buff);
                System.out.write(buff, 0, n);
                if (buff[0] == periodAscii) {
                    cont = false;
                } else {
                    outfile.write(buff, 0, n);
                }
            } catch (Exception e) {
                System.exit(1);
            }
            try {
                outfile.close();
            } catch (IOException e) {
                System.err.println("ファイルクローズエラー");
                System.exit(1);
            }
        }
    }
}
