package emris.snakes.gui.game.javafx;

import emris.snakes.Network.message.AddressedMessage;
import emris.snakes.game.descriptors.config.Config;
import emris.snakes.game.descriptors.config.ConfigData;
import emris.snakes.game.descriptors.config.ConfigValidator;
import emris.snakes.gui.game.GameWindow;
import emris.snakes.gui.menu.GameJoiner;
import emris.snakes.gui.menu.GameStarter;
import emris.snakes.gui.menu.RunningGamesView;
import emris.snakes.util.ExceptionInterfaces.UnsafeFunction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MenuWindowController implements RunningGamesView
{
    private GameJoiner gameJoiner;
    private Iterable<AddressedMessage> games;
    private Stage window;
    private ConfigData config;
    private GameStarter starter;

    private String playerName = "Player";

    public void setStarter(GameStarter starter)
    {
        this.starter = starter;
    }

    public void setConfig(Config config)
    {
        this.config = ConfigData.copyOf(config);

        width.setText(config.getPlaneWidth() + "");
        height.setText(config.getPlaneHeight() + "");
        foodStatic.setText(config.getFoodStatic() + "");
        foodPerPlayer.setText(config.getFoodPerPlayer() + "");
        chance.setText(config.getFoodSpawnOnDeathChance() + "");
        stateDelay.setText(config.getStateDelayMs() + "");
        pingDelay.setText(config.getPingDelayMs() + "");
        nodeTimeout.setText(config.getNodeTimeoutMs() + "");
    }

    public void setGameJoiner(GameJoiner gameJoiner)
    {
        this.gameJoiner = gameJoiner;
    }

    public void setGameList(Iterable<AddressedMessage> games)
    {
        this.games = games;
    }

    public void setWindow(Stage window)
    {
        this.window = window;
    }

    @FXML
    private VBox joinPanel;

    @FXML
    private TextField width;

    @FXML
    private TextField height;

    @FXML
    private TextField foodStatic;

    @FXML
    private TextField foodPerPlayer;

    @FXML
    private TextField chance;

    @FXML
    private TextField stateDelay;

    @FXML
    private TextField pingDelay;

    @FXML
    private TextField nodeTimeout;

    @FXML
    private Label userName;

    @FXML
    private Button startButton;

    @FXML
    private void setPlayerName()
    {
        String name = Dialogs.showInputDialog(window, "Enter new name!", "Player", "Enter new name for game session!");

        if(name == null || name.trim().isEmpty())
            return;

        playerName = name.trim();
        userName.setText(name);
    }

    @FXML
    private void startServer()
    {
        startButton.setDisable(true);
        val gameView = new GameWindow();
        gameView.getExitHookRegisterer().accept(() ->
                Platform.runLater(()-> {
                    startButton.setDisable(false);
                }));
        starter.startGame(playerName, config, gameView);
    }

    @FXML
    private void updateConfigInfo()
    {
        val toIntConverter = (UnsafeFunction<String, Integer>) Integer::valueOf;
        val toFloatConverter = (UnsafeFunction<String, Float>) Float::valueOf;
        val checker = (Supplier<Boolean>) () -> ConfigValidator.isValid(config);
        val onChanged = (Consumer<Boolean>) ok -> {
            if (ok)
                config.store();
        };

        if(!checkAndAccess(width, toIntConverter, config::setPlaneWidth, checker, onChanged))
            return;
        if(!checkAndAccess(height, toIntConverter, config::setPlaneHeight, checker, onChanged))
            return;
        if(!checkAndAccess(foodStatic, toIntConverter, config::setFoodStatic, checker, onChanged))
            return;
        if(!checkAndAccess(foodPerPlayer, toFloatConverter, config::setFoodPerPlayer, checker, onChanged))
            return;
        if(!checkAndAccess(chance, toFloatConverter, config::setFoodSpawnOnDeathChance, checker, onChanged))
            return;
        if(!checkAndAccess(stateDelay, toIntConverter, config::setStateDelayMs, checker, onChanged))
            return;
        if(!checkAndAccess(pingDelay, toIntConverter, config::setPingDelayMs, checker, onChanged))
            return;

        checkAndAccess(nodeTimeout, toIntConverter, config::setNodeTimeoutMs, checker, onChanged);
    }

    private <T extends Number> boolean checkAndAccess(
            TextField field,
            final @NotNull UnsafeFunction<@NotNull String, @NotNull T> converter,
            final @NotNull Consumer<@NotNull T> numericValueConsumer,
            final @NotNull Supplier<@NotNull Boolean> checker,
            final @NotNull Consumer<@NotNull Boolean> onChanged)
    {
        val text = field.getText();
        T value = null;
        boolean ok = true;
        try
        {
            value = converter.apply(text);
        }catch (Throwable e) {
            ok = false;
        }

        if(!ok)
        {
            Dialogs.showDefaultAlert(window, "Invalid value!", text, Alert.AlertType.ERROR);
            return false;
        }

        numericValueConsumer.accept(value);
        ok = checker.get();
        if(ok)
        {
            onChanged.accept(true);
            return true;
        }else {
            Dialogs.showDefaultAlert(window, "Can't apply changes!", "New value " + value + "is invalid!", Alert.AlertType.ERROR);
            return false;
        }
    }

    public void updateView()
    {
        Platform.runLater(() -> {
            joinPanel.getChildren().clear();

            val entries = new ArrayList<@NotNull AddressedMessage>();

            synchronized (this.games) {
                this.games.forEach(entries::add);
            }

            entries.sort(Comparator.comparingInt(it -> it.getMessage().getAnnouncement().getPlayers().getPlayersCount()));

            entries.forEach(it -> joinPanel.getChildren().add(getGameEntry(it)));
        });
    }

    private Node getGameEntry(final @NotNull AddressedMessage announcement)
    {
        val entry = new GridPane();

        val hostAddress = announcement.getAddress().getHostString();
        val contents = announcement.getMessage().getAnnouncement();

        val playersCount = contents.getPlayers().getPlayersCount();
        val config = contents.getConfig();
        val foodStatic = config.getFoodStatic();
        val foodPerPlayer = config.getFoodPerPlayer();
        val width = config.getWidth();
        val height = config.getHeight();

        var hostName = "???";
        for (int i = 0; i < playersCount; i += 1) {
            val player = contents.getPlayers().getPlayers(i);
            if (player.getIpAddress().isEmpty()
                    || player.getIpAddress().equals(hostAddress)
                    && player.getPort() == announcement.getAddress().getPort()) {
                hostName = player.getName();
            }
        }

        val hostNameLabel = new Label(hostName);
        hostNameLabel.setAlignment(Pos.TOP_CENTER);
        entry.add(hostNameLabel, 0, 0);
        val address = new Label(hostAddress);
        address.setAlignment(Pos.TOP_CENTER);
        entry.add(address, 1, 0);
        val size = new Label(width + " x " + height);
        size.setAlignment(Pos.TOP_CENTER);
        entry.add(size, 2, 0);
        val food = new Label(String.format("%d + %.2f per player", foodStatic, foodPerPlayer));
        food.setAlignment(Pos.TOP_CENTER);
        entry.add(food, 3, 0);

        val joinButton = new Button("Join");
        val finalHostName = hostName;

        joinButton.setOnAction(unused -> {
            joinButton.setDisable(true);
            val gameView = new GameWindow();
            gameView.getExitHookRegisterer().accept(() -> {
                Platform.runLater(() -> {

                    joinButton.setDisable(false);
                });
            });
            this.gameJoiner.joinGame(
                    playerName, Config.fromMessage(config),
                    announcement.getAddress(), gameView, () -> {},
                    errorMessage -> {
                        Dialogs.showDefaultAlert(window, "Cannot join " + finalHostName + "'s game", errorMessage, Alert.AlertType.ERROR);
                        joinButton.setDisable(false);
                    });
        });
        joinButton.setAlignment(Pos.TOP_CENTER);

        entry.add(joinButton, 4, 0);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(50);

        entry.getColumnConstraints().addAll(column1, column1, column1, column1, column1);

        return entry;
    }
}
