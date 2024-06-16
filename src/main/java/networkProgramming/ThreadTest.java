package networkProgramming;

public class ThreadTest {
    public static void main(String[] args) {
        try {
            CountTen no1 = new CountTen("No.1");
            CountTen no2 = new CountTen("No.2");

            Thread t1 = new Thread(no1);
            Thread t2 = new Thread(no2);

            t1.start();
            t2.start();
        } catch (Exception e) {
            System.out.println("Error: " + e);
            System.exit(1);
        }
    }

    static class CountTen implements Runnable {
        String myname;

        CountTen(String name) {
            myname = name;

        }

        public void run() {
            for (int i = 1; i <= 10; i++) {
                System.out.println(myname + " : " + i);
            }
        }
    }
}
