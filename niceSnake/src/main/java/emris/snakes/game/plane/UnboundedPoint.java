package emris.snakes.game.plane;

import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
public class UnboundedPoint {
    public int x;
    public int y;

    public @NotNull Coordinates toCoordinates() {
        return new UnboundedFixedPoint(this.x, this.y);
    }
}
