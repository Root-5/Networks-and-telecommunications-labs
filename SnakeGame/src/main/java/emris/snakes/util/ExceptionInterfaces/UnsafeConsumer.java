package emris.snakes.util.ExceptionInterfaces;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface UnsafeConsumer<T> {

    void accept(final @NotNull T value) throws Exception;
}
