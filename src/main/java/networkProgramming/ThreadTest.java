package networkProgramming;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ThreadTest {
    public static void main(String[] args) {
        CountTen no1 = new CountTen("No.1");
        CountTen no2 = new CountTen("No.2");

        Thread t1 = new Thread(no1);
        Thread t2 = new Thread(no2);

        List<Thread> asList = Arrays.asList(t1, t2);
        asList.forEach(Thread::start);
    }
}

class CountTen implements Runnable {
    private final String name;
    private final Random random = new Random();

    CountTen(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int number : numbers) {
            System.out.println(name + " : " + number);
            try {
                // 500ms から 1500ms の間でランダムに遅延を追加
                int sleeptime = 500 + random.nextInt(1001);
                // 各ループの間に少しの遅延を追加
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(name + " was interrupted");
            }
        }
    }
}
