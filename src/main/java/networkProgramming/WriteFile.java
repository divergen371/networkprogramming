package networkProgramming;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class WriteFile {
    private static final int    BUFFER_SIZE         = 1024;
    private static final int    PERIOD_ASCII        = '.';
    private static final String ERROR_NO_FILENAME   = "ファイル名を指定してください";
    private static final String ERROR_WRITE_FAILURE = "ファイルの書き込みに失敗しました: ";

    public static void main(String[] args) {
        byte[] buff = new byte[BUFFER_SIZE];
        boolean cont = true;

        if (args.length == 0) {
            System.err.println(ERROR_NO_FILENAME);
            System.exit(1);
        }

        try (FileOutputStream outfile = new FileOutputStream(args[0])) {
            while (cont) {
                int n = System.in.read(buff);
                if (buff[0] == PERIOD_ASCII) {
                    cont = false;
                } else {
                    outfile.write(buff, 0, n);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println(ERROR_NO_FILENAME);
            System.exit(1);
        } catch (IOException e) {
            System.err.println(ERROR_WRITE_FAILURE + e.getMessage());
            System.exit(1);
        }
    }
}