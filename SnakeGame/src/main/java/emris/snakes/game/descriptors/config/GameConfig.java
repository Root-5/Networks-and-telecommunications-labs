package emris.snakes.game.descriptors.config;

public interface GameConfig {

    int getPlaneWidth();

    int getPlaneHeight();

    int getFoodStatic();

    float getFoodPerPlayer();

    int getStateDelayMs();

    float getFoodSpawnOnDeathChance();
}
