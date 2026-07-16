package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.objective.Challenge;
import com.bikininjas.corelib.objective.CollectObjective;
import com.bikininjas.corelib.objective.KillObjective;
import com.bikininjas.corelib.objective.Objective;
import com.bikininjas.corelib.objective.ObjectiveTracker;
import com.bikininjas.corelib.objective.ReachObjective;
import com.bikininjas.corelib.objective.SurvivalObjective;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for the ObjectiveTracker module.
 * <p>
 * These run without a Minecraft runtime ({@code ./gradlew test}). Player state is
 * simulated with a minimal {@link ServerPlayer} stub built from a {@link GameProfile}
 * so that {@code getUUID()} is stable and {@code distanceToSqr} reflects the
 * default {@code 0,0,0} spawn position.
 */
class ObjectiveTrackerTests {

    /**
     * Minimal {@link ServerPlayer} stub created without invoking the heavy
     * Minecraft constructor (which requires a live {@code ServerLevel}).
     * <p>
     * We allocate the instance via {@link sun.misc.Unsafe} and inject only the
     * fields our objective logic reads: the entity {@code uuid} (for
     * {@code getUUID()}) and the entity {@code position} (for
     * {@code distanceToSqr}). This keeps the test a pure unit test with no
     * Minecraft runtime.
     */
    private static final class FakePlayer extends ServerPlayer {
        private static final sun.misc.Unsafe UNSAFE;
        static {
            try {
                var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                UNSAFE = (sun.misc.Unsafe) f.get(null);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        FakePlayer(UUID uuid) {
            super(null, null, null, null);
            try {
                setField(Entity.class, "uuid", uuid);
                setField(Entity.class, "position", new Vec3(0.0, 0.0, 0.0));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        static FakePlayer create(UUID uuid) {
            try {
                FakePlayer p = (FakePlayer) UNSAFE.allocateInstance(FakePlayer.class);
                p.setField(Entity.class, "uuid", uuid);
                p.setField(Entity.class, "position", new Vec3(0.0, 0.0, 0.0));
                return p;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private void setField(Class<?> declaring, String name, Object value) throws ReflectiveOperationException {
            var f = declaring.getDeclaredField(name);
            f.setAccessible(true);
            f.set(this, value);
        }
    }

    private final UUID PLAYER_ID = new UUID(0x1111_2222_3333_4444L, 0x5555_6666_7777_8888L);

    @AfterEach
    void clearState() {
        // Best-effort cleanup of static maps between tests.
        ObjectiveTracker.COUNTS.clear();
        ObjectiveTracker.START_TIMES.clear();
    }

    // ──────────────────────────────────────────────
    //  Structure
    // ──────────────────────────────────────────────

    @Test
    void classIsFinal() {
        assertTrue(Modifier.isFinal(ObjectiveTracker.class.getModifiers()),
                "ObjectiveTracker must be a final class");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<ObjectiveTracker> ctor = ObjectiveTracker.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                "ObjectiveTracker constructor must be private");
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance(), "Private constructor should be invokable");
    }

    // ──────────────────────────────────────────────
    //  Record implementations
    // ──────────────────────────────────────────────

    @Test
    void killObjectiveAccessors() {
        KillObjective o = new KillObjective("Slay zombies", EntityType.ZOMBIE, 10);
        assertEquals("Slay zombies", o.description());
        assertEquals(10, o.target());
        assertEquals(Objective.ObjectiveType.KILL, o.type());
        assertEquals(EntityType.ZOMBIE, o.entityType());
    }

    @Test
    void collectObjectiveAccessors() {
        Item item = net.minecraft.world.item.Items.DIAMOND;
        CollectObjective o = new CollectObjective("Gather diamonds", item, 5);
        assertEquals("Gather diamonds", o.description());
        assertEquals(5, o.target());
        assertEquals(Objective.ObjectiveType.COLLECT, o.type());
        assertEquals(item, o.item());
    }

    @Test
    void reachObjectiveAccessors() {
        BlockPos pos = new BlockPos(100, 64, 100);
        ReachObjective o = new ReachObjective("Reach shrine", pos, 4.0);
        assertEquals("Reach shrine", o.description());
        assertEquals(1, o.target());
        assertEquals(Objective.ObjectiveType.REACH, o.type());
        assertEquals(pos, o.targetPos());
        assertEquals(4.0, o.radius());
    }

    @Test
    void survivalObjectiveAccessors() {
        SurvivalObjective o = new SurvivalObjective("Survive 100 ticks", 100);
        assertEquals("Survive 100 ticks", o.description());
        assertEquals(100, o.target());
        assertEquals(Objective.ObjectiveType.SURVIVE, o.type());
        assertEquals(100, o.durationTicks());
    }

    // ──────────────────────────────────────────────
    //  Challenge record
    // ──────────────────────────────────────────────

    @Test
    void challengeRecord() {
        List<Objective> objs = List.of(
                new KillObjective("A", EntityType.CREEPER, 3),
                new CollectObjective("B", net.minecraft.world.item.Items.EMERALD, 2)
        );
        Challenge c = new Challenge("Trial", objs, 60);
        assertEquals("Trial", c.name());
        assertEquals(2, c.objectives().size());
        assertEquals(60, c.timeLimitSeconds());
    }

    // ──────────────────────────────────────────────
    //  Progress per implementation
    // ──────────────────────────────────────────────

    @Test
    void killObjectiveProgressReadsCounts() {
        ServerPlayer player = FakePlayer.create(PLAYER_ID);
        KillObjective o = new KillObjective("Kill pigs", EntityType.PIG, 4);

        // No count yet → 0.0
        assertEquals(0.0f, o.progress(player), 0.0001f);
        assertEquals(0, o.progressValue(player));

        // Inject 2 kills → 0.5
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        counts.put("Kill pigs", 2);
        ObjectiveTracker.COUNTS.put(PLAYER_ID, counts);
        assertEquals(0.5f, o.progress(player), 0.0001f);
        assertEquals(2, o.progressValue(player));

        // Inject 4 kills → complete
        counts.put("Kill pigs", 4);
        assertEquals(1.0f, o.progress(player), 0.0001f);
        assertTrue(o.isComplete(player));

        // Over-target is clamped
        counts.put("Kill pigs", 99);
        assertEquals(1.0f, o.progress(player), 0.0001f);
        assertEquals(4, o.progressValue(player));
    }

    @Test
    void collectObjectiveProgressReadsCounts() {
        ServerPlayer player = FakePlayer.create(PLAYER_ID);
        Item item = net.minecraft.world.item.Items.GOLD_INGOT;
        CollectObjective o = new CollectObjective("Gold", item, 2);

        assertEquals(0.0f, o.progress(player), 0.0001f);

        Map<String, Integer> counts = new ConcurrentHashMap<>();
        counts.put("Gold", 1);
        ObjectiveTracker.COUNTS.put(PLAYER_ID, counts);
        assertEquals(0.5f, o.progress(player), 0.0001f);

        counts.put("Gold", 2);
        assertEquals(1.0f, o.progress(player), 0.0001f);
        assertTrue(o.isComplete(player));
    }

    @Test
    void reachObjectiveProgressUsesDistance() {
        ServerPlayer player = FakePlayer.create(PLAYER_ID);

        // Target at origin → within radius → complete
        ReachObjective atOrigin = new ReachObjective("Here", new BlockPos(0, 0, 0), 3.0);
        assertEquals(1.0f, atOrigin.progress(player), 0.0001f);
        assertTrue(atOrigin.isComplete(player));

        // Target far away → not complete
        ReachObjective far = new ReachObjective("There", new BlockPos(100, 0, 100), 3.0);
        assertEquals(0.0f, far.progress(player), 0.0001f);
        assertFalse(far.isComplete(player));
    }

    @Test
    void survivalObjectiveProgressWithoutStartIsZero() {
        ServerPlayer player = FakePlayer.create(PLAYER_ID);
        SurvivalObjective o = new SurvivalObjective("Survive", 100);

        // No start time recorded → 0.0
        assertEquals(0.0f, o.progress(player), 0.0001f);
        assertEquals(0, o.progressValue(player));
        assertFalse(o.isComplete(player));
    }

    // ──────────────────────────────────────────────
    //  Tracker behaviour
    // ──────────────────────────────────────────────

    @Test
    void startChallengeRejectsNullPlayer() {
        Challenge c = new Challenge("C", List.of(), 0);
        assertThrows(NullPointerException.class,
                () -> ObjectiveTracker.startChallenge(null, c),
                "startChallenge(null, ...) must throw NPE");
    }

    @Test
    void startChallengeRejectsNullChallenge() {
        ServerPlayer player = FakePlayer.create(PLAYER_ID);
        assertThrows(NullPointerException.class,
                () -> ObjectiveTracker.startChallenge(player, null),
                "startChallenge(player, null) must throw NPE");
    }

    @Test
    void stopChallengeRejectsNullPlayer() {
        assertThrows(NullPointerException.class,
                () -> ObjectiveTracker.stopChallenge(null),
                "stopChallenge(null) must throw NPE");
    }

    @Test
    void getProgressNoObjectivesReturnsZero() {
        // No challenge started for this player → 0.0f
        assertEquals(0.0f, ObjectiveTracker.getProgress(FakePlayer.create(PLAYER_ID)), 0.0001f);
        // Null player also yields 0.0f (safe no-op)
        assertEquals(0.0f, ObjectiveTracker.getProgress(null), 0.0001f);
    }

    @Test
    void isTrackingReflectsState() {
        ServerPlayer player = FakePlayer.create(PLAYER_ID);
        assertFalse(ObjectiveTracker.isTracking(player));

        Challenge c = new Challenge("C",
                List.of(new KillObjective("K", EntityType.COW, 1)), 0);
        ObjectiveTracker.startChallenge(player, c);
        assertTrue(ObjectiveTracker.isTracking(player));
        assertEquals(1, ObjectiveTracker.getObjectives(player).size());

        ObjectiveTracker.stopChallenge(player);
        assertFalse(ObjectiveTracker.isTracking(player));
        assertTrue(ObjectiveTracker.getObjectives(player).isEmpty());
    }

    // ──────────────────────────────────────────────
    //  Record equality (counter bucket sharing)
    // ──────────────────────────────────────────────

    @Test
    void killObjectivesWithSameParamsAreEqual() {
        KillObjective a = new KillObjective("Same", EntityType.SHEEP, 7);
        KillObjective b = new KillObjective("Same", EntityType.SHEEP, 7);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        // Record equality means both resolve to the same COUNTS bucket (description key).
        ServerPlayer player = FakePlayer.create(PLAYER_ID);
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        counts.put(a.description(), 3);
        ObjectiveTracker.COUNTS.put(PLAYER_ID, counts);
        assertEquals(3, b.progressValue(player));
    }
}
