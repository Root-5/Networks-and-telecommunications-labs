package emris.snakes.game.event;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EventChannel {

    void submit(final @NotNull Event event) throws InterruptedException;
}
