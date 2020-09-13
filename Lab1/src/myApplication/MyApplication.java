package myApplication;

import java.io.IOException;
import java.util.HashMap;

public class MyApplication {

    private final Sender sender;
    private final Receiver receiver;
    private final HashMap<String, Long> connections = new HashMap<>();
    private boolean isChanged = false;

    public MyApplication(String multicastAddress) throws IOException {
        sender = new Sender(multicastAddress);
        receiver = new Receiver(multicastAddress);
    }

    public void startWork() throws IOException {
        while (true) {
            sender.send("GOT MESSAGE");
            isChanged = receiver.receive(connections);
            updateConnections();
            if (isChanged) {
                printApps();
                isChanged = false;
            }
        }
    }

    public void endWork() throws IOException {
        sender.close();
        receiver.close();
    }

    private void updateConnections() {
        int oldSize = connections.size();
        connections.values().removeIf(n -> (System.currentTimeMillis() - n > 10000));
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
}
