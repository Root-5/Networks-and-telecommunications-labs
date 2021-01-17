package emris.snakes.Network.MessageUtills;

import emris.snakes.Network.message.AddressedMessage;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import emris.snakes.util.Constants;
import emris.snakes.util.ExceptionInterfaces.UnsafeConsumer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MessageHistory {

    private static final @NotNull Logger logger = Logger.getLogger(MessageHistory.class.getSimpleName());

    private final @NotNull Map<@NotNull AddressedMessage, @NotNull Long> sendTime = new HashMap<>();
    private final @NotNull Map<@NotNull AddressedMessage, @NotNull InetSocketAddress> realDestinationAddress = new HashMap<>();
    private final @NotNull Map<@NotNull InetSocketAddress, @NotNull Long> lastSentToTime = new HashMap<>();
    private final @NotNull Map<@NotNull InetSocketAddress, @NotNull Long> receiveTime = new HashMap<>();
    private final @NotNull Map<@NotNull InetSocketAddress, @NotNull Long> announcementReceiveTime = new HashMap<>();

    public synchronized void messageSent(
            final @NotNull InetSocketAddress realAddress,
            final @NotNull AddressedMessage message) {
        val time = System.nanoTime() / Constants.MS_PER_NS;
        this.sendTime.put(message, time);
        this.lastSentToTime.put(realAddress, time);
        if (message.isAddressedToMaster()) {
            this.realDestinationAddress.put(message, realAddress);
        }
        logger.fine("Message " + message.getMessage().getMsgSeq() + " has been sent to " + realAddress);
    }

    public synchronized void deliveryConfirmed(final @NotNull AddressedMessage acknowledgeMessage) {
        this.sendTime.remove(acknowledgeMessage); // equals and hashCode use sequence number
        logger.fine("Message " + acknowledgeMessage.getMessage().getMsgSeq() + " delivery confirmed");
    }

    public synchronized void forEachNotAcknowledged(
            final int timeout,
            final @NotNull UnsafeConsumer<@NotNull AddressedMessage> action) throws Exception {
        val now = System.nanoTime() / Constants.MS_PER_NS;
        for (final var entry : this.sendTime.entrySet()) {
            val message = entry.getKey();
            val sendTime = entry.getValue();

            if (now - sendTime > timeout) {
                action.accept(message);
            }
        }
    }

    public synchronized void forEachNotContacted(
            final int timeout,
            final @NotNull UnsafeConsumer<@NotNull InetSocketAddress> action) throws Exception {
        val now = System.nanoTime() / Constants.MS_PER_NS;
        for (final var entry : this.lastSentToTime.entrySet()) {
            val address = entry.getKey();
            val sendTime = entry.getValue();

            if (now - sendTime > timeout) {
                action.accept(address);
            }
        }
    }

    public synchronized void messageReceived(final @NotNull InetSocketAddress fromAddress) {
        val time = System.nanoTime() / Constants.MS_PER_NS;
        this.receiveTime.put(fromAddress, time);
        logger.fine("Received message from " + fromAddress);
    }

    public synchronized void announcementReceived(final @NotNull InetSocketAddress fromAddress) {
        val time = System.nanoTime() / Constants.MS_PER_NS;
        this.announcementReceiveTime.put(fromAddress, time);
    }

    public synchronized void forEachOldAnnouncement(
            final int timeout,
            final @NotNull UnsafeConsumer<@NotNull InetSocketAddress> action) throws Exception {
        val now = System.nanoTime() / Constants.MS_PER_NS;
        for (final var entry : this.announcementReceiveTime.entrySet()) {
            val message = entry.getKey();
            val receiveTime = entry.getValue();

            if (now - receiveTime > timeout) {
                action.accept(message);
            }
        }
    }

    public synchronized void forEachDisconnected(
            final int timeout,
            final @NotNull UnsafeConsumer<@NotNull InetSocketAddress> action) throws Exception {
        val now = System.nanoTime() / Constants.MS_PER_NS;
        for (final var entry : this.receiveTime.entrySet()) {
            val address = entry.getKey();
            val receiveTime = entry.getValue();

            if (now - receiveTime > timeout) {
                action.accept(address);
            }
        }
    }

    public synchronized void removeAnnouncementRecord(final @NotNull InetSocketAddress fromAddress) {
        val removed = this.announcementReceiveTime.remove(fromAddress);
        logger.log((removed == null ? Level.WARNING : Level.FINE),
                "Record about announcement received from " + fromAddress
                        + (removed == null ? " hasn't been removed" : " has been removed"));
    }

    public synchronized void removeConnectionRecord(final @NotNull InetSocketAddress address) {
        var removed = this.receiveTime.remove(address);
        logger.log((removed == null ? Level.WARNING : Level.FINE),
                "Node " + address
                        + (removed == null ? " hasn't been removed" : " has been removed"));
        removed = this.lastSentToTime.remove(address);
        logger.log((removed == null ? Level.WARNING : Level.FINE),
                "Last send time for node " + address
                        + (removed == null ? " hasn't been removed" : " has been removed"));
        val messagesRemoved = this.sendTime.keySet().removeIf(
                message -> !message.isAddressedToMaster() && address.equals(message.getAddress())
                        || message.isAddressedToMaster() && address.equals(this.realDestinationAddress.get(message)));
        logger.fine(
                messagesRemoved
                        ? "Messages to " + address + " have been removed"
                        : "No messages were addressed to " + address);
        this.realDestinationAddress.keySet().removeIf(it -> this.realDestinationAddress.get(it).equals(address));
    }

    /**
     * Checks whether this node is connected to node with address {@code address}
     * @param address address of the other node
     * @return {@code true} if and only if this node received anything from the other node within timeout
     */
    public synchronized boolean isConnectedTo(final @NotNull InetSocketAddress address) {
        return this.receiveTime.containsKey(address);
    }

    public synchronized @Nullable InetSocketAddress getRealDestinationAddress(final @NotNull AddressedMessage message) {
        return this.realDestinationAddress.get(message);
    }
}
