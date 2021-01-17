package emris.snakes.gui.menu;

import emris.snakes.game.descriptors.config.Config;
import emris.snakes.Network.message.AddressedMessage;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import emris.snakes.gui.util.Colours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class MenuWindow extends JFrame {

    private static final int JOIN_TAB_INDEX = 0;
    private static final int HOST_TAB_INDEX = 1;

    private static final @NotNull String JOIN_TAB = "Join";
    private static final @NotNull String HOST_TAB = "Host";

    private static final @NotNull String JOIN_CAPTION = "Join an existing game";
    private static final @NotNull String HOST_CAPTION = "Host a new game with given parameters";
    
    static final int MENU_PANEL_WIDTH = 600;
    static final int MENU_PANEL_HEIGHT = 300;

    @Getter
    private final @NotNull RunningGamesView runningGamesView;
    @Getter
    private final @NotNull Deque<@NotNull Runnable> exitHooks = new ArrayDeque<>();

    final @NotNull TopPanel topPanel;

    public MenuWindow(
            final @NotNull String title,
            final @NotNull Config baseConfig,
            final @NotNull Iterable<@NotNull AddressedMessage> games,
            final @NotNull GameStarter gameStarter,
            final @NotNull GameJoiner gameJoiner)
            throws HeadlessException {
        super(title);

        this.exitHooks.push(this::dispose);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent unused) {
                MenuWindow.this.exitHooks.forEach(Runnable::run);
                super.windowClosing(unused);
            }
        });
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        this.topPanel = new TopPanel();
        this.topPanel.setCaption(JOIN_CAPTION);

        val tabs = new JTabbedPane(SwingConstants.LEFT);
        val joinTab = new JoinPanel(this, games, gameJoiner);
        this.runningGamesView = joinTab;
        tabs.add(JOIN_TAB, joinTab);
        tabs.add(HOST_TAB, new HostPanel(this, baseConfig, gameStarter));
        tabs.addChangeListener(unused -> {
            if (tabs.getSelectedIndex() == JOIN_TAB_INDEX) {
                this.topPanel.setCaption(JOIN_CAPTION);
            }
            if (tabs.getSelectedIndex() == HOST_TAB_INDEX) {
                this.topPanel.setCaption(HOST_CAPTION);
            }
        });

        val mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(this.topPanel, BorderLayout.NORTH);
        mainPanel.setBackground(Colours.DARK_LINING);
        mainPanel.add(tabs, BorderLayout.CENTER);
        this.getContentPane().add(mainPanel);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setResizable(false);
    }

    public void makeVisible() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }

    public @NotNull Consumer<@NotNull Runnable> getExitHookRegisterer() {
        return this.exitHooks::push;
    }
}
