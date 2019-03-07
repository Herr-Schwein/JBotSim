package examples.snakes;

public class Main {
    public static void main(String[] args) {
        //final int snake_len = 1;
        final int snake_num = 45;
        myTopology snakes = new myTopology(1, snake_num);
        if (!snakes.isInitialize) {
            // exit program
            return;
        }
    }
}