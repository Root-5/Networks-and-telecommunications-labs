package emris.snakes.game.descriptors.snake;

import emris.snakes.game.plane.Coordinates;
import emris.snakes.game.plane.Direction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class SnakeDescriptor implements SnakeState {
    @EqualsAndHashCode.Include
    private final int playerId;
    private final @NotNull List<@NotNull Coordinates> points;
    private final boolean alive;
    private final @NotNull Direction direction;
}
