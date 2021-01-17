package emris.snakes;

import emris.snakes.Network.MessageUtills.MessageHistory;
import emris.snakes.Network.MessageUtills.MessageReceiver;
import emris.snakes.Network.MessageUtills.MessageSender;
import emris.snakes.Network.MessageUtills.MessageTimeoutWatcher;
import emris.snakes.Network.Node;
import emris.snakes.Network.message.AddressedMessage;
import emris.snakes.game.descriptors.config.Config;
import emris.snakes.game.descriptors.config.InvalidConfigException;
import emris.snakes.game.event.EventQueueProcessor;
import emris.snakes.game.event.events.*;
import emris.snakes.gui.menu.MenuWindow;
import emris.snakes.gui.util.GuiUtils;
import emris.snakes.util.Constants;
import emris.snakes.util.LoggedTimer;
import emris.snakes.util.Scheduler;
import emris.snakes.util.ExceptionInterfaces.UnsafeRunnable;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.logging.LogManager;
import java.util.logging.Logger;


@UtilityClass
public class Application {

    private final @NotNull String LOGGING_PROPERTIES_FILE = "/logging.properties";
    private final @NotNull Logger logger;

    static {
        tryInitLogger();
        logger = Logger.getLogger(Application.class.getSimpleName());
    }

    public void main(final String[] args) {

        GuiUtils.setUIComponentsColors();

        var cfg = Config.DEFAULT_CONFIG;
        try {
            cfg = Config.load();
        } catch (final @NotNull InvalidConfigException e) {
            logger.warning("Invalid config: " + e.getMessage() + ", will use default instead");
        }
        val config = cfg;

        val eventProcessor = new EventQueueProcessor();
        val eventProcessorThread = createEventProcessorDaemon(eventProcessor);

        val messageHistory = new MessageHistory();

        eventProcessor.addHandler(
                event -> event instanceof IncomingMessage
                        && event.<IncomingMessage>get().message.getMessage().hasAck(),
                event -> messageHistory.deliveryConfirmed(event.<IncomingMessage>get().message));

        val announcements = new HashSet<@NotNull AddressedMessage>();

        try (
                val multicastReceiverSocket = new MulticastSocket(Constants.MULTICAST_PORT);
                val generalPurposeSocket = new MulticastSocket(Integer.parseInt(args[0]))) {
            multicastReceiverSocket.joinGroup(Constants.announceAddress,NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));


            logger.info("Running on " + generalPurposeSocket.getLocalSocketAddress());

            val sender = new MessageSender(generalPurposeSocket, messageHistory);

            // General handlers
            eventProcessor.addHandler(
                    event -> event instanceof OutgoingMessage,
                    event -> sender.send(event.<OutgoingMessage>get().message));

            eventProcessor.addHandler(
                    event -> event instanceof NotAcknowledged,
                    event -> eventProcessor.submit(new OutgoingMessage(event.<NotAcknowledged>get().message)));

            eventProcessor.addHandler(
                    event -> event instanceof NodeTimedOut,
                    event -> messageHistory.removeConnectionRecord(event.<NodeTimedOut>get().nodeAddress));
            // /General handlers

            val unicastReceiver = new MessageReceiver(generalPurposeSocket, message -> {
                messageHistory.messageReceived(message.getAddress());
                eventProcessor.submit(new IncomingMessage(message));
            });

            val unicastReceiverThread = createUnicastReceiverDaemon(unicastReceiver);
            unicastReceiverThread.start();

            val multicastReceiver = new MessageReceiver(multicastReceiverSocket, message -> {
                messageHistory.announcementReceived(message.getAddress());
                eventProcessor.submit(new Announcement(message));
            });

            val timer = new LoggedTimer();
            val scheduler = Scheduler.fromTimer(timer);

            val timeoutManager = new MessageTimeoutWatcher(
                    eventProcessor, messageHistory, config.getPingDelayMs(),
                    config.getPingDelayMs(), config.getNodeTimeoutMs(),
                    Constants.ANNOUNCE_DELAY_MS * 3 / 2);
            val handleTimeoutsTask = scheduler.schedule(
                    timeoutManager::handleTimeouts, (config.getPingDelayMs() + 1) / 2);
            eventProcessorThread.start();

            val menuWindow = new MenuWindow(
                    "Snakes", config, announcements,
                    (name, baseConfig, view) ->
                            Node.createHost(name, baseConfig, view, scheduler, eventProcessor, eventProcessor),
                    (name, baseConfig, host, view, onSuccess, onError) -> {
                        try {
                            Node.createClient(
                                    name, baseConfig, host, view, scheduler, eventProcessor,
                                    eventProcessor, sender::setMasterAddressSupplier, onSuccess, onError);
                        } catch (final InterruptedException unused) {
                            logger.info("Interrupted when connecting to " + host);
                        }
                    });
            menuWindow.getExitHookRegisterer().accept(handleTimeoutsTask::cancel);
            menuWindow.getExitHookRegisterer().accept(timer::cancel);
            menuWindow.getExitHookRegisterer().accept(generalPurposeSocket::close);
            menuWindow.getExitHookRegisterer().accept(multicastReceiverSocket::close);
            menuWindow.makeVisible();

            val runningGamesView = menuWindow.getRunningGamesView();

            eventProcessor.addHandler(
                    event -> event instanceof Announcement,
                    event -> {
                        synchronized (announcements) {
                            val message = event.<Announcement>get().message;
                            announcements.removeIf(it -> it.getAddress().equals(message.getAddress()));
                            announcements.add(message);
                        }
                        runningGamesView.updateView();
                    });
            eventProcessor.addHandler(
                    event -> event instanceof AnnouncementTimedOut,
                    event -> {
                        val fromAddress = event.<AnnouncementTimedOut>get().fromAddress;
                        synchronized (announcements) {
                            announcements.removeIf(it -> it.getAddress().equals(fromAddress));
                        }
                        messageHistory.removeAnnouncementRecord(fromAddress);
                        runningGamesView.updateView();
                    });

            multicastReceiver.run();
        } catch (final InterruptedException e) {
            logger.info(Thread.currentThread().getName() + " interrupted");
        } catch (final Exception e) {
            logger.severe(e.getMessage());
        }
    }

    private void tryInitLogger() {
        try {
            final var config = Application.class.getResourceAsStream(LOGGING_PROPERTIES_FILE);
            if (config == null) {
                throw new IOException("Cannot load \"" + LOGGING_PROPERTIES_FILE + "\"");
            }
            LogManager.getLogManager().readConfiguration(config);
        } catch (final IOException e) {
            System.err.println("Could not setup logger configuration: " + e.getMessage());
        }
    }

    private @NotNull Thread createUnicastReceiverDaemon(
            final @NotNull UnsafeRunnable task) {
        val thread = new Thread(() -> {
            try {
                task.run();
            } catch (final Exception e) {
                logger.info(Thread.currentThread().getName() + ": " + e.getMessage());
            }
        });
        thread.setName("Unicast-Receiver-Thread");
        thread.setDaemon(true);
        return thread;
    }

    private @NotNull Thread createEventProcessorDaemon(
            final @NotNull Runnable task) {
        val thread = new Thread(task);
        thread.setName("Event-Processor-Thread");
        thread.setDaemon(true);
        return thread;
    }
}



