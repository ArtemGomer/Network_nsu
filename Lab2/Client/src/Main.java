import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLockInterruptionException;
import java.util.logging.*;

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("resources" + File.separator + "log.properties"));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Can not read configuration", ex);
        }
        if (args.length != 3) {
            logger.log(Level.SEVERE, "Bad input format!");
            System.exit(1);
        }
        int port = 0;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            logger.log(Level.SEVERE, "Port can be only a number!", ex);
            System.exit(1);
        }
        logger.log(Level.INFO, "Trying to create client!");
        try (Client client = new Client(args[0], port)) {
            logger.log(Level.INFO, "Client was successfully created!");
            client.start(args[2]);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Can not create client!", ex);
            ex.printStackTrace();
            System.exit(1);
        }
    }
}