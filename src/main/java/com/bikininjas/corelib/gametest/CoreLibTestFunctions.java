package com.bikininjas.corelib.gametest;

import com.bikininjas.corelib.kit.Kit;
import com.bikininjas.corelib.kit.KitManager;
import com.bikininjas.corelib.randomevent.RandomEvent;
import com.bikininjas.corelib.randomevent.RandomEventManager;
import com.bikininjas.corelib.stats.StatsDisplayPrefs;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * GameTest functions for core-lib features.
 * <p>
 * Registered automatically via {@link GameTestHolder @GameTestHolder}.
 * Each test runs inside a 3×3×3 structure with an iron block floor.
 */
@GameTestHolder("core_lib")
public final class CoreLibTestFunctions {

    private CoreLibTestFunctions() {
    }

    private static void cleanup() {
        KitManager.clear();
        RandomEventManager.getInstance().reset();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void statsOverlay_toggle(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        boolean start = StatsDisplayPrefs.isEnabled(player);
        StatsDisplayPrefs.toggle(player);
        helper.assertTrue(start != StatsDisplayPrefs.isEnabled(player),
                "Toggle should flip enabled state");
        StatsDisplayPrefs.toggle(player);
        helper.assertTrue(StatsDisplayPrefs.isEnabled(player) == start,
                "Double toggle should restore state");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void statsOverlay_visibleFields(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        var fields = Set.of(StatsDisplayPrefs.FIELD_DEATHS, StatsDisplayPrefs.FIELD_KILLS);
        StatsDisplayPrefs.setVisibleFields(player, fields);
        var result = StatsDisplayPrefs.getVisibleFields(player);
        helper.assertTrue(result.contains(StatsDisplayPrefs.FIELD_DEATHS), "Missing deaths");
        helper.assertTrue(result.contains(StatsDisplayPrefs.FIELD_KILLS), "Missing kills");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void statsOverlay_defaultEnabled(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        helper.assertTrue(StatsDisplayPrefs.isEnabled(player),
                "Overlay should be enabled by default");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void kit_giveSimple(@NotNull GameTestHelper helper) {
        cleanup();
        KitManager.register(Kit.of("test_kit",
                new ItemStack(Items.BREAD, 16),
                new ItemStack(Items.COOKED_BEEF, 8)));
        var player = makePlayer(helper);
        KitManager.give(player, "test_kit");
        helper.assertTrue(
                player.getInventory().hasAnyOf(Set.of(Items.BREAD)),
                "Player should have bread from kit");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void kit_duplicateName(@NotNull GameTestHelper helper) {
        cleanup();
        KitManager.register(Kit.of("dup_kit", new ItemStack(Items.STONE)));
        KitManager.register(Kit.of("dup_kit", new ItemStack(Items.DIRT)));
        var player = makePlayer(helper);
        KitManager.give(player, "dup_kit");
        helper.assertTrue(
                player.getInventory().hasAnyOf(Set.of(Items.DIRT)),
                "Last registered duplicate should win");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void randomEvent_selectsRegistered(@NotNull GameTestHelper helper) {
        cleanup();
        var mgr = RandomEventManager.getInstance();
        mgr.reset();
        mgr.register(new RandomEvent() {
            @Override public @NotNull String name() { return "test_event"; }
            @Override
            public void execute(@NotNull ServerLevel level, @NotNull Vec3 origin) {
            }

            @Override
            public int weight() {
                return 1;
            }
        });

        var selected = mgr.selectRandomEvent();
        helper.assertTrue(selected != null, "Should select a registered event");
        if (selected != null) {
            helper.assertTrue("test_event".equals(selected.name()), "Name mismatch");
        }
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void randomEvent_emptySelectsNull(@NotNull GameTestHelper helper) {
        cleanup();
        var mgr = RandomEventManager.getInstance();
        mgr.reset();
        helper.assertTrue(mgr.selectRandomEvent() == null,
                "Empty manager should return null");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void randomEvent_zeroWeightNotSelected(@NotNull GameTestHelper helper) {
        cleanup();
        var mgr = RandomEventManager.getInstance();
        mgr.reset();
        mgr.register(new RandomEvent() {
            @Override public @NotNull String name() { return "zero"; }
            @Override
            public void execute(@NotNull ServerLevel level, @NotNull Vec3 origin) {
            }

            @Override
            public int weight() {
                return 0;
            }
        });

        helper.assertTrue(mgr.selectRandomEvent() == null,
                "Zero-weight events should never be selected");
        helper.succeed();
    }

    private static @NotNull ServerPlayer makePlayer(@NotNull GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(
                helper.absolutePos(player.blockPosition()).getX() + 1.5,
                helper.absolutePos(player.blockPosition()).getY() + 2,
                helper.absolutePos(player.blockPosition()).getZ() + 1.5
        );
        return (ServerPlayer) player;
    }
}
