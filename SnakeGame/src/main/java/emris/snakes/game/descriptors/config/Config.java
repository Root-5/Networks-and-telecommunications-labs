package emris.snakes.game.descriptors.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import lombok.val;
import me.ippolitov.fit.snakes.SnakesProto;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.logging.Logger;

public interface Config extends GameConfig, NetworkConfig {

    @NotNull Path CONFIG_FILE_PATH = Paths.get(
            System.getProperty("user.dir"), "config", "config.json");
    @NotNull String CONFIG_FILE_NAME = CONFIG_FILE_PATH.toString();

    @NotNull Charset CHARSET = StandardCharsets.UTF_8;

    int DEFAULT_FOOD_STATIC = 1;
    int DEFAULT_HEIGHT = 30;
    int DEFAULT_WIDTH = 40;
    int DEFAULT_NODE_TIMEOUT_MS = 3000;
    int DEFAULT_PING_DELAY_MS = 1_000;
    int DEFAULT_STEP_DELAY_MS = 1_000;
    float DEFAULT_PER_PLAYER = DEFAULT_FOOD_STATIC;
    float DEFAULT_FOOD_SPAWN_ON_DEATH_CHANCE = 0.1f;

    @NotNull Config DEFAULT_CONFIG = new ConfigData();

    static @NotNull Config load() throws InvalidConfigException {
        try (
                val in = new FileReader(CONFIG_FILE_NAME, CHARSET);
                val jsonReader = new JsonReader(in)) {
            val g = new Gson();
            try {
                final Config config = g.fromJson(jsonReader, ConfigData.class);
                ConfigValidator.isValid(config);
                return config;
            } catch (final @NotNull JsonSyntaxException e) {
                throw new InvalidConfigException(e.getMessage());
            } catch (final @NotNull JsonIOException e) {
                throw new IOException(e);
            }
        } catch (final @NotNull FileNotFoundException e) {
            Logger.getLogger(Config.class.getSimpleName()).info("Config not found in " + CONFIG_FILE_PATH);
            DEFAULT_CONFIG.store();
            return DEFAULT_CONFIG;
        } catch (final @NotNull IOException e) {
            Logger.getLogger(Config.class.getSimpleName()).warning("Unexpected IOException occurred when loading config");
            DEFAULT_CONFIG.store();
            return DEFAULT_CONFIG;
        }
    }

    default void store() {
        val dir = CONFIG_FILE_PATH.getParent();
        val dirFile = dir.toFile();
        if (!dirFile.exists()) {
            val success = dir.toFile().mkdirs();
            if (!success) {
                Logger.getLogger(Config.class.getSimpleName()).warning("Failed to create parent directories for config file");
                return;
            }
        } else if (!dirFile.isDirectory()) {
            Logger.getLogger(Config.class.getSimpleName()).warning("Cannot create config file in " + dir.toString() + ": not a directory");
            return;
        }

        try (val out = new FileWriter(CONFIG_FILE_NAME, CHARSET)) {
            val g = new Gson();
            val jsonConfig = g.toJson(this);
            out.write(jsonConfig);
            return;
        } catch (final @NotNull FileNotFoundException e) {
            Logger.getLogger(Config.class.getSimpleName()).info("Cannot open file " + CONFIG_FILE_PATH);
        } catch (final @NotNull IOException e) {
            Logger.getLogger(Config.class.getSimpleName()).warning("Unexpected IOException occurred when storing config");
        }

        Logger.getLogger(Config.class.getSimpleName()).info("Created default config " + CONFIG_FILE_NAME);
    }

    static @NotNull Config fromMessage(final @NotNull SnakesProto.GameConfigOrBuilder config) {
        val result = new ConfigData();
        result.setPlaneWidth(config.getWidth());
        result.setPlaneHeight(config.getHeight());
        result.setFoodStatic(config.getFoodStatic());
        result.setFoodPerPlayer(config.getFoodPerPlayer());
        result.setFoodSpawnOnDeathChance(config.getDeadFoodProb());
        result.setStateDelayMs(config.getStateDelayMs());
        result.setPingDelayMs(config.getPingDelayMs());
        result.setNodeTimeoutMs(config.getNodeTimeoutMs());
        return result;
    }

    default @NotNull SnakesProto.GameConfig toMessage() {
        return SnakesProto.GameConfig.newBuilder()
                .setWidth(this.getPlaneWidth())
                .setHeight(this.getPlaneHeight())
                .setFoodStatic(this.getFoodStatic())
                .setFoodPerPlayer(this.getFoodPerPlayer())
                .setDeadFoodProb(this.getFoodSpawnOnDeathChance())
                .setStateDelayMs(this.getStateDelayMs())
                .setPingDelayMs(this.getPingDelayMs())
                .setNodeTimeoutMs(this.getNodeTimeoutMs())
                .build();
    }
}

