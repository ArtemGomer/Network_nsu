package ClientStuff;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;

public class ClientReceiver extends Thread{
    private Random randomGenerator;
    private int lossPercentage;
    private DatagramSocket socket;
    private byte[] buffer;

    public ClientReceiver(DatagramSocket socket, int lossPercentage) {
        this.socket = socket;
        this.lossPercentage = lossPercentage;
        this.buffer = new byte[512];
        randomGenerator = new Random();
    }

    @Override
    public void run() {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (!isInterrupted()) {
                socket.receive(packet);
                int randomNumber = randomGenerator.nextInt(101);
                if (randomNumber < lossPercentage) {
                    //failure
                } else {
                    //confirm
                }
            }
        } catch (IOException ex) {
            System.err.println("Some I/O errors occurred.");
        }
    }

}
