package com.bikininjas.corelib.log;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModLoggerTest {

    @Test
    void prefixInjected() {
        var collector = new CollectLogger();
        var log = new ModLogger(collector, "test_mod", ModLoggerTest.class);
        log.info("hello");
        assertTrue(collector.messages.get(0).contains("[test_mod][ModLoggerTest] hello"));
    }

    @Test
    void infoMessage() {
        var collector = new CollectLogger();
        var log = new ModLogger(collector, "m", ModLoggerTest.class);
        log.info("msg");
        assertEquals(1, collector.messages.size());
        assertTrue(collector.messages.get(0).contains("msg"));
    }

    @Test
    void debugMessage() {
        var collector = new CollectLogger();
        var log = new ModLogger(collector, "m", ModLoggerTest.class);
        log.debug("debug val");
        assertTrue(collector.messages.get(0).contains("debug val"));
    }

    @Test
    void warnMessage() {
        var collector = new CollectLogger();
        var log = new ModLogger(collector, "m", ModLoggerTest.class);
        log.warn("warn msg");
        assertTrue(collector.messages.get(0).contains("warn msg"));
    }

    @Test
    void errorMessage() {
        var collector = new CollectLogger();
        var log = new ModLogger(collector, "m", ModLoggerTest.class);
        log.error("err msg").report();
        assertTrue(collector.messages.get(0).contains("err msg"));
    }

    @Test
    void skipsWhenDisabled() {
        var collector = new CollectLogger();
        collector.enabled = false;
        var log = new ModLogger(collector, "m", ModLoggerTest.class);
        log.info("should not appear");
        assertTrue(collector.messages.isEmpty());
    }

    @Test
    void errorBuilderReturnsNOOPWhenDisabled() {
        var collector = new CollectLogger();
        collector.enabled = false;
        var log = new ModLogger(collector, "m", ModLoggerTest.class);
        var builder = log.error("fail");
        assertSame(ErrorBuilder.NOOP, builder);
    }

    @Test
    void errorBuilderReturnsRealWhenEnabled() {
        var collector = new CollectLogger();
        var log = new ModLogger(collector, "m", ModLoggerTest.class);
        var builder = log.error("fail");
        assertNotSame(ErrorBuilder.NOOP, builder);
    }

    @Test
    void nullChecks() {
        var collector = new CollectLogger();
        assertThrows(NullPointerException.class, () -> new ModLogger(null, "m", ModLoggerTest.class));
        assertThrows(NullPointerException.class, () -> new ModLogger(collector, null, ModLoggerTest.class));
        assertThrows(NullPointerException.class, () -> new ModLogger(collector, "m", null));
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
