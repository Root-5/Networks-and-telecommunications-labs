package emris.snakes.gui.game;

import emris.snakes.game.SnakesGameInfo;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import emris.snakes.gui.util.Colours;
import emris.snakes.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;

final class GamePanel extends JPanel {

    final @NotNull SnakesPanel snakesPanel;
    final @NotNull SidePanel sidePanel;

    GamePanel(
            final @NotNull GameWindow view,
            final @NotNull SnakesGameInfo gameState) {
        super(new BorderLayout());

        this.snakesPanel = new SnakesPanel(view, gameState);
        this.sidePanel = new SidePanel(view, gameState, this.snakesPanel);

        this.add(this.snakesPanel, BorderLayout.WEST);
        val sep = new JSeparator(SwingConstants.VERTICAL);
        GuiUtils.setColours(sep, Colours.LINING, Colours.BACKGROUND_COLOUR);
        this.add(sep, BorderLayout.CENTER);
        this.add(this.sidePanel, BorderLayout.EAST);
    }

    void update() {
        this.snakesPanel.repaint();
        this.sidePanel.scorePanel.updateScores();
    }
}
