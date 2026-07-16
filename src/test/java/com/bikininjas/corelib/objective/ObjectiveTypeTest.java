package com.bikininjas.corelib.objective;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectiveTypeTest {

    @Test
    void enumContainsExpectedValues() {
        assertEquals(4, ObjectiveType.values().length);
        assertNotNull(ObjectiveType.valueOf("KILL"));
        assertNotNull(ObjectiveType.valueOf("COLLECT"));
        assertNotNull(ObjectiveType.valueOf("REACH"));
        assertNotNull(ObjectiveType.valueOf("SURVIVE"));
    }

    @Test
    void valueOfIsCaseSensitiveAndExact() {
        assertEquals(ObjectiveType.KILL, ObjectiveType.valueOf("KILL"));
        assertEquals(ObjectiveType.COLLECT, ObjectiveType.valueOf("COLLECT"));
        assertEquals(ObjectiveType.REACH, ObjectiveType.valueOf("REACH"));
        assertEquals(ObjectiveType.SURVIVE, ObjectiveType.valueOf("SURVIVE"));
    }

    @Test
    void valueOfThrowsOnUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> ObjectiveType.valueOf("UNKNOWN"));
    }

    @Test
    void eachValueHasStableNameAndOrdinal() {
        assertEquals("KILL", ObjectiveType.KILL.name());
        assertEquals(0, ObjectiveType.KILL.ordinal());
        assertEquals("COLLECT", ObjectiveType.COLLECT.name());
        assertEquals(1, ObjectiveType.COLLECT.ordinal());
        assertEquals("REACH", ObjectiveType.REACH.name());
        assertEquals(2, ObjectiveType.REACH.ordinal());
        assertEquals("SURVIVE", ObjectiveType.SURVIVE.name());
        assertEquals(3, ObjectiveType.SURVIVE.ordinal());
    }

    @Test
    void valueConsistencyAcrossValueOfAndValues() {
        for (var type : ObjectiveType.values()) {
            assertSame(type, ObjectiveType.valueOf(type.name()));
        }
    }
}
