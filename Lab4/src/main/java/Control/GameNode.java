package Control;

import Control.Network.AnnounceMessageReceiver;
import Control.Network.GameMessageReceiver;
import Control.Network.GameMessageSender;
import Model.Cell;
import Model.SnakeModel;
import View.GamePanel;
import View.MainMenu;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;
import me.ippolitov.fit.snakes.SnakesProto.GameMessage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

public class GameNode implements Closeable {

    //TODO методы deletePlayer(), sendChangeRoleMessage(), добавить node в model
    private final static Logger logger = LoggerFactory.getLogger(GameNode.class);
    private final List<GameMessage> noAckMessages = Collections.synchronizedList(new ArrayList<>());
    private final Map<Integer, Long> lastMessagesFrom = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastMessagesTo = new ConcurrentHashMap<>();
    private GameMessageSender gameMessageSender;
    private GameMessageReceiver gameMessageReceiver;
    private AnnounceMessageReceiver announceMessageReceiver;
    private DatagramSocket datagramSocket;
    private SnakeModel model;
    private JFrame view;

    private final Properties properties = new Properties();
    private GameConfig config;

    private Timer modelUpdater = new Timer("modelUpdater");
    private Timer announceSender = new Timer("announceSender");
    private Timer messagesResender = new Timer("messageResender");
    private Timer aliveChecker = new Timer("aliveChecker");

    private NodeRole role;
    private int id;
    private int masterId;
    private GameState state;
    private InetAddress masterIp;
    private int masterPort;

    public GameNode() {
        try {
            view = new JFrame("Snake");
            datagramSocket = new DatagramSocket();
            properties.load(GameNode.class.getResourceAsStream("/snake.properties"));
            logger.info("GameNode was created");
        } catch (IOException ex) {
            logger.error("Can not create GameNode.", ex);
            logger.info("Close GameNode");
            close();
        }
    }

    public void toMainMenu() {
        logger.info("Start MainMenu");
        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                close();
            }
        });
        view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        view.setSize(new Dimension(800, 500));
        JPanel mainMenu = new MainMenu(this);
        view.getContentPane().removeAll();
        view.getContentPane().add(mainMenu);
        view.revalidate();
        mainMenu.requestFocus();
        view.setLocationRelativeTo(null);
        view.setResizable(false);
        view.setVisible(true);
        logger.info("MainMenu was created.");
    }

    public void createNewGame() {
        id = 1;
        config = createNewConfig();
        GamePlayer player = createGamePlayer(id, NodeRole.MASTER, "", datagramSocket.getLocalPort(), properties.getProperty("name"));
        Snake snake = createSnake(1, createCoord(2, 2));
        GamePlayers players = GamePlayers.newBuilder()
                .addPlayers(player)
                .build();
        state = GameState.newBuilder()
                .setStateOrder(0)
                .setConfig(config)
                .setPlayers(players)
                .addSnakes(snake)
                .build();
        logger.info("Create SnakeModel");
        model = new SnakeModel(config, this);
        model.modelFromState(state);
        logger.info("Create GameMessageSender.");
        gameMessageSender = new GameMessageSender(lastMessagesTo, noAckMessages, model, datagramSocket);
        logger.info("Create GameMessageReceiver.");
        gameMessageReceiver = new GameMessageReceiver(lastMessagesFrom, gameMessageSender,
                datagramSocket, this);
        logger.info("Start GameMessageReceiver.");
        gameMessageReceiver.start();
        logger.info("Init GamePanel.");
        initGamePanel();
        logger.info("Start as master.");
        startAsMaster();
        logger.info("New game was created.");
    }

    private void initGamePanel() {
        JPanel gamePanel;
        if (role == NodeRole.VIEWER) {
            logger.info("Create Viewer panel.");
            gamePanel = new GamePanel(this).viewer();
        } else {
            logger.info("Create Player panel.");
            gamePanel = new GamePanel(this).player();
        }
        view.getContentPane().removeAll();
        view.getContentPane().add(gamePanel);
        view.revalidate();
        gamePanel.requestFocus();
        view.setSize(1500, 840);
        view.setLocationRelativeTo(null);
    }

    public void startAsMaster() {
        role = NodeRole.MASTER;
        GamePlayer player = model.getPlayerById(id);
        player = player.toBuilder()
                .setRole(NodeRole.MASTER)
                .build();
        model.addPlayer(player);
        modelUpdater = new Timer("modelUpdater");
        announceSender = new Timer("announceSender");

        logger.info("Start updating model.");
        modelUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                model.update();
                state = model.getState();
                view.getContentPane().getComponent(0).repaint();
                GamePlayers players = state.getPlayers();
                for (int i = 0; i < players.getPlayersCount(); i++) {
                    GamePlayer player = players.getPlayers(i);
                    if (player.getRole() != NodeRole.MASTER) {
                        try {
                            InetAddress address = InetAddress.getByName(players.getPlayers(i).getIpAddress());
                            gameMessageSender.sendStateMsg(id, player.getId(), state, address,
                                    players.getPlayers(i).getPort());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }, 0, state.getConfig().getStateDelayMs());

        logger.info("Start announce game.");
        announceSender.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                gameMessageSender.sendAnnouncementMsg(state);
            }
        }, 0, 1000);

        logger.info("Init messageResender.");
        initMessageResender();
        logger.info("Init aliveChecker");
        initAliveChecker();
    }

    public void updateModelFromState(GameState state) {
        if (this.state == null) {
            config = state.getConfig();
            this.state = state;
            model.modelFromState(state);
            view.getContentPane().getComponent(0).repaint();
        } else if (this.state.getStateOrder() < state.getStateOrder()) {
            System.out.println("UPDATE MODEL AND FIELD");
            this.state = state;
            model.modelFromState(state);
            view.getContentPane().getComponent(0).repaint();
        }
    }

    public void stopPlaying() {
        if (role == NodeRole.MASTER) {
            logger.info("Cancel modelUpdater");
            modelUpdater.cancel();
            logger.info("Cancel announceSender");
            announceSender.cancel();
        }
        logger.info("Interrupt GameMessageReceiver");
        gameMessageReceiver.interrupt();
        logger.info("Cancel aliveChecker");
        aliveChecker.cancel();
        logger.info("Cancel messageResender");
        messagesResender.cancel();
    }

    public void connectToAGame(boolean isViewer, String info) {

        state = null;
        config = createNewConfig();
        logger.info("Create SnakeModel");
        model = new SnakeModel(config, this);
        logger.info("Create GameMessageSender");
        gameMessageSender = new GameMessageSender(lastMessagesTo, noAckMessages, model, datagramSocket);

        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        String[] infoTokens = info.split(":");
        int port = Integer.parseInt(infoTokens[2]);
        try {
            InetAddress address = InetAddress.getByName(infoTokens[1].substring(1));
            System.out.println(address.toString() + " " + port);
            logger.info("Send JoinMessage");
            gameMessageSender.sendJoinMessage(1, properties.getProperty("name"), isViewer, address, port);
            while (true) {
                datagramSocket.receive(packet);
                logger.info("Receive packet from master");
                GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                noAckMessages.clear();
                if (gameMessage.hasError()) {
                    ErrorMsg errorMsg = gameMessage.getError();
                    JOptionPane.showMessageDialog(null, errorMsg.getErrorMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    logger.warn("Can not connect to a game");
                } else if (gameMessage.hasAck()) {
                    if (gameMessage.getMsgSeq() == 1) {
                        logger.info("Got ackMsg [seq, receiver] " + gameMessage.getMsgSeq() + " " + gameMessage.getReceiverId());
                        id = gameMessage.getReceiverId();
                        masterId = gameMessage.getSenderId();
                        masterIp = packet.getAddress();
                        masterPort = packet.getPort();
                        if (!isViewer) {
                            logger.info("Become normal");
                            role = NodeRole.NORMAL;
                        } else {
                            logger.info("Become viewer");
                            role = NodeRole.VIEWER;
                        }
                        logger.info("Start as connected");
                        startAsViewerOrNormal();
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("IOException occurred", ex);
        }
    }

    public void startAsViewerOrNormal() {
        logger.info("Init GamePanel");
        initGamePanel();
        logger.info("Create GameMessageReceiver");
        gameMessageReceiver = new GameMessageReceiver(lastMessagesFrom, gameMessageSender,
                datagramSocket, this);
        logger.info("Start GameMessageReceiver");
        gameMessageReceiver.start();
        logger.info("Init aliveChecker");
        initAliveChecker();
        logger.info("Init messageResender");
        initMessageResender();
    }

    private void initMessageResender() {
        messagesResender = new Timer("messageResender");
        int ping_delay_ms = config.getPingDelayMs();
        messagesResender.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                gameMessageSender.sendNoAckMessages();
            }
        }, 0, ping_delay_ms);
    }

    private void initAliveChecker() {
        aliveChecker = new Timer("aliveChecker");
        int ping_delay_ms = config.getPingDelayMs();
        int node_timeout_ms = config.getNodeTimeoutMs();
        aliveChecker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lastMessagesFrom.keySet().removeIf(n -> {
                    if (System.currentTimeMillis() - lastMessagesFrom.get(n) > node_timeout_ms) {
                        GamePlayer player = model.getPlayerById(n);
                        if (role == NodeRole.MASTER) {
                            model.removePlayer(n);
                            if (player.getRole() == NodeRole.DEPUTY) {
                                findNewDeputy();
                            }
                        } else if (role != NodeRole.DEPUTY) {
                            if (player.getRole() == NodeRole.MASTER) {
                                makeDeputyMaster();
                            }
                        } else {
                            startAsMaster();
                        }
                        return true;
                    }
                    return false;
                });
                gameMessageSender.sendPingMessages(id, ping_delay_ms);
            }
        }, 0, ping_delay_ms);
    }

    public void findNewDeputy() {
        boolean hasDeputy = false;
        ArrayList<GamePlayer> players = model.getPlayers();
        for (GamePlayer player : players) {
            if (player.getRole() == NodeRole.DEPUTY) {
                hasDeputy = true;
                break;
            }
        }
        if (!hasDeputy) {
            for (GamePlayer player : players) {
                if (player.getRole() == NodeRole.NORMAL) {
                    try {
                        int port = player.getPort();
                        InetAddress address = InetAddress.getByName(player.getIpAddress());
                        gameMessageSender.sendRoleChangeMsg(id, player.getId(), role, NodeRole.DEPUTY, address, port);
                        break;
                    } catch (IOException ex) {
                        logger.error("IOException occurred", ex);
                    }
                }
            }
        }
    }

    public void sendChangeRoleMessage(int receiverId, NodeRole receiverRole) {
        try {
            GamePlayer player = model.getPlayerById(receiverId);
            InetAddress address = InetAddress.getByName(player.getIpAddress());
            int port = player.getPort();
            gameMessageSender.sendRoleChangeMsg(id, receiverId, role, receiverRole, address, port);
        } catch (IOException ex) {
            logger.error("IOException occurred", ex);
        }
    }

    private void makeDeputyMaster() {
        GamePlayers players = state.getPlayers();
        for (int i = 0; i < players.getPlayersCount(); i++) {
            GamePlayer player = players.getPlayers(i);
            if (player.getRole() == NodeRole.DEPUTY) {
                try {
                    masterId = player.getId();
                    masterPort = player.getPort();
                    masterIp = InetAddress.getByName(player.getIpAddress());
                    break;
                } catch (IOException ex) {
                    logger.error("IOException occurred", ex);
                }
            }
        }
    }

    public void sendSteerMsg(Direction direction) {
        if (role == NodeRole.MASTER) {
            SteerMsg steerMsg = SteerMsg.newBuilder()
                    .setDirection(direction)
                    .build();
            GameMessage gameMessage = GameMessage.newBuilder()
                    .setSteer(steerMsg)
                    .setSenderId(id)
                    .setMsgSeq(gameMessageSender.getNextMsgSeq())
                    .build();
            addSteer(id, gameMessage);
        } else {
            gameMessageSender.sendSteerMsg(id, masterId, direction, masterIp, masterPort);
        }
    }

    public void addSteer(int id, GameMessage gameMessage) {
        model.addChangeDirection(id, gameMessage);
    }

    private Coord createCoord(int x, int y) {
        return Coord.newBuilder()
                .setX(x)
                .setY(y)
                .build();
    }

    public Snake createSnake(int id, Coord headCoord) {
        Random random = new Random();
        Direction headDirection = Direction.forNumber(random.nextInt(4) + 1);
        Coord tailCoord;
        switch (headDirection) {
            case UP: {
                tailCoord = createCoord(0, 1);
                break;
            }
            case DOWN: {
                tailCoord = createCoord(0, -1);
                break;
            }
            case RIGHT: {
                tailCoord = createCoord(-1, 0);
                break;
            }
            default: {
                tailCoord = createCoord(1, 0);
                break;
            }
        }
        return Snake.newBuilder()
                .setPlayerId(id)
                .addPoints(headCoord)
                .addPoints(tailCoord)
                .setHeadDirection(headDirection)
                .setState(Snake.SnakeState.ALIVE)
                .build();
    }

    public GamePlayer createGamePlayer(int id, NodeRole role, String ip, int port, String name) {
        return GamePlayer.newBuilder()
                .setId(id)
                .setRole(role)
                .setIpAddress(ip)
                .setPort(port)
                .setName(name)
                .setScore(0)
                .build();
    }

    private GameConfig createNewConfig() {
        return GameConfig.newBuilder()
                .setWidth(Integer.parseInt(properties.getProperty("width")))
                .setHeight(Integer.parseInt(properties.getProperty("height")))
                .setFoodPerPlayer(Integer.parseInt(properties.getProperty("food_per_player")))
                .setFoodStatic(Integer.parseInt(properties.getProperty("food_static")))
                .setStateDelayMs(Integer.parseInt(properties.getProperty("state_delay_ms")))
                .setPingDelayMs(Integer.parseInt(properties.getProperty("ping_delay_ms")))
                .setNodeTimeoutMs(Integer.parseInt(properties.getProperty("node_timeout_ms")))
                .build();
    }

    public void startReceiveAnnouncement(DefaultTableModel table) {
        logger.info("Create AnnounceMessageReceiver");
        announceMessageReceiver = new AnnounceMessageReceiver(table);
        logger.info("Start AnnounceMessageReceiver");
        announceMessageReceiver.start();
    }

    public void stopReceiveAnnouncement() {
        logger.info("Stop AnnounceMessageReceiver");
        announceMessageReceiver.close();
    }

    @Override
    public void close() {
        if (datagramSocket != null) {
            logger.info("Close datagramSocket");
            datagramSocket.close();
        }
        if (announceMessageReceiver != null) {
            logger.info("Stop AnnounceMessageReceiver");
            announceMessageReceiver.close();
        }
        if (gameMessageReceiver != null) {
            logger.info("Stop GameMessageReceiver");
            gameMessageReceiver.interrupt();
        }
        logger.info("Cancel modelUpdater");
        modelUpdater.cancel();
        logger.info("Cancel messageResender");
        messagesResender.cancel();
        logger.info("Cancel announceSender");
        announceSender.cancel();
        logger.info("Cancel aliveChecker");
        aliveChecker.cancel();
    }

    public Vector<Vector<String>> getPlayersScore() {
        Vector<Vector<String>> score = new Vector<>();
        //TODO тут nullPointer
        ArrayList<GamePlayer> players = model.getPlayers();
        for (GamePlayer player : players) {
            score.add(new Vector<>(Arrays.asList(player.getName(), Integer.toString(player.getScore()))));
        }
        return score;
    }

    private boolean isSuitableSquare(int x, int y) {
        for (int i = x; i < 5; i++) {
            for (int j = y; j < 5; j++) {
                if (model.getCellState(i, j) != Cell.State.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    public Coord findHeadCoord() {
        for (int i = 0; i < getFieldWidth() - 5; i++) {
            for (int j = 0; j < getFieldHeight() - 5; j++) {
                if (isSuitableSquare(i, j)) {
                    return createCoord(i + 2, j + 2);
                }
            }
        }
        return createCoord(-1, -1);
    }

    public void addPlayer(GamePlayer player) {
        model.addPlayer(player);
    }

    public void addSnake(Snake snake) {
        model.addSnake(snake);
    }

    public int getId() {
        return id;
    }

    public Cell.State getCellState(int x, int y) {
        return model.getCellState(x, y);
    }

    public int getFieldWidth() {
        return model.getFieldWidth();
    }

    public int getFieldHeight() {
        return model.getFieldHeight();
    }

    public int getCellId(int x, int y) {
        return model.getCellId(x, y);
    }

    public int getPlayersCount() {
        return model.getPlayersCount();
    }

    public NodeRole getRole() {
        return role;
    }

    public void setRole(NodeRole role) {
        this.role = role;
    }
}
