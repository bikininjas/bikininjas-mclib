package com.bikininjas.corelib.randomevent;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Built-in {@link RandomEvent} factory methods.
 * <p>
 * Each static method returns a ready-to-register {@code RandomEvent} lambda.
 * Register them via {@link RandomEventManager#register(RandomEvent, String)}:
 * <pre>{@code
 * RandomEventManager.getInstance()
 *     .register(RandomEvents.announceEvent("Boo!"), "ghost_announce")
 *     .register(RandomEvents.randomExplosionEvent(3.0f, false), "mini_boom");
 * }</pre>
 * <p>
 * This class is a static utility and cannot be instantiated.
 */
public final class RandomEvents {

    private RandomEvents() {}

    /**
     * Broadcast a plain-text system message to every player.
     *
     * @param message the message text
     * @return a {@link RandomEvent} that announces the message
     */
    public static RandomEvent announceEvent(String message) {
        return (level, origin) -> {
            if (level.getServer() == null) {
                return;
            }
            level.getServer().getPlayerList()
                    .broadcastSystemMessage(Component.literal(message), false);
        };
    }

    /**
     * Broadcast a pre-built chat component to every player.
     *
     * @param message the component to broadcast
     * @return a {@link RandomEvent} that announces the component
     */
    public static RandomEvent announceEvent(Component message) {
        return (level, origin) -> {
            if (level.getServer() == null) {
                return;
            }
            level.getServer().getPlayerList()
                    .broadcastSystemMessage(message, false);
        };
    }

    /**
     * Spawn {@code count} entities of the given type at random positions near
     * the event origin.
     *
     * @param type  the entity type to spawn
     * @param count how many entities to spawn
     * @return a {@link RandomEvent} that spawns the entities
     */
    public static RandomEvent spawnEntityEvent(EntityType<?> type, int count) {
        return (level, origin) -> {
            for (int i = 0; i < count; i++) {
                var entity = type.create(level);
                if (entity == null) {
                    continue;
                }
                double dx = origin.x + (level.random.nextDouble() - 0.5) * 16.0;
                double dy = origin.y + 2.0;
                double dz = origin.z + (level.random.nextDouble() - 0.5) * 16.0;
                entity.moveTo(dx, dy, dz, level.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(entity);
            }
        };
    }

    /**
     * Create an explosion at a random position near the event origin.
     *
     * @param power the explosion power
     * @param fire  whether the explosion sets fires
     * @return a {@link RandomEvent} that triggers the explosion
     */
    public static RandomEvent randomExplosionEvent(float power, boolean fire) {
        return (level, origin) -> {
            double x = origin.x + (level.random.nextDouble() - 0.5) * 16.0;
            double y = origin.y + 1.0;
            double z = origin.z + (level.random.nextDouble() - 0.5) * 16.0;
            level.explode(null, x, y, z, power, fire, Level.ExplosionInteraction.MOB);
        };
    }

    /**
     * Clear the weather (sunny, no rain, no thunder).
     *
     * @return a {@link RandomEvent} that clears the weather
     */
    public static RandomEvent clearWeatherEvent() {
        return (level, origin) -> level.setWeatherParameters(6000, 0, false, false);
    }

    /**
     * Set random weather — either rain or a thunderstorm.
     *
     * @return a {@link RandomEvent} that randomises the weather
     */
    public static RandomEvent randomWeatherEvent() {
        return (level, origin) -> {
            boolean thunder = level.random.nextDouble() < 0.3;
            level.setWeatherParameters(0, 12000, true, thunder);
        };
    }
}
