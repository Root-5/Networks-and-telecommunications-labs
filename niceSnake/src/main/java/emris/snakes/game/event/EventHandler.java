package emris.snakes.game.event;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EventHandler {

    void handle(final @NotNull Event message) throws Exception;
}
