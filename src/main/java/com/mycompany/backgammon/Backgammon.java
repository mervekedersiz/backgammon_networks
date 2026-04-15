package com.mycompany.backgammon;

import com.mycompany.backgammon.client.BackgammonClient;
import com.mycompany.backgammon.server.BackgammonServer;

/**
 * Unified launcher.
 *
 *   java -jar backgammon.jar              -> starts the Swing client
 *   java -jar backgammon.jar server [port] -> starts the console server
 *   java -jar backgammon.jar client        -> starts the Swing client explicitly
 *
 * The server is intended to run on AWS (console). The client connects to the
 * AWS server's public IP from the Start screen.
 */
public class Backgammon {
    public static void main(String[] args) {
        String mode = (args.length > 0) ? args[0].toLowerCase() : "client";
        switch (mode) {
            case "server" -> {
                String[] rest = new String[Math.max(0, args.length - 1)];
                System.arraycopy(args, 1, rest, 0, rest.length);
                BackgammonServer.main(rest);
            }
            case "client" -> BackgammonClient.main(new String[0]);
            default -> BackgammonClient.main(args);
        }
    }
}
