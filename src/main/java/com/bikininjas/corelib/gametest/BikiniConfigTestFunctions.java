package com.bikininjas.corelib.gametest;

import com.bikininjas.corelib.config.BikiniConfigRegistry;
import com.bikininjas.corelib.config.ConfigOption;
import net.minecraft.network.chat.Component;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import org.jetbrains.annotations.NotNull;

/**
 * GameTests for the BikiniConfig system.
 */
@ForEachTest(groups = "core_lib")
public final class BikiniConfigTestFunctions {

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void registry_registerModAndOption(@NotNull ExtendedGameTestHelper helper) {
        BikiniConfigRegistry.clear();
        BikiniConfigRegistry.registerMod("test_mod", "Test Mod");

        helper.assertTrue(BikiniConfigRegistry.getRegisteredMods().contains("test_mod"),
                "test_mod should be registered");
        helper.assertTrue(BikiniConfigRegistry.getRegisteredMods().size() == 1,
                "Expected 1 mod, got " + BikiniConfigRegistry.getRegisteredMods().size());

        BikiniConfigRegistry.registerOption(new ConfigOption(
                "test_mod", "test_key", "General",
                Component.literal("Test Option"),
                Component.literal("A test option"),
                ConfigOption.OptionType.BOOL,
                true, true, null
        ));

        var options = BikiniConfigRegistry.getOptions("test_mod");
        helper.assertTrue(options.size() == 1, "Expected 1 option, got " + options.size());

        var opt = options.iterator().next();
        helper.assertTrue("test_key".equals(opt.key()), "Option key should be 'test_key'");
        helper.assertTrue((boolean) opt.currentValue(), "Default value should be true");

        BikiniConfigRegistry.clear();
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void registry_toggleBoolOption(@NotNull ExtendedGameTestHelper helper) {
        BikiniConfigRegistry.clear();
        BikiniConfigRegistry.registerMod("test_mod", "Test");
        BikiniConfigRegistry.registerOption(new ConfigOption(
                "test_mod", "enabled", "General",
                Component.literal("Enabled"),
                Component.literal("Toggle test"),
                ConfigOption.OptionType.BOOL,
                true, true, null
        ));

        BikiniConfigRegistry.updateValue("test_mod", "enabled", false);
        var opt = BikiniConfigRegistry.getOptions("test_mod").iterator().next();
        helper.assertTrue(!(boolean) opt.currentValue(),
                "Value should be false after update");

        BikiniConfigRegistry.clear();
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void registry_enumOptionCycles(@NotNull ExtendedGameTestHelper helper) {
        BikiniConfigRegistry.clear();
        BikiniConfigRegistry.registerMod("test_mod", "Test");
        BikiniConfigRegistry.registerOption(new ConfigOption(
                "test_mod", "difficulty", "Gameplay",
                Component.literal("Difficulty"),
                Component.literal("Game difficulty"),
                ConfigOption.OptionType.ENUM,
                "normal", "normal", new String[]{"easy", "normal", "hard", "insane"}
        ));

        BikiniConfigRegistry.updateValue("test_mod", "difficulty", "hard");
        var opt = BikiniConfigRegistry.getOptions("test_mod").iterator().next();
        helper.assertTrue("hard".equals(opt.currentValue()),
                "Value should be 'hard', was '" + opt.currentValue() + "'");

        BikiniConfigRegistry.clear();
        helper.succeed();
    }

    @EmptyTemplate(value = "3x3x3", floor = true)
    public static void registry_saveAndLoadConfig(@NotNull ExtendedGameTestHelper helper) {
        BikiniConfigRegistry.clear();
        BikiniConfigRegistry.registerMod("test_mod", "Test");
        BikiniConfigRegistry.registerOption(new ConfigOption(
                "test_mod", "speed", "General",
                Component.literal("Speed"),
                Component.literal("Speed multiplier"),
                ConfigOption.OptionType.INT,
                1, 5, null
        ));

        BikiniConfigRegistry.updateValue("test_mod", "speed", 3);
        BikiniConfigRegistry.saveConfig();
        BikiniConfigRegistry.clear();

        // Re-register and load
        BikiniConfigRegistry.registerMod("test_mod", "Test");
        BikiniConfigRegistry.registerOption(new ConfigOption(
                "test_mod", "speed", "General",
                Component.literal("Speed"),
                Component.literal("Speed multiplier"),
                ConfigOption.OptionType.INT,
                1, 1, null
        ));
        BikiniConfigRegistry.loadConfig();

        var opt = BikiniConfigRegistry.getOptions("test_mod").iterator().next();
        helper.assertTrue(opt.currentValue() instanceof Number && ((Number) opt.currentValue()).intValue() == 3,
                "Loaded value should be 3, was " + opt.currentValue());

        BikiniConfigRegistry.clear();
        helper.succeed();
    }
}
