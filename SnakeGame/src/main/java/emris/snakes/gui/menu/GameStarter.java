package emris.snakes.gui.menu;

import emris.snakes.game.descriptors.config.Config;
import org.jetbrains.annotations.NotNull;
import emris.snakes.gui.game.SnakesGameView;

@FunctionalInterface
public interface GameStarter {

    void startGame(
            final @NotNull String playerName,
            final @NotNull Config gameConfig,
            final @NotNull SnakesGameView gameView);
}
