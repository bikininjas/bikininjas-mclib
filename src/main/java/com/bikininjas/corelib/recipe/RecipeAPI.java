package com.bikininjas.corelib.recipe;

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API for programmatically adding and removing recipes at runtime.
 * <p>
 * Recipes are stored in an internal registry and applied when the server starts.
 * Changes can be synchronized to players.
 * <p>
 * All methods are static. No event bus registration (relies on explicit calls).
 */
public final class RecipeAPI {

    private static final Map<String, RecipeHolder<?>> pendingAdditions = new ConcurrentHashMap<>();
    private static final java.util.Set<String> pendingRemovals = ConcurrentHashMap.newKeySet();

    static {
        NeoForge.EVENT_BUS.register(EventHandler.class);
    }

    private RecipeAPI() {
    }

    /**
     * Add a recipe. The recipe ID must be unique (e.g. {@code "my_mod:custom_sword"}).
     */
    public static void addRecipe(@NotNull String id, @NotNull RecipeHolder<?> holder) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(holder, "holder must not be null");
        pendingAdditions.put(id, holder);
        pendingRemovals.remove(id);
    }

    /**
     * Remove a recipe by its ID.
     */
    public static void removeRecipe(@NotNull String id) {
        Objects.requireNonNull(id, "id must not be null");
        pendingRemovals.add(id);
        pendingAdditions.remove(id);
    }

    /**
     * Sync the server's current recipe list to a single player.
     */
    public static void syncToPlayer(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        player.connection.send(new ClientboundUpdateRecipesPacket(
                java.util.List.copyOf(player.server.getRecipeManager().getRecipes())));
    }

    /**
     * Sync the current modified recipe set to all players on the server.
     */
    public static void syncToAll(@NotNull MinecraftServer server) {
        Objects.requireNonNull(server, "server must not be null");
        for (var player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }

    // -- Internal ------------------------------------------------------------

    /**
     * Apply pending recipe additions and removals to the server's {@link RecipeManager}.
     * Called automatically on {@link ServerAboutToStartEvent} via core-lib's init.
     * Mods can also call this directly to force immediate application.
     */
    public static void applyPending(@NotNull MinecraftServer server) {
        var manager = server.getRecipeManager();
        if (pendingAdditions.isEmpty() && pendingRemovals.isEmpty()) {
            return;
        }

        var allRecipes = new ArrayList<>(manager.getRecipes());
        allRecipes.removeIf(holder -> pendingRemovals.contains(holder.id().toString()));
        allRecipes.addAll(pendingAdditions.values());

        manager.replaceRecipes(allRecipes);

        pendingAdditions.clear();
        pendingRemovals.clear();
    }

    private static final class EventHandler {
        private EventHandler() {}

        @SubscribeEvent
        static void onServerAboutToStart(ServerAboutToStartEvent event) {
            applyPending(event.getServer());
        }
    }
}
