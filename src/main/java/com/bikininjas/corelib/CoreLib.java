package com.bikininjas.corelib;

import com.bikininjas.corelib.client.StatsOverlayRenderer;
import com.bikininjas.corelib.command.CommandRegister;
import com.bikininjas.corelib.command.ConfigCommand;
import com.bikininjas.corelib.cooldown.CooldownManager;
import com.bikininjas.corelib.loot.LootTableHelper;
import com.bikininjas.corelib.network.NetworkHandler;
import com.bikininjas.corelib.objective.ObjectiveTracker;
import com.bikininjas.corelib.particle.ParticleHelper;
import com.bikininjas.corelib.randomevent.RandomEventManager;
import com.bikininjas.corelib.registry.Registers;
import com.bikininjas.corelib.restriction.RestrictionManager;
import com.bikininjas.corelib.stats.PlayerStatsManager;
import com.bikininjas.corelib.time.TimeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Core Lib — shared library mod for all Bikininjas Minecraft mods.
 * <p>
 * Provides utility APIs, managers, and helpers via static utility classes.
 * No concrete features — only reusable infrastructure consumed by child mods.
 */
@Mod(CoreLib.MODID)
public final class CoreLib {

    public static final String MODID = "core_lib";

    public CoreLib(IEventBus modBus) {
        // Register DeferredRegisters to mod bus
        Registers.ITEMS.register(modBus);
        Registers.BLOCKS.register(modBus);
        Registers.BLOCK_ENTITY_TYPES.register(modBus);
        Registers.ENTITY_TYPES.register(modBus);

        NetworkHandler.register(modBus);

        // Client setup
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> {
                NeoForge.EVENT_BUS.register(StatsOverlayRenderer.Renderer.class);
            });
        }

        // Loot table injection on load
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            LootTableHelper.injectInto(event.getName(), event.getTable());
        });

        // Server init — force module class loading after server starts
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> {
            initModules();
        });
    }

    /**
     * Force-loads all utility module classes so their static initializers
     * (event bus registration, singleton creation) run on the server thread.
     * <p>
     * Each module provides a no-op public method that is called here purely
     * to trigger {@code static {} } blocks in the JVM.
     */
    public static void initModules() {
        CooldownManager.init();
        ParticleHelper.init();
        LootTableHelper.init();
        RandomEventManager.getInstance();
        ObjectiveTracker.currentTick();
        PlayerStatsManager.init();
        RestrictionManager.init();
        CommandRegister.init();
        ConfigCommand.init();
    }
}
