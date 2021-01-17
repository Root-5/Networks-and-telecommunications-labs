package emris.snakes.game;

import emris.snakes.game.descriptors.config.Config;
import emris.snakes.game.descriptors.game.GameDescriptor;
import emris.snakes.game.descriptors.game.GameState;
import emris.snakes.game.descriptors.player.PlayerDescriptor;
import emris.snakes.game.descriptors.player.PlayerInfo;
import emris.snakes.game.descriptors.snake.SnakeState;
import emris.snakes.game.plane.*;
import emris.snakes.game.snake.Snake;
import emris.snakes.game.snake.SnakeInfo;
import emris.snakes.game.snake.impl.LocalSnake;
import emris.snakes.game.snake.impl.RemoteSnake;
import emris.snakes.game.snake.impl.SnakeImplementationSupplier;
import emris.snakes.util.ExceptionInterfaces.UnsafeConsumer;
import lombok.val;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameModel implements SnakesGameInfo {

    private static final int INITIAL_SNAKE_SIZE = 2;
    private static final int REQUIRED_EMPTY_SPACE_SIZE = 5;

    private static final int NS_PER_MS = 1_000_000;

    private static final @NotNull Logger logger = Logger.getLogger(GameModel.class.getSimpleName());

    private @NotNull Coordinates bounds;
    private final @NotNull Coordinates origin = new UnboundedFixedPoint(0, 0);

    private final @NotNull Map<@NotNull Integer, @NotNull SnakeState> snakeDescriptors = new LinkedHashMap<>();
    private final @NotNull Collection<@NotNull BoundedPoint> food = new LinkedHashSet<>();
    private final @NotNull Collection<@NotNull BoundedPoint> empty = new LinkedHashSet<>();

    private final @NotNull Map<@NotNull Integer, @NotNull Snake> snakeById = new HashMap<>();
    private final @NotNull Collection<@NotNull Snake> snakes = this.snakeById.values();
    private final @NotNull Map<@NotNull Integer, @NotNull PlayerDescriptor> playerById = new HashMap<>();
    private final @NotNull Collection<@NotNull Integer> disconnectedPlayers = new HashSet<>();

    private final @NotNull Collection<@NotNull Snake> processedSnakes = new LinkedHashSet<>();
    private final @NotNull Collection<@NotNull Snake> deadSnakes = new LinkedHashSet<>();
    private final @NotNull Collection<@NotNull PlayerInfo> deadSnakeOwners = new LinkedHashSet<>();

    private final @NotNull boolean[][] isOccupiedBySnakes;
    private final @NotNull boolean[][] isOccupiedByFood;
    private final @NotNull Function<@NotNull BoundedPoint, @NotNull Boolean> foodChecker;

    private final @NotNull Collection<@NotNull Integer> newIds = new HashSet<>();

    private @NotNull Config config;

    private final @NotNull GameDescriptor stateDescriptor;

    public GameModel(final @NotNull Config config) {
        this.config = config;
        this.stateDescriptor = new GameDescriptor(
                0, Collections.unmodifiableCollection(this.snakeDescriptors.values()),
                Collections.unmodifiableCollection(this.food),
                Collections.unmodifiableCollection(this.playerById.values()), this.config);
        this.bounds = new UnboundedFixedPoint(this.config.getPlaneWidth(), this.config.getPlaneHeight());
        this.isOccupiedBySnakes = new boolean[this.bounds.getX()][];
        for (int i = 0; i < this.bounds.getX(); i += 1) {
            this.isOccupiedBySnakes[i] = new boolean[this.bounds.getY()];
        }
        this.isOccupiedByFood = new boolean[this.bounds.getX()][];
        for (int i = 0; i < this.bounds.getX(); i += 1) {
            this.isOccupiedByFood[i] = new boolean[this.bounds.getY()];
        }
        this.foodChecker = coordinates -> this.isOccupiedByFood[coordinates.getX()][coordinates.getY()];
        this.spawnFood();
    }

    public synchronized void setState(final @NotNull GameState remoteGameDescriptor) {
        if (remoteGameDescriptor.getStateOrder() <= this.stateDescriptor.getStateOrder()) {
            logger.info(
                    "Received old state order: " + remoteGameDescriptor.getStateOrder()
                            + " (own is " + this.stateDescriptor.getStateOrder() + ")");
            return;
        }

        this.bounds = new UnboundedFixedPoint(
                remoteGameDescriptor.getConfig().getPlaneWidth(),
                remoteGameDescriptor.getConfig().getPlaneHeight());
        this.config = remoteGameDescriptor.getConfig();

        this.newIds.clear();
        remoteGameDescriptor.getPlayers().forEach(it -> {
            if (!this.playerById.containsKey(it.getId())) {
                this.newIds.add(it.getId());
            }
        });

        this.clearState();

        remoteGameDescriptor.getPlayers().forEach(it -> {
            this.playerById.put(it.getId(), PlayerDescriptor.copyOf(it));
            logger.finest(String.format("Player %s score %d", it.getName(), it.getScore()));
        });
        remoteGameDescriptor.getSnakes().forEach(it -> {
            val player = this.playerById.get(it.getPlayerId());
            if (player != null) {
                val snake = new RemoteSnake(player.getId(), this.bounds, it);
                this.snakeById.put(it.getPlayerId(), LocalSnake.copyOf(snake));
            } else {
                logger.warning("No player with id " + it.getPlayerId() + " received (snake without owner, skipped)");
            }
        });
        remoteGameDescriptor.getFood().forEach(it -> {
            this.isOccupiedByFood[it.getX()][it.getY()] = true;
            this.food.add(new BoundedMovablePoint(it, this.bounds));
        });

        this.stateDescriptor.setStateOrder(remoteGameDescriptor.getStateOrder());
    }

    public @NotNull Collection<@NotNull Integer> getNewIds() {
        return Collections.unmodifiableCollection(this.newIds);
    }

    private void clearState() {
        for (int x = 0; x < this.bounds.getX(); x += 1) {
            for (int y = 0; y < this.bounds.getY(); y += 1) {
                this.isOccupiedByFood[x][y] = false;
                this.isOccupiedBySnakes[x][y] = false;
            }
        }
        this.food.clear();
        this.playerById.clear();
        this.snakeById.clear();
    }

    void moveSnakes() {
        val start = System.nanoTime();
        this.processedSnakes.clear();
        this.deadSnakeOwners.clear();
        this.deadSnakes.clear();
        val clear = System.nanoTime();

        this.snakes.forEach(snake -> snake.move(this.foodChecker));
        val move = System.nanoTime();

        val foodToRemove = new LinkedHashSet<@NotNull BoundedPoint>();

        for (final var thisSnake : this.snakes) {
            if (this.isFood(thisSnake.getHead())) {
                foodToRemove.add(thisSnake.getHead());
                this.getPlayer(thisSnake).increaseScore();
            }

            for (final var otherSnake : this.snakes) {
                if (this.processedSnakes.contains(otherSnake)) {
                    continue;
                }

                // Handle collisions with self
                if (thisSnake == otherSnake) {
                    val head = thisSnake.getHead();
                    thisSnake.forEachSegment(point -> {
                        if (point.equals(thisSnake.getHead()) && head != point) {
                            this.deadSnakes.add(thisSnake);
                        }
                    });
                    continue;
                }

                // Find intersection between currently examined snakes
                forEachInIntersection(thisSnake, otherSnake, point -> {
                    if (point.equals(thisSnake.getHead()) && point.equals(otherSnake.getHead())) {
                        this.deadSnakes.add(thisSnake);
                        this.deadSnakes.add(otherSnake);
                    } else if (point.equals(thisSnake.getHead())) {
                        this.deadSnakes.add(thisSnake);
                        this.getPlayer(otherSnake).increaseScore();
                    } else if (point.equals(otherSnake.getHead())) {
                        this.deadSnakes.add(otherSnake);
                        this.getPlayer(thisSnake).increaseScore();
                    } else {
                        throw new IllegalStateException("There is an intersection but heads didn't collide");
                    }
                });
            }

            this.processedSnakes.add(thisSnake);
        }
        val collisions = System.nanoTime();

        val removed = this.snakes.removeAll(this.deadSnakes);
        if (!this.deadSnakes.isEmpty() && !removed) {
            throw new IllegalStateException("Dead snakes were not removed");
        }
        foodToRemove.forEach(this::removeFood);
        this.flushIsOccupiedBySnakes();
        this.deadSnakes.forEach(it -> {
            this.replaceSnakeWithFood(it);
            this.snakeById.remove(it.getPlayerId());
            val owner = this.playerById.get(it.getPlayerId());
            if (owner != null) {
                this.deadSnakeOwners.add(owner);
                owner.setRole(SnakesProto.NodeRole.VIEWER);
            } else {
                logger.warning("Dead snake has no owner");
            }
            if (this.disconnectedPlayers.contains(it.getPlayerId())) {
                this.playerById.remove(it.getPlayerId());
                this.disconnectedPlayers.remove(it.getPlayerId());
            }
            this.snakeDescriptors.remove(it.getPlayerId());
        });
        this.snakes.forEach(
                snake -> {
                    snake.forEachSegment(
                            it -> this.isOccupiedBySnakes[it.getX()][it.getY()] = true);
                    this.snakeDescriptors.put(snake.getPlayerId(), SnakeState.forSnake(snake));
                });
        val end = System.nanoTime();
        logger.log(Level.FINEST, "clear {0} | move {1} | collisions {2} | end {3}", new Object[]{
                (clear - start) / NS_PER_MS,
                (move - clear) / NS_PER_MS,
                (collisions - move) / NS_PER_MS,
                (end - collisions) / NS_PER_MS
        });
    }

    public void forEachDeadSnakeOwner(
            final @NotNull UnsafeConsumer<@NotNull PlayerInfo> action) throws Exception {
        for (final var it : this.deadSnakeOwners) {
            action.accept(it);
        }
    }

    private void replaceSnakeWithFood(final @NotNull SnakeInfo snake) {
        val head = snake.getHead();
        snake.forEachSegment(it -> {
            // head is inside someone's body -> no food should ever be spawned there
            if (head != it) {
                if (ThreadLocalRandom.current().nextDouble(0.0, 1.0)
                        < this.config.getFoodSpawnOnDeathChance()) {
                    this.placeFood(it.copy());
                }
            }
        });
    }

    private static void forEachInIntersection(
            final @NotNull SnakeInfo s1,
            final @NotNull SnakeInfo s2,
            final @NotNull Consumer<@NotNull BoundedPoint> action) {
        if (s1 == s2) {
            s1.forEachSegment(action);
        }

        s1.forEachSegment(segment1 -> s2.forEachSegment(segment2 -> {
            if (segment1.equals(segment2)) {
                action.accept(segment1);
            }
        }));
    }

    private void spawnFood() {
        val totalRequiredFood = this.config.getFoodStatic()
                + (int) (this.config.getFoodPerPlayer()
                * this.snakes.stream().filter(it -> !it.isZombie()).count());
        val foodToSpawn = totalRequiredFood - this.food.size();

        if (foodToSpawn > 0) {
            outer:
            for (int i = 0; i < Math.min(this.empty.size(), foodToSpawn); i += 1) {
                val index = ThreadLocalRandom.current().nextInt(0, this.empty.size());
                int j = 0;
                for (final var point : this.empty) {
                    if (index == j) {
                        this.placeFood(point);
                        continue outer;
                    }
                    j += 1;
                }
                throw new IllegalStateException("Should never be reached");
            }
        }
    }

    private void placeFood(final @NotNull BoundedPoint point) {
        this.isOccupiedByFood[point.getX()][point.getY()] = true;
        this.food.add(point);
        this.empty.remove(point);
    }

    private void removeFood(final @NotNull BoundedPoint point) {
        this.isOccupiedByFood[point.getX()][point.getY()] = false;
        this.food.remove(point);
    }

    private void flushIsOccupiedBySnakes() {
        for (int x = 0; x < this.bounds.getX(); x += 1) {
            for (int y = 0; y < this.bounds.getY(); y += 1) {
                this.isOccupiedBySnakes[x][y] = false;
            }
        }
    }

    void updateEmpty() {
        val point = new BoundedMovablePoint(this.origin, this.bounds);
        for (int x = 0; x < this.bounds.getX(); x += 1) {
            for (int y = 0; y < this.bounds.getY(); y += 1) {
                point.setXY(x, y);
                if (this.isOccupiedByFood[x][y] || this.isOccupiedBySnakes[x][y]) {
                    this.empty.remove(point);
                } else {
                    this.empty.add(point.copy());
                }
            }
        }
    }

    /**
     * Returned state is only guaranteed to be valid until next mutating method call
     *
     * @return current state of this game
     */

    @Override
    public @NotNull GameState getState() {
        return this.stateDescriptor;
    }

    @Override
    public int getStateOrder() {
        return this.stateDescriptor.getStateOrder();
    }

    public synchronized void nextState() {
        val start = System.nanoTime();
        this.moveSnakes();
        val move = System.nanoTime();
        this.updateEmpty();
        val updateEmpty = System.nanoTime();
        this.spawnFood();
        val spawnFood = System.nanoTime();
        logger.log(Level.FINEST, "move {0} | update empty {1} | spawn food {2}", new Object[]{
                (move - start) / NS_PER_MS,
                (updateEmpty - move) / NS_PER_MS,
                (spawnFood - updateEmpty) / NS_PER_MS,
        });
        this.stateDescriptor.incrementStateOrder();
    }

    public synchronized <SnakeType extends Snake> @Nullable SnakeType addPlayer(
            final @NotNull PlayerDescriptor player,
            final @NotNull SnakeImplementationSupplier<SnakeType> implProvider) {
        val direction = Direction.fromNumber(
                ThreadLocalRandom.current().nextInt(1, 5));
        val spawnPoint = this.findSpawnPoint(direction);
        if (spawnPoint == null) {
            return null;
        }
        val snake = implProvider.get(player.getId(), spawnPoint, direction, INITIAL_SNAKE_SIZE);
        snake.forEachSegment(point -> this.isOccupiedBySnakes[point.getX()][point.getY()] = true);

        this.snakeById.put(player.getId(), snake);
        this.playerById.put(player.getId(), player);
        this.snakeDescriptors.put(player.getId(), SnakeState.forSnake(snake));

        this.updateEmpty();
        this.spawnFood();

        this.stateDescriptor.incrementStateOrder();

        return snake;
    }

    private @Nullable BoundedPoint findSpawnPoint(final @NotNull Direction direction) {
        int offX = ThreadLocalRandom.current().nextInt(0, this.config.getPlaneWidth());
        int offY = ThreadLocalRandom.current().nextInt(0, this.config.getPlaneHeight());

        val point = new BoundedMovablePoint(0, 0, this.config.getPlaneWidth(), this.config.getPlaneHeight());

        for (int x = 0; x < this.config.getPlaneWidth(); x += 1) {
            y_loop:
            for (int y = 0; y < this.config.getPlaneHeight(); y += 1) {

                for (int dx = 0; dx < REQUIRED_EMPTY_SPACE_SIZE; dx += 1) {
                    for (int dy = 0; dy < REQUIRED_EMPTY_SPACE_SIZE; dy += 1) {
                        point.setXY(x + dx + offX, y + dy + offY);
                        if (this.isOccupiedBySnakes[point.getX()][point.getY()]) {
                            continue y_loop;
                        }
                    }
                }

                val spawnX = x + REQUIRED_EMPTY_SPACE_SIZE / 2 + offX;
                val spawnY = y + REQUIRED_EMPTY_SPACE_SIZE / 2 + offY;
                point.setXY(spawnX, spawnY);

                for (int i = 0; i < INITIAL_SNAKE_SIZE; i += 1) {
                    point.move(direction, i);
                    val canPlaceSegment = !this.isFood(point);
                    if (!canPlaceSegment) {
                        continue y_loop;
                    }
                }

                point.setXY(spawnX, spawnY);
                return point;
            }
        }

        return null;
    }

    public synchronized void playerDisconnected(final int playerId) {
        this.playerLeft(playerId);
        if (this.hasSnakeWithPlayerId(playerId)) {
            this.disconnectedPlayers.add(playerId);
        } else {
            this.playerById.remove(playerId);
        }
    }

    public synchronized void playerLeft(final int playerId) {
        val player = this.playerById.get(playerId);
        if (player != null) {
            player.setRole(SnakesProto.NodeRole.VIEWER);
            logger.info("Player " + player.getName() + " (" + playerId + ") has lost control over their snake");
        } else {
            logger.info("Player left: no player with id " + playerId);
        }
        val snake = this.snakeById.get(playerId);
        if (snake != null) {
            snake.zombify();
        } else {
            logger.info("Player left: no snake for player with id " + playerId);
        }
    }

    @Override
    public @NotNull PlayerDescriptor getPlayerById(final int playerId) {
        return this.getPlayer(playerId);
    }

    @Override
    public void forEachSnake(
            final @NotNull Consumer<@NotNull SnakeInfo> action) {
        this.snakes.forEach(action);
    }

    @Override
    public @NotNull Snake getSnakeById(final int playerId) {
        val snake = this.snakeById.get(playerId);
        if (snake == null) {
            throw new IllegalStateException("No snake with player id " + playerId);
        }
        return snake;
    }

    @Override
    public @Nullable SnakeInfo getAnySnake() {
        return this.snakes.stream().findAny().orElse(null);
    }

    @Override
    public void forEachFood(
            final @NotNull Consumer<@NotNull BoundedPoint> action) {
        this.food.forEach(action);
        //        val point = new BoundedMovablePoint(0, 0, this.bounds);
        //        for (int x = 0; x < this.bounds.getX(); x += 1) {
        //            for (int y = 0; y < this.bounds.getY(); y += 1) {
        //                if (this.isOccupiedByFood[x][y]) {
        //                    point.setXY(x, y);
        //                    action.accept(point);
        //                }
        //            }
        //        }
    }

    @Override
    public boolean isFood(final @NotNull BoundedPoint point) {
        return this.isOccupiedByFood[point.getX()][point.getY()];
    }

    @Override
    public void forEachPlayer(
            final @NotNull Consumer<@NotNull PlayerInfo> action) {
        this.playerById.values().stream()
                .sorted((p1, p2) -> p2.getScore() - p1.getScore())
                .forEach(action);
    }

    @Override
    public boolean hasPlayerWithId(final int playerId) {
        return this.playerById.containsKey(playerId);
    }

    @Override
    public boolean hasSnakeWithPlayerId(final int playerId) {
        return this.snakeById.containsKey(playerId);
    }

    private @NotNull PlayerDescriptor getPlayer(final @NotNull SnakeInfo snake) {
        return this.getPlayer(snake.getPlayerId());
    }

    private @NotNull PlayerDescriptor getPlayer(final int playerId) {
        val player = this.playerById.get(playerId);
        if (player == null) {
            throw new IllegalStateException("No player with id " + playerId);
        }
        return player;
    }

    @Override
    public @NotNull Coordinates getPlaneBounds() {
        return this.bounds;
    }
}
