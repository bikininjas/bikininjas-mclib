package com.bikininjas.corelib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Static utilities for block, chunk, and entity manipulation.
 * <p>
 * Every method is a pure delegate to the Minecraft/NeoForge API with null-safety
 * and convenience overloads. This class is a final utility and cannot be
 * instantiated.
 */
public final class WorldUtils {

    private WorldUtils() {
    }

    // ============================================================
    //  Block operations
    // ============================================================

    /**
     * Get the {@link BlockState} at a position.
     *
     * @param level the level (must not be {@code null})
     * @param pos   the position (must not be {@code null})
     * @return the block state (never {@code null})
     */
    public static @NotNull BlockState getBlock(@NotNull Level level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        return level.getBlockState(pos);
    }

    /**
     * Set a block state at a position with the default update flags (3 = notify
     * neighbours + send to clients).
     *
     * @param level the level (must not be {@code null})
     * @param pos   the position (must not be {@code null})
     * @param state the new block state (must not be {@code null})
     * @return {@code true} if the block was changed
     */
    public static boolean setBlock(@NotNull Level level, @NotNull BlockPos pos,
                                   @NotNull BlockState state) {
        return setBlock(level, pos, state, 3);
    }

    /**
     * Set a block state at a position with custom flags.
     *
     * @param level the level (must not be {@code null})
     * @param pos   the position (must not be {@code null})
     * @param state the new block state (must not be {@code null})
     * @param flags update flags (bitmask: 1=notify neighbours, 2=send to clients, 4=no re-render, …)
     * @return {@code true} if the block was changed
     */
    public static boolean setBlock(@NotNull Level level, @NotNull BlockPos pos,
                                   @NotNull BlockState state, int flags) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        return level.setBlock(pos, state, flags);
    }

    /**
     * Check whether the block at a position is air.
     *
     * @param level the level (must not be {@code null})
     * @param pos   the position (must not be {@code null})
     * @return {@code true} if the block is air
     */
    public static boolean isAir(@NotNull Level level, @NotNull BlockPos pos) {
        return getBlock(level, pos).isAir();
    }

    /**
     * Fill a cuboid region with a single block state.
     *
     * @param level the level (must not be {@code null})
     * @param from  one corner of the cuboid (inclusive, must not be {@code null})
     * @param to    the opposite corner (inclusive, must not be {@code null})
     * @param state the block state to place (must not be {@code null})
     * @return the number of blocks changed
     */
    public static int fillArea(@NotNull Level level, @NotNull BlockPos from,
                               @NotNull BlockPos to, @NotNull BlockState state) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(state, "state");

        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());

        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (level.setBlock(new BlockPos(x, y, z), state, 3)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // ============================================================
    //  Chunk operations
    // ============================================================

    /**
     * Get the {@link LevelChunk} at a position.
     *
     * @param level the level (must not be {@code null})
     * @param pos   the position (must not be {@code null})
     * @return the chunk containing the position
     */
    public static @NotNull LevelChunk getChunk(@NotNull Level level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        return level.getChunkAt(pos);
    }

    /**
     * Check whether the chunk at a position is fully loaded.
     *
     * @param level the level (must not be {@code null})
     * @param pos   the position (must not be {@code null})
     * @return {@code true} if the chunk containing {@code pos} is loaded
     */
    public static boolean isLoaded(@NotNull Level level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        return level.isLoaded(pos);
    }

    // ============================================================
    //  Entity operations
    // ============================================================

    /**
     * Get all entities of a given type within an axis-aligned bounding box.
     *
     * @param level   the level (must not be {@code null})
     * @param type    the entity type filter (use {@link EntityType#ENTITY} for all)
     * @param aabb    the bounding box (must not be {@code null})
     * @param filter  optional additional predicate (may be {@code null})
     * @param <T>     the entity type
     * @return list of matching entities (never {@code null})
     */
    @SuppressWarnings("unchecked")
    public static @NotNull <T extends Entity> List<T> getEntities(
            @NotNull Level level, @NotNull EntityTypeTest<Entity, T> type,
            @NotNull AABB aabb, @Nullable Predicate<? super T> filter) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(aabb, "aabb");
        return level.getEntities(type, aabb, filter != null ? filter : e -> true);
    }

    /**
     * Get all entities within a sphere around a centre point.
     *
     * @param level  the level (must not be {@code null})
     * @param centre the centre of the sphere (must not be {@code null})
     * @param radius the search radius in blocks
     * @return list of matching entities (never {@code null})
     */
    public static @NotNull List<Entity> getEntitiesInRange(
            @NotNull Level level, @NotNull Vec3 centre, double radius) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(centre, "centre");
        AABB aabb = new AABB(centre.x - radius, centre.y - radius, centre.z - radius,
                centre.x + radius, centre.y + radius, centre.z + radius);
        return level.getEntities((Entity) null, aabb, e -> true);
    }

    /**
     * Get all players within a sphere around a centre point.
     *
     * @param level  the server level (must not be {@code null})
     * @param centre the centre of the sphere (must not be {@code null})
     * @param radius the search radius in blocks
     * @return list of matching players (never {@code null})
     */
    public static @NotNull List<ServerPlayer> getPlayersInRange(
            @NotNull ServerLevel level, @NotNull Vec3 centre, double radius) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(centre, "centre");
        double radiusSq = radius * radius;
        return level.players().stream()
                .filter(p -> p.distanceToSqr(centre) <= radiusSq)
                .toList();
    }

    /**
     * Find the nearest player to a centre point within a maximum distance.
     *
     * @param level       the server level (must not be {@code null})
     * @param centre      the centre point (must not be {@code null})
     * @param maxDistance the maximum search distance in blocks
     * @return the nearest player, or {@code null} if none are within range
     */
    public static @Nullable ServerPlayer getNearestPlayer(
            @NotNull ServerLevel level, @NotNull Vec3 centre, double maxDistance) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(centre, "centre");
        double maxSq = maxDistance * maxDistance;
        ServerPlayer nearest = null;
        double nearestSq = maxSq;
        for (ServerPlayer player : level.players()) {
            double distSq = player.distanceToSqr(centre);
            if (distSq < nearestSq) {
                nearest = player;
                nearestSq = distSq;
            }
        }
        return nearest;
    }
}
