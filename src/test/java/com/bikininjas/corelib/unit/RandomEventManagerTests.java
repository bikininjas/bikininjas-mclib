package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.randomevent.RandomEvent;
import com.bikininjas.corelib.randomevent.RandomEventManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for {@link RandomEventManager}.
 * <p>
 * These exercise registration, removal, weighted selection and interval
 * configuration without requiring a live Minecraft server. Event execution is
 * mocked with simple counters; {@link ServerLevel} is passed as {@code null}
 * because the mock events ignore it.
 */
class RandomEventManagerTests {

    /** A mock event that counts how many times it was executed. */
    static final class CountingEvent implements RandomEvent {
        int calls = 0;
        private final int weight;
        private final String name;

        CountingEvent(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }

        @Override
        public void execute(ServerLevel level, Vec3 origin) {
            calls++;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int weight() {
            return weight;
        }
    }

    private RandomEventManager manager;

    @BeforeEach
    void setUp() {
        manager = RandomEventManager.getInstance();
        manager.reset();
    }

    @Test
    void registerAddsEventToPool() {
        manager.register(new CountingEvent("alpha", 100), "alpha");
        assertEquals(1, manager.getEventCount());
        assertTrue(manager.getAllEvents().contains("alpha"));
    }

    @Test
    void registerReturnsThisForChaining() {
        var result = manager.register(new CountingEvent("a", 100), "a")
                            .register(new CountingEvent("b", 100), "b");
        assertSame(manager, result);
        assertEquals(2, manager.getEventCount());
    }

    @Test
    void removeDeletesEventByName() {
        manager.register(new CountingEvent("alpha", 100), "alpha");
        manager.remove("alpha");
        assertEquals(0, manager.getEventCount());
        assertFalse(manager.getAllEvents().contains("alpha"));
    }

    @Test
    void removeUnknownNameIsNoOp() {
        manager.register(new CountingEvent("alpha", 100), "alpha");
        manager.remove("does_not_exist");
        assertEquals(1, manager.getEventCount());
    }

    @Test
    void getAllEventsReturnsNames() {
        manager.register(new CountingEvent("x", 100), "x")
               .register(new CountingEvent("y", 100), "y")
               .register(new CountingEvent("z", 100), "z");
        List<String> names = manager.getAllEvents();
        assertEquals(3, names.size());
        assertTrue(names.containsAll(List.of("x", "y", "z")));
    }

    @Test
    void setIntervalUpdatesBounds() {
        manager.setInterval(100, 500);
        assertEquals(100, manager.getMinInterval());
        assertEquals(500, manager.getMaxInterval());
    }

    @Test
    void setIntervalClampsNegativeAndInvertedBounds() {
        manager.setInterval(-50, 10);
        assertEquals(0, manager.getMinInterval());
        assertEquals(10, manager.getMaxInterval());

        manager.setInterval(800, 200);
        assertEquals(200, manager.getMinInterval());
        assertEquals(800, manager.getMaxInterval());
    }

    @Test
    void setEnabledTogglesSystem() {
        manager.setEnabled(false);
        assertFalse(manager.isEnabled());
        manager.setEnabled(true);
        assertTrue(manager.isEnabled());
    }

    @Test
    void fireEventExecutesTargetedEvent() {
        var event = new CountingEvent("boom", 100);
        manager.register(event, "boom");
        manager.fireEvent("boom", null, Vec3.ZERO);
        assertEquals(1, event.calls);
    }

    @Test
    void fireEventUnknownNameDoesNothing() {
        var event = new CountingEvent("boom", 100);
        manager.register(event, "boom");
        manager.fireEvent("missing", null, Vec3.ZERO);
        assertEquals(0, event.calls);
    }

    @Test
    void fireEventRespectsDisabledSystem() {
        var event = new CountingEvent("boom", 100);
        manager.register(event, "boom");
        manager.setEnabled(false);
        manager.fireEvent("boom", null, Vec3.ZERO);
        assertEquals(0, event.calls);
    }

    @Test
    void fireRandomEventExecutesOneEvent() {
        var event = new CountingEvent("solo", 100);
        manager.register(event, "solo");
        manager.fireRandomEvent(null);
        assertEquals(1, event.calls);
    }

    @Test
    void fireRandomEventDoesNothingWhenEmpty() {
        // Pool is empty after reset()
        manager.fireRandomEvent(null);
        // No exception, no events to fire.
        assertEquals(0, manager.getEventCount());
    }

    @Test
    void weightedSelectionFavoursHeavierEvents() {
        var heavy = new CountingEvent("heavy", 1000);
        var light = new CountingEvent("light", 1);
        manager.register(heavy, "heavy").register(light, "light");

        int heavyHits = 0;
        int trials = 5000;
        for (int i = 0; i < trials; i++) {
            if (manager.selectRandomEvent() == heavy) {
                heavyHits++;
            }
        }
        // Heavy weight is ~99.9% of the total — it must dominate selection.
        assertTrue(heavyHits > trials * 0.95,
                "Heavy event should win >95% of the time, got " + heavyHits + "/" + trials);
    }

    @Test
    void weightedSelectionNeverPicksZeroWeight() {
        var zero = new CountingEvent("zero", 0);
        var normal = new CountingEvent("normal", 100);
        manager.register(zero, "zero").register(normal, "normal");

        for (int i = 0; i < 1000; i++) {
            assertNotSame(zero, manager.selectRandomEvent(),
                    "Zero-weight event must never be selected");
        }
    }

    @Test
    void selectRandomEventReturnsNullWhenEmpty() {
        assertNull(manager.selectRandomEvent());
    }
}
