package com.bikininjas.corelib.particle;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for spawning particles on the server side.
 * <p>
 * All methods are static. No event bus — pure helper.
 */
public final class ParticleHelper {

    private ParticleHelper() {
    }

    /**
     * Force class loading (called from CoreLib.initModules()).
     */
    public static void init() {
    }

    // -- Public API ----------------------------------------------------------

    /**
     * Spawn colored dust particles in a small sphere around a position.
     *
     * @param level    the server level
     * @param pos      center position
     * @param rgbColor 24-bit RGB color (e.g. 0xFF0000 for red)
     * @param count    number of particles to spawn
     */
    public static void spawnColoredDust(@NotNull ServerLevel level,
                                         @NotNull Vec3 pos,
                                         int rgbColor,
                                         int count) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");

        Vector3f color = rgbToVector(rgbColor);
        DustParticleOptions options = new DustParticleOptions(color, 1.0F);

        for (int i = 0; i < count; i++) {
            double ox = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
            double oy = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
            double oz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
            level.sendParticles(options,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn colored dust particles around a player (server-side only).
     *
     * @param player       the player
     * @param rgbColor     24-bit RGB color
     * @param count        number of particles
     * @param heightOffset vertical offset from player feet
     */
    public static void spawnColoredDustOnPlayer(@NotNull Player player,
                                                  int rgbColor,
                                                  int count,
                                                  double heightOffset) {
        Objects.requireNonNull(player, "player must not be null");

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 pos = player.position().add(0, heightOffset, 0);
        spawnColoredDust(serverLevel, pos, rgbColor, count);
    }

    /**
     * Spawn particles in a horizontal circle.
     *
     * @param level          the server level
     * @param center         circle center
     * @param radius         circle radius
     * @param particleCount  number of particles to distribute around the circle
     * @param options        the particle type and data
     */
    public static void spawnCircle(@NotNull ServerLevel level,
                                    @NotNull Vec3 center,
                                    double radius,
                                    int particleCount,
                                    @NotNull ParticleOptions options) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(center, "center must not be null");
        Objects.requireNonNull(options, "options must not be null");

        double angleStep = (2.0 * Math.PI) / Math.max(1, particleCount);
        for (int i = 0; i < particleCount; i++) {
            double angle = angleStep * i;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(options, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn particles evenly distributed along a line between two points.
     *
     * @param level    the server level
     * @param start    starting point
     * @param end      ending point
     * @param steps    number of particle positions along the line
     * @param options  the particle type and data
     */
    public static void spawnLine(@NotNull ServerLevel level,
                                  @NotNull Vec3 start,
                                  @NotNull Vec3 end,
                                  int steps,
                                  @NotNull ParticleOptions options) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        Objects.requireNonNull(options, "options must not be null");

        for (int i = 0; i <= steps; i++) {
            double t = (steps == 0) ? 0.0 : (double) i / steps;
            double x = start.x + (end.x - start.x) * t;
            double y = start.y + (end.y - start.y) * t;
            double z = start.z + (end.z - start.z) * t;
            level.sendParticles(options, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawn particles bursting outward in random directions from a point.
     *
     * @param level    the server level
     * @param pos      burst origin
     * @param count    number of particles
     * @param options  the particle type and data
     * @param speed    maximum speed multiplier for random direction
     */
    public static void spawnBurst(@NotNull ServerLevel level,
                                   @NotNull Vec3 pos,
                                   int count,
                                   @NotNull ParticleOptions options,
                                   double speed) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        Objects.requireNonNull(options, "options must not be null");

        for (int i = 0; i < count; i++) {
            double vx = (ThreadLocalRandom.current().nextDouble() - 0.5) * speed;
            double vy = (ThreadLocalRandom.current().nextDouble() - 0.5) * speed;
            double vz = (ThreadLocalRandom.current().nextDouble() - 0.5) * speed;
            level.sendParticles(options, pos.x, pos.y, pos.z, 0, vx, vy, vz, 1.0);
        }
    }

    // -- Internal helpers ----------------------------------------------------

    /**
     * Convert a 24-bit RGB int to a JOML Vector3f (values 0.0–1.0).
     */
    private static @NotNull Vector3f rgbToVector(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        return new Vector3f(r, g, b);
    }
}
