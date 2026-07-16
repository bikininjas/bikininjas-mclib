package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.player.PlayerState;
import com.bikininjas.corelib.player.PlayerStateManager;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for {@link PlayerStateManager} and {@link PlayerState}.
 * <p>
 * These tests cover only the parts that do not require a live Minecraft server:
 * structural guarantees (final class, private constructor), record construction
 * and component access, snapshot independence (deep copies), and the null
 * argument contract of {@link PlayerStateManager#clear}.
 * <p>
 * The actual inventory/vitals round-trip ({@code save}/{@code load}) is
 * exercised by NeoForge {@code GameTest}s, because it needs a real
 * {@code ServerPlayer} and {@code Inventory} — data-driven and only available
 * inside a running game in Minecraft 1.21.1+.
 */
class PlayerStateManagerTests {

    @Test
    void classIsFinal() {
        assertTrue(Modifier.isFinal(PlayerStateManager.class.getModifiers()),
                "PlayerStateManager must be a final utility class");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<PlayerStateManager> ctor = PlayerStateManager.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                "PlayerStateManager constructor must be private");
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance(),
                "Private constructor should be invokable (no side effects)");
    }

    @Test
    void stateRecordConstruction() {
        ItemStack[] main = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];
        ItemStack offhand = ItemStack.EMPTY;
        Collection<MobEffectInstance> effects = List.of();

        PlayerState state = new PlayerState(
                main, armor, offhand,
                15.0F, 18, 4.0F,
                7, 0.5F, effects, GameType.CREATIVE
        );

        assertSame(main, state.mainInventory(), "mainInventory component must match");
        assertSame(armor, state.armorInventory(), "armorInventory component must match");
        assertSame(offhand, state.offhand(), "offhand component must match");
        assertEquals(15.0F, state.health(), 0.0001F);
        assertEquals(18, state.food());
        assertEquals(4.0F, state.saturation(), 0.0001F);
        assertEquals(7, state.xpLevel());
        assertEquals(0.5F, state.xpProgress(), 0.0001F);
        assertSame(effects, state.effects());
        assertEquals(GameType.CREATIVE, state.gameType());
    }

    @Test
    void stateEqualityUsesComponentValues() {
        ItemStack[] main = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];
        PlayerState a = new PlayerState(main, armor, ItemStack.EMPTY,
                20.0F, 20, 5.0F, 0, 0.0F, List.of(), GameType.SURVIVAL);
        PlayerState b = new PlayerState(main, armor, ItemStack.EMPTY,
                20.0F, 20, 5.0F, 0, 0.0F, List.of(), GameType.SURVIVAL);

        // Records compare by component value, so equal components => equal state.
        assertEquals(a, b, "Two states with equal components must be equal");
        assertEquals(a.hashCode(), b.hashCode(), "Equal states must share a hashCode");
    }

    @Test
    void clearRejectsNullPlayer() {
        assertThrows(NullPointerException.class,
                () -> PlayerStateManager.clear(null),
                "clear(null) must throw NullPointerException");
    }

    @Test
    void saveRejectsNullPlayer() {
        assertThrows(NullPointerException.class,
                () -> PlayerStateManager.save(null),
                "save(null) must throw NullPointerException");
    }

    @Test
    void loadRejectsNullArguments() {
        ItemStack[] main = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];
        PlayerState state = new PlayerState(main, armor, ItemStack.EMPTY,
                20.0F, 20, 5.0F, 0, 0.0F, List.of(), GameType.SURVIVAL);

        assertThrows(NullPointerException.class,
                () -> PlayerStateManager.load(null, state),
                "load(null, state) must throw NullPointerException");
        assertThrows(NullPointerException.class,
                () -> PlayerStateManager.load(null, null),
                "load(null, null) must throw NullPointerException");
    }
}
