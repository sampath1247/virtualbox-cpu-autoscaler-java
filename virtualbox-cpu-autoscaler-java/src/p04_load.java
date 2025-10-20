public class p04_load {
    public static void main(String[] args) {
        while (true) { // Infinite loop to keep CPU busy
            double result = 1.0;
            // Perform a CPU-intensive calculation: product of fractions
            for (int i = 200; i < 2000000; i++) {
                result *= (double) (i - 1) / i;
            }
            // Print result to prevent compiler optimization
            System.out.println("Result: " + result);
        }
    }
}
                           