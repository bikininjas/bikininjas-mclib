package com.bikininjas.corelib.gametest;

import com.bikininjas.corelib.cooldown.CooldownManager;
import com.bikininjas.corelib.loot.LootTableHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import org.jetbrains.annotations.NotNull;

/**
 * GameTest functions for new core-lib features: CooldownManager, LootTableHelper.
 */
@ForEachTest(groups = "core_lib")
public final class CoreLibNewFeaturesTest {

    private CoreLibNewFeaturesTest() {
    }

    private static @NotNull ServerPlayer makePlayer(@NotNull ExtendedGameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(
                helper.absolutePos(player.blockPosition()).getX() + 1.5,
                helper.absolutePos(player.blockPosition()).getY() + 2,
                helper.absolutePos(player.blockPosition()).getZ() + 1.5
        );
        return (ServerPlayer) player;
    }

    // ========================================================================
    // CooldownManager
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void cooldown_setAndCheck(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var level = helper.getLevel();

        CooldownManager.setCooldown(level, player.getUUID(), "test_action", 200);
        helper.assertTrue(CooldownManager.isOnCooldown(player.getUUID(), "test_action"),
                "Player should be on cooldown immediately after setCooldown");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void cooldown_multipleActions(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var level = helper.getLevel();

        CooldownManager.setCooldown(level, player.getUUID(), "action_a", 200);
        CooldownManager.setCooldown(level, player.getUUID(), "action_b", 50);
        helper.assertTrue(CooldownManager.isOnCooldown(player.getUUID(), "action_a"),
                "Player should be on cooldown for action_a");
        helper.assertTrue(CooldownManager.isOnCooldown(player.getUUID(), "action_b"),
                "Player should be on cooldown for action_b");
        helper.assertTrue(!CooldownManager.isOnCooldown(player.getUUID(), "action_c"),
                "Player should NOT be on cooldown for unset action_c");
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void cooldown_getRemainingTicks(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var level = helper.getLevel();

        CooldownManager.setCooldown(level, player.getUUID(), "timed_action", 200);
        long remaining = CooldownManager.getRemainingTicks(level, player.getUUID(), "timed_action");
        helper.assertTrue(remaining > 0 && remaining <= 200,
                "Remaining ticks should be between 1 and 200, got " + remaining);
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void cooldown_expiredIsNotOnCooldown(@NotNull ExtendedGameTestHelper helper) {
        var player = makePlayer(helper);
        var level = helper.getLevel();

        // Set a cooldown that already expired (0 ticks)
        CooldownManager.setCooldown(level, player.getUUID(), "expired_action", 0);
        helper.assertTrue(!CooldownManager.isOnCooldown(player.getUUID(), "expired_action"),
                "Expired cooldown should not report as on cooldown");
        helper.succeed();
    }

    // ========================================================================
    // LootTableHelper
    // ========================================================================

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void lootTable_injectAddsPool(@NotNull ExtendedGameTestHelper helper) {
        var builder = LootTable.lootTable();
        var lootId = ResourceLocation.fromNamespaceAndPath("minecraft", "blocks/stone");

        LootTableHelper.injectInto(lootId, builder.build());
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void lootTable_injectDoesNotCrashOnUnknownItem(@NotNull ExtendedGameTestHelper helper) {
        var builder = LootTable.lootTable();
        var lootId = ResourceLocation.fromNamespaceAndPath("minecraft", "blocks/stone");

        LootTableHelper.injectInto(lootId, builder.build());
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void lootTable_injectMultipleTimesNoCrash(@NotNull ExtendedGameTestHelper helper) {
        var lootId = ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon");

        LootTableHelper.addToLootTable(lootId,
                ResourceLocation.fromNamespaceAndPath("minecraft", "diamond"), 10f, 1, 3);
        LootTableHelper.addToLootTable(lootId,
                ResourceLocation.fromNamespaceAndPath("minecraft", "emerald"), 5f, 1, 2);

        var builder = LootTable.lootTable();
        LootTableHelper.injectInto(lootId, builder.build());
        helper.succeed();
    }
}
