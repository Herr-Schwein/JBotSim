package jbotsim;

import java.util.Random;

public final class PRNG {
    public static boolean nextBoolean() {
        return prng.nextBoolean();
    }

    public static int nextInt() {
        return prng.nextInt();
    }

    public static int nextInt(int bound) {
        return prng.nextInt(bound);
    }

    public static double nextDouble() {
        return prng.nextDouble();
    }

    public static void setSeed(long seed) {
        prng.setSeed(seed);
    }

    private static final Random prng = new Random();
}
