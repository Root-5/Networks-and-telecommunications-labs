package emris.snakes.gui.menu;

import emris.snakes.game.descriptors.config.Config;
import emris.snakes.gui.game.GameWindow;
import emris.snakes.gui.util.Colours;
import emris.snakes.gui.util.CustomScrollGUI;
import emris.snakes.gui.util.GuiUtils;
import emris.snakes.Network.message.AddressedMessage;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

import static javax.swing.BoxLayout.Y_AXIS;

public class JoinPanel extends JPanel implements RunningGamesView {

    private final @NotNull Iterable<@NotNull AddressedMessage> games;
    private final @NotNull GameJoiner gameJoiner;
    private final @NotNull JComponent mainList = new JPanel();
    private final @NotNull MenuWindow window;

    JoinPanel(
            final @NotNull MenuWindow window,
            final @NotNull Iterable<@NotNull AddressedMessage> games,
            final @NotNull GameJoiner gameJoiner) {
        super(new BorderLayout());

        this.window = window;
        this.games = games;
        this.gameJoiner = gameJoiner;

        this.setPreferredSize(new Dimension(MenuWindow.MENU_PANEL_WIDTH, MenuWindow.MENU_PANEL_HEIGHT));
        GuiUtils.setColours(this, Colours.FOREGROUND_COLOUR, Colours.INTERFACE_BACKGROUND);

        val header = new JPanel(new GridLayout(1, 5));
        val hostName = new JLabel("Host name");
        hostName.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(hostName);
        val hostAddress = new JLabel("Host address");
        hostAddress.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(hostAddress);
        val planeSize = new JLabel("Plane size");
        planeSize.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(planeSize);
        val foodCount = new JLabel("Food count");
        foodCount.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(foodCount);
        val join = new JLabel("Join");
        join.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(join);

        val expectedSlideWidth = 20;
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, expectedSlideWidth));
        this.add(header, BorderLayout.NORTH);

        this.mainList.setLayout(new BoxLayout(this.mainList, Y_AXIS));
        this.mainList.setAlignmentY(TOP_ALIGNMENT);
        val scroll = new JScrollPane(
                this.mainList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        val scrollContainer = new JPanel(new BorderLayout());
        scrollContainer.add(scroll, BorderLayout.NORTH);
        this.add(scrollContainer, BorderLayout.CENTER);

        scroll.getVerticalScrollBar().setBackground(Colours.INTERFACE_BACKGROUND);
        scroll.getHorizontalScrollBar().setBackground(Colours.INTERFACE_BACKGROUND);
        scroll.getVerticalScrollBar().setUI(new CustomScrollGUI());
        scroll.setBorder(BorderFactory.createEmptyBorder());

        GuiUtils.setColours(scroll, Colours.LINING, Colours.INTERFACE_BACKGROUND);
    }

    @Override
    public void updateView() {
        this.mainList.removeAll();

        val entries = new ArrayList<@NotNull AddressedMessage>();

        synchronized (this.games) {
            this.games.forEach(entries::add);
        }

        entries.sort(Comparator.comparingInt(it -> it.getMessage().getAnnouncement().getPlayers().getPlayersCount()));

        entries.forEach(it -> this.mainList.add(this.createGameEntry(it)));

        this.revalidate();
        this.repaint();
    }

    private @NotNull JPanel createGameEntry(final @NotNull AddressedMessage announcement) {
        val outer = new JPanel(new BorderLayout());
        val entry = new JPanel(new GridLayout(1, 5));

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

        val hostNameLabel = new JLabel(hostName);
        hostNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        entry.add(hostNameLabel);
        val address = new JLabel(hostAddress);
        address.setHorizontalAlignment(SwingConstants.CENTER);
        entry.add(address);
        val size = new JLabel(width + " x " + height);
        size.setHorizontalAlignment(SwingConstants.CENTER);
        entry.add(size);
        val food = new JLabel(String.format("%d + %.2f per player", foodStatic, foodPerPlayer));
        food.setHorizontalAlignment(SwingConstants.CENTER);
        entry.add(food);

        val joinButton = new JButton("Join");
        val finalHostName = hostName;

        joinButton.addActionListener(unused -> {
            joinButton.setEnabled(false);
            val gameView = new GameWindow();
            gameView.getExitHookRegisterer().accept(() -> {
                this.window.setVisible(true);
                joinButton.setEnabled(true);
            });
            this.gameJoiner.joinGame(
                    this.window.topPanel.getName(), Config.fromMessage(config),
                    announcement.getAddress(), gameView, () -> this.window.setVisible(false),
                    errorMessage -> {
                        JOptionPane.showMessageDialog(
                                this.window, errorMessage, "Cannot join " + finalHostName + "'s game",
                                JOptionPane.PLAIN_MESSAGE);
                        joinButton.setEnabled(true);
                    });
        });
        joinButton.setHorizontalAlignment(SwingConstants.CENTER);

        entry.add(joinButton);
        entry.setPreferredSize(new Dimension(entry.getPreferredSize().width, entry.getMinimumSize().height));

        outer.add(entry, BorderLayout.NORTH);
        outer.setPreferredSize(new Dimension(entry.getPreferredSize().width, outer.getMinimumSize().height));

        return outer;
    }
}
