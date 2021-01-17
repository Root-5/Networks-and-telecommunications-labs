package emris.snakes.Network.message;

import lombok.experimental.UtilityClass;
import lombok.val;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import emris.snakes.game.descriptors.game.GameState;
import emris.snakes.game.plane.Direction;

import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class MessageFactory {

    private final @NotNull AtomicLong sequenceNumber = new AtomicLong(0);

    private @NotNull SnakesProto.GameMessage.Builder createBuilderWithSequenceNumber() {
        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(sequenceNumber.getAndIncrement());
    }

    public @NotNull SnakesProto.GameMessage createPingMessage() {
        return createBuilderWithSequenceNumber()
                .setPing(
                        SnakesProto.GameMessage.PingMsg.newBuilder()
                                .build())
                .build();
    }

    public @NotNull SnakesProto.GameMessage createSteerMessage(final @NotNull Direction direction) {
        return createBuilderWithSequenceNumber()
                .setSteer(
                        SnakesProto.GameMessage.SteerMsg.newBuilder()
                                .setDirection(SnakesProto.Direction.valueOf(direction.toString()))
                                .build())
                .build();
    }

    public @NotNull SnakesProto.GameMessage createAcknowledgementMessage(
            final long sequenceNumber,
            final int senderId,
            final int receiverId) {
        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(sequenceNumber)
                .setAck(
                        SnakesProto.GameMessage.AckMsg.newBuilder()
                                .build())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }

    public @NotNull SnakesProto.GameMessage createStateMessage(final @NotNull SnakesProto.GameState state) {
        return createBuilderWithSequenceNumber()
                .setState(
                        SnakesProto.GameMessage.StateMsg.newBuilder()
                                .setState(state)
                                .build())
                .build();
    }

    public @NotNull SnakesProto.GameMessage createStateMessage(final @NotNull GameState state) {
        return createBuilderWithSequenceNumber()
                .setState(
                        SnakesProto.GameMessage.StateMsg.newBuilder()
                                .setState(state.toMessage())
                                .build())
                .build();
    }

    public @NotNull SnakesProto.GameMessage createAnnouncementMessage(
            final @NotNull GameState state) {
        val playersBuilder = SnakesProto.GamePlayers.newBuilder();
        state.getPlayers().forEach(it -> playersBuilder.addPlayers(it.toMessage()));

        return createBuilderWithSequenceNumber()
                .setAnnouncement(
                        SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                                .setConfig(state.getConfig().toMessage())
                                .setPlayers(playersBuilder.build()))
                .build();

    }

    public @NotNull SnakesProto.GameMessage createJoinMessage(
            final @NotNull String name,
            final boolean isBot,
            final boolean watchOnly) {
        return createBuilderWithSequenceNumber()
                .setJoin(
                        SnakesProto.GameMessage.JoinMsg.newBuilder()
                                .setName(name)
                                .setOnlyView(watchOnly)
                                .setPlayerType(isBot ? SnakesProto.PlayerType.ROBOT : SnakesProto.PlayerType.HUMAN)
                                .build())
                .build();
    }

    public @NotNull SnakesProto.GameMessage createErrorMessage(final @NotNull String what) {
        return createBuilderWithSequenceNumber()
                .setError(
                        SnakesProto.GameMessage.ErrorMsg.newBuilder()
                                .setErrorMessage(what)
                                .build())
                .build();
    }

    public @NotNull SnakesProto.GameMessage createRoleChangingMessage(
            final @NotNull SnakesProto.NodeRole senderRole,
            final @NotNull SnakesProto.NodeRole receiverRole,
            final int senderId,
            final int receiverId) {
        return createBuilderWithSequenceNumber()
                .setRoleChange(
                        SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                .setSenderRole(senderRole)
                                .setReceiverRole(receiverRole)
                                .build())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }

    public @NotNull SnakesProto.GameMessage createRoleChangingMessage(
            final @NotNull SnakesProto.NodeRole senderRole,
            final int senderId,
            final int receiverId) {
        return createBuilderWithSequenceNumber()
                .setRoleChange(
                        SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                .setSenderRole(senderRole)
                                .build())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }
}
