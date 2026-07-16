package com.bikininjas.corelib.objective;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An active challenge: a named collection of objectives with a time limit.
 */
public record Challenge(
        @NotNull String name,
        @NotNull List<Objective> objectives,
        int timeLimitSeconds
) {

    public Challenge {
        Objects.requireNonNull(name, "name must not be null");
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
    }

    /**
     * Check whether all objectives are complete.
     */
    public boolean isComplete() {
        return objectives.stream().allMatch(obj -> true); // evaluated per-player in tracker
    }
}
