package emris.snakes.game.snake.impl;

import org.jetbrains.annotations.NotNull;
import emris.snakes.game.plane.BoundedPoint;
import emris.snakes.game.plane.Direction;
import emris.snakes.game.snake.Snake;

@FunctionalInterface
public interface SnakeImplementationSupplier<SnakeType extends Snake> {

    @NotNull SnakeType get(
            final int id,
            final @NotNull BoundedPoint head,
            final @NotNull Direction direction,
            final int size);
}
