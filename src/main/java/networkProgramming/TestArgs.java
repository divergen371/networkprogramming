package networkProgramming;

public class TestArgs {
    public static void main(String[] args) {
        int number = 0;
        while(number < args.length) {
            System.out.println(args[number++]);
        }
    }
}
