package examples.snakes;

import jbotsimx.ui.JViewer;

public class Main {
    public static void main(String[] args) {
        final int snake_len = 3;
        final int snake_num = 8;
        myTopology snakes = new myTopology(snake_len, snake_num);
        if (!snakes.isInitialize) {
            // exit program
            return;
        }
        new JViewer(snakes);
    }
}