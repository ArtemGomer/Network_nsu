import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("resources/log.properties"));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Can not read configuration!", ex);
        }
        if (args.length != 1) {
            logger.log(Level.WARNING, "Bad input format!");
            System.exit(1);
        }
        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            logger.log(Level.SEVERE, "Port can be only a number!", ex);
            System.exit(1);
        }
        try (Server server = new Server(port)) {
            logger.log(Level.INFO, "Server was successfully created!");
            server.start();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Can not create server!");
            System.exit(1);
        }
    }
}
