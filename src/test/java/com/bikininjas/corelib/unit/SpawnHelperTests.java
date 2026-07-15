package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.entity.SpawnHelper;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for {@link SpawnHelper}.
 * <p>
 * These exercise the runtime-free position math ({@link SpawnHelper#randomOffset}
 * and {@link SpawnHelper#circlePosition}) plus structural guarantees of the
 * utility class. They run with {@code ./gradlew test} (NeoForge {@code unitTest}
 * block) and need no live game instance.
 */
class SpawnHelperTests {

    private static final double EPS = 1e-9;

    // ──────────────────────────────────────────────
    //  randomOffset
    // ──────────────────────────────────────────────

    @Test
    void randomOffsetAtAngleZeroIsOnPositiveX() {
        Vec3 offset = SpawnHelper.randomOffset(5.0, 0.0, 1.0);
        assertEquals(5.0, offset.x, EPS, "x should be radius at angle 0");
        assertEquals(0.0, offset.y, EPS, "y is always 0");
        assertEquals(0.0, offset.z, EPS, "z should be 0 at angle 0");
    }

    @Test
    void randomOffsetAtQuarterTurnIsOnPositiveZ() {
        Vec3 offset = SpawnHelper.randomOffset(4.0, Math.PI / 2.0, 1.0);
        assertEquals(0.0, offset.x, EPS);
        assertEquals(4.0, offset.z, EPS, "z should be radius at angle pi/2");
    }

    @Test
    void randomOffsetAtFullTurnReturnsToStart() {
        Vec3 a = SpawnHelper.randomOffset(3.0, 0.0, 1.0);
        Vec3 b = SpawnHelper.randomOffset(3.0, 2.0 * Math.PI, 1.0);
        assertEquals(a.x, b.x, EPS);
        assertEquals(a.z, b.z, EPS);
    }

    @Test
    void randomOffsetScalesWithDistanceFraction() {
        Vec3 half = SpawnHelper.randomOffset(10.0, 0.0, 0.5);
        assertEquals(5.0, half.x, EPS, "half fraction -> half radius");
    }

    @Test
    void randomOffsetZeroFractionIsOrigin() {
        Vec3 offset = SpawnHelper.randomOffset(10.0, 1.234, 0.0);
        assertEquals(0.0, offset.x, EPS);
        assertEquals(0.0, offset.z, EPS);
    }

    @Test
    void randomOffsetClampsFractionAboveOne() {
        // fraction > 1 must be clamped to 1 (no overshoot beyond radius)
        Vec3 offset = SpawnHelper.randomOffset(2.0, 0.0, 5.0);
        assertEquals(2.0, offset.x, EPS, "must clamp to radius");
    }

    @Test
    void randomOffsetClampsFractionBelowZero() {
        Vec3 offset = SpawnHelper.randomOffset(2.0, 0.0, -3.0);
        assertEquals(0.0, offset.x, EPS, "must clamp to 0");
    }

    @Test
    void randomOffsetZeroRadiusIsAlwaysOrigin() {
        Vec3 offset = SpawnHelper.randomOffset(0.0, 0.7, 0.9);
        assertEquals(0.0, offset.x, EPS);
        assertEquals(0.0, offset.z, EPS);
    }

    // ──────────────────────────────────────────────
    //  circlePosition
    // ──────────────────────────────────────────────

    @Test
    void circlePositionIndexZeroIsOnPositiveX() {
        Vec3 center = new Vec3(10.0, 64.0, 20.0);
        Vec3 p = SpawnHelper.circlePosition(center, 3.0, 0, 4);
        assertEquals(13.0, p.x, EPS, "x = center.x + radius");
        assertEquals(64.0, p.y, EPS, "y unchanged");
        assertEquals(20.0, p.z, EPS, "z = center.z at index 0");
    }

    @Test
    void circlePositionIsRadiusAwayFromCenter() {
        Vec3 center = new Vec3(0.0, 0.0, 0.0);
        double radius = 7.0;
        int total = 6;
        for (int i = 0; i < total; i++) {
            Vec3 p = SpawnHelper.circlePosition(center, radius, i, total);
            double dist = Math.hypot(p.x - center.x, p.z - center.z);
            assertEquals(radius, dist, EPS, "point " + i + " must sit on the circle");
        }
    }

    @Test
    void circlePositionDistributesEvenly() {
        Vec3 center = new Vec3(0.0, 0.0, 0.0);
        int total = 4;
        double radius = 2.0;
        // index 1 of 4 -> angle pi/2 -> positive Z
        Vec3 p1 = SpawnHelper.circlePosition(center, radius, 1, total);
        assertEquals(0.0, p1.x, EPS);
        assertEquals(radius, p1.z, EPS);
        // index 2 of 4 -> angle pi -> negative X
        Vec3 p2 = SpawnHelper.circlePosition(center, radius, 2, total);
        assertEquals(-radius, p2.x, EPS);
        assertEquals(0.0, p2.z, EPS);
    }

    @Test
    void circlePositionRejectsNonPositiveTotal() {
        Vec3 center = new Vec3(0.0, 0.0, 0.0);
        assertThrows(IllegalArgumentException.class,
                () -> SpawnHelper.circlePosition(center, 1.0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> SpawnHelper.circlePosition(center, 1.0, 0, -1));
    }

    // ──────────────────────────────────────────────
    //  Structural guarantees
    // ──────────────────────────────────────────────

    @Test
    void classIsFinal() {
        assertTrue(Modifier.isFinal(SpawnHelper.class.getModifiers()),
                "SpawnHelper must be final");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<SpawnHelper> ctor = SpawnHelper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                "SpawnHelper must have a private constructor");
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance(), "private constructor should be invokable");
    }
}
