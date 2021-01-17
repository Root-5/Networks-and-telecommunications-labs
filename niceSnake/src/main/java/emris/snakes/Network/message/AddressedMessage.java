package emris.snakes.Network.message;

import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

public final class AddressedMessage {

    private static final int MAX_RETRIES_COUNT = 3;

    private @Nullable InetSocketAddress address;
    private final @NotNull SnakesProto.GameMessage message;
    private int retriesLeft = MAX_RETRIES_COUNT;

    private AddressedMessage(
            final @NotNull SnakesProto.GameMessage message) {
        this.address = null;
        this.message = message;
    }

    private AddressedMessage(
            final @NotNull InetSocketAddress address,
            final @NotNull SnakesProto.GameMessage message) {
        this.address = address;
        this.message = message;
    }

    public static @NotNull AddressedMessage createMessageToMaster(
            final @NotNull SnakesProto.GameMessage message) {
        return new AddressedMessage(message);
    }

    public static @NotNull AddressedMessage create(
            final @NotNull InetSocketAddress address,
            final @NotNull SnakesProto.GameMessage message) {
        return new AddressedMessage(address, message);
    }

    public boolean isAddressedToMaster() {
        return this.address == null;
    }

    public boolean isAcknowledgeable() {
        return !this.message.hasAck() && !this.message.hasAnnouncement();
    }

    public @NotNull InetSocketAddress getAddress() {
        if (this.address == null) {
            throw new IllegalStateException("No destination address");
        }
        return this.address;
    }

    public void setAddress(final @NotNull InetSocketAddress address) {
        this.address = address;
    }

    public @NotNull SnakesProto.GameMessage getMessage() {
        return this.message;
    }

    public void decrementRetriesCount() {
        this.retriesLeft -= 1;
    }

    public boolean retriesLeft() {
        return this.retriesLeft > 0;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != this.getClass()) {
            return false;
        }
        return this.message.getMsgSeq() == ((AddressedMessage) other).message.getMsgSeq();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.message.getMsgSeq());
    }
}
