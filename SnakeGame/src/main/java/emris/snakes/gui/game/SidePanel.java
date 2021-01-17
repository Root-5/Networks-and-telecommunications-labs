package emris.snakes.gui.game;

import emris.snakes.game.SnakesGameInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

final class SidePanel extends JPanel {

    static final int PREFERRED_WIDTH = 120;
    final @NotNull ScorePanel scorePanel;

    SidePanel(
            final @NotNull GameWindow view,
            final @NotNull SnakesGameInfo gameState,
            final @NotNull SnakesPanel snakesPanel) {
        super(new BorderLayout());

        this.scorePanel = new ScorePanel(view, gameState);
        this.add(this.scorePanel, BorderLayout.CENTER);
        this.add(new ControlPanel(view, snakesPanel), BorderLayout.SOUTH);

        setMaxSizeOf(this);
    }

    static void setMaxSizeOf(final @NotNull JComponent component) {
        component.setMaximumSize(new Dimension(PREFERRED_WIDTH, component.getMinimumSize().height));
    }
}
