package com.bikininjas.corelib.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectiveTests {

    @Test
    void killObjectiveStoresFieldsAndType() {
        var obj = new KillObjective("Slay the zombies", EntityType.ZOMBIE, 10);
        assertEquals("Slay the zombies", obj.description());
        assertSame(EntityType.ZOMBIE, obj.targetType());
        assertEquals(10, obj.target());
        assertEquals(ObjectiveType.KILL, obj.type());
    }

    @Test
    void killObjectiveRejectsNullDescription() {
        assertThrows(NullPointerException.class, () -> new KillObjective(null, EntityType.ZOMBIE, 1));
    }

    @Test
    void killObjectiveRejectsNullTargetType() {
        assertThrows(NullPointerException.class, () -> new KillObjective("desc", null, 1));
    }

    @Test
    void collectObjectiveStoresFieldsAndType() {
        var obj = new CollectObjective("Gather diamonds", Items.DIAMOND, 64);
        assertEquals("Gather diamonds", obj.description());
        assertSame(Items.DIAMOND, obj.targetItem());
        assertEquals(64, obj.target());
        assertEquals(ObjectiveType.COLLECT, obj.type());
    }

    @Test
    void collectObjectiveRejectsNullDescription() {
        assertThrows(NullPointerException.class, () -> new CollectObjective(null, Items.DIAMOND, 1));
    }

    @Test
    void collectObjectiveRejectsNullTargetItem() {
        assertThrows(NullPointerException.class, () -> new CollectObjective("desc", null, 1));
    }

    @Test
    void reachObjectiveStoresFieldsAndType() {
        var pos = new BlockPos(100, 64, -200);
        var obj = new ReachObjective("Reach the shrine", pos, 5.0);
        assertEquals("Reach the shrine", obj.description());
        assertEquals(pos, obj.targetPos());
        assertEquals(5.0, obj.radius());
        assertEquals(1, obj.target());
        assertEquals(ObjectiveType.REACH, obj.type());
    }

    @Test
    void reachObjectiveRejectsNullDescription() {
        assertThrows(NullPointerException.class, () -> new ReachObjective(null, new BlockPos(0, 0, 0), 1.0));
    }

    @Test
    void reachObjectiveRejectsNullTargetPos() {
        assertThrows(NullPointerException.class, () -> new ReachObjective("desc", null, 1.0));
    }

    @Test
    void survivalObjectiveStoresFieldsAndType() {
        var obj = new SurvivalObjective("Survive the night", 12000);
        assertEquals("Survive the night", obj.description());
        assertEquals(12000, obj.durationTicks());
        assertEquals(12000, obj.target());
        assertEquals(ObjectiveType.SURVIVE, obj.type());
    }

    @Test
    void survivalObjectiveRejectsNullDescription() {
        assertThrows(NullPointerException.class, () -> new SurvivalObjective(null, 100));
    }

    @Test
    void allSubtypesAreObjectives() {
        var objectives = java.util.List.<Objective>of(
                new KillObjective("k", EntityType.ZOMBIE, 1),
                new CollectObjective("c", Items.DIRT, 1),
                new ReachObjective("r", new BlockPos(0, 0, 0), 1.0),
                new SurvivalObjective("s", 100)
        );
        for (var obj : objectives) {
            assertNotNull(obj.description());
            assertNotNull(obj.type());
        }
    }
}
