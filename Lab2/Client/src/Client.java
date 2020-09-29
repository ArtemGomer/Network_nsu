import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class Client implements AutoCloseable {
    private final static Logger logger = Logger.getLogger(Client.class.getName());
    private final Socket socket;
    private  final String fileName;

    public Client(String ip, int port, String fileName) throws IOException {
        socket = new Socket(ip, port);
        this.fileName = fileName;
    }

    public void start() {

    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
