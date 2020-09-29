import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable, Closeable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (DataOutputStream socketOutputStream = new DataOutputStream(clientSocket.getOutputStream());
             DataInputStream socketInputStream = new DataInputStream(clientSocket.getInputStream())) {
            if (socketInputStream.readInt() > 4096) {
                logger.log(Level.WARNING, "Name is too long");
                socketOutputStream.writeBoolean(false);
                close();
                return;
            } else {
                socketOutputStream.writeBoolean(true);
            }
            String fileName = socketInputStream.readUTF();
            long clientFileSize = 0;
            if ((clientFileSize = socketInputStream.readLong()) > 1e12) {
                socketOutputStream.writeBoolean(false);
                logger.log(Level.WARNING, "File is too big!");
                close();
                return;
            } else {
                socketOutputStream.writeBoolean(true);
            }
            File file = new File("uploads" + File.separator + fileName);
            long allBytes = 0;
            logger.log(Level.INFO, "Start to receive data.");
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                int readBytes;
                byte[] buffer = new byte[4096];
                while (allBytes < clientFileSize) {
                    readBytes = socketInputStream.read(buffer);
                    allBytes += readBytes;
                    System.out.println(readBytes);
                    fileOutputStream.write(buffer, 0, readBytes);
                }
            }
            logger.log(Level.INFO, "All data was received!");
            if (clientFileSize != allBytes) {
                logger.log(Level.WARNING, "Size of file is wrong.!");
                socketOutputStream.writeBoolean(false);
                close();
            } else {
                socketOutputStream.writeBoolean(true);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Some IO errors occurred", ex);
        }
    }

    @Override
    public void close() {
        logger.log(Level.INFO, "Trying to close client socket.");
        try {
            clientSocket.close();
        } catch (IOException ex) {
            logger.log(Level.INFO, "Can not close client socket.");
        }
        logger.log(Level.INFO, "Server client was closed.");
    }
}
