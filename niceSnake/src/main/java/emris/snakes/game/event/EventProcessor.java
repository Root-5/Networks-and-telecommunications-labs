package emris.snakes.game.event;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface EventProcessor {

    @NotNull HandlerDescriptor addHandler(
            @NotNull Predicate<Event> shouldHandle,
            @NotNull EventHandler handler);

    @NotNull HandlerDescriptor addOneOffHandler(
            @NotNull Predicate<Event> shouldHandle,
            @NotNull EventHandler handler);
}
