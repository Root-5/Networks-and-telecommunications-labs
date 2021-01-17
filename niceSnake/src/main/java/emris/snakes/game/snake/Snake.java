package emris.snakes.game.snake;

import org.jetbrains.annotations.NotNull;
import emris.snakes.game.plane.BoundedPoint;

import java.util.function.Function;

public interface Snake extends Steerable, SnakeInfo {

    void zombify();

    void move(final @NotNull Function<@NotNull BoundedPoint, @NotNull Boolean> isFood);
}
