package com.bikininjas.corelib.objective;

import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for challenge templates/definitions.
 * <p>
 * Supports filtering by loaded mods: definitions requiring a mod that is not
 * loaded will be excluded from {@link #getAvailable()}.
 * <p>
 * All methods are static. No event bus registration.
 */
public final class ChallengeRegistry {

    private static final Map<String, ChallengeDefinition> definitions = new ConcurrentHashMap<>();

    private ChallengeRegistry() {
    }

    /**
     * Register a challenge definition.
     */
    public static void register(@NotNull ChallengeDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        definitions.put(definition.name(), definition);
    }

    /**
     * Get a definition by name.
     */
    public static @Nullable ChallengeDefinition get(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        return definitions.get(name);
    }

    /**
     * Get all registered definitions.
     */
    public static @NotNull List<ChallengeDefinition> getAll() {
        return List.copyOf(definitions.values());
    }

    /**
     * Get definitions whose required mods are all loaded.
     */
    public static @NotNull List<ChallengeDefinition> getAvailable() {
        return definitions.values().stream()
                .filter(ChallengeRegistry::areModsLoaded)
                .toList();
    }

    /**
     * Check if all mods required by a definition are loaded.
     */
    public static boolean areModsLoaded(@NotNull ChallengeDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        var modList = ModList.get();
        if (modList == null) return definition.requiredMods().isEmpty();
        return definition.requiredMods().stream()
                .allMatch(modId -> modList.isLoaded(modId));
    }

    /**
     * Clear all definitions.
     */
    public static void clear() {
        definitions.clear();
    }
}
