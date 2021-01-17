package emris.snakes.gui.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class Colours {

    public final @NotNull Color BACKGROUND_COLOUR = new Color(43, 43, 43); // Dark gray
    public final @NotNull Color FOREGROUND_COLOUR = new Color(212, 212, 212); // Almost white

    public final @NotNull Color NAME_COLOUR = FOREGROUND_COLOUR;
    public final @NotNull Color DEAD_SNAKE_COLOUR = FOREGROUND_COLOUR;
    public final @NotNull Color RED = new Color(199, 84, 80); // Red
    public final @NotNull Color GREEN = new Color(73, 156, 84); // Green
    public final @NotNull Color LIGHT_GRAY = new Color(135, 147, 154); // Light gary
    public final @NotNull Color BLUE = new Color(53, 146, 196);
    public final @NotNull Color YELLOW = new Color(244, 175, 61);

    public final @NotNull Color INTERFACE_BACKGROUND = new Color(60, 63, 65);
    public final @NotNull Color GAME_PANEL_BACKGROUND = new Color(43, 43, 43); // Dark gray
    public final @NotNull Color LIGHT_LINING = new Color(100, 100, 100);
    public final @NotNull Color LINING = new Color(81, 81, 81);
    public final @NotNull Color DARK_LINING = new Color(49, 51, 53);
    public final @NotNull Color TEXT = new Color(212, 212, 212); // Almost white
    public final @NotNull Color SCROLL_THUMB = new Color(78, 78, 78);
    public final @NotNull Color TEXT_ENTRY_FORM = new Color(69, 73, 74);
    public final @NotNull Color TOOLTIP = new Color(75, 77, 75);

    private final @NotNull Collection<@NotNull Color> snakeColours = new HashSet<>();

    static {
        snakeColours.add(LIGHT_GRAY);
        snakeColours.add(BLUE);
        snakeColours.add(YELLOW);
        snakeColours.add(Color.CYAN);
        snakeColours.add(Color.MAGENTA);
    }

    public @NotNull Color getRandomColour() {
        var num = ThreadLocalRandom.current().nextInt(0, snakeColours.size());
        for (final var t : snakeColours) {
            --num;
            if (num < 0) {
                return t;
            }
        }
        throw new AssertionError();
    }
}
