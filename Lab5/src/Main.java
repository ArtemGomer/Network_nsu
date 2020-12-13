import SOCKS5Server.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.err.println("Bad input format");
                System.exit(1);
            }
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);
            server.run();
        } catch (NumberFormatException ex) {
            System.err.println("Bad input format");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.err.println("Can not create server");
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
