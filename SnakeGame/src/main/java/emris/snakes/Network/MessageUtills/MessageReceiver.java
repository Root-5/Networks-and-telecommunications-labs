package emris.snakes.Network.MessageUtills;

import emris.snakes.Network.message.AddressedMessage;
import emris.snakes.util.Constants;
import emris.snakes.util.ExceptionInterfaces.UnsafeConsumer;
import emris.snakes.util.ExceptionInterfaces.UnsafeRunnable;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class MessageReceiver implements UnsafeRunnable {

    private static final @NotNull Logger logger = Logger.getLogger(MessageReceiver.class.getSimpleName());

    private final @NotNull DatagramSocket in;
    private final @NotNull UnsafeConsumer<@NotNull AddressedMessage> onReceived;

    private final byte[] buffer = new byte[Constants.MAX_PACKET_SIZE_B];

    @Override
    public void run() throws IOException
    {
        while (!Thread.currentThread().isInterrupted()) {
            val packet = new DatagramPacket(this.buffer, 0, this.buffer.length);

            try {
                this.in.receive(packet);
                logger.finest(" Received " + packet.getLength() + " bytes long packet from "
                        + packet.getSocketAddress());
                if (!(packet.getSocketAddress() instanceof InetSocketAddress)) {
                    logger.info(" Unsupported remote socket address: "
                            + packet.getSocketAddress().getClass().getName());
                    continue;
                }
                if (this.in.getPort() == packet.getPort() && isThisMyIpAddress(packet.getAddress())) {
                    logger.info("Received a packet from self, dropping");
                    continue;
                }
                val fromAddress = (InetSocketAddress) packet.getSocketAddress();

                val data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                try {
                    val contents = SnakesProto.GameMessage.parseFrom(data);
                    this.onReceived.accept(AddressedMessage.create(fromAddress, contents));
                } catch (final Exception e) {
                    logger.info("Received invalid message: " + e.getMessage());
                }
            } catch (final SocketException e) {
                logger.info("SocketException: " + e.getMessage());
                return;
            }
        }
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
