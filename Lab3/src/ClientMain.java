import ClientStuff.ChatClient;

import java.io.IOException;
import java.net.SocketException;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length == 3 || args.length == 5) {
            try {
                int port = Integer.parseInt(args[1]);
                int lossPercentage = Integer.parseInt(args[2]);
                String name = args[0];
                if (args.length == 3) {
                    try {
                        ChatClient chatClient = new ChatClient(name, port, lossPercentage);
                        chatClient.start();
                    } catch (SocketException ex) {
                        System.err.println("Can not create client.");
                        ex.printStackTrace();
                        System.exit(1);
                    }
                } else {
                    try {
                        int neighbourPort = Integer.parseInt(args[4]);
                        String neighbourIP = args[3];
                        ChatClient chatClient = new ChatClient(name, port, lossPercentage, neighbourIP,
                                neighbourPort);
                        chatClient.start();
                    } catch (NumberFormatException ex) {
                        System.err.println("Can not convert port or lossPercentage.");
                        ex.printStackTrace();
                        System.exit(1);
                    } catch (IOException ex) {
                        System.err.println("Can not create client.");
                        ex.printStackTrace();
                        System.exit(1);
                    }
                }
            } catch (NumberFormatException ex) {
                System.err.println("Can not convert port or lossPercentage.");
                ex.printStackTrace();
                System.exit(1);
            }
        } else {
            System.err.println("Wrong number of arguments.");
            System.exit(1);
        }
    }
}
