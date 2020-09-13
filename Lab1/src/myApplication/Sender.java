package myApplication;

import java.io.IOException;
import java.net.*;

public class Sender {
    private final DatagramSocket datagramSocket;
    private final InetAddress groupAddress;

    public Sender(String groupAddress) throws IOException {
        datagramSocket = new DatagramSocket();
        this.groupAddress = InetAddress.getByName(groupAddress);
    }

    public void send(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupAddress, 4446);
        datagramSocket.send(packet);
    }

    public void close() {
        datagramSocket.close();
    }
}
