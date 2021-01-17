package emris.snakes.game.descriptors.game;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import emris.snakes.game.descriptors.config.Config;
import emris.snakes.game.descriptors.snake.SnakeState;
import emris.snakes.game.plane.Coordinates;
import emris.snakes.game.descriptors.player.PlayerInfo;

@Getter
@ToString
public class GameDescriptor implements GameState {
    private int stateOrder;
    private final @NotNull Iterable<@NotNull SnakeState> snakes;
    private final @NotNull Iterable<@NotNull Coordinates> food;
    private final @NotNull Iterable<@NotNull PlayerInfo> players;
    private final @NotNull Config config;

    public GameDescriptor(
            final int stateOrder,
            final @NotNull Iterable<@NotNull SnakeState> snakes,
            final @NotNull Iterable<@NotNull Coordinates> food,
            final @NotNull Iterable<@NotNull PlayerInfo> players,
            final @NotNull Config config) {
        this.stateOrder = stateOrder;
        this.snakes = snakes;
        this.food = food;
        this.players = players;
        this.config = config;
    }

    private GameDescriptor(final @NotNull GameState other) {
        this(other.getStateOrder(), other.getSnakes(), other.getFood(), other.getPlayers(), other.getConfig());
    }

    public static @NotNull GameDescriptor fromOther(final @NotNull GameState other) {
        return new GameDescriptor(other);
    }

    public void incrementStateOrder() {
        this.stateOrder += 1;
    }

    public void setStateOrder(final int stateOrder) {
        this.stateOrder = stateOrder;
    }
}
