package emris.snakes.gui.menu;

import emris.snakes.game.descriptors.config.Config;
import emris.snakes.gui.game.SnakesGameView;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

@FunctionalInterface
public interface GameJoiner {

    void joinGame(
            final @NotNull String playerName,
            final @NotNull Config gameConfig,
            final @NotNull InetSocketAddress hostAddress,
            final @NotNull SnakesGameView gameView,
            final @NotNull Runnable onSuccess,
            final @NotNull Consumer<@NotNull String> onError);
}
