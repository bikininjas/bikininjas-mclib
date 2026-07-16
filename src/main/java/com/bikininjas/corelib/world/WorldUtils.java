package com.bikininjas.corelib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utilities for world and level manipulation.
 * <p>
 * All methods are static. No event bus registration.
 */
public final class WorldUtils {

    private WorldUtils() {
    }

    // -- Block operations ----------------------------------------------------

    /**
     * Set a block at the given position.
     *
     * @return true if the block was changed
     */
    public static boolean setBlock(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        Objects.requireNonNull(state, "state must not be null");
        return level.setBlock(pos, state, 3);
    }

    /**
     * Get the block state at the given position.
     */
    public static @NotNull BlockState getBlock(@NotNull Level level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        return level.getBlockState(pos);
    }

    /**
     * Check if the block at the given position is air.
     */
    public static boolean isAir(@NotNull Level level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        return level.getBlockState(pos).isAir();
    }

    /**
     * Fill a rectangular area with a block state.
     *
     * @return the number of blocks changed
     */
    public static int fillArea(
            @NotNull Level level,
            @NotNull BlockPos from,
            @NotNull BlockPos to,
            @NotNull BlockState state) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(state, "state must not be null");

        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (level.setBlock(new BlockPos(x, y, z), state, 3)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // -- Chunk operations ----------------------------------------------------

    /**
     * Get the chunk at the given position.
     */
    public static @Nullable LevelChunk getChunk(@NotNull Level level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        return (LevelChunk) level.getChunk(pos);
    }

    /**
     * Check if the chunk at the given position is loaded.
     */
    public static boolean isLoaded(@NotNull Level level, @NotNull BlockPos pos) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        return level.isLoaded(pos);
    }

    // -- Entity queries ------------------------------------------------------

    /**
     * Get all entities of a specific class in the level.
     */
    public static @NotNull <T extends Entity> List<T> getEntities(
            @NotNull Level level, @NotNull Class<T> entityClass) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        return level.getEntitiesOfClass(entityClass,
                new AABB(level.getMinBuildHeight(), level.getMinBuildHeight(), level.getMinBuildHeight(),
                        level.getMaxBuildHeight(), level.getMaxBuildHeight(), level.getMaxBuildHeight()));
    }

    /**
     * Get all entities of a specific class within a radius of a position.
     */
    public static @NotNull <T extends Entity> List<T> getEntitiesInRange(
            @NotNull Level level, @NotNull BlockPos pos,
            double radius, @NotNull Class<T> entityClass) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        var aabb = new AABB(pos).inflate(radius);
        return level.getEntitiesOfClass(entityClass, aabb);
    }

    /**
     * Get all players within a radius of a position.
     */
    public static @NotNull List<ServerPlayer> getPlayersInRange(
            @NotNull Level level, @NotNull BlockPos pos, double radius) {
        return getEntitiesInRange(level, pos, radius, ServerPlayer.class);
    }

    /**
     * Get the nearest player to a position within a radius, or null if none.
     */
    public static @Nullable ServerPlayer getNearestPlayer(
            @NotNull Level level, @NotNull BlockPos pos, double radius) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        if (level instanceof ServerLevel serverLevel) {
            var player = serverLevel.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), radius, false);
            return player instanceof ServerPlayer sp ? sp : null;
        }
        return null;
    }
}
