package emris.snakes.game.plane;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public static @NotNull Direction fromNumber(final int num) {
        return switch (num) {
            case 1 -> UP;
            case 2 -> DOWN;
            case 3 -> LEFT;
            case 4 -> RIGHT;
            default -> throw new IllegalArgumentException("Invalid direction number");
        };
    }

    public int toNumber() {
        return switch (this) {
            case UP -> 1;
            case DOWN -> 2;
            case LEFT -> 3;
            case RIGHT -> 4;
        };
    }

    public boolean isNotOppositeTo(final @NotNull Direction other) {
        return !switch (this) {
            case UP -> other == DOWN;
            case DOWN -> other == UP;
            case LEFT -> other == RIGHT;
            case RIGHT -> other == LEFT;
        };
    }

    public @NotNull Direction getOpposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    public @NotNull Direction nextClockWise() {
        return switch (this) {
            case UP -> RIGHT;
            case RIGHT -> DOWN;
            case DOWN -> LEFT;
            case LEFT -> UP;
        };
    }

    public @NotNull Direction nextCounterClockWise() {
        return switch (this) {
            case UP -> LEFT;
            case LEFT -> DOWN;
            case DOWN -> RIGHT;
            case RIGHT -> UP;
        };
    }

    public static @NotNull Direction getRandom() {
        return fromNumber(ThreadLocalRandom.current().nextInt(1, 5));
    }
}
