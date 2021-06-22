import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author Deus Jeraldy
 * @Email: deusjeraldy@gmail.com
 */
public class np {

    private static Random random;
    private static long seed;

    static {
        seed = System.currentTimeMillis();
        random = new Random(seed);
    }

    /**
     * Sets the seed of the pseudo-random number generator. This method enables
     * you to produce the same sequence of "random" number for each execution of
     * the program. Ordinarily, you should call this method at most once per
     * program.
     *
     * @param s the seed
     */
    public static void setSeed(long s) {
        seed = s;
        random = new Random(seed);
    }

    /**
     * Returns the seed of the pseudo-random number generator.
     *
     * @return the seed
     */
    public static long getSeed() {
        return seed;
    }

    /**
     * Returns a random real number uniformly in [0, 1).
     *
     * @return a random real number uniformly in [0, 1)
     */
    public static double uniform() {
        return random.nextDouble();
    }

    /**
     * Returns a random integer uniformly in [0, n).
     *
     * @param n number of possible integers
     * @return a random integer uniformly between 0 (inclusive) and {@code n}
     * (exclusive)
     * @throws IllegalArgumentException if {@code n <= 0}
     */
    public static int uniform(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("argument must be positive: " + n);
        }
        return random.nextInt(n);
    }

    /**
     * Returns a random long integer uniformly in [0, n).
     *
     * @param n number of possible {@code long} integers
     * @return a random long integer uniformly between 0 (inclusive) and
     * {@code n} (exclusive)
     * @throws IllegalArgumentException if {@code n <= 0}
     */
    public static long uniform(long n) {
        if (n <= 0L) {
            throw new IllegalArgumentException("argument must be positive: " + n);
        }

        long r = random.nextLong();
        long m = n - 1;

        // power of two
        if ((n & m) == 0L) {
            return r & m;
        }

        // reject over-represented candidates
        long u = r >>> 1;
        while (u + m - (r = u % n) < 0L) {
            u = random.nextLong() >>> 1;
        }
        return r;
    }

    /**
     * Returns a random integer uniformly in [a, b).
     *
     * @param a the left endpoint
     * @param b the right endpoint
     * @return a random integer uniformly in [a, b)
     * @throws IllegalArgumentException if {@code b <= a}
     * @throws IllegalArgumentException if {@code b - a >= Integer.MAX_VALUE}
     */
    public static int uniform(int a, int b) {
        if ((b <= a) || ((long) b - a >= Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("invalid range: [" + a + ", " + b + ")");
        }
        return a + uniform(b - a);
    }

    /**
     * Returns a random real number uniformly in [a, b).
     *
     * @param a the left endpoint
     * @param b the right endpoint
     * @return a random real number uniformly in [a, b)
     * @throws IllegalArgumentException unless {@code a < b}
     */
    public static double uniform(double a, double b) {
        if (!(a < b)) {
            throw new IllegalArgumentException("invalid range: [" + a + ", " + b + ")");
        }
        return a + uniform() * (b - a);
    }

    /**
     * @param m
     * @param n
     * @return random m-by-n matrix with values between 0 and 1
     */
    public static double[][] random(int m, int n) {
        double[][] a = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = uniform(0.0, 1.0);
            }
        }
        return a;
    }

    /**
     * Transpose of a matrix
     *
     * @param a matrix
     * @return b = A^T
     */
    public static double[][] T(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        double[][] b = new double[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                b[j][i] = a[i][j];
            }
        }
        return b;
    }

    /**
     * @param a matrix
     * @param b matrix
     * @return c = a + b
     */
    public static double[][] add(double[][] a, double[][] b) {
        int m = a.length;
        int n = a[0].length;
        double[][] c = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                c[i][j] = a[i][j] + b[i][j];
            }
        }
        return c;
    }

    /**
     * @param a matrix
     * @param b matrix
     * @return c = a - b
     */
    public static double[][] subtract(double[][] a, double[][] b) {
        int m = a.length;
        int n = a[0].length;
        double[][] c = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                c[i][j] = a[i][j] - b[i][j];
            }
        }
        return c;
    }

    /**
     * Element wise subtraction
     *
     * @param a scaler
     * @param b matrix
     * @return c = a - b
     */
    public static double[][] subtract(double a, double[][] b) {
        int m = b.length;
        int n = b[0].length;
        double[][] c = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                c[i][j] = a - b[i][j];
            }
        }
        return c;
    }

    /**
     * @param a matrix
     * @param b matrix
     * @return c = a * b
     */
    public static double[][] dot(double[][] a, double[][] b) {
        int m1 = a.length;
        int n1 = a[0].length;
        int m2 = b.length;
        int n2 = b[0].length;
        if (n1 != m2) {
            throw new RuntimeException("Illegal matrix dimensions.");
        }
        double[][] c = new double[m1][n2];
        for (int i = 0; i < m1; i++) {
            for (int j = 0; j < n2; j++) {
                for (int k = 0; k < n1; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return c;
    }

    /**
     * Element wise multiplication
     *
     * @param a matrix
     * @param x matrix
     * @return y = a * x
     */
    public static double[][] multiply(double[][] x, double[][] a) {
        int m = a.length;
        int n = a[0].length;

        if (x.length != m || x[0].length != n) {
            throw new RuntimeException("Illegal matrix dimensions.");
        }
        double[][] y = new double[m][n];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                y[j][i] = a[j][i] * x[j][i];
            }
        }
        return y;
    }

    /**
     * Element wise multiplication
     *
     * @param a matrix
     * @param x scaler
     * @return y = a * x
     */
    public static double[][] multiply(double x, double[][] a) {
        int m = a.length;
        int n = a[0].length;

        double[][] y = new double[m][n];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                y[j][i] = a[j][i] * x;
            }
        }
        return y;
    }

    /**
     * Element wise power
     *
     * @param x matrix
     * @param a scaler
     * @return y
     */
    public static double[][] power(double[][] x, int a) {
        int m = x.length;
        int n = x[0].length;

        double[][] y = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                y[i][j] = Math.pow(x[i][j], a);
            }
        }
        return y;
    }

    /**
     * @param a matrix
     * @return shape of matrix a
     */
    public static String shape(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        String Vshape = "(" + m + "," + n + ")";
        return Vshape;
    }

    /**
     * @param a matrix
     * @return sigmoid of matrix a
     */
    public static double[][] sigmoid(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        double[][] z = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                z[i][j] = (1.0 / (1 + Math.exp(-a[i][j])));
            }
        }
        return z;
    }

    /**
     * Element wise division
     *
     * @param a scaler
     * @param x matrix
     * @return x / a
     */
    public static double[][] divide(double[][] x, int a) {
        int m = x.length;
        int n = x[0].length;

        double[][] z = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                z[i][j] = (x[i][j] / a);
            }
        }
        return z;
    }
    /**
     * Element wise division
     *
     * @param A matrix
     * @param Y matrix
     * @param batch_size scaler
     * @return loss
     */
    public static double cross_entropy(int batch_size, double[][] Y, double[][] A) {
        int m = A.length;
        int n = A[0].length;
        double[][] z = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                z[i][j] = (Y[i][j] * Math.log(A[i][j])) + ((1 - Y[i][j]) * Math.log(1 - A[i][j]));
            }
        }

        double sum = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                sum += z[i][j];
            }
        }
        return -sum / batch_size;
    }
    public static double[][] softmax(double[][] z) {
        double[][] zout = new double[z.length][z[0].length];
        double sum = 0.;
        for (int i = 0; i < z.length; i++) {
            for (int j = 0; j < z[0].length; j++) {
                sum += Math.exp(z[i][j]);
            }
        }
        for (int i = 0; i < z.length; i++) {
            for (int j = 0; j < z[0].length; j++) {
                zout[i][j] = Math.exp(z[i][j]) / sum;
            }
        }
        return zout;
    }

    public static void print(String val) {
        System.out.println(val);
    }
}