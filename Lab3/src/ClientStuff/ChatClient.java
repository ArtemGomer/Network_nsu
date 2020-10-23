package ClientStuff;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class ChatClient implements Closeable {
    private final String name;
    private final DatagramSocket socket;
    private final List<String> neighbours = Collections.synchronizedList(new ArrayList<>());
    private final MessagesController messagesController = new MessagesController();
    ClientReceiver receiver;
    ClientSender sender;

    public ChatClient(String name, int port, int lossPercentage) throws SocketException {
        socket = new DatagramSocket(port);
        this.name = name;
        sender = new ClientSender(socket, neighbours, messagesController);
        receiver = new ClientReceiver(socket, lossPercentage, messagesController, sender, neighbours);
    }

    public ChatClient(String name, int port, int lossPercentage, String neighbourIP, int neighbourPort)
            throws IOException {
        this(name, port, lossPercentage);
        neighbours.add(neighbourIP + ":" + neighbourPort);
    }

    public void start() {
        receiver.start();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String message = scanner.nextLine();
            if (message.equals("#EXIT")) {
                sender.sendExitMessages();
                this.close();
                return;
            }
            String uuid = UUID.randomUUID().toString();
            String fullMessage = name + ":" + message;
            messagesController.addMessage(uuid, fullMessage);
            sender.sendMessageToAll(uuid, null, -1);
        }
    }

    @Override
    public void close(){
        receiver.interrupt();
        socket.close();
    }
}
