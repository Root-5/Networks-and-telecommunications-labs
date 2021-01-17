package emris.snakes.game.descriptors.player;

import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

public interface PlayerInfo {

    @NotNull String getName();

    int getId();

    @NotNull String getAddress();

    int getPort();

    @NotNull SnakesProto.NodeRole getRole();

    @NotNull SnakesProto.PlayerType getType();

    int getScore();

    static @NotNull PlayerInfo fromMessage(final @NotNull SnakesProto.GamePlayerOrBuilder player) {
        return new PlayerDescriptor(
                player.getName(),
                player.getId(),
                player.getIpAddress(),
                player.getPort(),
                player.getRole(),
                player.getType(),
                player.getScore());
    }

    default @NotNull SnakesProto.GamePlayer toMessage() {
        return SnakesProto.GamePlayer.newBuilder()
                .setName(this.getName())
                .setId(this.getId())
                .setIpAddress(this.getAddress())
                .setPort(this.getPort())
                .setRole(this.getRole())
                .setType(this.getType())
                .setScore(this.getScore())
                .build();
    }
}
