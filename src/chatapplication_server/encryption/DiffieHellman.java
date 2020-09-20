package chatapplication_server.encryption;

// This class helps with implementing aspects the DiffieHellman protocol
public class DiffieHellman {
    public static boolean isCoprime(int a, int b) {
        return GreatestCommonDenominator(a, b) == 1;
    }
    private static int GreatestCommonDenominator(int a, int b) {
        while (a != 0 && b != 0) {
            if (a > b) {
                a %= b;
            } else {
                a %= b;
            }
        }
        return Math.max(a, b);
    }

    // TODO these functions should be be dynamic
    public static int pickP() {
        return 23;
    }
    public static int pickG(int P) {
        return 5;
    }
    public static int pickA() {
        return 4;
    }
    public static int pickB() {
        return 3;
    }
}
