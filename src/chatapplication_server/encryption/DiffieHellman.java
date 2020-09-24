package chatapplication_server.encryption;

import java.util.Random;

// This class helps with implementing aspects of the Diffie-Hellman protocol
public class DiffieHellman {
    /* p and g are public parameters, therefore hardcoded for clients to share them.
    * p is a prime number an g is a number which is coprime to p-1.
    * Large values of p makes it computationally infeasible to break, which is what the following tries to simulate
    * */
    public static int chooseP() {
        return 429437291;
    }
    public static int chooseG() {
        return 867421387;
    }

    /* a and b are local parameters which are chosen randomly.
    It should hold that a and b are elements of {1, 2, ..., p-1}
     * */
    public static int chooseSmallA() {
        Random random = new Random();
        return random.nextInt(chooseP() - 1) + 1;
    }
    public static int chooseSmallB() {
        Random random = new Random();
        return random.nextInt(chooseP() - 1) + 1;
    }
}
