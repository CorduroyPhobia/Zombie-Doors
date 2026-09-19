package com.greysonloomis.zombiedoors.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import java.util.function.Consumer;

public final class ZombieDoorsConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Logger logger;
    private volatile ZombieDoorsConfig config = ZombieDoorsConfig.defaults();
    private Consumer<ZombieDoorsConfig> changeListener = ignored -> {};

    public ZombieDoorsConfigManager(Path file, Logger logger) {
        this.file = file.toAbsolutePath().normalize();
        this.logger = logger;
    }

    public ZombieDoorsConfig get() { return config; }

    public synchronized void setChangeListener(Consumer<ZombieDoorsConfig> listener) {
        changeListener = listener;
    }

    public synchronized ZombieDoorsConfig load() {
        try {
            if (Files.notExists(file)) {
                updateAndSave(ZombieDoorsConfig.defaults());
            } else {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject supplied = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject merged = GSON.toJsonTree(ZombieDoorsConfig.defaults()).getAsJsonObject();
                    for (String key : merged.keySet()) {
                        if (supplied.has(key) && !supplied.get(key).isJsonNull()) {
                            merged.add(key, supplied.get(key));
                        }
                    }
                    config = GSON.fromJson(merged, ZombieDoorsConfig.class);
                    changeListener.accept(config);
                }
            }
        } catch (IOException | RuntimeException exception) {
            logger.error("Could not load {}; keeping the last valid Zombie Doors settings", file, exception);
        }
        return config;
    }

    public synchronized void updateAndSave(ZombieDoorsConfig updated) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "zombiedoors-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(updated) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            config = updated;
            changeListener.accept(config);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
