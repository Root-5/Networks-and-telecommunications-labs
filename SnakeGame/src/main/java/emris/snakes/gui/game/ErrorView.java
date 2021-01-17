package emris.snakes.gui.game;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ErrorView {

    void showErrorMessage(final @NotNull String message);
}
