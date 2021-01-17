package emris.snakes.gui.game;

import emris.snakes.game.SnakesGameInfo;
import lombok.val;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import emris.snakes.gui.util.Colours;
import emris.snakes.gui.util.CustomScrollGUI;
import emris.snakes.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;

import static javax.swing.BoxLayout.Y_AXIS;

final class ScorePanel extends JPanel {

    private final @NotNull SnakesGameView view;
    private final @NotNull SnakesGameInfo gameState;

    private final @NotNull JComponent mainList = new JPanel();

    ScorePanel(
            final @NotNull SnakesGameView view,
            final @NotNull SnakesGameInfo gameState) {
        this.setLayout(new BorderLayout());

        this.view = view;
        this.gameState = gameState;

        val title = new JLabel("Score");
        title.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colours.DARK_LINING));

        this.add(title, BorderLayout.NORTH);
        this.mainList.setLayout(new BoxLayout(this.mainList, Y_AXIS));
        this.mainList.setAlignmentY(TOP_ALIGNMENT);
        val scroll = new JScrollPane(
                this.mainList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        this.add(scroll, BorderLayout.CENTER);

        scroll.getVerticalScrollBar().setBackground(Colours.INTERFACE_BACKGROUND);
        scroll.getHorizontalScrollBar().setBackground(Colours.INTERFACE_BACKGROUND);
        scroll.getVerticalScrollBar().setUI(new CustomScrollGUI());
        scroll.setBorder(BorderFactory.createEmptyBorder());

        GuiUtils.setColours(scroll, Colours.LINING, Colours.INTERFACE_BACKGROUND);
    }

    void updateScores() {
        SwingUtilities.invokeLater(this::doUpdateScore);
    }

    private void doUpdateScore() {
        this.mainList.removeAll();
        val no = new int[] { 1 };
        synchronized (this.gameState) {
            this.gameState.forEachPlayer(it -> {
                if (this.gameState.hasSnakeWithPlayerId(it.getId())) {
                    val snake = this.gameState.getSnakeById(it.getId());
                    if (snake.isZombie()) {
                        return;
                    }
                    val scoreEntry = new JPanel(new BorderLayout());
                    final int thisEntryPlayerId = it.getId();

                    val nameAndNo = new JPanel();

                    val name = new JButton(GuiUtils.trimNameToFitMaxLength(
                            it.getName(),
                            it.getType() == SnakesProto.PlayerType.ROBOT,
                            this.gameState.getSnakeById(it.getId()).isZombie()));
                    name.setBorder(BorderFactory.createEmptyBorder());
                    name.addActionListener(unused -> this.view.follow(thisEntryPlayerId));
                    GuiUtils.setColours(name, this.view.getColour(it.getId()), Colours.INTERFACE_BACKGROUND);
                    SidePanel.setMaxSizeOf(name);
                    val noLabel = new JLabel(no[0] + ". ");
                    nameAndNo.add(noLabel);
                    nameAndNo.add(name);
                    SidePanel.setMaxSizeOf(nameAndNo);

                    val score = new JLabel(it.getScore() + " "); // hell yeah DeSiGn
                    SidePanel.setMaxSizeOf(score);
                    score.setAlignmentX(RIGHT_ALIGNMENT);

                    scoreEntry.add(nameAndNo, BorderLayout.WEST);
                    scoreEntry.add(score, BorderLayout.EAST);

                    SidePanel.setMaxSizeOf(scoreEntry);

                    this.mainList.add(scoreEntry);

                    no[0] += 1;
                }
            });
        }
        this.revalidate();
        this.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        val def = super.getPreferredSize();
        return new Dimension(SidePanel.PREFERRED_WIDTH, def.height);
    }
}
