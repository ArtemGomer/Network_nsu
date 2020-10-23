package ClientStuff;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class ClientSender {

    private final DatagramSocket socket;
    private final List<String> neighbours;
    private final MessagesController messagesController;

    public ClientSender(DatagramSocket socket, List<String> neighbours, MessagesController messagesController) {
        this.socket = socket;
        this.neighbours = neighbours;
        this.messagesController = messagesController;
    }

    public void sendMessageToAll(String uuid, String notIP, int notPort) {
        byte[] buffer = ("MESSAGE:" + uuid + ":" + messagesController.getMessage(uuid)).getBytes();
        try {
            for (String neighbourAddress : neighbours) {
                String[] info = neighbourAddress.split(":");
                int port = Integer.parseInt(info[1]);
                if (!(info[0].equals(notIP) && notPort == port)) {
                    InetAddress inetAddress = InetAddress.getByName(info[0]);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, port);
                    socket.send(packet);
                    messagesController.increaseStat(uuid);
                }
            }
            messagesController.removeMessageIfZero(uuid);
        } catch (IOException ex) {
            System.err.println("Some I/O errors occurred.");
            ex.printStackTrace();
        }
    }

    public void sendConfirmToOne(InetAddress IP, int port, String type, String uuid) {
        byte[] buffer = (type + ":" + uuid).getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length, IP, port);
            socket.send(packet);
        } catch (IOException ex) {
            System.err.println("Some I/O errors occurred.");
            ex.printStackTrace();
        }
    }

    public void resendMessage(String uuid, InetAddress IP, int port) {
        byte[] buffer = ("MESSAGE:" + uuid + ":" + messagesController.getMessage(uuid)).getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length, IP, port);
            socket.send(packet);
        } catch (IOException ex) {
            System.err.println("Some I/O errors occurred.");
            ex.printStackTrace();
        }
    }

    public void sendExitMessages() {
        if (neighbours.size() > 0) {
            String depute = neighbours.get(0);
            String message = "EXIT:" + depute;
            byte[] buffer = message.getBytes();
            try {
                for (String neighbour : neighbours) {
                    String[] info = neighbour.split(":");
                    String IP = info[0];
                    int port = Integer.parseInt(info[1]);
                    InetAddress inetAddress = InetAddress.getByName(IP);
                    DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length, inetAddress, port);
                    socket.send(packet);
                }
            } catch (IOException ex) {
                System.err.println("Some I/O errors occurred.");
                ex.printStackTrace();
            }
        }
    }

}
