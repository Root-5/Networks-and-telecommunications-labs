package emris.snakes.game.descriptors;

import lombok.experimental.UtilityClass;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import emris.snakes.game.plane.Coordinates;

@UtilityClass
public class Utility {

    public @NotNull SnakesProto.GameState.Coord coordinates(@NotNull final Coordinates coordinates) {
        return SnakesProto.GameState.Coord.newBuilder()
                .setX(coordinates.getX())
                .setY(coordinates.getY())
                .build();
    }
}
