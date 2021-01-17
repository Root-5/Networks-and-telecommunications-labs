package emris.snakes.game.descriptors.config;

import org.jetbrains.annotations.NotNull;

public class InvalidConfigException extends Exception {

    public InvalidConfigException(final @NotNull String message) {
        super(message);
    }
}
