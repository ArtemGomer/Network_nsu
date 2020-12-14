package Model;

import me.ippolitov.fit.snakes.SnakesProto.GameState.*;

public class Cell {

    public enum State {
        EMPTY,
        FOOD,
        SNAKE_HEAD,
        SNAKE_BODY
    }

    private int id = 0;
    private final Coord coord;
    private State state;

    public Cell (int x, int y){
        state = State.EMPTY;
        coord = Coord.newBuilder()
                .setX(x)
                .setY(y)
                .build();
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Coord getCoord() {
        return coord;
    }

    public boolean isEmpty() {
        return (state==State.EMPTY);
    }

    public State getState(){
        return this.state;
    }

    public void setState(State state){
        this.state = state;
    }
}
