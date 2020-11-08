package ClientStuff;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Random;

public class ClientReceiver extends Thread {
    private final List<String> neighbours;
    private final ClientSender sender;
    private final Random randomGenerator = new Random();
    private final int lossPercentage;
    private final DatagramSocket socket;
    private final MessagesController messagesController;

    public ClientReceiver(DatagramSocket socket, int lossPercentage, MessagesController messagesController,
                          ClientSender sender, List<String> neighbours) {
        this.socket = socket;
        this.sender = sender;
        this.neighbours = neighbours;
        this.lossPercentage = lossPercentage;
        this.messagesController = messagesController;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (!isInterrupted()) {

                socket.receive(packet);
                InetAddress senderIP = packet.getAddress();
                int senderPort = packet.getPort();
                String data = new String(packet.getData(), 0, packet.getLength());
                String body = data.substring(data.indexOf(":") + 1);
                String type = data.substring(0, data.indexOf(":"));
                switch (type) {
                    case "MESSAGE":
                        this.validateMessage(senderIP, senderPort, data);
                        break;
                    case "DISMISS": {
                        sender.resendMessage(body, senderIP, senderPort);
                        break;
                    }
                    case "CONFIRM": {
                        messagesController.decreaseStat(body);
                        break;
                    }
                    case "EXIT":
                        neighbours.remove(senderIP.toString().substring(1) + ":" + senderPort);
                        String localAddress = InetAddress.getLocalHost().getHostAddress() + ":" + socket.getLocalPort();
                        if (!body.equals(localAddress)) {
                            neighbours.add(body);
                        }
                        break;
                }

            }
        } catch (SocketException ignored) {

        } catch (IOException ex) {
            System.err.println("Some I/) errors occurred!");
            ex.printStackTrace();
        }
    }

    private void validateMessage(InetAddress senderIP, int senderPort, String data) {

        String neighbourAddress = senderIP.toString().substring(1) + ":" + senderPort;
        if (!neighbours.contains(neighbourAddress)) {
            neighbours.add(neighbourAddress);
        }

        String body = data.substring(data.indexOf(":") + 1);
        String uuid = body.substring(0, body.indexOf(":"));
        String message = body.substring(body.indexOf(":") + 1);

        int randomNumber = randomGenerator.nextInt();
        if (randomNumber < lossPercentage) {
            sender.sendConfirmToOne(senderIP, senderPort, "DISMISS", uuid);
        } else {
            messagesController.addMessage(uuid, message);
            System.out.println(message);
            sender.sendConfirmToOne(senderIP, senderPort, "CONFIRM", uuid);
            sender.sendMessageToAll(uuid, senderIP.toString().substring(1), senderPort);
        }
    }
}
