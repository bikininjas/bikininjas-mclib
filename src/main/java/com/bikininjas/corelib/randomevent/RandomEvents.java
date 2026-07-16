package com.bikininjas.corelib.randomevent;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Factory methods for common {@link RandomEvent} implementations.
 */
public final class RandomEvents {

    private RandomEvents() {
    }

    /**
     * Create an event that broadcasts an announcement to all players.
     */
    public static @NotNull RandomEvent announceEvent(@NotNull String message) {
        Objects.requireNonNull(message, "message must not be null");
        return new AnnounceEvent(message);
    }

    /**
     * Create an event that spawns a number of entities at the origin.
     */
    public static @NotNull RandomEvent spawnEntityEvent(@NotNull EntityType<?> type, int count) {
        Objects.requireNonNull(type, "type must not be null");
        return new SpawnEntityEvent(type, count);
    }

    /**
     * Create an event that triggers an explosion at the origin.
     */
    public static @NotNull RandomEvent randomExplosionEvent(float power, boolean fire) {
        return new ExplosionEvent(power, fire);
    }

    /**
     * Create an event that clears the weather.
     */
    public static @NotNull RandomEvent clearWeatherEvent() {
        return new ClearWeatherEvent();
    }

    /**
     * Create an event that sets random weather (rain or thunder).
     */
    public static @NotNull RandomEvent randomWeatherEvent() {
        return new RandomWeatherEvent();
    }

    // -- Implementations -----------------------------------------------------

    private record AnnounceEvent(String message) implements RandomEvent {
        @Override public @NotNull String name() { return "announce"; }
        @Override
        public void execute(@NotNull ServerLevel level, @NotNull Vec3 origin) {
            var server = level.getServer();
            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(message), false);
            }
        }
    }

    private record SpawnEntityEvent(EntityType<?> type, int count) implements RandomEvent {
        @Override public @NotNull String name() { return "spawn_entity"; }
        @Override
        public void execute(@NotNull ServerLevel level, @NotNull Vec3 origin) {
            for (int i = 0; i < count; i++) {
                var entity = type.create(level);
                if (entity != null) {
                    var angle = 2 * Math.PI * i / count;
                    var pos = origin.add(Math.cos(angle) * 3, 0.5, Math.sin(angle) * 3);
                    entity.setPos(pos);
                    level.addFreshEntity(entity);
                }
            }
        }
    }

    private record ExplosionEvent(float power, boolean fire) implements RandomEvent {
        @Override public @NotNull String name() { return "explosion"; }
        @Override
        public void execute(@NotNull ServerLevel level, @NotNull Vec3 origin) {
            level.explode(null, origin.x, origin.y, origin.z, power, fire,
                    Level.ExplosionInteraction.TNT);
        }

        @Override
        public int weight() {
            return 1; // very low weight by default — dangerous!
        }
    }

    private record ClearWeatherEvent() implements RandomEvent {
        @Override public @NotNull String name() { return "clear_weather"; }
        @Override
        public void execute(@NotNull ServerLevel level, @NotNull Vec3 origin) {
            level.setWeatherParameters(6000, 0, false, false);
        }

        @Override
        public int weight() {
            return 3;
        }
    }

    private record RandomWeatherEvent() implements RandomEvent {
        @Override public @NotNull String name() { return "random_weather"; }
        @Override
        public void execute(@NotNull ServerLevel level, @NotNull Vec3 origin) {
            boolean rain = level.random.nextBoolean();
            boolean thunder = rain && level.random.nextBoolean();
            int duration = 600 + level.random.nextInt(5400);
            level.setWeatherParameters(0, duration, rain, thunder);
        }

        @Override
        public int weight() {
            return 2;
        }
    }
}
