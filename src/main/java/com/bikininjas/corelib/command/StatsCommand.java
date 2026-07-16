package com.bikininjas.corelib.command;

import com.bikininjas.corelib.network.StatsSyncPayload;
import com.bikininjas.corelib.stats.PlayerStats;
import com.bikininjas.corelib.stats.PlayerStatsManager;
import com.bikininjas.corelib.stats.StatsDisplayPrefs;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

/**
 * {@code /stats} command — toggle the stats HUD overlay and select visible fields.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /stats} — toggle the overlay on/off</li>
 *   <li>{@code /stats deaths kills} — show only deaths and kills on the overlay</li>
 * </ul>
 */
public final class StatsCommand {

    private StatsCommand() {}

    /**
     * Register the {@code /stats} command tree on the given dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stats")
                .requires(src -> src.getPlayer() != null)

                // /stats — toggle overlay
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player == null) return 0;

                    boolean nowEnabled = StatsDisplayPrefs.toggle(player);
                    syncNow(player);

                    if (nowEnabled) {
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("Stats overlay enabled.")
                                        .withStyle(ChatFormatting.GREEN), false);
                    } else {
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("Stats overlay disabled.")
                                        .withStyle(ChatFormatting.GRAY), false);
                    }
                    return 1;
                })

                // /stats <fields...> — set visible fields + enable
                .then(Commands.argument("fields", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            for (String f : StatsDisplayPrefs.getAllFields()) {
                                if (f.startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(f);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return 0;

                            String raw = StringArgumentType.getString(ctx, "fields");
                            Set<String> requested = new HashSet<>();
                            for (String token : raw.split("\\s+")) {
                                requested.add(token.trim().toLowerCase());
                            }

                            StatsDisplayPrefs.setVisibleFields(player, requested);
                            StatsDisplayPrefs.toggle(player); // ensure enabled
                            syncNow(player);

                            ctx.getSource().sendSuccess(() -> Component.literal("Stats overlay updated: ")
                                    .withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(String.join(", ", StatsDisplayPrefs.getVisibleFields(player)))
                                            .withStyle(ChatFormatting.WHITE)), false);
                            return 1;
                        })
                )
        );
    }

    private static void syncNow(ServerPlayer player) {
        boolean enabled = StatsDisplayPrefs.isEnabled(player);
        Set<String> fields = StatsDisplayPrefs.getVisibleFields(player);
        PlayerStats stats = PlayerStatsManager.getStats(player);
        var payload = new StatsSyncPayload(enabled, fields,
                stats.deaths(), stats.kills(), stats.blocksBroken(), stats.crafts());
        PacketDistributor.sendToPlayer(player, payload);
    }
}
