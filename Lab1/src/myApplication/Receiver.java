package myApplication;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class Receiver {
    private final MulticastSocket multicastSocket;
    private final byte[] buffer;
    private final InetAddress groupAddress;

    public Receiver(String address) throws IOException {
        buffer = new byte[256];
        multicastSocket = new MulticastSocket(4446);
        groupAddress = InetAddress.getByName(address);
        multicastSocket.joinGroup(groupAddress);
    }

public boolean receive(HashMap<String, Long> connections) throws IOException {
        boolean isChanged = false;
        try {
            multicastSocket.setSoTimeout(5000);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                String appAddress = packet.getAddress().toString() + ":" + packet.getPort();
                if (connections.containsKey(appAddress)) {
                    connections.replace(appAddress, System.currentTimeMillis());
                } else {
                    connections.put(appAddress, System.currentTimeMillis());
                    isChanged = true;
                }
            }
        } catch (SocketTimeoutException ex) {
            return isChanged;
        }
    }

    public void close() throws IOException {
        multicastSocket.leaveGroup(groupAddress);
        multicastSocket.close();
    }
}
