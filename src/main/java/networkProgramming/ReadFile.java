package networkProgramming;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ReadFile {
    private static final int BUFFER_SIZE = 1024;
    private static final String ERROR_NO_FILENAME = "ファイル名を指定してください";
    private static final String ERROR_FILE_NOT_FOUND = "ファイルがありません";
    private static final String ERROR_READ_FAILURE = "ファイル読み込みエラー";

    public static void main(String[] args) {
        byte[] buff = new byte[BUFFER_SIZE];

        if (args.length == 0) {
            System.err.println(ERROR_NO_FILENAME);
            System.exit(1);
        }

        try (FileInputStream infile = new FileInputStream(args[0])) {
            int n;
            while ((n = infile.read(buff)) != -1) {
                System.out.write(buff, 0, n);
            }
        } catch (FileNotFoundException e) {
            System.err.println(ERROR_FILE_NOT_FOUND);
            System.exit(1);
        } catch (IOException e) {
            System.err.println(ERROR_READ_FAILURE);
            System.exit(1);
        }
    }
}