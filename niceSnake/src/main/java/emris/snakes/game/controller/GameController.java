package emris.snakes.game.controller;

import emris.snakes.game.descriptors.config.Config;
import emris.snakes.game.descriptors.game.GameState;
import emris.snakes.game.event.EventChannel;
import emris.snakes.game.event.EventProcessor;
import emris.snakes.game.event.events.IncomingMessage;
import emris.snakes.game.event.events.OutgoingMessage;
import emris.snakes.game.GameModel;
import emris.snakes.gui.game.SnakesGameView;
import emris.snakes.gui.util.Colours;
import emris.snakes.Network.message.AddressedMessage;
import emris.snakes.Network.message.MessageFactory;
import emris.snakes.game.plane.Direction;
import lombok.val;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.net.InetSocketAddress;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class GameController {

    private static final @NotNull Logger logger = Logger.getLogger(GameController.class.getSimpleName());

    private @NotNull InetSocketAddress host;
    private int hostId;
    private final @NotNull SnakesGameView view;
    private final int ownId;
    private final @NotNull Config config;
    private final @NotNull EventProcessor in;
    private final @NotNull EventChannel out;

    private final @NotNull GameModel game;

    private GameController(
            final @NotNull InetSocketAddress host,
            final int hostId,
            final @NotNull SnakesGameView view,
            final int ownId,
            final @NotNull Config config,
            final @NotNull EventProcessor in,
            final @NotNull EventChannel out) {
        this.host = host;
        this.hostId = hostId;
        this.view = view;
        this.ownId = ownId;
        this.config = config;
        this.in = in;
        this.out = out;

        this.game = new GameModel(config);
        this.view.bindTo(this.game);
        this.view.setPreferredColour(ownId, Colours.GREEN);
        this.view.follow(ownId);
        bindRemoteToControls(host, out, view.getKeyBindingsRegisterer());
        this.setStateHandler();
        this.view.makeVisible();
    }

    private static void bindRemoteToControls(
            final @NotNull InetSocketAddress hostAddress,
            final @NotNull EventChannel out,
            final @NotNull BiConsumer<@NotNull Integer, @NotNull Runnable> keyRegisterer) {
        keyRegisterer.accept(
                KeyEvent.VK_UP,
                () -> submitSuppressInterrupt(
                        out,
                        AddressedMessage.create(hostAddress, MessageFactory.createSteerMessage(Direction.UP))));
        keyRegisterer.accept(
                KeyEvent.VK_DOWN,
                () -> submitSuppressInterrupt(
                        out,
                        AddressedMessage.create(hostAddress, MessageFactory.createSteerMessage(Direction.DOWN))));
        keyRegisterer.accept(
                KeyEvent.VK_LEFT,
                () -> submitSuppressInterrupt(
                        out,
                        AddressedMessage.create(hostAddress, MessageFactory.createSteerMessage(Direction.LEFT))));
        keyRegisterer.accept(
                KeyEvent.VK_RIGHT,
                () -> submitSuppressInterrupt(
                        out,
                        AddressedMessage.create(hostAddress, MessageFactory.createSteerMessage(Direction.RIGHT))));
    }

    private static void submitSuppressInterrupt(
            final @NotNull EventChannel out,
            final @NotNull AddressedMessage message) {
        try {
            out.submit(new OutgoingMessage(message));
        } catch (final InterruptedException ignored) {
            logger.info(Thread.currentThread().getName() + " Interrupted when submitting steer message");
        }
    }

    public static @NotNull GameController createAndShow(
            final @NotNull InetSocketAddress host,
            final int hostId,
            final @NotNull SnakesGameView view,
            final int ownId,
            final @NotNull Config config,
            final @NotNull EventProcessor in,
            final @NotNull EventChannel out) {
        return new GameController(host, hostId, view, ownId, config, in, out);
    }

    private void setStateHandler() {
        this.in.addHandler(
                event -> event instanceof IncomingMessage
                        && event.<IncomingMessage>get().message.getMessage().hasState(),
                event -> this.updateState(event.<IncomingMessage>get().message));
    }

    private void updateState(final @NotNull AddressedMessage message) throws InterruptedException {
        val fromAddress = message.getAddress();
        val gameMessage = message.getMessage();

        if (!gameMessage.hasState()) {
            logger.warning("Non-state message passed to state message handler");
            return;
        }
        if (!fromAddress.equals(this.host)) {
            logger.info("Received state but sender is not the host");
            return;
        }

        val stateMessage = gameMessage.getState();

        synchronized (this.game) {
            this.game.setState(GameState.fromMessage(stateMessage.getState()));
            this.view.updateView();
            logger.finest("Updated game state");
            this.submitAcknowledged(gameMessage.getMsgSeq());
        }
    }

    private void submitAcknowledged(final long seq) throws InterruptedException {
        this.out.submit(
                new OutgoingMessage(
                        AddressedMessage.createMessageToMaster(
                                MessageFactory.createAcknowledgementMessage(seq, this.ownId, this.hostId))));
    }

    public void handleMessage(
            final @NotNull InetSocketAddress from,
            final SnakesProto.@NotNull GameMessage message) {
    }

    public @NotNull SnakesProto.GameState getGameState() {
        synchronized (this.game) {
            return this.game.getState().toMessage();
        }
    }
}
