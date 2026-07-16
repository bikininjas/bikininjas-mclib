package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.kit.Kit;
import com.bikininjas.corelib.kit.KitManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for {@link KitManager}.
 * <p>
 * These exercise the registry contract (register / get / getAll / remove /
 * clear) and parameter validation without requiring a live Minecraft runtime.
 * Kit instances are built with {@code null} item/armor/offhand/effects
 * components — the record permits {@code null} components and the registry
 * never dereferences them, so no {@code ItemStack} is instantiated and the
 * tests run in a plain JVM.
 * <p>
 * The full {@code give()} path (clear inventory, set armor slots, apply
 * effects) needs a real {@code ServerPlayer} and is covered by a disabled test.
 */
class KitManagerTests {

    @AfterEach
    void tearDown() {
        KitManager.clear();
    }

    /** Build a minimal kit without instantiating any Minecraft runtime type. */
    private static Kit kit(String name) {
        return new Kit(name, null, null, null, null);
    }

    // ──────────────────────────────────────────────
    //  Structure
    // ──────────────────────────────────────────────

    @Test
    void classIsFinal() {
        assertTrue(Modifier.isFinal(KitManager.class.getModifiers()),
                "KitManager must be final");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<KitManager> ctor = KitManager.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                "KitManager constructor must be private");
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance(), "Private constructor should be invokable for tests");
    }

    // ──────────────────────────────────────────────
    //  Registry contract
    // ──────────────────────────────────────────────

    @Test
    void registerAndGet() {
        Kit k = kit("starter");
        KitManager.register("starter", k);
        Kit fetched = KitManager.get("starter");

        assertNotNull(fetched);
        assertSame(k, fetched);
        assertEquals("starter", fetched.name());
    }

    @Test
    void registerAndGetAll() {
        KitManager.register("a", kit("a"));
        KitManager.register("b", kit("b"));
        KitManager.register("c", kit("c"));

        Collection<String> all = KitManager.getAll();
        assertEquals(3, all.size());
        assertTrue(all.containsAll(List.of("a", "b", "c")));
    }

    @Test
    void remove() {
        KitManager.register("temp", kit("temp"));
        KitManager.remove("temp");
        assertNull(KitManager.get("temp"));
    }

    @Test
    void removeUnknownIsNoOp() {
        KitManager.register("kept", kit("kept"));
        KitManager.remove("does_not_exist");
        assertNotNull(KitManager.get("kept"));
        assertEquals(1, KitManager.getAll().size());
    }

    @Test
    void clearEmptiesRegistry() {
        KitManager.register("x", kit("x"));
        KitManager.register("y", kit("y"));
        KitManager.clear();
        assertTrue(KitManager.getAll().isEmpty());
    }

    // ──────────────────────────────────────────────
    //  give() contract (lookup branch only)
    // ──────────────────────────────────────────────

    @Test
    void giveReturnsFalseForUnknown() {
        // No kit registered under "missing" → lookup branch returns false.
        // player is null here on purpose: the null check happens AFTER the
        // lookup, so an unknown name short-circuits before any dereference.
        assertFalse(KitManager.give(null, "missing"));
    }

    // ──────────────────────────────────────────────
    //  Validation
    // ──────────────────────────────────────────────

    @Test
    void registerNullNameThrows() {
        assertThrows(NullPointerException.class, () -> KitManager.register(null, kit("k")));
    }

    @Test
    void registerNullKitThrows() {
        assertThrows(NullPointerException.class, () -> KitManager.register("k", null));
    }

    @Test
    void registerBlankNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> KitManager.register("   ", kit("k")));
    }

    @Test
    void getNullNameThrows() {
        assertThrows(NullPointerException.class, () -> KitManager.get(null));
    }

    @Test
    void removeNullNameThrows() {
        assertThrows(NullPointerException.class, () -> KitManager.remove(null));
    }

    // ──────────────────────────────────────────────
    //  Runtime-dependent (disabled)
    // ──────────────────────────────────────────────

    @Test
    @org.junit.jupiter.api.Disabled("requires Minecraft runtime (ServerPlayer + ItemStack)")
    void giveWithoutRuntimeIsUntestable() {
        // The full give() path (clear inventory, set armor slots, apply effects)
        // requires a live ServerPlayer and cannot run in a pure unit environment.
        // Covered structurally by giveReturnsFalseForUnknown() and the registry
        // tests above; enable this with a Minecraft test harness / GameTest.
        KitManager.register("runtime", new Kit("runtime",
                new net.minecraft.world.item.ItemStack[0],
                new net.minecraft.world.item.ItemStack[4],
                net.minecraft.world.item.ItemStack.EMPTY,
                List.of()));
    }
}
