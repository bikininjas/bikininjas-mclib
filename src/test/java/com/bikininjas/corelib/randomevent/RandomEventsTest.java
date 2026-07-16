package com.bikininjas.corelib.randomevent;

import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomEventsTest {

    @Test
    void factoryMethodsProduceEventsWithExpectedNames() {
        assertEquals("announce", RandomEvents.announceEvent("hello").name());
        assertEquals("spawn_entity", RandomEvents.spawnEntityEvent(EntityType.PIG, 3).name());
        assertEquals("explosion", RandomEvents.randomExplosionEvent(4.0f, true).name());
        assertEquals("clear_weather", RandomEvents.clearWeatherEvent().name());
        assertEquals("random_weather", RandomEvents.randomWeatherEvent().name());
    }

    @Test
    void factoryEventsAreNonNullAndHaveNonBlankNames() {
        var events = java.util.List.of(
                RandomEvents.announceEvent("hello"),
                RandomEvents.spawnEntityEvent(EntityType.PIG, 3),
                RandomEvents.randomExplosionEvent(4.0f, true),
                RandomEvents.clearWeatherEvent(),
                RandomEvents.randomWeatherEvent()
        );
        for (var event : events) {
            assertNotNull(event, "factory event must not be null");
            assertNotNull(event.name(), "event name must not be null");
            assertFalse(event.name().isBlank(), "event name must not be blank");
        }
    }

    @Test
    void announceEventRejectsNullMessage() {
        assertThrows(NullPointerException.class, () -> RandomEvents.announceEvent(null));
    }

    @Test
    void spawnEntityEventRejectsNullType() {
        assertThrows(NullPointerException.class, () -> RandomEvents.spawnEntityEvent(null, 1));
    }

    @Test
    void weightsAreConfiguredAsDocumented() {
        // explosion is intentionally low weight (dangerous), weather events higher.
        assertEquals(1, RandomEvents.randomExplosionEvent(1.0f, false).weight());
        assertEquals(3, RandomEvents.clearWeatherEvent().weight());
        assertEquals(2, RandomEvents.randomWeatherEvent().weight());
    }
}
