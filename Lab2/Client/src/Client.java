import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements AutoCloseable {
    private final static Logger logger = Logger.getLogger(Client.class.getName());
    private final Socket socket;

    public Client(String ip, int port) throws IOException {
        logger.log(Level.INFO, "Trying to open socket: " + ip + ":" + port);
        socket = new Socket(ip, port);
        logger.log(Level.INFO, "Socket was successfully opened");
    }

    public void start(String fileName) {
        File file = new File(fileName);
        byte[] buffer = new byte[4096];
        try (DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
             DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {
            logger.log(Level.INFO,"Sending length of file name!");
            outputStream.writeInt(file.getName().length());
            if (inputStream.readBoolean()) {
                outputStream.writeUTF(file.getName());
            } else {
                logger.log(Level.INFO,"Name of the file is too big!");
                return;
            }
            logger.log(Level.INFO,"Sending length of file!");
            outputStream.writeLong(file.length());
            if (inputStream.readBoolean()) {
                logger.log(Level.INFO,"Start to send file.");
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    int readBytes;
                    while ((readBytes = fileInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, readBytes);
                        outputStream.flush();
                    }
                    logger.log(Level.INFO, "All data was sent!");
                    if (inputStream.readBoolean()) {
                        logger.log(Level.INFO, "File was successfully sent!");
                        System.out.println("SUCCESS!!!");
                    } else {
                        logger.log(Level.INFO, "File was not sent!");
                        System.out.println("FAILURE");
                    }
                } catch (FileNotFoundException ex) {
                    logger.log(Level.SEVERE,"Can not find file!");
                }
            } else {
                logger.log(Level.INFO,"File is too big!");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Some IO errors occurred", ex);
        }
    }

    @Override
    public void close() {
        try {
            logger.log(Level.INFO, "Trying to close socket");
            socket.close();
            logger.log(Level.INFO, "Socket was successfully closed");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Can not close socket", ex);
        }
    }
}
