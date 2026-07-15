package com.bikininjas.corelib;

import com.bikininjas.corelib.entity.SpawnHelper;
import com.bikininjas.corelib.enchantment.EnchantmentUtils;
import com.bikininjas.corelib.randomevent.RandomEventManager;
import com.bikininjas.corelib.registry.Registers;
import com.bikininjas.corelib.time.TimeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CoreLib.MODID)
public final class CoreLib {

    public static final String MODID = "core_lib";

    public CoreLib(IEventBus modBus) {
        // Register deferred registers
        Registers.ITEMS.register(modBus);
        Registers.BLOCKS.register(modBus);
        Registers.BLOCK_ENTITY_TYPES.register(modBus);
        Registers.ENTITY_TYPES.register(modBus);

        // Initialize core modules
        initModules();
    }

    /**
     * Force-load all module classes to trigger their static initializers
     * (event bus subscriptions) and singleton initialization.
     * <p>
     * {@link TimeManager} registers a tick handler via its static block.
     * {@link RandomEventManager#getInstance()} creates the singleton event engine.
     * {@link SpawnHelper} and {@link EnchantmentUtils} are stateless utilities
     * that need no initialization.
     */
    private static void initModules() {
        // Force TimeManager class load -> triggers static event-bus registration
        TimeManager.computeExtraTicks(1.0f, 1.0f);

        // Initialize the random event manager singleton (self-registers on event bus)
        RandomEventManager.getInstance();
    }
}
