package Control.Network;

import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.DefaultTableModel;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;
import java.util.Timer;

public class AnnounceMessageReceiver extends Thread implements Closeable {
    private final static Logger logger = LoggerFactory.getLogger(AnnounceMessageReceiver.class);
    private static final int MULTICAST_PORT = 9192;
    private static final String MULTICAST_IP = "239.192.0.4";
    private MulticastSocket multicastSocket;
    private DefaultTableModel table;
    private final Timer timer = new Timer();
    private final HashMap<Vector<String>, Long> lastMessages = new HashMap<>();

    public AnnounceMessageReceiver(DefaultTableModel table) {
        try {
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            multicastSocket.joinGroup(group);
            this.table = table;
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    deleteGames();
                }
            }, 0, 1000);
        } catch (IOException ex) {
            logger.info("Can not create AnnounceMessageReceiver");
            logger.info("Close AnnounceMessageReceiver");
            close();
        }
    }

    @Override
    public void run() {
        startReceive();
    }

    private void deleteGames() {
        lastMessages.keySet().removeIf(n -> System.currentTimeMillis() - lastMessages.get(n) > 1200);
    }

    private void startReceive() {
        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (!isInterrupted()) {
                multicastSocket.receive(packet);
                logger.info("Got announceMessage");
                GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                if (gameMessage.hasAnnouncement()) {
                    AnnouncementMsg announcementMsg = gameMessage.getAnnouncement();
                    GamePlayers players = announcementMsg.getPlayers();
                    GameConfig config = announcementMsg.getConfig();
                    String size = config.getWidth() + "X" + config.getHeight();
                    String food = config.getFoodStatic() + ":" + config.getFoodPerPlayer();
                    String playersNumber = Integer.toString(players.getPlayersCount());
                    String canJoin = Boolean.toString(announcementMsg.getCanJoin());
                    String nameIpPort = null;
                    for (int i = 0; i < players.getPlayersCount(); i++) {
                        GamePlayer player = players.getPlayers(i);
                        if (player.getRole() == NodeRole.MASTER) {
                            nameIpPort = player.getName() + ":" + packet.getAddress() + ":" + packet.getPort();
                            break;
                        }
                    }
                    Vector<String> game = new Vector<>(Arrays.asList(nameIpPort, size, food, playersNumber, canJoin));
                    if (!lastMessages.containsKey(game)) {
                        table.addRow(game);
                    }
                    lastMessages.put(game, System.currentTimeMillis());
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (multicastSocket != null) {
            logger.info("Close MulticastSocket");
            multicastSocket.close();
        }
        if (timer != null) {
            logger.info("Cancel timer(deleter)");
            timer.cancel();
        }
    }
}
