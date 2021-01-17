package emris.snakes.game.event;

import org.jetbrains.annotations.NotNull;

public interface Event {

    @SuppressWarnings("unchecked")
    default  <T extends Event> @NotNull T get() throws ClassCastException {
        return (T) this;
    }
}
