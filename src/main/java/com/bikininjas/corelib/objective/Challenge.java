package com.bikininjas.corelib.objective;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Immutable description of a challenge: a named set of {@link Objective}s that a
 * player must complete within an optional time limit.
 *
 * @param name             the display name of the challenge; never {@code null}.
 * @param objectives       the objectives composing the challenge; never {@code null}.
 * @param timeLimitSeconds optional wall-clock limit in seconds ({@code 0} means no limit).
 */
public record Challenge(
        @NotNull String name,
        @NotNull List<Objective> objectives,
        int timeLimitSeconds
) {
    public Challenge {
        objectives = List.copyOf(objectives);
    }
}
