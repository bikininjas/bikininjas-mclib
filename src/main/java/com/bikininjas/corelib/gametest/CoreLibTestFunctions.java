package com.bikininjas.corelib.gametest;

import com.bikininjas.corelib.kit.Kit;
import com.bikininjas.corelib.kit.KitManager;
import com.bikininjas.corelib.message.MessageHelper;
import com.bikininjas.corelib.objective.Challenge;
import com.bikininjas.corelib.objective.ChallengeDefinition;
import com.bikininjas.corelib.objective.ChallengeRegistry;
import com.bikininjas.corelib.objective.Objective;
import com.bikininjas.corelib.objective.ObjectiveTracker;
import com.bikininjas.corelib.player.PlayerState;
import com.bikininjas.corelib.player.PlayerStateManager;
import com.bikininjas.corelib.randomevent.RandomEvent;
import com.bikininjas.corelib.randomevent.RandomEventManager;
import com.bikininjas.corelib.recipe.RecipeAPI;
import com.bikininjas.corelib.recipe.RecipeBuilder;
import com.bikininjas.corelib.restriction.RestrictionManager;
import com.bikininjas.corelib.restriction.RestrictionType;
import com.bikininjas.corelib.stats.StatsDisplayPrefs;
import com.bikininjas.corelib.time.TimeManager;
import com.bikininjas.corelib.entity.SpawnHelper;
import com.bikininjas.corelib.world.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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


    @GameTest(template = "core_lib:empty3x3x3")
    public static void timeManager_setDay(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        TimeManager.setDay(level);
        helper.assertTrue(TimeManager.getDayTime(level) == 1000,
                "setDay should set time to 1000");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void timeManager_setNight(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        TimeManager.setNight(level);
        helper.assertTrue(TimeManager.getDayTime(level) == 13000,
                "setNight should set time to 13000");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void timeManager_toggleTimeFreeze(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        boolean before = TimeManager.isTimeFrozen(level);
        TimeManager.toggleTimeFreeze(level);
        boolean after = TimeManager.isTimeFrozen(level);
        helper.assertTrue(before != after,
                "toggleTimeFreeze should flip the frozen state");
        TimeManager.toggleTimeFreeze(level);
        helper.assertTrue(TimeManager.isTimeFrozen(level) == before,
                "Double toggle should restore frozen state");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void timeManager_setTimeRate(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        TimeManager.setTimeRate(level, 2.0f);
        TimeManager.addTime(level, 50);
        helper.assertTrue(TimeManager.getDayTime(level) == 50,
                "addTime should advance day time by 50 ticks");
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void spawnHelper_spawnAt(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        var pos = new Vec3(0, 0, 0);
        var entity = SpawnHelper.spawnAt(level, EntityType.ZOMBIE, pos);
        helper.assertTrue(entity != null, "spawnAt should create an entity");
        if (entity != null) {
            helper.assertTrue(entity.position().distanceToSqr(pos) < 0.01,
                    "spawned entity should be at the requested position");
        }
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void spawnHelper_spawnAtPlayer(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        var entity = SpawnHelper.spawnAtPlayer(player, EntityType.SKELETON);
        helper.assertTrue(entity != null, "spawnAtPlayer should create an entity");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void spawnHelper_spawnWithConfig(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        var pos = new Vec3(0, 0, 0);
        var entity = SpawnHelper.spawnWithConfig(level, EntityType.ZOMBIE, pos,
                e -> e.setCustomName(net.minecraft.network.chat.Component.literal("TestZombie")));
        helper.assertTrue(entity != null, "spawnWithConfig should create an entity");
        if (entity != null) {
            helper.assertTrue(entity.getCustomName() != null
                            && "TestZombie".equals(entity.getCustomName().getString()),
                    "configurator should set the custom name");
        }
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void messageHelper_chat(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        MessageHelper.chat(player, "Hello test");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void messageHelper_title(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        MessageHelper.title(player, "Title", "Subtitle");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void messageHelper_actionBar(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        MessageHelper.actionBar(player, "Action bar message");
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void worldUtils_setBlock(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        WorldUtils.setBlock(level, pos, Blocks.STONE.defaultBlockState());
        helper.assertTrue(WorldUtils.getBlock(level, pos).getBlock() == Blocks.STONE,
                "getBlock should return the placed block");
        helper.assertTrue(!WorldUtils.isAir(level, pos),
                "placed block should not be air");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void worldUtils_fillArea(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        var from = helper.absolutePos(new BlockPos(0, 1, 0));
        var to = helper.absolutePos(new BlockPos(2, 1, 2));
        int count = WorldUtils.fillArea(level, from, to, Blocks.DIRT.defaultBlockState());
        helper.assertTrue(count > 0, "fillArea should change at least one block");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void worldUtils_getPlayersInRange(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        var player = makePlayer(helper);
        var pos = player.blockPosition();
        var players = WorldUtils.getPlayersInRange(level, pos, 100.0);
        helper.assertTrue(players.contains(player),
                "getPlayersInRange should contain the mock player");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void worldUtils_getBlockAir(@NotNull GameTestHelper helper) {
        cleanup();
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(0, 5, 0));
        helper.assertTrue(WorldUtils.isAir(level, pos),
                "an unmodified position should be air");
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void playerState_saveLoad(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        var saved = PlayerStateManager.save(player);
        player.setHealth(1.0f);
        player.getFoodData().setFoodLevel(1);
        PlayerStateManager.load(player, saved);
        helper.assertTrue(player.getHealth() == 20.0f,
                "load should restore health to 20");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void playerState_clear(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        player.setHealth(5.0f);
        player.getFoodData().setFoodLevel(3);
        player.setExperienceLevels(7);
        PlayerStateManager.clear(player);
        helper.assertTrue(player.getHealth() == 20.0f, "clear should reset health to 20");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "clear should reset food to 20");
        helper.assertTrue(player.experienceLevel == 0, "clear should reset xp to 0");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void playerState_capture(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        var snapshot = PlayerState.capture(player);
        helper.assertTrue(snapshot.health() == player.getHealth(),
                "captured health should match player health");
        helper.assertTrue(snapshot.food() == player.getFoodData().getFoodLevel(),
                "captured food should match player food");
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void restrictionManager_register(@NotNull GameTestHelper helper) {
        cleanup();
        var id = ResourceLocation.fromNamespaceAndPath("minecraft", "stone");
        RestrictionManager.register(RestrictionType.PLACE_BLOCK, id);
        helper.assertTrue(RestrictionManager.isRestricted(RestrictionType.PLACE_BLOCK, id),
                "registered block should be restricted");
        RestrictionManager.clear();
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void restrictionManager_unregister(@NotNull GameTestHelper helper) {
        cleanup();
        var id = ResourceLocation.fromNamespaceAndPath("minecraft", "stone");
        RestrictionManager.register(RestrictionType.PLACE_BLOCK, id);
        RestrictionManager.unregister(RestrictionType.PLACE_BLOCK, id);
        helper.assertTrue(!RestrictionManager.isRestricted(RestrictionType.PLACE_BLOCK, id),
                "unregistered block should not be restricted");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void restrictionManager_getAll(@NotNull GameTestHelper helper) {
        cleanup();
        var id = ResourceLocation.fromNamespaceAndPath("minecraft", "stone");
        RestrictionManager.register(RestrictionType.PLACE_BLOCK, id);
        helper.assertTrue(RestrictionManager.getAll(RestrictionType.PLACE_BLOCK).size() >= 1,
                "getAll should contain the registered restriction");
        RestrictionManager.clear();
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void restrictionManager_clear(@NotNull GameTestHelper helper) {
        cleanup();
        var id = ResourceLocation.fromNamespaceAndPath("minecraft", "stone");
        RestrictionManager.register(RestrictionType.PLACE_BLOCK, id);
        RestrictionManager.clear();
        helper.assertTrue(RestrictionManager.getAll(RestrictionType.PLACE_BLOCK).isEmpty(),
                "clear should empty all restrictions");
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void challengeRegistry_registerGet(@NotNull GameTestHelper helper) {
        cleanup();
        var def = ChallengeDefinition.of("test_challenge", "Test Challenge", List.of(), 60);
        ChallengeRegistry.register(def);
        helper.assertTrue(ChallengeRegistry.get("test_challenge") == def,
                "get should return the registered definition");
        ChallengeRegistry.clear();
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void challengeRegistry_getAll(@NotNull GameTestHelper helper) {
        cleanup();
        var def = ChallengeDefinition.of("test_challenge2", "Test Challenge 2", List.of(), 60);
        ChallengeRegistry.register(def);
        helper.assertTrue(ChallengeRegistry.getAll().size() >= 1,
                "getAll should contain the registered definition");
        ChallengeRegistry.clear();
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void objectiveTracker_startStop(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        var challenge = new Challenge("track_challenge", List.of(), 60);
        ObjectiveTracker.startChallenge(player, challenge);
        helper.assertTrue(ObjectiveTracker.isTracking(player),
                "startChallenge should begin tracking");
        helper.assertTrue("track_challenge".equals(ObjectiveTracker.getActiveChallengeName(player)),
                "active challenge name should match");
        ObjectiveTracker.stopChallenge(player);
        helper.assertTrue(!ObjectiveTracker.isTracking(player),
                "stopChallenge should end tracking");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void objectiveTracker_addRemoveObjective(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        var objective = new com.bikininjas.corelib.objective.SurvivalObjective("survive_obj", 100);
        ObjectiveTracker.addObjective(player, objective);
        helper.assertTrue(ObjectiveTracker.getObjectives(player).contains(objective),
                "getObjectives should contain the added objective");
        ObjectiveTracker.removeObjective(player, "survive_obj");
        helper.assertTrue(!ObjectiveTracker.getObjectives(player).contains(objective),
                "removeObjective should remove the objective");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void objectiveTracker_isTracking(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        helper.assertTrue(!ObjectiveTracker.isTracking(player),
                "no challenge should be tracked initially");
        var challenge = new Challenge("track_challenge2", List.of(), 60);
        ObjectiveTracker.startChallenge(player, challenge);
        helper.assertTrue(ObjectiveTracker.isTracking(player),
                "isTracking should be true after start");
        ObjectiveTracker.stopChallenge(player);
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void objectiveTracker_getActiveChallengeName(@NotNull GameTestHelper helper) {
        cleanup();
        var player = makePlayer(helper);
        var challenge = new Challenge("name_challenge", List.of(), 60);
        ObjectiveTracker.startChallenge(player, challenge);
        helper.assertTrue("name_challenge".equals(ObjectiveTracker.getActiveChallengeName(player)),
                "getActiveChallengeName should return the challenge name");
        ObjectiveTracker.stopChallenge(player);
        helper.succeed();
    }


    @GameTest(template = "core_lib:empty3x3x3")
    public static void recipeAPI_addRemove(@NotNull GameTestHelper helper) {
        cleanup();
        var server = helper.getLevel().getServer();
        var id = "core_lib:test_recipe";
        var holder = RecipeBuilder.shapeless(new ItemStack(Items.DIAMOND))
                .requires(new ItemStack(Items.STONE))
                .build();
        helper.assertTrue(holder.isPresent(), "shapeless builder should produce a recipe");
        if (holder.isPresent()) {
            RecipeAPI.addRecipe(id, holder.get());
            helper.assertTrue(server.getRecipeManager().byKey(ResourceLocation.parse(id)).isPresent(),
                    "added recipe should be present in the server recipe manager");
            RecipeAPI.removeRecipe(id);
            helper.assertTrue(server.getRecipeManager().byKey(ResourceLocation.parse(id)).isEmpty(),
                    "removed recipe should no longer be present");
        }
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void recipeBuilder_shapeless(@NotNull GameTestHelper helper) {
        cleanup();
        var holder = RecipeBuilder.shapeless(new ItemStack(Items.GOLD_INGOT))
                .requires(new ItemStack(Items.STONE))
                .requires(new ItemStack(Items.COBBLESTONE))
                .build();
        helper.assertTrue(holder.isPresent(), "shapeless builder should build a non-empty recipe");
        helper.succeed();
    }

    @GameTest(template = "core_lib:empty3x3x3")
    public static void recipeBuilder_shaped(@NotNull GameTestHelper helper) {
        cleanup();
        var holder = RecipeBuilder.shaped(new ItemStack(Items.IRON_INGOT))
                .pattern(" D ")
                .pattern(" D ")
                .pattern(" S ")
                .where('D', new ItemStack(Items.DIAMOND))
                .where('S', new ItemStack(Items.STICK))
                .build();
        helper.assertTrue(holder.isPresent(), "shaped builder should build a non-empty recipe");
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
