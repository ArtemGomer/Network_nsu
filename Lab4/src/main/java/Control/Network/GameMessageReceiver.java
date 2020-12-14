package Control.Network;

import Control.GameNode;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Map;

public class GameMessageReceiver extends Thread {

    private final static Logger logger = LoggerFactory.getLogger(GameMessageReceiver.class);
    private final DatagramSocket datagramSocket;
    private final GameNode node;
    private final GameMessageSender sender;
    private final Map<Integer, Long> lastMessagesFrom;
    private int playersId = 1;

    public GameMessageReceiver(Map<Integer, Long> lastMessagesFrom, GameMessageSender sender,
                               DatagramSocket datagramSocket, GameNode node) {
        this.datagramSocket = datagramSocket;
        this.node = node;
        this.sender = sender;
        this.lastMessagesFrom = lastMessagesFrom;
        logger.info("GameMessageReceiver created");
    }

    @Override
    public void run() {
        logger.info("Start receive");
        startReceive();
    }

    private void startReceive() {
        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
        try {
            while (!isInterrupted()) {
                datagramSocket.receive(packet);
                logger.info("Got packet");
                GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                if (!gameMessage.hasJoin()) {
                    logger.info("Put Time to lastMessagesFrom [send, recv] " + gameMessage.getSenderId()
                            + " " + gameMessage.getReceiverId());
                    lastMessagesFrom.put(gameMessage.getSenderId(), System.currentTimeMillis());
                }
                if (gameMessage.hasSteer()) {
                    processSteerMessage(gameMessage, packet.getAddress(), packet.getPort());
                } else if (gameMessage.hasAck()) {
                    processAckMessage(gameMessage);
                } else if (gameMessage.hasState()) {
                    processStateMessage(gameMessage, packet.getAddress(), packet.getPort());
                } else if (gameMessage.hasJoin()) {
                    processJoinMessage(gameMessage, packet.getAddress(), packet.getPort());
                } else if (gameMessage.hasPing()) {
                    processPingMessage(gameMessage, packet.getAddress(), packet.getPort());
                } else if (gameMessage.hasRoleChange()) {
                    processRoleChangeMessage(gameMessage, packet.getAddress(), packet.getPort());
                }
            }
        } catch (IOException ex) {
            logger.error("IOException occurred", ex);
        }
    }

    private void processPingMessage(GameMessage gameMessage, InetAddress address, int port) {
        logger.info("Got PingMsg [seq, send, recv]" + gameMessage.getMsgSeq() + " "
                + gameMessage.getSenderId() + " " + gameMessage.getReceiverId());
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }

    private void processSteerMessage(GameMessage gameMessage, InetAddress address, int port) {
        logger.info("Got SteerMsg [seq, send, recv]" + gameMessage.getMsgSeq() + " "
                + gameMessage.getSenderId() + " " + gameMessage.getReceiverId());
        node.addSteer(gameMessage.getSenderId(), gameMessage);
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }

    private void processAckMessage(GameMessage gameMessage) {
        logger.info("Got AckMsg [seq, send, recv]" + gameMessage.getMsgSeq() + " "
                + gameMessage.getSenderId() + " " + gameMessage.getReceiverId());
        sender.deleteMessageByMsgSeq(gameMessage.getMsgSeq());
    }

    private void processStateMessage(GameMessage gameMessage, InetAddress address, int port) {
        StateMsg stateMsg = gameMessage.getState();
        GameState state = stateMsg.getState();
        logger.info("Got StateMsg [seq, send, recv]" + gameMessage.getMsgSeq() + " "
                + gameMessage.getSenderId() + " " + gameMessage.getReceiverId());
        node.updateModelFromState(state);
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }

    private void processJoinMessage(GameMessage gameMessage, InetAddress address, int port) {
        logger.info("Got JoinMsg [seq, send, recv]" + gameMessage.getMsgSeq() + " "
                + gameMessage.getSenderId() + " " + gameMessage.getReceiverId());
        JoinMsg joinMsg = gameMessage.getJoin();
        String addressString = address.toString().substring(1);
        playersId++;
        logger.info("PlayersId = " + playersId);
        if (!joinMsg.getOnlyView()) {
            GamePlayer player = node.createGamePlayer(playersId, NodeRole.NORMAL,
                    addressString, port, joinMsg.getName());
            Coord headCoord = node.findHeadCoord();
            if (headCoord.getX() == -1) {
                sender.sendErrorMsg(node.getId(), playersId, "Can not find square", address, port);
            } else {
                Snake snake = node.createSnake(playersId, headCoord);
                node.addPlayer(player);
                node.addSnake(snake);
                sender.sendAckMsg(node.getId(), playersId, gameMessage.getMsgSeq(), address, port);
            }
        } else {
            GamePlayer player = node.createGamePlayer(playersId, NodeRole.VIEWER,
                    addressString, port, joinMsg.getName());
            node.addPlayer(player);
            sender.sendAckMsg(node.getId(), playersId, gameMessage.getMsgSeq(), address, port);
        }
        logger.info("Put Time to lastMessagesFrom [send, recv] " + playersId
                + " " + gameMessage.getReceiverId());
        lastMessagesFrom.put(playersId, System.currentTimeMillis());
        node.findNewDeputy();
    }

    private void processRoleChangeMessage(GameMessage gameMessage, InetAddress address, int port) {
        RoleChangeMsg roleChangeMsg = gameMessage.getRoleChange();
        NodeRole oldRole = node.getRole();
        NodeRole newRole = roleChangeMsg.getReceiverRole();
        node.setRole(roleChangeMsg.getReceiverRole());
        if (oldRole == NodeRole.DEPUTY && newRole == NodeRole.MASTER) {
            node.findNewDeputy();
            node.startAsMaster();
        } else if (newRole == NodeRole.VIEWER) {
            node.stopPlaying();
            node.startAsViewerOrNormal();
        }
        sender.sendAckMsg(node.getId(), gameMessage.getSenderId(), gameMessage.getMsgSeq(), address, port);
    }
}
