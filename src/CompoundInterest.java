public class CompoundInterest {
    public static void main(String[] args) {
        final double STARTRATE = 10;
        final int NYEARS = 10;
        final int NRATES = 6;
        double [] interestrate = new double[NRATES];
        for (int j = 0; j < interestrate.length; j++) {
            interestrate[j] = (STARTRATE + j) / 100;
        }
        double[][] balance = new double[NYEARS][NRATES];
        for (int j = 0; j < balance[0].length; j++)
            balance[0][j] = 10000;
        for (int i = 1; i < balance.length; i++)
        {
            for (int j = 0; j < balance[i].length; j++) {
                double oldbalance = balance[i - 1][j];
                double interest = oldbalance * interestrate[j];
                balance[i][j] = oldbalance + interest;
            }
    }
        for (int j=0; j< interestrate.length; j++)
            System.out.printf("%9.0f%%", 100*interestrate[j]);
        System.out.println();
        for (double[] row: balance) {
            for (double b : row)
                System.out.printf("%10.2f", b);
                System.out.println();
        }
    }
}
