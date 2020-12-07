package Model;

import Control.GameNode;
import me.ippolitov.fit.snakes.SnakesProto.*;
import me.ippolitov.fit.snakes.SnakesProto.GameState.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SnakeModel {
    private int stateOrder;
    private final Map<Integer, GameMessage> directionChanges = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(SnakeModel.class);
    private final GameConfig config;
    private final Map<Integer, GamePlayer> players = new ConcurrentHashMap<>();
    private Cell[][] field;
    private final Map<Integer, Snake> snakes = new ConcurrentHashMap<>();
    private ArrayList<Coord> foods = new ArrayList<>();

    private final GameNode node;

    public SnakeModel(GameConfig config, GameNode node) {
        this.config = config;
        this.node = node;
        initField();
    }

    public ArrayList<GamePlayer> getPlayers() {
        return new ArrayList<>(players.values());
    }

    public GamePlayer getPlayerById(int id) {
        return players.get(id);
    }

    public boolean isPlayerExists(int id) {
        return players.get(id) != null;
    }


    public void modelFromState(GameState state) {
        emptyField();
        stateOrder = state.getStateOrder();
        players.clear();
        for (int i = 0; i < state.getPlayers().getPlayersCount(); i++) {
            GamePlayer player = state.getPlayers().getPlayers(i);
            players.put(player.getId(), player);
        }
        snakes.clear();
        for (int i = 0; i < state.getSnakesCount(); i++) {
            System.out.println(state.getSnakesCount());
            Snake snake = state.getSnakesList().get(i);
            snakes.put(snake.getPlayerId(), snake);
        }
        foods = new ArrayList<>(state.getFoodsList());
        putFoodToField();
        putSnakesToField();
    }

    public GameState getState() {
        stateOrder++;
        return GameState.newBuilder()
                .addAllFoods(foods)
                .addAllSnakes(snakes.values())
                .setPlayers(GamePlayers.newBuilder().addAllPlayers(players.values()))
                .setConfig(config)
                .setStateOrder(stateOrder)
                .build();
    }

    public void update() {
        emptyField();
        putFoodToField();
        changeSnakesDirection();
        moveSnakes();
        checkCollisions();
        putSnakesToField();
        createEnoughFood();
    }

    private void emptyField() {
        for (int i = 0; i < getFieldWidth(); i++) {
            for (int j = 0; j < getFieldHeight(); j++) {
                field[i][j].setState(Cell.State.EMPTY);
                field[i][j].setId(0);
            }
        }
    }

    private Coord createCoord(int x, int y) {
        return Coord.newBuilder()
                .setX(x)
                .setY(y)
                .build();
    }

    private synchronized void moveSnakes() {
        snakes.values().forEach(n -> {
            List<Coord> newPoints = new ArrayList<>();
            Coord oldHead = n.getPoints(0);
            Coord newPoint;
            switch (n.getHeadDirection()) {
                case UP: {
                    newPoint = createCoord(oldHead.getX(), (oldHead.getY() - 1 + getFieldHeight()) % getFieldHeight());
                    break;
                }
                case DOWN: {
                    newPoint = createCoord(oldHead.getX(), (oldHead.getY() + 1 + getFieldHeight()) % getFieldHeight());
                    break;
                }
                case RIGHT: {
                    newPoint = createCoord((oldHead.getX() + 1 + getFieldWidth()) % getFieldWidth(), oldHead.getY());
                    break;
                }
                default: {
                    newPoint = createCoord((oldHead.getX() - 1 + getFieldWidth()) % getFieldWidth(), oldHead.getY());
                    break;
                }
            }
            newPoints.add(newPoint);

            switch (n.getHeadDirection()) {
                case UP: {
                    newPoint = createCoord(0, 1);
                    break;
                }
                case DOWN: {
                    newPoint = createCoord(0, -1);
                    break;
                }
                case RIGHT: {
                    newPoint = createCoord(-1, 0);
                    break;
                }
                default: {
                    newPoint = createCoord(1, 0);
                    break;
                }
            }
            newPoints.add(newPoint);

            for (int i = 1; i < n.getPointsCount(); i++) {
                newPoints.add(n.getPoints(i));
            }

            Coord head = newPoints.get(0);
            if (field[head.getX()][head.getY()].getState() != Cell.State.FOOD) {

                newPoint = newPoints.get(newPoints.size() - 1);
                newPoints.remove(newPoints.size() - 1);

                if (newPoint.getX() < 0) {
                    if (newPoint.getX() != -1) {
                        newPoints.add(createCoord(newPoint.getX() + 1, 0));
                    }
                } else if (newPoint.getX() > 0) {
                    if (newPoint.getX() != 1) {
                        newPoints.add(createCoord(newPoint.getX() - 1, 0));
                    }
                } else {
                    if (newPoint.getY() < 0) {
                        if (newPoint.getY() != -1) {
                            newPoints.add(createCoord(0, newPoint.getY() + 1));
                        }
                    } else if (newPoint.getY() > 0) {
                        if (newPoint.getY() != 1) {
                            newPoints.add(createCoord(0, newPoint.getY() - 1));
                        }
                    }
                }
            } else {
                GamePlayer player = players.get(n.getPlayerId());
                player = player.toBuilder()
                        .setScore(player.getScore() + 1)
                        .build();
                players.put(player.getId(), player);
            }

            Snake newSnake = Snake.newBuilder()
                    .setHeadDirection(n.getHeadDirection())
                    .setPlayerId(n.getPlayerId())
                    .addAllPoints(newPoints)
                    .setState(Snake.SnakeState.ALIVE)
                    .build();
            snakes.put(n.getPlayerId(), newSnake);
        });
    }

    private void checkCollisions() {
        foods.removeIf(n -> {
            for (Snake snake : snakes.values()) {
                Coord head = snake.getPoints(0);
                if (head.getX() == n.getX() && head.getY() == n.getY()) {
                    return true;
                }
            }
            return false;
        });

        ArrayList<Snake> deadSnakes = new ArrayList<>();

        for (Snake snake1 : snakes.values()) {
            for (Snake snake2 : snakes.values()) {
                if (snake1 != snake2 && snake1.getPoints(0).getX() == snake2.getPoints(0).getX()
                        && snake1.getPoints(0).getY() == snake2.getPoints(0).getY()) {
                    deadSnakes.add(snake1);
                    break;
                }
            }
        }

        for (Snake snake1 : snakes.values()) {
            Coord headCoord = snake1.getPoints(0);
            for (Snake snake2 : snakes.values()) {
                if (snake1 != snake2) {
                    ArrayList<Coord> normalCoords = getNormalCoordinates(snake2);
                    for (Coord coord : normalCoords) {
                        if (headCoord.getX() == coord.getX() && headCoord.getY() == coord.getY()) {
                            if (!deadSnakes.contains(snake1)) {
                                deadSnakes.add(snake1);
                            }
                            GamePlayer player2 = getPlayerById(snake2.getPlayerId());
                            player2 = player2.toBuilder()
                                    .setScore(player2.getScore() + 1)
                                    .build();
                            players.put(player2.getId(), player2);
                            break;
                        }
                    }
                }
            }
        }

        deadSnakes.forEach(n -> {
            GamePlayer player = getPlayerById(n.getPlayerId());
            NodeRole oldRole = player.getRole();
            player = player.toBuilder()
                    .setRole(NodeRole.VIEWER)
                    .build();
            if (node.getRole() == NodeRole.MASTER && oldRole == NodeRole.DEPUTY) {
                node.sendChangeRoleMessage(player.getId(), NodeRole.VIEWER);
                node.findNewDeputy();
            }
            players.put(n.getPlayerId(), player);
            snakes.values().remove(n);
        });

    }

    public void addChangeDirection(int id, GameMessage gameMessage) {
        if (directionChanges.get(id) == null || directionChanges.get(id).getMsgSeq() < gameMessage.getMsgSeq()) {
            directionChanges.put(id, gameMessage);
        }
    }

    public void changeSnakesDirection() {
        directionChanges.keySet().forEach(n -> {
            Snake snake = snakes.get(n);
            Direction oldDirection = snake.getHeadDirection();
            Direction newDirection = directionChanges.get(n).getSteer().getDirection();
            if (!(newDirection == Direction.UP && oldDirection == Direction.DOWN) &&
                    !(newDirection == Direction.DOWN && oldDirection == Direction.UP) &&
                    !(newDirection == Direction.RIGHT && oldDirection == Direction.LEFT) &&
                    !(newDirection == Direction.LEFT && oldDirection == Direction.RIGHT)) {
                snake = snake.toBuilder()
                        .setHeadDirection(newDirection)
                        .build();
                snakes.put(n, snake);
            }
        });
        directionChanges.clear();
    }

    public synchronized void addPlayer(GamePlayer player) {
        System.out.println("ADD PLAYER");
        players.put(player.getId(), player);
    }

    public synchronized void addSnake(Snake snake) {
        System.out.println("ADD SNAKE");
        snakes.put(snake.getPlayerId(), snake);
    }

    public synchronized void removePlayer(int id) {
        players.values().removeIf(n -> n.getId() == id);
    }

    public synchronized void removeSnake(int id) {
        snakes.values().removeIf(n -> n.getPlayerId() == id);
    }

    private void initField() {
        int height = config.getHeight();
        int width = config.getWidth();
        logger.info("init [" + width + "][" + height + "] field.");
        field = new Cell[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                field[i][j] = new Cell(i, j);
            }
        }
    }


    public int getFieldWidth() {
        return config.getWidth();
    }

    public int getFieldHeight() {
        return config.getHeight();
    }

    public Cell.State getCellState(int x, int y) {
        return field[x][y].getState();
    }

    public int getCellId(int x, int y) {
        return field[x][y].getId();
    }

    private ArrayList<Coord> getNormalCoordinates(Snake snake) {
        ArrayList<Coord> normalCoords = new ArrayList<>();
        normalCoords.add(snake.getPoints(0));
        Coord curCoord = snake.getPoints(0);
        for (int i = 1; i < snake.getPointsCount(); i++) {
            int xOffset = snake.getPoints(i).getX();
            int yOffset = snake.getPoints(i).getY();
            if (xOffset == 0) {
                if (yOffset < 0) {
                    for (int j = -1; j >= yOffset; j--) {
                        normalCoords.add(createCoord(curCoord.getX(), (curCoord.getY() + j + getFieldHeight()) % getFieldHeight()));
                    }
                } else {
                    for (int j = 1; j <= yOffset; j++) {
                        normalCoords.add(createCoord(curCoord.getX(), (curCoord.getY() + j + getFieldHeight()) % getFieldHeight()));
                    }
                }
            } else if (yOffset == 0) {
                if (xOffset < 0) {
                    for (int j = -1; j >= xOffset; j--) {
                        normalCoords.add(createCoord((curCoord.getX() + j + getFieldWidth()) % getFieldWidth(), curCoord.getY()));
                    }
                } else {
                    for (int j = 1; j <= xOffset; j++) {
                        normalCoords.add(createCoord((curCoord.getX() + j + getFieldWidth()) % getFieldWidth(), curCoord.getY()));
                    }
                }
            }
            curCoord = createCoord((curCoord.getX() + xOffset + getFieldWidth()) % getFieldWidth(),
                    (curCoord.getY() + yOffset + getFieldHeight()) % getFieldHeight());
        }
        return normalCoords;
    }

    private void putSnakesToField() {
        snakes.values().forEach(n -> {
            List<Coord> coords = n.getPointsList();
            Coord headCoord = coords.get(0);
            field[headCoord.getX()][headCoord.getY()].setState(Cell.State.SNAKE_HEAD);
            field[headCoord.getX()][headCoord.getY()].setId(n.getPlayerId());
            Coord curCoord = headCoord;
            for (int i = 1; i < coords.size(); i++) {
                int xOffset = coords.get(i).getX();
                int yOffset = coords.get(i).getY();
                if (xOffset == 0) {
                    if (yOffset < 0) {
                        for (int j = -1; j >= yOffset; j--) {
                            field[curCoord.getX()][(curCoord.getY() + j + getFieldHeight()) % getFieldHeight()].setState(Cell.State.SNAKE_BODY);
                            field[curCoord.getX()][(curCoord.getY() + j + getFieldHeight()) % getFieldHeight()].setId(n.getPlayerId());
                        }
                    } else {
                        for (int j = 1; j <= yOffset; j++) {
                            field[curCoord.getX()][(curCoord.getY() + j + getFieldHeight()) % getFieldHeight()].setState(Cell.State.SNAKE_BODY);
                            field[curCoord.getX()][(curCoord.getY() + j + getFieldHeight()) % getFieldHeight()].setId(n.getPlayerId());
                        }
                    }
                } else if (yOffset == 0) {
                    if (xOffset < 0) {
                        for (int j = -1; j >= xOffset; j--) {
                            field[(curCoord.getX() + j + getFieldWidth()) % getFieldWidth()][curCoord.getY()].setState(Cell.State.SNAKE_BODY);
                            field[(curCoord.getX() + j + getFieldWidth()) % getFieldWidth()][curCoord.getY()].setId(n.getPlayerId());
                        }
                    } else {
                        for (int j = 1; j <= xOffset; j++) {
                            field[(curCoord.getX() + j + getFieldWidth()) % getFieldWidth()][curCoord.getY()].setState(Cell.State.SNAKE_BODY);
                            field[(curCoord.getX() + j + getFieldWidth()) % getFieldWidth()][curCoord.getY()].setId(n.getPlayerId());
                        }
                    }
                }
                curCoord = createCoord((curCoord.getX() + xOffset + getFieldWidth()) % getFieldWidth(),
                        (curCoord.getY() + yOffset + getFieldHeight()) % getFieldHeight());
            }
        });
    }

    private void putFoodToField() {
        foods.forEach(n -> field[n.getX()][n.getY()].setState(Cell.State.FOOD));
    }

    private void createEnoughFood() {
        int foodAmount = config.getFoodStatic() + (int) (players.size() * config.getFoodPerPlayer()) - foods.size();
        if (foodAmount > 0) {

            List<Cell> emptyCells = new ArrayList<>();
            for (int i = 0; i < getFieldWidth(); i++) {
                for (int j = 0; j < getFieldHeight(); j++) {
                    if (field[i][j].isEmpty()) {
                        emptyCells.add(field[i][j]);
                    }
                }
            }

            Random random = new Random();
            for (int i = 0; i < foodAmount; i++) {
                int index = random.nextInt(emptyCells.size());
                foods.add(emptyCells.get(index).getCoord());
                emptyCells.remove(index);
            }
        }
    }

    public int getPlayersCount() {
        return players.size();
    }
}
