package chatapplication_server;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// This class acts as a small database for the server to associate users to secret keys.
// The implementation isn't ideal, but its just a meant to illustrate the idea
public class ServerSecretManager {
    public static String getSecret(String username) {

        Map<String, String> serverSecrets = Stream.of(new String[][] {
                { "username", "secretKey" },
                { "Thanassis", "RhdBESLLtsftTZxMlxlH" },
                { "Emil", "IvJXZWEizBvgecbhCzuN" },
                { "Saadman", "cBlAomwVSlkpvkRyLHVO" },
                { "Fahad", "PDQtGENLZWkqolitJLGO" },
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        return serverSecrets.get(username);
    }
}