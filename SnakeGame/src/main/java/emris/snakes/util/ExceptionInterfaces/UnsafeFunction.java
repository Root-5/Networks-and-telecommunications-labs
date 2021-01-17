package emris.snakes.util.ExceptionInterfaces;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface UnsafeFunction<ArgumentT, ResultT> {

    @NotNull ResultT apply(final @NotNull ArgumentT argument) throws Exception;
}
