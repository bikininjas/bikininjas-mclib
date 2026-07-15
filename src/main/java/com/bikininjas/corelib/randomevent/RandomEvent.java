package com.bikininjas.corelib.randomevent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * A {@code RandomEvent} is a single, self-contained action that the
 * {@link RandomEventManager} can schedule and fire at random intervals.
 * <p>
 * Implementations are functional: they only need to provide the
 * {@link #execute(ServerLevel, Vec3)} behaviour. The event's
 * {@link #name()} and {@link #weight()} are derived automatically but can
 * be overridden by the implementation.
 * <p>
 * These are <em>not</em> Minecraft {@code net.minecraftforge.event} events —
 * they are this system's own custom, user-defined actions.
 */
@FunctionalInterface
public interface RandomEvent {

    /**
     * Perform the event's action.
     *
     * @param level  the server level the event runs in (never {@code null} during normal use)
     * @param origin the world-space origin the event should act around
     */
    void execute(ServerLevel level, Vec3 origin);

    /**
     * The registry key / display name of this event.
     * <p>
     * Defaults to the simple class name of the implementation. Lambda or
     * anonymous implementations should be registered with an explicit name via
     * {@link RandomEventManager#register(RandomEvent, String)} to avoid unstable
     * generated class names.
     *
     * @return the event name, never {@code null}
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * The relative selection weight used for weighted-random firing.
     * <p>
     * Higher weights are chosen more often. Defaults to {@code 100}.
     *
     * @return the selection weight (values {@code < 0} are treated as {@code 0})
     */
    default int weight() {
        return 100;
    }
}
