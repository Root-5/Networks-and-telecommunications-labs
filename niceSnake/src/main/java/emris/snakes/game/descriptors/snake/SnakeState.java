package emris.snakes.game.descriptors.snake;

import emris.snakes.game.descriptors.Utility;
import emris.snakes.game.plane.*;
import emris.snakes.game.snake.SnakeInfo;
import lombok.val;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

public interface SnakeState {

    static @NotNull SnakeState forSnake(@NotNull final SnakeInfo snake) {
        val playerId = snake.getPlayerId();

        val points = new ArrayList<@NotNull Coordinates>();
        val lastKeyPoint = new BoundedMovablePoint(snake.getHead(), snake.getHead().getBounds());
        val previous = new BoundedMovablePoint(snake.getHead(), snake.getHead().getBounds());
        val offset = new UnboundedPoint();
        val bounds = snake.getHead().getBounds();

        // i'm not proud of this if you've been wondering
        snake.forEachSegment(it -> {
            if (it == snake.getHead()) {
                offset.x = lastKeyPoint.getX();
                offset.y = lastKeyPoint.getY();
                points.add(offset.toCoordinates());
                offset.x = 0;
                offset.y = 0;
            } else {
                if (lastKeyPoint.getX() != it.getX() && lastKeyPoint.getY() != it.getY()) {
                    lastKeyPoint.setCoordinates(previous);
                    points.add(offset.toCoordinates());
                    offset.x = 0;
                    offset.y = 0;
                }
                val dx = it.getX() - previous.getX();
                val dy = it.getY() - previous.getY();
                if (abs(dx) == 1 && abs(dy) == 0 || abs(dx) == 0 && abs(dy) == 1) { // no warp
                    offset.x += dx;
                    offset.y += dy;
                } else if (abs(dx) == bounds.getX() - 1 && abs(dy) == 0) { // x warp
                    if (dx > 0) {
                        offset.x -= 1;
                    } else {
                        offset.x += 1;
                    }
                } else if (abs(dx) == 0 && abs(dy) == bounds.getY() - 1) { // y warp
                    if (dy > 0) {
                        offset.y -= 1;
                    } else {
                        offset.y += 1;
                    }
                } else {
                    throw new IllegalStateException("Warp " + dx + ", " + dy);
                }
            }
            previous.setCoordinates(it);
        });
        points.add(offset.toCoordinates());

        val isAlive = !snake.isZombie(); // will probably fix later
        val direction = snake.getDirection();

        return new SnakeDescriptor(playerId, points, isAlive, direction);
    }

    int getPlayerId();

    List<Coordinates> getPoints();

    boolean isAlive();

    Direction getDirection();

    static @NotNull SnakeState fromMessage(@NotNull final SnakesProto.GameState.SnakeOrBuilder snake) {
        val points = new ArrayList<@NotNull Coordinates>();
        val result = new SnakeDescriptor(
                snake.getPlayerId(),
                points,
                snake.getState() == SnakesProto.GameState.Snake.SnakeState.ALIVE,
                Direction.fromNumber(snake.getHeadDirection().getNumber()));
        for (int i = 0; i < snake.getPointsCount(); i += 1) {
            val point = snake.getPoints(i);
            points.add(new UnboundedFixedPoint(point.getX(), point.getY()));
        }
        return result;
    }

    default @NotNull SnakesProto.GameState.Snake toMessage() {
        val builder = SnakesProto.GameState.Snake.newBuilder()
                .setPlayerId(this.getPlayerId())
                .setState(
                        this.isAlive()
                                ? SnakesProto.GameState.Snake.SnakeState.ALIVE
                                : SnakesProto.GameState.Snake.SnakeState.ZOMBIE)
                .setHeadDirection(
                        SnakesProto.Direction.forNumber(this.getDirection().toNumber()));
        this.getPoints().forEach(it -> builder.addPoints(Utility.coordinates(it)));
        return builder.build();
    }
}
