package emris.snakes.gui.game.javafx;

import emris.snakes.App;
import emris.snakes.Network.message.AddressedMessage;
import emris.snakes.game.descriptors.config.Config;
import emris.snakes.gui.menu.GameJoiner;
import emris.snakes.gui.menu.GameStarter;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class NewMenuWindow
{
    private @NotNull final Stage window;
    private @NotNull MenuWindowController controller;

    private final @NotNull Deque<@NotNull Runnable> exitHooks = new ArrayDeque<>();

    public NewMenuWindow(
            @NotNull Stage primaryStage,
            final @NotNull String title,
            final @NotNull Config baseConfig,
            final @NotNull Iterable<@NotNull AddressedMessage> games,
            final @NotNull GameStarter gameStarter,
            final @NotNull GameJoiner gameJoiner)
            throws IOException
    {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(App.class.getResource("/fxml/MainWindow.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        controller.setWindow(primaryStage);
        controller.setGameJoiner(gameJoiner);
        controller.setGameList(games);
        controller.setConfig(baseConfig);
        controller.setStarter(gameStarter);

        primaryStage.setTitle(title);
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> {
            exitHooks.forEach(Runnable::run);
        });

        this.window = primaryStage;
    }

    public MenuWindowController getRunningGamesView()
    {
        return controller;
    }

    public void makeVisible() {
        window.show();
    }

    public @NotNull Consumer<@NotNull Runnable> getExitHookRegisterer() {
        return this.exitHooks::push;
    }
}
