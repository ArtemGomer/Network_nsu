package Control.Network;

import Model.SnakeModel;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameMessageSender {

    private final static Logger logger = LoggerFactory.getLogger(GameMessageSender.class);

    private int msg_seq = 0;

    private static final int MULTICAST_PORT = 9192;
    private static final String MULTICAST_IP = "239.192.0.4";

    private InetAddress group;
    private DatagramSocket datagramSocket;
    private List<GameMessage> noAckMessages;
    private Map<Integer, Long> lastMessagesTo = new ConcurrentHashMap<>();
    private SnakeModel model;

    public GameMessageSender(Map<Integer, Long> lastMessagesTo,
                             List<GameMessage> noAckMessages, SnakeModel model, DatagramSocket datagramSocket) {
        try {
            this.model = model;
            this.lastMessagesTo = lastMessagesTo;
            this.noAckMessages = noAckMessages;
            this.datagramSocket = datagramSocket;
            this.group = InetAddress.getByName(MULTICAST_IP);
            logger.info("GameMessageSender was created");
        } catch (IOException ex) {
            logger.info("Can mot create GameMessageSender");
        }
    }

    public synchronized void sendNoAckMessages() {
        noAckMessages.removeIf(n -> {
            if (model.isPlayerExists(n.getReceiverId())) {
                try {
                    GamePlayer player = model.getPlayerById(n.getReceiverId());
                    InetAddress address = InetAddress.getByName(player.getIpAddress());
                    int port = player.getPort();
                    sendGameMessage(true, n, address, port);
                    return false;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return true;
                }
            } else {
                return true;
            }
        });
    }

    public synchronized void sendPingMessages(int senderId, int ping_delay_ms) {
        lastMessagesTo.keySet().removeIf(n -> {
                if (System.currentTimeMillis() - lastMessagesTo.get(n) > ping_delay_ms) {
                    if (model.isPlayerExists(n)) {
                        try {
                            GamePlayer player = model.getPlayerById(n);
                            InetAddress address = InetAddress.getByName(player.getIpAddress());
                            int port = player.getPort();
                            sendPingMsg(senderId, n, address, port);
                            return false;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            return true;
                        }
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
        });
    }

    public synchronized void deleteMessageByMsgSeq(long msg_seq) {
        noAckMessages.removeIf(n -> {
            if (n.getMsgSeq() == msg_seq) {
                logger.info("Remove message from noAckMessages [seq, send, recv] " + n.getMsgSeq() + " "
                        + n.getSenderId() + " " + n.getReceiverId());
                return true;
            } else {
                return false;
            }
        });
    }

    public synchronized void sendSteerMsg(int senderId, int receiverId,
                                          Direction direction, InetAddress masterIp, int masterPort) {
        msg_seq++;
        logger.info("Send steerMsg [seq, send, recv] " + msg_seq + " " + senderId + " " + receiverId);
        SteerMsg steerMsg = SteerMsg.newBuilder()
                .setDirection(direction)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setSteer(steerMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(msg_seq)
                .build();

        sendGameMessage(false, gameMessage, masterIp, masterPort);
    }

    public synchronized void sendStateMsg(int senderId, int receiverId,
                                          GameState state, InetAddress address, int port) {
        msg_seq++;
        logger.info("Send stateMsg [seq, send, recv] " + msg_seq + " " + senderId + " " + receiverId);
        StateMsg stateMsg = StateMsg.newBuilder()
                .setState(state)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setState(stateMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(msg_seq)
                .build();

        sendGameMessage(false, gameMessage, address, port);
    }

    public synchronized void sendAnnouncementMsg(GameState state) {
        msg_seq++;
        GamePlayers players = state.getPlayers();
        GameConfig config = state.getConfig();
        AnnouncementMsg announcementMsg = AnnouncementMsg.newBuilder()
                .setConfig(config)
                .setPlayers(players)
                .build();
        GameMessage gameMsg = GameMessage.newBuilder()
                .setMsgSeq(msg_seq)
                .setAnnouncement(announcementMsg)
                .build();
        sendGameMessage(false, gameMsg, group, MULTICAST_PORT);
    }

    public synchronized void sendPingMsg(int senderId, int receiverId, InetAddress address, int port) {
        msg_seq++;
        logger.info("Send pingMsg [seq, send, recv] " + msg_seq + " " + senderId + " " + receiverId);
        PingMsg pingMsg = PingMsg.newBuilder().build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setPing(pingMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(msg_seq)
                .build();
        sendGameMessage(false, gameMessage, address, port);
    }


    public synchronized void sendGameMessage(boolean isResend, GameMessage gameMsg, InetAddress address, int port) {
        byte[] gameMsgBytes = gameMsg.toByteArray();
        DatagramPacket packet = new DatagramPacket(gameMsgBytes, gameMsgBytes.length, address, port);
        if (!gameMsg.hasAnnouncement() && !gameMsg.hasAck()) {
            if (!isResend) {
                logger.info("Add gameMessage to noAckMessages [seq, send, recv] " + gameMsg.getMsgSeq() + " "
                + gameMsg.getSenderId() + " " + gameMsg.getReceiverId());
                noAckMessages.add(gameMsg);
            }
            logger.info("Put time to lastMessagesTo");
            lastMessagesTo.put(gameMsg.getReceiverId(), System.currentTimeMillis());
        }
        try {
            datagramSocket.send(packet);
        } catch (IOException ex) {
            logger.error("IOException occurred", ex);
        }
    }

    public synchronized void sendJoinMessage(int receiverId, String name, boolean isViewer, InetAddress address, int port) {
        logger.info("Send steerMsg [seq, recv] " + msg_seq + " " + receiverId);
        msg_seq++;
        System.out.println("MSG_SEQ = " + msg_seq);
        JoinMsg joinMsg = JoinMsg.newBuilder()
                .setName(name)
                .setOnlyView(isViewer)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setReceiverId(receiverId)
                .setMsgSeq(msg_seq)
                .build();
        sendGameMessage(false, gameMessage, address, port);
    }

    public synchronized void sendAckMsg(int senderId, int receiverId, long msg_seq, InetAddress address, int port) {
        AckMsg ackMsg = AckMsg.newBuilder().build();
        logger.info("Send ackMsg [seq, send, recv] " + msg_seq + " " + senderId + " " + receiverId);
        GameMessage gameMessage = GameMessage.newBuilder()
                .setAck(ackMsg)
                .setMsgSeq(msg_seq)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
        sendGameMessage(false, gameMessage, address, port);
    }

    public synchronized void sendErrorMsg(int senderId, int receiverId, String message, InetAddress address, int port) {
        msg_seq++;
        logger.info("Send errorMsg [seq, send, recv] " + msg_seq + " " + senderId + " " + receiverId);
        ErrorMsg errorMsg = ErrorMsg.newBuilder()
                .setErrorMessage(message)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setError(errorMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setMsgSeq(msg_seq)
                .build();
        sendGameMessage(false, gameMessage, address, port);
    }

    public synchronized void sendRoleChangeMsg(int senderId, int receiverId, NodeRole senderRole, NodeRole receiverRole,
                                  InetAddress address, int port) {
        msg_seq++;
        logger.info("Send roleChangeMsg [seq, send, recv] " + msg_seq + " " + senderId + " " + receiverId);
        RoleChangeMsg roleChangeMsg = RoleChangeMsg.newBuilder()
                .setReceiverRole(receiverRole)
                .setSenderRole(senderRole)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setRoleChange(roleChangeMsg)
                .setMsgSeq(msg_seq)
                .setReceiverId(receiverId)
                .setSenderId(senderId)
                .build();
        sendGameMessage(false, gameMessage, address, port);
    }

    public synchronized long getNextMsgSeq() {
        return ++msg_seq;
    }

}
