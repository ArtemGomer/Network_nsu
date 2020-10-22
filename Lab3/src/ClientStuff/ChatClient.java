package ClientStuff;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class ChatClient {
    private int port;
    private String name;
    private int lossPercentage;
    private DatagramSocket socket;
    private ArrayList<DatagramSocket> neighbours = new ArrayList<>();
    private HashMap<String, String> messages = new HashMap<>();

    public ChatClient(String name, int port, int lossPercentage) throws SocketException {
        this.port = port;
        socket = new DatagramSocket(port);
        this.name = name;
        this.lossPercentage = lossPercentage;
    }

    public ChatClient(String name, int port, int lossPercentage, String neighbourIP, int neighbourPort) throws IOException{
        this(name, port, lossPercentage);
        InetAddress inetAddress = InetAddress.getByName(neighbourIP);
        DatagramSocket neighbourSocket = new DatagramSocket();
        socket.bind(new InetSocketAddress(inetAddress, neighbourPort));
        neighbours.add(neighbourSocket);
    }

    public void startWork() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String message = scanner.nextLine();
            String uuid = UUID.randomUUID().toString();
        }
    }
}
