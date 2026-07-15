package com.bikininjas.corelib.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Static utility for spawning entities into a {@link ServerLevel}.
 * <p>
 * All methods are server-side only — they operate on {@link ServerLevel} and
 * never touch client-side code. The helper is fully stateless: it holds no
 * fields, subscribes to no events, and registers nothing.
 * <p>
 * Position math is exposed through pure, Minecraft-runtime-free methods
 * ({@link #randomOffset(double, double, double)} and
 * {@link #circlePosition(Vec3, double, int, int)}) so the geometry can be unit
 * tested without booting a game instance.
 */
public final class SpawnHelper {

    /** Default radius (blocks) used to ring mobs around a player. */
    private static final double MOB_RING_RADIUS = 3.0;

    private SpawnHelper() {}

    // ──────────────────────────────────────────────
    //  Pure position math (unit-testable, no MC runtime)
    // ──────────────────────────────────────────────

    /**
     * Compute a horizontal offset inside a circle of the given radius.
     * <p>
     * Pure math — no Minecraft runtime required. {@code angle} is in radians,
     * {@code distanceFraction} is in {@code [0, 1]} (1 = on the rim). The Y
     * component is always {@code 0}.
     *
     * @param radius           circle radius in blocks (must be non-negative)
     * @param angle            direction in radians
     * @param distanceFraction fraction of the radius to travel (0..1)
     * @return the offset vector (x, 0, z)
     */
    public static Vec3 randomOffset(double radius, double angle, double distanceFraction) {
        double distance = radius * clamp01(distanceFraction);
        return new Vec3(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
    }

    /**
     * Compute a point on a circle of {@code radius} around {@code center}.
     * <p>
     * Pure math — no Minecraft runtime required. The {@code index}-th point of
     * {@code total} evenly spaced points is returned. {@code index 0} sits on
     * the positive X axis; points advance counter-clockwise.
     *
     * @param center the circle center
     * @param radius the circle radius (must be non-negative)
     * @param index  which point to compute (0-based)
     * @param total  total number of points around the circle (must be &gt; 0)
     * @return the world position of the point
     */
    public static Vec3 circlePosition(Vec3 center, double radius, int index, int total) {
        if (total <= 0) {
            throw new IllegalArgumentException("total must be > 0, was " + total);
        }
        double angle = (2.0 * Math.PI * index) / total;
        return new Vec3(
                center.x + Math.cos(angle) * radius,
                center.y,
                center.z + Math.sin(angle) * radius
        );
    }

    // ──────────────────────────────────────────────
    //  Spawning
    // ──────────────────────────────────────────────

    /**
     * Spawn an entity of the given type at a position.
     *
     * @param level the server level to spawn into
     * @param type  the entity type to create
     * @param pos   the world position
     * @return the spawned entity
     * @throws IllegalStateException if the entity could not be created
     */
    public static Entity spawn(ServerLevel level, EntityType<?> type, Vec3 pos) {
        Entity entity = requireCreated(type, type.create(level));
        entity.setPos(pos);
        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Spawn an entity of the given type at explicit coordinates.
     *
     * @param level the server level to spawn into
     * @param type  the entity type to create
     * @param x     world X
     * @param y     world Y
     * @param z     world Z
     * @return the spawned entity
     * @throws IllegalStateException if the entity could not be created
     */
    public static Entity spawn(ServerLevel level, EntityType<?> type, double x, double y, double z) {
        return spawn(level, type, new Vec3(x, y, z));
    }

    /**
     * Spawn an entity and configure it before it is added to the world.
     * <p>
     * The configurator runs after the position is set but before the entity is
     * registered with the level, so it can safely tweak entity state (e.g.
     * custom name, health, rotation) without double-adding.
     *
     * @param level         the server level to spawn into
     * @param type          the entity type to create
     * @param pos           the world position
     * @param configurator  callback applied to the entity pre-add
     * @return the spawned entity
     * @throws IllegalStateException if the entity could not be created
     */
    public static Entity spawnWithConfig(
            ServerLevel level,
            EntityType<?> type,
            Vec3 pos,
            Consumer<Entity> configurator
    ) {
        Entity entity = requireCreated(type, type.create(level));
        entity.setPos(pos);
        configurator.accept(entity);
        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Spawn {@code count} entities randomly scattered within {@code radius}
     * blocks of {@code center}. Each entity gets an independent random angle
     * and an area-uniform random distance (using a square-root distribution so
     * points are not clustered toward the center).
     *
     * @param level  the server level to spawn into
     * @param type   the entity type to create
     * @param center the center point
     * @param radius scatter radius in blocks (must be non-negative)
     * @param count  how many entities to spawn (must be &ge; 0)
     * @return the list of spawned entities (size == count)
     */
    public static List<Entity> spawnRandomNearby(
            ServerLevel level,
            EntityType<?> type,
            Vec3 center,
            double radius,
            int count
    ) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0, was " + count);
        }
        List<Entity> result = new ArrayList<>(count);
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double distanceFraction = Math.sqrt(random.nextDouble());
            Vec3 offset = randomOffset(radius, angle, distanceFraction);
            result.add(spawn(level, type, center.add(offset)));
        }
        return result;
    }

    /**
     * Spawn an entity at the player's current position.
     *
     * @param level  the server level to spawn into
     * @param player the player whose position is used
     * @param type   the entity type to create
     * @return the spawned entity
     * @throws IllegalStateException if the entity could not be created
     */
    public static Entity spawnAtPlayer(ServerLevel level, ServerPlayer player, EntityType<?> type) {
        return spawn(level, type, player.position());
    }

    /**
     * Spawn an entity at the player's position with a vertical offset.
     *
     * @param level    the server level to spawn into
     * @param player   the player whose position is used
     * @param type     the entity type to create
     * @param offsetY  added to the player's Y (e.g. 1.0 to spawn one block up)
     * @return the spawned entity
     * @throws IllegalStateException if the entity could not be created
     */
    public static Entity spawnAtPlayer(
            ServerLevel level,
            ServerPlayer player,
            EntityType<?> type,
            double offsetY
    ) {
        return spawn(level, type, player.position().add(0.0, offsetY, 0.0));
    }

    /**
     * Spawn an entity at a random point within {@code radius} blocks of the
     * player (a single-shot variant of {@link #spawnRandomNearby}).
     *
     * @param level  the server level to spawn into
     * @param player the player to spawn near
     * @param type   the entity type to create
     * @param radius scatter radius in blocks (must be non-negative)
     * @return the spawned entity
     * @throws IllegalStateException if the entity could not be created
     */
    public static Entity spawnNearPlayer(
            ServerLevel level,
            ServerPlayer player,
            EntityType<?> type,
            double radius
    ) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double distanceFraction = Math.sqrt(random.nextDouble());
        Vec3 offset = randomOffset(radius, angle, distanceFraction);
        return spawn(level, type, player.position().add(offset));
    }

    /**
     * Spawn {@code mobCount} hostile mobs evenly distributed in a ring around
     * the player — like dropping a "mob trap" on them.
     *
     * @param level    the server level to spawn into
     * @param player   the player to surround
     * @param type     the mob type to create (must extend {@link Mob})
     * @param mobCount how many mobs to spawn (must be &gt; 0)
     * @throws IllegalArgumentException if {@code mobCount} is not positive
     * @throws IllegalStateException    if a mob could not be created
     */
    public static void spawnMobAtPlayer(
            ServerLevel level,
            ServerPlayer player,
            EntityType<? extends Mob> type,
            int mobCount
    ) {
        if (mobCount <= 0) {
            throw new IllegalArgumentException("mobCount must be > 0, was " + mobCount);
        }
        Vec3 center = player.position();
        for (int i = 0; i < mobCount; i++) {
            Mob mob = requireCreated(type, type.create(level));
            mob.setPos(circlePosition(center, MOB_RING_RADIUS, i, mobCount));
            level.addFreshEntity(mob);
        }
    }

    // ──────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────

    private static <T extends Entity> T requireCreated(EntityType<?> type, T entity) {
        if (entity == null) {
            throw new IllegalStateException(
                    "EntityType.create() returned null for " + type + ". "
                            + "This usually means the type cannot be instantiated on the server.");
        }
        return entity;
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
