package emris.snakes.game.snake;

import emris.snakes.game.plane.Direction;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Steerable {

    void changeDirection(final @NotNull Direction direction);
}
