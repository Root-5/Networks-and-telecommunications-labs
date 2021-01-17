package emris.snakes.gui.game;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import emris.snakes.gui.util.Colours;

import javax.swing.*;
import java.awt.*;

final class ControlPanel extends JPanel {

    ControlPanel(
            final @NotNull GameWindow view,
            final @NotNull SnakesPanel snakesPanel) {
        super(new GridLayout(3, 1));

        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colours.DARK_LINING));

        var inner = BorderFactory.createEmptyBorder(3, 4, 3, 4);
        val outer = BorderFactory.createMatteBorder(0, 0, 1, 0, Colours.LINING);
        inner = BorderFactory.createCompoundBorder(outer, inner);
        val border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 5, 0, 5),
                inner);

        val unbindButton = new JButton("Stop following");
//        unbindButton.setBorder(border);
        unbindButton.setFocusPainted(false);
        unbindButton.addActionListener(unused -> view.unfollow());
        this.add(unbindButton);

        val namesToggle = new JButton(snakesPanel.isShowingNames() ? "Hide names" : "Show names");
//        namesToggle.setBorder(border);
        namesToggle.setFocusPainted(false);
        namesToggle.addActionListener(unused -> {
            if (snakesPanel.isShowingNames()) {
                namesToggle.setText("Show names");
                snakesPanel.doNotShowNames();
            } else {
                namesToggle.setText("Hide names");
                snakesPanel.showNames();
            }
        });
        this.add(namesToggle);

        val leaveButton = new JButton("Give up");
//        leaveButton.setBorder(inner);
        leaveButton.setFocusPainted(false);
        leaveButton.addActionListener(unused -> {
            leaveButton.setEnabled(false);
            view.getLeaveHooks().forEach(Runnable::run);
        });
        this.add(leaveButton);
    }
}
