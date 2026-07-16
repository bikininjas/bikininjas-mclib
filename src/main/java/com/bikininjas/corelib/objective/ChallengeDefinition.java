package com.bikininjas.corelib.objective;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A challenge template/definition that can be registered and instantiated.
 * Supports filtering by required mods.
 */
public record ChallengeDefinition(
        @NotNull String name,
        @NotNull String displayName,
        @NotNull List<Objective> objectives,
        int timeLimitSeconds,
        @NotNull List<String> requiredMods
) {

    public ChallengeDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        requiredMods = requiredMods == null ? List.of() : List.copyOf(requiredMods);
    }

    /**
     * Create a definition with no mod requirements.
     */
    public static @NotNull ChallengeDefinition of(
            @NotNull String name, @NotNull String displayName,
            @NotNull List<Objective> objectives, int timeLimitSeconds) {
        return new ChallengeDefinition(name, displayName, objectives, timeLimitSeconds, List.of());
    }

    /**
     * Create a {@link Challenge} from this definition.
     */
    public @NotNull Challenge toChallenge() {
        return new Challenge(name, objectives, timeLimitSeconds);
    }
}
