package com.bikininjas.corelib.log;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Factory for creating {@link ModLogger} instances.
 * <p>
 * Usage:
 * <pre>{@code
 * private static final ModLogger LOGGER = LogManager.getLogger("my_mod", MyClass.class);
 * }</pre>
 */
public final class LogManager {

    private LogManager() {
    }

    /**
     * Create a ModLogger for the given mod ID and class.
     *
     * @param modId the mod identifier (e.g. {@code "core_lib"})
     * @param clazz the class requesting the logger (its simple name is prefixed)
     * @return a structured ModLogger (never null)
     */
    public static @NotNull ModLogger getLogger(@NotNull String modId, @NotNull Class<?> clazz) {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        var delegate = LoggerFactory.getLogger(clazz);
        return new ModLogger(delegate, modId, clazz);
    }
}
