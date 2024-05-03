package networkProgramming;

public class ReadWrite {
    public static void main(String[] args) {
        byte[] buff = new byte[1024];
        while (true) {
            try {
                int n = System.in.read(buff);
                System.out.write(buff, 2, n);
            } catch (Exception e) {
                System.exit(1);
            }
        }
    }
}
