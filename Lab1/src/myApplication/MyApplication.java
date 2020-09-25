package myApplication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

public class MyApplication implements AutoCloseable{

    private final int UPDATE_TIME = 5000;
    private final Sender sender;
    private final Receiver receiver;
    private final HashMap<String, Long> connections = new HashMap<>();
    private boolean isChanged = false;

    public MyApplication(String multicastAddress) throws IOException {
        sender = new Sender(multicastAddress);
        try {
            receiver = new Receiver(multicastAddress);
        } catch (IOException ex) {
            sender.close();
            throw ex;
        }
    }

    public void startWork() throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (System.in.available() != 0) {
                String word = scanner.next();
                if (word.equalsIgnoreCase("end")) {
                    break;
                }
            }
            sender.send("GOT MESSAGE");
            isChanged = receiver.receive(connections);
            updateConnections();
            if (isChanged) {
                printApps();
                isChanged = false;
            }
        }
    }

    private void updateConnections() {
        int oldSize = connections.size();
        connections.values().removeIf(n -> (System.currentTimeMillis() - n > UPDATE_TIME));
        if (oldSize != connections.size()) {
            isChanged = true;
        }
    }

    public void printApps() {
        System.out.println("----------------------");
        for (String address : connections.keySet()) {
            System.out.println(address);
        }
        System.out.println("----------------------");
    }

    @Override
    public void close() {
        sender.close();
        receiver.close();
    }
}
