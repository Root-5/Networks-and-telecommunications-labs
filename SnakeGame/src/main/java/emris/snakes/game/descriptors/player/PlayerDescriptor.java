package emris.snakes.game.descriptors.player;

import lombok.*;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class PlayerDescriptor implements PlayerInfo {
    private final @NotNull String name;
    @EqualsAndHashCode.Include
    private final int id;
    @Setter
    private @NotNull String address;
    @Setter
    private int port;
    private @NotNull SnakesProto.NodeRole role;
    private final @NotNull SnakesProto.PlayerType type;
    private int score = 0;

    public static @NotNull PlayerDescriptor copyOf(final @NotNull PlayerInfo other) {
        return new PlayerDescriptor(other);
    }

    private PlayerDescriptor(final @NotNull PlayerInfo other) {
        this(
                other.getName(), other.getId(), other.getAddress(),
                other.getPort(), other.getRole(), other.getType(), other.getScore());
    }

    public void setRole(final @NotNull SnakesProto.NodeRole newRole) {
        this.role = newRole;
    }

    public void increaseScore() {
        this.score += 1;
    }
}
