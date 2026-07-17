package com.bikininjas.corelib.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;

public final class BikiniConfigRegistry {

    private static final Map<String, Map<String, ConfigOption>> options = new ConcurrentHashMap<>();
    private static final Map<String, String> modDisplayNames = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    private static Path configPath = Path.of("config", "bikininjas.json");

    private BikiniConfigRegistry() {}

    public static void registerMod(@NotNull String modId, @NotNull String displayName) {
        options.putIfAbsent(modId, new ConcurrentHashMap<>());
        modDisplayNames.putIfAbsent(modId, displayName);
    }

    public static void registerOption(@NotNull ConfigOption option) {
        options.computeIfAbsent(option.modId(), k -> new ConcurrentHashMap<>())
                .put(option.key(), option);
    }

    public static Collection<ConfigOption> getOptions(@NotNull String modId) {
        var modOptions = options.get(modId);
        return modOptions != null ? modOptions.values() : Collections.emptyList();
    }

    public static Set<String> getRegisteredMods() {
        return Collections.unmodifiableSet(options.keySet());
    }

    public static String getModDisplayName(@NotNull String modId) {
        return modDisplayNames.getOrDefault(modId, modId);
    }

    public static void updateValue(@NotNull String modId, @NotNull String key, @NotNull Object value) {
        var modOptions = options.get(modId);
        if (modOptions != null) {
            var option = modOptions.get(key);
            if (option != null) {
                modOptions.put(key, option.withValue(value));
            }
        }
    }

    public static void clear() {
        options.clear();
        modDisplayNames.clear();
    }

    public static void saveConfig() {
        var root = new JsonObject();
        for (var modEntry : options.entrySet()) {
            var modJson = new JsonObject();
            for (var opt : modEntry.getValue().values()) {
                var optJson = new JsonObject();
                optJson.addProperty("key", opt.key());
                optJson.addProperty("type", opt.type().name());
                addValueToJson(optJson, "value", opt.currentValue(), opt.type());
                modJson.add(opt.key(), optJson);
            }
            root.add(modEntry.getKey(), modJson);
        }
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(root));
        } catch (IOException ignored) {}
    }

    public static void loadConfig() {
        if (!Files.exists(configPath)) return;
        try {
            var content = Files.readString(configPath);
            var root = JsonParser.parseString(content).getAsJsonObject();
            for (var modEntry : root.entrySet()) {
                var modJson = modEntry.getValue().getAsJsonObject();
                for (var optEntry : modJson.entrySet()) {
                    var optJson = optEntry.getValue().getAsJsonObject();
                    var key = optJson.get("key").getAsString();
                    var typeStr = optJson.get("type").getAsString();
                    var type = ConfigOption.OptionType.valueOf(typeStr);
                    var value = readValueFromJson(optJson, "value", type);
                    updateValue(modEntry.getKey(), key, value);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void addValueToJson(JsonObject json, String key, Object value, ConfigOption.OptionType type) {
        switch (type) {
            case BOOL -> json.addProperty(key, (Boolean) value);
            case INT -> json.addProperty(key, ((Number) value).intValue());
            case FLOAT -> json.addProperty(key, ((Number) value).floatValue());
            case ENUM, STRING -> json.addProperty(key, String.valueOf(value));
        }
    }

    private static Object readValueFromJson(JsonObject json, String key, ConfigOption.OptionType type) {
        return switch (type) {
            case BOOL -> json.get(key).getAsBoolean();
            case INT -> json.get(key).getAsInt();
            case FLOAT -> json.get(key).getAsFloat();
            case ENUM, STRING -> json.get(key).getAsString();
        };
    }

    // -- Convenience methods for child mods ------------------------------------

    public static void registerBool(@NotNull String modId, @NotNull String key, @NotNull String name, boolean defaultValue) {
        registerMod(modId, modId);
        registerOption(new ConfigOption(modId, key, "General",
                Component.literal(name), Component.empty(),
                ConfigOption.OptionType.BOOL, defaultValue, defaultValue, null));
    }

    public static void registerEnum(@NotNull String modId, @NotNull String key, @NotNull String name,
                                    @NotNull String defaultValue, @NotNull String... values) {
        registerMod(modId, modId);
        registerOption(new ConfigOption(modId, key, "General",
                Component.literal(name), Component.empty(),
                ConfigOption.OptionType.ENUM, defaultValue, defaultValue, values));
    }

    public static boolean isEnabled(@NotNull String modId, @NotNull String key) {
        for (var opt : getOptions(modId)) {
            if (opt.key().equals(key) && opt.type() == ConfigOption.OptionType.BOOL) {
                return (boolean) opt.currentValue();
            }
        }
        return true;
    }

    public static String getString(@NotNull String modId, @NotNull String key, @NotNull String defaultVal) {
        for (var opt : getOptions(modId)) {
            if (opt.key().equals(key)) {
                return String.valueOf(opt.currentValue());
            }
        }
        return defaultVal;
    }
}
