package emris.snakes.game.plane;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@ToString
@EqualsAndHashCode
public class BoundedMovablePoint implements BoundedPoint {
    private final int boundX;
    private final int boundY;

    @Getter
    private int x;
    @Getter
    private int y;

    public BoundedMovablePoint(final int x, final int y, final @NotNull Coordinates bounds) {
        if (bounds.getX() <= 0 || bounds.getY() <= 0) {
            throw new IllegalArgumentException("Invalid bounds");
        }
        this.boundX = bounds.getX();
        this.boundY = bounds.getY();

        this.x = x;
        this.y = y;

        this.makeFitBounds();
    }

    public BoundedMovablePoint(final @NotNull Coordinates coordinates, final @NotNull Coordinates bounds) {
        if (bounds.getX() <= 0 || bounds.getY() <= 0) {
            throw new IllegalArgumentException("Invalid bounds");
        }
        this.boundX = bounds.getX();
        this.boundY = bounds.getY();

        this.x = coordinates.getX();
        this.y = coordinates.getY();

        this.makeFitBounds();
    }

    public BoundedMovablePoint(final @NotNull BoundedPoint other) {
        if (other.getBounds().getX() <= 0 || other.getBounds().getY() <= 0) {
            throw new IllegalArgumentException("Invalid bounds");
        }

        this.boundX = other.getBounds().getX();
        this.boundY = other.getBounds().getY();
        this.x = other.getX();
        this.y = other.getY();

        this.makeFitBounds();
    }

    public BoundedMovablePoint(final int x, final int y, final int boundX, final int boundY) {
        if (boundX <= 0 || boundY <= 0) {
            throw new IllegalArgumentException("Invalid bounds");
        }
        this.boundX = boundX;
        this.boundY = boundY;

        this.x = x;
        this.y = y;

        this.makeFitBounds();
    }

    @Override
    public @NotNull Coordinates getBounds() {
        return new UnboundedFixedPoint(this.boundX, this.boundY);
    }

    @Override
    public @NotNull BoundedMovablePoint copy() {
        return new BoundedMovablePoint(this.getX(), this.getY(), this.boundX, this.boundY);
    }

    public void setX(final int x) {
        this.x = makeFit(x, this.boundX);
    }

    public void setY(final int y) {
        this.x = makeFit(y, this.boundY);
    }

    public void setXY(final int x, final int y) {
        this.x = x;
        this.y = y;

        this.makeFitBounds();
    }

    public void setCoordinates(final @NotNull Coordinates coordinates) {
        this.x = coordinates.getX();
        this.y = coordinates.getY();

        this.makeFitBounds();
    }

    private void makeFitBounds() {
        this.x = makeFit(this.x, this.boundX);
        this.y = makeFit(this.y, this.boundY);
    }

    private static int makeFit(final int value, final int max) {
        int i = value;
        while (i < 0) {
            i += max;
        }
        return i % max;
    }

    public void move(final @NotNull Coordinates offset) {
        this.x += offset.getX();
        this.y += offset.getY();
        this.makeFitBounds();
    }

    public void move(final int offsetX, final int offsetY) {
        this.x += offsetX;
        this.y += offsetY;
        this.makeFitBounds();
    }

    public void move(final @NotNull Direction direction) {
        var dx = 0;
        var dy = 0;
        switch (direction) {
            case UP -> dy = -1;
            case DOWN -> dy = 1;
            case LEFT -> dx = -1;
            case RIGHT -> dx = 1;
        }
        this.setXY(this.getX() + dx, this.getY() + dy);
    }

    public void move(final @NotNull Direction direction, final int times) {
        var dx = 0;
        var dy = 0;
        switch (direction) {
            case UP -> dy = -times;
            case DOWN -> dy = times;
            case LEFT -> dx = -times;
            case RIGHT -> dx = times;
        }
        this.setXY(this.getX() + dx, this.getY() + dy);
    }

    public @NotNull BoundedMovablePoint moved(final @NotNull Direction direction) {
        var dx = 0;
        var dy = 0;
        switch (direction) {
            case UP -> dy = -1;
            case DOWN -> dy = 1;
            case LEFT -> dx = -1;
            case RIGHT -> dx = 1;
        }
        return new BoundedMovablePoint(
                this.getX() + dx, this.getY() + dy, this.boundX, this.boundY);
    }

    public void forEachWithinExceptSelf(
            final @NotNull Coordinates offset,
            final @NotNull Consumer<@NotNull BoundedPoint> action) {
        val dx = offset.getX() < 0 ? -1 : (offset.getX() > 0 ? 1 : 0);
        val dy = offset.getY() < 0 ? -1 : (offset.getY() > 0 ? 1 : 0);

        val point = this.copy();

        if (dx == 0) {
            for (int y = this.getY() + dy; y != this.getY() + offset.getY() + dy; y += dy) {
                point.setXY(this.getX(), y);
                action.accept(point);
            }
        } else if (dy == 0) {
            for (int x = this.getX() + dx; x != this.getX() + offset.getX() + dx; x += dx) {
                point.setXY(x, this.getY());
                action.accept(point);
            }
        } else {
            for (int x = this.getX() + dx; x != this.getX() + offset.getX() + dx; x += dx) {
                for (int y = this.getY() + dy; y != this.getY() + offset.getY() + dy; y += dy) {
                    point.setXY(x, y);
                    action.accept(point);
                }
            }
        }
    }

    @Override
    public void centerRelativeTo(final @NotNull BoundedPoint other) {
        if (this.boundX != other.getBounds().getX() || this.boundY != other.getBounds().getY()) {
            throw new IllegalArgumentException("Different bounds");
        }
        val offsetX = other.getX() - this.boundX / 2;
        val offsetY = other.getY() - this.boundY / 2;

        this.move(-offsetX, -offsetY);
    }

}
