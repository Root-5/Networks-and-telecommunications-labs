package emris.snakes;

import emris.snakes.Network.MessageUtills.MessageHistory;
import emris.snakes.Network.MessageUtills.MessageReceiver;
import emris.snakes.Network.MessageUtills.MessageSender;
import emris.snakes.Network.MessageUtills.MessageTimeoutWatcher;
import emris.snakes.Network.Node;
import emris.snakes.Network.message.AddressedMessage;
import emris.snakes.game.descriptors.config.Config;
import emris.snakes.game.event.EventQueueProcessor;
import emris.snakes.game.event.events.*;
import emris.snakes.gui.game.javafx.NewMenuWindow;
import emris.snakes.util.Constants;
import emris.snakes.util.LoggedTimer;
import emris.snakes.util.Scheduler;
import emris.snakes.util.ExceptionInterfaces.UnsafeRunnable;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class App extends Application
{

    private final @NotNull Logger logger;
    private static @NotNull String arg = "5778";

    {
        tryInitLogger();
        logger = Logger.getLogger(App.class.getSimpleName());
    }

    public static void main(final String[] args) {
        arg = args[0];
        launch(args);
    }

    private void tryInitLogger() {
        try {
            @NotNull String LOGGING_PROPERTIES_FILE = "/logging.properties";
            final var config = App.class.getResourceAsStream(LOGGING_PROPERTIES_FILE);
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

    private @NotNull Thread createMulticastReceiverDaemon(
            final @NotNull UnsafeRunnable task) {
        val thread = new Thread(() -> {
            try {
                task.run();
            } catch (final Exception e) {
                logger.info(Thread.currentThread().getName() + ": " + e.getMessage());
            }
        });
        thread.setName("Multicast-Receiver-Thread");
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

    @Override
    public void start(Stage primaryStage)
    {
        var cfg = Config.DEFAULT_CONFIG;

        val eventProcessor = new EventQueueProcessor();//Обработчик сообщений: чистка сообщений, обновление поля и прочее дерьмо
        val eventProcessorThread = createEventProcessorDaemon(eventProcessor);//то, что гребёт говно выше

        val messageHistory = new MessageHistory();

        eventProcessor.addHandler(
                event -> event instanceof IncomingMessage
                        && event.<IncomingMessage>get().message.getMessage().hasAck(),
                event -> messageHistory.deliveryConfirmed(event.<IncomingMessage>get().message));

        val announcements = new HashSet<@NotNull AddressedMessage>();//объявления игр

        try
        {
            val multicastReceiverSocket = new MulticastSocket(Constants.MULTICAST_PORT);//сокет для проверки игр
            val generalPurposeSocket = new MulticastSocket(Integer.parseInt(arg));//сокет для общения с серверами
            multicastReceiverSocket.joinGroup(Constants.announceAddress,NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));

            logger.info("Running on " + generalPurposeSocket.getLocalSocketAddress());

            val sender = new MessageSender(generalPurposeSocket, messageHistory);//через EventProcess добавляет handler

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
            // General handlers

            val unicastReceiver = new MessageReceiver(generalPurposeSocket, message -> {//принимаются сообщения
                messageHistory.messageReceived(message.getAddress());
                eventProcessor.submit(new IncomingMessage(message));
            });

            val unicastReceiverThread = createUnicastReceiverDaemon(unicastReceiver);//обработчик пришедших сообщений


            val multicastReceiver = new MessageReceiver(multicastReceiverSocket, message -> {
                messageHistory.announcementReceived(message.getAddress());
                eventProcessor.submit(new Announcement(message));
            });

            val multicastReceiverThread = createMulticastReceiverDaemon(multicastReceiver);//обработчик пришедших сообщений


            val timer = new LoggedTimer();
            val scheduler = Scheduler.fromTimer(timer);

            val timeoutManager = new MessageTimeoutWatcher(
                    eventProcessor, messageHistory, cfg.getPingDelayMs(),
                    cfg.getPingDelayMs(), cfg.getNodeTimeoutMs(),
                    Constants.ANNOUNCE_DELAY_MS * 3 / 2);//чекает мертвых игроков
            val handleTimeoutsTask = scheduler.schedule(
                    timeoutManager::handleTimeouts, (cfg.getPingDelayMs() + 1) / 2);

            NewMenuWindow menuWindow = new NewMenuWindow(primaryStage, "Snakes", cfg, announcements,
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

            eventProcessorThread.start();
            unicastReceiverThread.start();
            multicastReceiverThread.start();
        } catch (final Exception e) {
            logger.severe(e.getMessage());
        }
    }
}



