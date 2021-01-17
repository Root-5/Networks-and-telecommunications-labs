package emris.snakes.game.plane;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
public class UnboundedFixedPoint implements Coordinates {
    private final int x;
    private final int y;
}
