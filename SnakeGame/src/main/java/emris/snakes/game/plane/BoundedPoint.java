package emris.snakes.game.plane;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface BoundedPoint extends Coordinates {

    @NotNull Coordinates getBounds();

    @NotNull BoundedPoint copy();

    @Contract(mutates = "this")
    void centerRelativeTo(final @NotNull BoundedPoint other);

}
