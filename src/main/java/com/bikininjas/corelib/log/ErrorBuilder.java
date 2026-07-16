package com.bikininjas.corelib.log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent error builder for structured, context-rich log messages.
 * <p>
 * Usage:
 * <pre>{@code
 * LOGGER.error("Failed to register recipe")
 *     .ctx("recipe", recipeId)
 *     .ctx("input", inputItem)
 *     .cause(exception)
 *     .report();
 * }</pre>
 */
public class ErrorBuilder {

    private final Logger logger;
    private final String message;
    private final Map<String, Object> context = new LinkedHashMap<>();
    @Nullable
    private Throwable cause;
    @Nullable
    private String modIdOverride;

    // Sentinel NOOP instance
    static final ErrorBuilder NOOP = new ErrorBuilder(null, "") {
        @Override
        public @NotNull ErrorBuilder ctx(@NotNull String key, @Nullable Object value) {
            return this;
        }

        @Override
        public @NotNull ErrorBuilder cause(@Nullable Throwable cause) {
            return this;
        }

        @Override
        public @NotNull ErrorBuilder mod(@Nullable String modId) {
            return this;
        }

        @Override
        public void report() {
            // no-op
        }
    };

    ErrorBuilder(@Nullable Logger logger, @NotNull String message) {
        this.logger = logger;
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    /**
     * Add a context key-value pair to the error report.
     */
    public @NotNull ErrorBuilder ctx(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key, "context key must not be null");
        context.put(key, value);
        return this;
    }

    /**
     * Set the causing exception.
     */
    public @NotNull ErrorBuilder cause(@Nullable Throwable cause) {
        this.cause = cause;
        return this;
    }

    /**
     * Override the mod ID prefix for this error report.
     */
    public @NotNull ErrorBuilder mod(@Nullable String modId) {
        this.modIdOverride = modId;
        return this;
    }

    /**
     * Emit the formatted error log line with all attached context.
     */
    public void report() {
        if (logger == null || !logger.isErrorEnabled()) {
            return;
        }

        var sb = new StringBuilder(message);
        if (!context.isEmpty()) {
            sb.append(" —");
            for (var entry : context.entrySet()) {
                sb.append(" [").append(entry.getKey()).append('=').append(entry.getValue()).append(']');
            }
        }

        if (cause != null) {
            logger.error(MessageFormatter.basicArrayFormat(sb.toString(), new Object[0]), cause);
        } else {
            logger.error(sb.toString());
        }
    }
}
