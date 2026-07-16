package com.bikininjas.corelib.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Utilities for spawning entities with various positioning options.
 * <p>
 * All methods are static. No event bus registration.
 */
public final class SpawnHelper {

    private SpawnHelper() {
    }

    // -- Basic spawn ---------------------------------------------------------

    /**
     * Spawn an entity at an exact position.
     *
     * @param level the server level
     * @param type  the entity type
     * @param pos   the position
     * @param <T>   the entity type
     * @return the spawned entity, or {@code null} if the entity could not be created
     */
    public static @Nullable <T extends Entity> T spawnAt(
            @NotNull ServerLevel level, @NotNull EntityType<T> type, @NotNull Vec3 pos) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        var entity = type.create(level);
        if (entity != null) {
            entity.setPos(pos);
            level.addFreshEntity(entity);
        }
        return entity;
    }

    /**
     * Spawn an entity at coordinates.
     */
    public static @Nullable <T extends Entity> T spawnAt(
            @NotNull ServerLevel level, @NotNull EntityType<T> type,
            double x, double y, double z) {
        return spawnAt(level, type, new Vec3(x, y, z));
    }

    /**
     * Spawn an entity with a consumer-based configuration callback
     * (e.g., to set equipment, AI, or potion effects).
     */
    public static @Nullable <T extends Entity> T spawnWithConfig(
            @NotNull ServerLevel level, @NotNull EntityType<T> type, @NotNull Vec3 pos,
            @NotNull java.util.function.Consumer<T> configurator) {
        Objects.requireNonNull(configurator, "configurator must not be null");
        var entity = spawnAt(level, type, pos);
        if (entity != null) {
            configurator.accept(entity);
        }
        return entity;
    }

    // -- Random nearby spawn -------------------------------------------------

    /**
     * Spawn an entity at a random position within a radius of the center.
     */
    public static @Nullable <T extends Entity> T spawnRandomNearby(
            @NotNull ServerLevel level, @NotNull EntityType<T> type,
            @NotNull Vec3 center, double radius) {
        Objects.requireNonNull(center, "center must not be null");
        var offset = randomOffset(radius, level.random.nextDouble() * 2 * Math.PI, level.random.nextDouble());
        return spawnAt(level, type, center.add(offset));
    }

    // -- Player-relative spawn -----------------------------------------------

    /**
     * Spawn an entity at a specific distance in front of the player.
     */
    public static @Nullable <T extends Entity> T spawnAtPlayer(
            @NotNull ServerPlayer player, @NotNull EntityType<T> type, double distance) {
        Objects.requireNonNull(player, "player must not be null");
        var lookAngle = player.getLookAngle();
        var pos = player.position().add(lookAngle.x * distance, 0.5, lookAngle.z * distance);
        return spawnAt(player.serverLevel(), type, pos);
    }

    /**
     * Spawn an entity 3 blocks in front of the player.
     */
    public static @Nullable <T extends Entity> T spawnAtPlayer(
            @NotNull ServerPlayer player, @NotNull EntityType<T> type) {
        return spawnAtPlayer(player, type, 3.0);
    }

    /**
     * Spawn an entity at a random position near the player.
     */
    public static @Nullable <T extends Entity> T spawnNearPlayer(
            @NotNull ServerPlayer player, @NotNull EntityType<T> type, double radius) {
        Objects.requireNonNull(player, "player must not be null");
        return spawnRandomNearby(player.serverLevel(), type, player.position(), radius);
    }

    /**
     * Spawn a monster near the player (convenience for hostile mobs).
     */
    public static @Nullable <T extends Monster> T spawnMobAtPlayer(
            @NotNull ServerPlayer player, @NotNull EntityType<T> type) {
        return spawnNearPlayer(player, type, 5.0);
    }

    // -- Math helpers --------------------------------------------------------

    /**
     * Compute a random offset within a uniform disk distribution.
     * Pure math — testable without a Minecraft runtime.
     */
    public static @NotNull Vec3 randomOffset(double radius, double angle, double fraction) {
        var r = radius * Math.sqrt(fraction); // uniform disk
        return new Vec3(r * Math.cos(angle), 0, r * Math.sin(angle));
    }

    /**
     * Compute a position on a circle's perimeter for the given index and total.
     * Useful for spawning entities in a ring formation.
     */
    public static @NotNull Vec3 circlePosition(@NotNull Vec3 center, double radius, int index, int total) {
        Objects.requireNonNull(center, "center must not be null");
        var angle = 2 * Math.PI * index / total;
        return new Vec3(
                center.x + radius * Math.cos(angle),
                center.y,
                center.z + radius * Math.sin(angle)
        );
    }
}
