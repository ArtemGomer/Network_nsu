import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final static Logger logger = Logger.getLogger(Server.class.getName());
    private final File dir;

    public Server(int port) throws IOException {
        logger.log(Level.INFO, "Trying to open server socket");
        serverSocket = new ServerSocket(port);
        logger.log(Level.INFO, "Socket was successfully opened");
        dir = new File("uploads");
    }

    public void start() {
        try {
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    logger.log(Level.SEVERE, "Can not create uploads");
                    return;
                }
            } else {
                if (!dir.isDirectory()) {
                    logger.log(Level.SEVERE, "Can not create dir uploads." +
                            " File with this name is already exists");
                    return;
                }
            }
            while (true) {
                logger.log(Level.INFO, "Creating client handler.");
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread handlerThread = new Thread(handler);
                handlerThread.start();
                logger.log(Level.INFO, "Handler started.");
            }
        } catch (
                IOException ex) {
            logger.log(Level.SEVERE, "Server can not accept connections!");
        }
    }


    @Override
    public void close() {
        logger.log(Level.INFO, "Trying to close server socket.");
        try {
            serverSocket.close();
        } catch (IOException ex) {
            logger.log(Level.INFO, "Can not close server socket.");
        }
        logger.log(Level.INFO, "Server server was closed.");
    }
}
