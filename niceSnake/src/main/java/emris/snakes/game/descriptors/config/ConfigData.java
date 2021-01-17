package emris.snakes.game.descriptors.config;

import lombok.Data;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@Data
public class ConfigData implements Config {
    private int planeWidth = DEFAULT_WIDTH;
    private int planeHeight = DEFAULT_HEIGHT;
    private int foodStatic = DEFAULT_FOOD_STATIC;
    private float foodPerPlayer = DEFAULT_PER_PLAYER;
    private float foodSpawnOnDeathChance = DEFAULT_FOOD_SPAWN_ON_DEATH_CHANCE;
    private int stateDelayMs = DEFAULT_STEP_DELAY_MS;
    private int pingDelayMs = DEFAULT_PING_DELAY_MS;
    private int nodeTimeoutMs = DEFAULT_NODE_TIMEOUT_MS;

    public static @NotNull ConfigData copyOf(final @NotNull Config other) {
        val result = new ConfigData();
        result.setPlaneWidth(other.getPlaneWidth());
        result.setPlaneHeight(other.getPlaneHeight());
        result.setFoodStatic(other.getFoodStatic());
        result.setFoodPerPlayer(other.getFoodPerPlayer());
        result.setFoodSpawnOnDeathChance(other.getFoodSpawnOnDeathChance());
        result.setStateDelayMs(other.getStateDelayMs());
        result.setPingDelayMs(other.getPingDelayMs());
        result.setNodeTimeoutMs(other.getNodeTimeoutMs());
        return result;
    }
}
