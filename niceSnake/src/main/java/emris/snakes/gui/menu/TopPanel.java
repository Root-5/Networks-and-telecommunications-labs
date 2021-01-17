package emris.snakes.gui.menu;

import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import emris.snakes.gui.util.Colours;
import emris.snakes.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;

public class TopPanel extends JPanel {

    @Getter
    private @NotNull String name = "Player";

    private final @NotNull JLabel caption = new JLabel();

    TopPanel() {
        super(new BorderLayout());

        GuiUtils.setColours(this, Colours.FOREGROUND_COLOUR, Colours.INTERFACE_BACKGROUND);

        this.caption.setBorder(BorderFactory.createEmptyBorder(0, 5,0 ,0));

        val nameSelectionPanel = new JPanel(new BorderLayout());
        val name = new JLabel(this.name);
        name.setBorder(BorderFactory.createEmptyBorder(0, 0,0 ,5));
        nameSelectionPanel.add(name, BorderLayout.CENTER);

        val changeName = new JButton("Change name");
        changeName.setFocusPainted(false);
        changeName.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(3, 1, 3, 1),
                        changeName.getBorder()));
        GuiUtils.setColours(changeName, Colours.TEXT, Colours.INTERFACE_BACKGROUND);
        changeName.addActionListener(e -> {
            var newName = JOptionPane.showInputDialog(
                    null, "Enter new name", "Change name", JOptionPane.PLAIN_MESSAGE);
            if (newName == null) {
                return;
            }
            newName = newName.trim();
            if (newName.isEmpty()) {
                return;
            }
            name.setText(GuiUtils.trimNameToFitMaxLength(newName, false, false) + " ");
            this.name = newName;
        });
        nameSelectionPanel.add(changeName, BorderLayout.EAST);
        this.add(nameSelectionPanel, BorderLayout.EAST);
        this.add(this.caption, BorderLayout.WEST);
        this.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Colours.LINING),
                        BorderFactory.createEmptyBorder(0, 0, 0, 5)));
    }

    void setCaption(final @NotNull String caption) {
        this.caption.setText(caption);
    }
}
