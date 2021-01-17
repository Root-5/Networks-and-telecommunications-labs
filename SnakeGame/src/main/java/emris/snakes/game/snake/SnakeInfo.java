package emris.snakes.game.snake;

import org.jetbrains.annotations.NotNull;
import emris.snakes.game.plane.BoundedPoint;
import emris.snakes.game.plane.Direction;

import java.util.function.Consumer;

public interface SnakeInfo {

    @NotNull BoundedPoint getHead();

    void forEachSegment(final @NotNull Consumer<@NotNull BoundedPoint> action);

    int getPlayerId();

    @NotNull Direction getDirection();

    boolean isZombie();
}
