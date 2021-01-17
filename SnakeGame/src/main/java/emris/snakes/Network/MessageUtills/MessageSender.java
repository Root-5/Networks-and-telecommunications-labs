package emris.snakes.Network.MessageUtills;

import emris.snakes.Network.message.AddressedMessage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.*;
import emris.snakes.util.Constants;

import java.io.IOException;
import java.net.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class MessageSender {

    private static final @NotNull Logger logger = Logger.getLogger(MessageSender.class.getSimpleName());

    private final @NotNull DatagramSocket out;
    private final @NotNull MessageHistory messageHistory;

    private @Nullable Supplier<@Nullable InetSocketAddress> masterAddressSupplier;

    public void send(final @NotNull AddressedMessage message) throws IOException {
        var address = this.getActualDestinationAddress(message);
        if (address == null) {
            return;
        }
        if (this.out.getLocalPort() == address.getPort() && isThisMyIpAddress(address.getAddress())) {
            logger.info("Message addressed to self, won't send");
            return;
        }
        val contents = message.getMessage();
        val serializedSize = contents.getSerializedSize();
        if (serializedSize > Constants.MAX_PACKET_SIZE_B) {
            logger.warning(" Message serialized size is too big (" + serializedSize
                    + " > " + Constants.MAX_PACKET_SIZE_B + "), dropped");
            return;
        }

        val bytes = contents.toByteArray();
        val packet = new DatagramPacket(bytes, 0, bytes.length, address);

        if (contents.hasPing() && !this.messageHistory.isConnectedTo(address)) {
            logger.info("Destination address " + address + " is unknown for a ping message");
            return;
        }
        if (contents.hasJoin()) {
            logger.info("Sent a join request to " + address);
        }

        this.out.send(packet);
        message.decrementRetriesCount();
        logger.finest("Sent " + serializedSize + " bytes long packet to " + address);

        if (Constants.announceAddress.equals(address) || contents.hasAck()) {
            // oh yes, fire & forget, my favourite type of messaging
            return;
        }

        this.messageHistory.messageSent(address, message);
    }

    private synchronized @Nullable InetSocketAddress getActualDestinationAddress(
            final @NotNull AddressedMessage message) {
        if (!message.isAddressedToMaster()) {
            return message.getAddress();
        }
        if (this.masterAddressSupplier == null) {
            logger.warning("Message is for master but no master address supplier provided");
            return null;
        }
        val address = this.masterAddressSupplier.get();
        if (address == null) {
            logger.warning("Master address supplier returned null: is this node the master?");
            return null;
        }
        return address;
    }

    public synchronized void setMasterAddressSupplier(
            final @NotNull Supplier<@Nullable InetSocketAddress> masterAddressSupplier) {
        this.masterAddressSupplier = masterAddressSupplier;
    }


    public boolean isThisMyIpAddress(final @NotNull InetAddress address) {

        if (address.isAnyLocalAddress() || address.isLoopbackAddress())
            return true;

        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (final SocketException e) {
            return false;
        }
    }
}
