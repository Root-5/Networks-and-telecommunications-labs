package emris.snakes.game;

import emris.snakes.game.descriptors.game.GameState;
import emris.snakes.game.descriptors.player.PlayerInfo;
import emris.snakes.game.plane.BoundedPoint;
import emris.snakes.game.plane.Coordinates;
import emris.snakes.game.snake.SnakeInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface SnakesGameInfo {

    void forEachSnake(final @NotNull Consumer<@NotNull SnakeInfo> action);

    @NotNull SnakeInfo getSnakeById(final int playerId);

    @Nullable SnakeInfo getAnySnake();

    void forEachFood(final @NotNull Consumer<@NotNull BoundedPoint> action);

    boolean isFood(final @NotNull BoundedPoint point);

    void forEachPlayer(final @NotNull Consumer<@NotNull PlayerInfo> action);

    @NotNull PlayerInfo getPlayerById(final int playerId);

    boolean hasPlayerWithId(final int playerId);

    boolean hasSnakeWithPlayerId(final int playerId);

    @Contract(pure = true)
    @NotNull Coordinates getPlaneBounds();

    @NotNull GameState getState();

    int getStateOrder();
}
