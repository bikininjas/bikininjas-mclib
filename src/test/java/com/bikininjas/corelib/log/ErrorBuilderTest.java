package com.bikininjas.corelib.log;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorBuilderTest {

    @Test
    void reportWithoutContext() {
        var collector = new CollectLogger();
        var builder = new ErrorBuilder(collector, "[m][C] fail");
        builder.report();
        assertEquals(1, collector.messages.size());
        assertTrue(collector.messages.get(0).contains("fail"));
    }

    @Test
    void reportWithContext() {
        var collector = new CollectLogger();
        var builder = new ErrorBuilder(collector, "[m][C] fail");
        builder.ctx("player", "Alex").ctx("reason", "timeout").report();
        assertEquals(1, collector.messages.size());
        var msg = collector.messages.get(0);
        assertTrue(msg.contains("[player=Alex]"));
        assertTrue(msg.contains("[reason=timeout]"));
    }

    @Test
    void reportWithCause() {
        var collector = new CollectLogger();
        var builder = new ErrorBuilder(collector, "[m][C] fail");
        var cause = new RuntimeException("oops");
        builder.cause(cause).report();
        assertEquals(1, collector.messages.size());
        assertTrue(collector.messages.get(0).endsWith(":oops"));
    }

    @Test
    void chaining() {
        var collector = new CollectLogger();
        var result = new ErrorBuilder(collector, "x").ctx("k", "v").cause(new Exception("e"));
        result.report();
        assertEquals(1, collector.messages.size());
    }

    @Test
    void noopDoesNothing() {
        var builder = ErrorBuilder.NOOP;
        builder.ctx("k", "v").cause(new Exception()).mod("other").report();
    }

    @Test
    void nullChecks() {
        var builder = new ErrorBuilder(new CollectLogger(), "x");
        assertThrows(NullPointerException.class, () -> builder.ctx(null, "v"));
    }

    @Test
    void reportSkipsWhenLoggerDisabled() {
        var collector = new CollectLogger();
        collector.enabled = false;
        var builder = new ErrorBuilder(collector, "[m][C] fail");
        builder.report();
        assertTrue(collector.messages.isEmpty());
    }

    @Test
    void constructorAcceptsNullLogger() {
        var builder = new ErrorBuilder(null, "x");
        assertNotNull(builder);
    }

    // -- In-memory SLF4J logger for testing --------------------------------

    static final class CollectLogger extends MarkerIgnoringBase {
        final List<String> messages = new ArrayList<>();
        boolean enabled = true;

        @Override public boolean isTraceEnabled() { return enabled; }
        @Override public boolean isDebugEnabled() { return enabled; }
        @Override public boolean isInfoEnabled() { return enabled; }
        @Override public boolean isWarnEnabled() { return enabled; }
        @Override public boolean isErrorEnabled() { return enabled; }

        @Override public void trace(String msg) { if (enabled) messages.add(msg); }
        @Override public void debug(String msg) { if (enabled) messages.add(msg); }
        @Override public void info(String msg) { if (enabled) messages.add(msg); }
        @Override public void warn(String msg) { if (enabled) messages.add(msg); }
        @Override public void error(String msg) { if (enabled) messages.add(msg); }
        @Override public void error(String msg, Throwable t) { if (enabled) messages.add(msg + ":" + t.getMessage()); }
        @Override public void trace(String msg, Throwable t) { if (enabled) messages.add(msg); }
        @Override public void debug(String msg, Throwable t) { if (enabled) messages.add(msg); }
        @Override public void info(String msg, Throwable t) { if (enabled) messages.add(msg); }
        @Override public void warn(String msg, Throwable t) { if (enabled) messages.add(msg); }

        @Override public void trace(String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void debug(String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void info(String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void warn(String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void error(String format, Object arg) { if (enabled) messages.add(format); }

        @Override public void trace(String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void debug(String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void info(String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void warn(String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void error(String format, Object a1, Object a2) { if (enabled) messages.add(format); }

        @Override public void trace(String format, Object... a) { if (enabled) messages.add(format); }
        @Override public void debug(String format, Object... a) { if (enabled) messages.add(format); }
        @Override public void info(String format, Object... a) { if (enabled) messages.add(format); }
        @Override public void warn(String format, Object... a) { if (enabled) messages.add(format); }
        @Override public void error(String format, Object... a) { if (enabled) messages.add(format); }

        @Override public void trace(Marker marker, String msg) { if (enabled) messages.add(msg); }
        @Override public void debug(Marker marker, String msg) { if (enabled) messages.add(msg); }
        @Override public void info(Marker marker, String msg) { if (enabled) messages.add(msg); }
        @Override public void warn(Marker marker, String msg) { if (enabled) messages.add(msg); }
        @Override public void error(Marker marker, String msg) { if (enabled) messages.add(msg); }

        @Override public void trace(Marker marker, String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void debug(Marker marker, String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void info(Marker marker, String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void warn(Marker marker, String format, Object arg) { if (enabled) messages.add(format); }
        @Override public void error(Marker marker, String format, Object arg) { if (enabled) messages.add(format); }

        @Override public void trace(Marker marker, String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void debug(Marker marker, String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void info(Marker marker, String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void warn(Marker marker, String format, Object a1, Object a2) { if (enabled) messages.add(format); }
        @Override public void error(Marker marker, String format, Object a1, Object a2) { if (enabled) messages.add(format); }

        @Override public void trace(Marker marker, String format, Object... argArray) { if (enabled) messages.add(format); }
        @Override public void debug(Marker marker, String format, Object... argArray) { if (enabled) messages.add(format); }
        @Override public void info(Marker marker, String format, Object... argArray) { if (enabled) messages.add(format); }
        @Override public void warn(Marker marker, String format, Object... argArray) { if (enabled) messages.add(format); }
        @Override public void error(Marker marker, String format, Object... argArray) { if (enabled) messages.add(format); }

        @Override public void trace(Marker marker, String msg, Throwable t) { if (enabled) messages.add(msg); }
        @Override public void debug(Marker marker, String msg, Throwable t) { if (enabled) messages.add(msg); }
        @Override public void info(Marker marker, String msg, Throwable t) { if (enabled) messages.add(msg); }
        @Override public void warn(Marker marker, String msg, Throwable t) { if (enabled) messages.add(msg); }
        @Override public void error(Marker marker, String msg, Throwable t) { if (enabled) messages.add(msg); }
    }
}
