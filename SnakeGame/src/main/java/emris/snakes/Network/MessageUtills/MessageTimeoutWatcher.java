package emris.snakes.Network.MessageUtills;

import emris.snakes.game.event.EventChannel;
import emris.snakes.game.event.events.AnnouncementTimedOut;
import emris.snakes.game.event.events.NodeTimedOut;
import emris.snakes.game.event.events.NotAcknowledged;
import emris.snakes.game.event.events.TimeToPing;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.net.*;
import java.util.HashSet;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class MessageTimeoutWatcher {

    private static final @NotNull Logger logger = Logger.getLogger(MessageTimeoutWatcher.class.getSimpleName());

    private final @NotNull EventChannel out;
    private final @NotNull MessageHistory messageHistory;

    private final int ackTimeout;
    private final int pingTimeout;
    private final int nodeTimeout;
    private final int announcementTimeout;

    public void handleTimeouts() throws Exception {
        synchronized (this.messageHistory) {
            val toRemove = new HashSet<@NotNull InetSocketAddress>();

            logger.finest("Checking last received time");
            this.messageHistory.forEachDisconnected(this.nodeTimeout, address -> {
                logger.fine("Haven't received anything from " + address + " for longer than timeout");
                toRemove.add(address);
                this.out.submit(new NodeTimedOut(address));
            });

            toRemove.forEach(this.messageHistory::removeConnectionRecord);
            toRemove.clear();

            logger.finest("Checking sent messages");
            this.messageHistory.forEachNotAcknowledged(this.ackTimeout, message -> {
                logger.finest(
                        "Message " + message.getMessage().getMsgSeq() + " addressed to "
                                + (message.isAddressedToMaster() ? "master" : message.getAddress())
                                + " not acknowledged within timeout");
                val toAddress = message.isAddressedToMaster()
                        ? this.messageHistory.getRealDestinationAddress(message)
                        : message.getAddress();
                if (toAddress == null) {
                    logger.warning("No real destination address for message " + message.getMessage().getMsgSeq());
                } else {
                    // For special cases when this node started sending messages to another node
                    // without knowing anything about it (i.e. if it's even online)
                    if (!message.retriesLeft() && !this.messageHistory.isConnectedTo(toAddress)) {
                        // If a message has no retries left and no messages were ever received from it's destination
                        // generate a NodeTimedOut event to tell whoever who's been sending the message to stop
                        // sending anything to this destination
                        this.out.submit(new NodeTimedOut(toAddress));
                        toRemove.add(toAddress);
                        logger.info("Submitted NodeTimedOut event");
                    }
                }
                if (message.retriesLeft()) {
                    this.out.submit(new NotAcknowledged(message));
                }
            });

            toRemove.forEach(this.messageHistory::removeConnectionRecord);

            logger.finest("Checking last contacted to time");
            this.messageHistory.forEachNotContacted(this.pingTimeout, address -> {
                logger.fine("Haven't sent anything to " + address + " for longer than timeout");
                this.out.submit(new TimeToPing(address));
            });

            logger.finest("Checking announcements receive time");
            this.messageHistory.forEachOldAnnouncement(this.announcementTimeout, address -> {
                logger.finest("Announcement form " + address + " timed out");
                this.out.submit(new AnnouncementTimedOut(address));
            });
        }
    }
}
