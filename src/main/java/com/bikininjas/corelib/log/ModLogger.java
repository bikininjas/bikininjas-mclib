package com.bikininjas.corelib.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import java.util.Objects;

/**
 * Structured logger that auto-prefixes messages with {@code [modId][ClassName]}.
 * <p>
 * Provides chainable {@link #error(String)} for structured error reporting via {@link ErrorBuilder}.
 */
public final class ModLogger {

    private final Logger delegate;
    private final String prefix;

    ModLogger(@NotNull Logger delegate, @NotNull String modId, @NotNull Class<?> clazz) {
        this.delegate = Objects.requireNonNull(delegate, "delegate logger must not be null");
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        this.prefix = "[" + modId + "][" + clazz.getSimpleName() + "] ";
    }

    // -- Convenience methods -------------------------------------------------

    public void info(@NotNull String msg, @Nullable Object... args) {
        if (delegate.isInfoEnabled()) {
            delegate.info(prefix + msg, args);
        }
    }

    public void debug(@NotNull String msg, @Nullable Object... args) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(prefix + msg, args);
        }
    }

    public void warn(@NotNull String msg, @Nullable Object... args) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(prefix + msg, args);
        }
    }

    public void error(@NotNull String msg, @Nullable Object... args) {
        if (delegate.isErrorEnabled()) {
            delegate.error(prefix + msg, args);
        }
    }

    // -- Error builder (fluent, structured) -----------------------------------

    /**
     * Start a structured error report with chainable context.
     * <p>
     * If error logging is disabled for this logger, returns a NOOP builder
     * whose {@code report()} is a no-op — avoids unnecessary string formatting.
     *
     * @return an {@link ErrorBuilder} (never null)
     */
    public @NotNull ErrorBuilder error(@NotNull String msg) {
        Objects.requireNonNull(msg, "message must not be null");
        if (!delegate.isErrorEnabled()) {
            return ErrorBuilder.NOOP;
        }
        return new ErrorBuilder(delegate, prefix + msg);
    }
}
