import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements AutoCloseable{
    private final ServerSocket serverSocket;
    private final static Logger logger = Logger.getLogger(Server.class.getName());

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Server can not accept connections!");
            try {
                this.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
    }


    @Override
    public void close() throws IOException {
        System.out.println("CLOSE");
        serverSocket.close();
    }
}
