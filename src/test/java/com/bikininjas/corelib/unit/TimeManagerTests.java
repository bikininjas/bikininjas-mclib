package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.time.TimeManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for {@link TimeManager}.
 * <p>
 * These run without a Minecraft runtime ({@code ./gradlew test}). Only the
 * static, side-effect-free logic is exercised here; methods that require a
 * live {@code ServerLevel} are validated structurally (final class, private
 * constructor) and via their pure helpers.
 */
class TimeManagerTests {

    // ──────────────────────────────────────────────
    //  Structure
    // ──────────────────────────────────────────────

    @Test
    void classIsFinal() {
        assertTrue(Modifier.isFinal(TimeManager.class.getModifiers()),
                "TimeManager must be a final utility class");
    }

    @Test
    void constructorIsPrivateAndCallable() throws Exception {
        Constructor<TimeManager> ctor = TimeManager.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                "TimeManager constructor must be private");
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance(),
                "Private constructor should be invokable (no side effects)");
    }

    // ──────────────────────────────────────────────
    //  Validation (no Minecraft runtime needed)
    // ──────────────────────────────────────────────

    @Test
    void setTimeRateRejectsNegativeRate() {
        // Validation runs before any ServerLevel access, so a null level is safe.
        assertThrows(IllegalArgumentException.class,
                () -> TimeManager.setTimeRate(null, -1.0f),
                "Negative time rate must be rejected");
    }

    // ──────────────────────────────────────────────
    //  Pure tick math
    // ──────────────────────────────────────────────

    @Test
    void computeExtraTicksNormalRateIsZero() {
        assertEquals(0L, TimeManager.computeExtraTicks(1.0f, 1.0f),
                "Rate 1.0 must add no extra ticks (normal speed)");
    }

    @Test
    void computeExtraTicksFasterRateAddsExtra() {
        assertEquals(71L, TimeManager.computeExtraTicks(72.0f, 1.0f),
                "Rate 72.0 must add 71 extra ticks per tick (72× faster)");
        assertEquals(1L, TimeManager.computeExtraTicks(2.0f, 1.0f),
                "Rate 2.0 must add 1 extra tick per tick (2× faster)");
    }

    @Test
    void computeExtraTicksFrozenRateCancelsNaturalIncrement() {
        assertEquals(-1L, TimeManager.computeExtraTicks(0.0f, 1.0f),
                "Rate 0.0 must subtract 1 tick, cancelling the natural +1 increment");
    }

    @Test
    void computeExtraTicksScalesWithTickDelta() {
        // (2.0 - 1) * 0.5 = 0.5 → rounds to 1
        assertEquals(1L, TimeManager.computeExtraTicks(2.0f, 0.5f));
        // (3.0 - 1) * 0.25 = 0.5 → rounds to 1
        assertEquals(1L, TimeManager.computeExtraTicks(3.0f, 0.25f));
        // (1.5 - 1) * 1.0 = 0.5 → rounds to 1
        assertEquals(1L, TimeManager.computeExtraTicks(1.5f, 1.0f));
    }
}
