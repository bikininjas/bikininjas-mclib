package com.bikininjas.corelib.message;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Utilities for sending chat messages, titles, action bars, and broadcasts.
 * <p>
 * Supports color formatting via {@code &}-codes ({@code &c}, {@code &l}, {@code &a}...).
 * All methods are static. No event bus registration.
 */
public final class MessageHelper {

    private MessageHelper() {
    }

    // -- Chat -----------------------------------------------------------------

    /**
     * Send a chat message to a single player.
     */
    public static void chat(@NotNull ServerPlayer player, @NotNull String message) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(message, "message must not be null");
        player.sendSystemMessage(text(message));
    }

    /**
     * Send a chat Component to a single player.
     */
    public static void chat(@NotNull ServerPlayer player, @NotNull Component message) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(message, "message must not be null");
        player.sendSystemMessage(message);
    }

    // -- Title ----------------------------------------------------------------

    /**
     * Send a title with default timings (10 fade-in, 70 stay, 20 fade-out ticks).
     */
    public static void title(@NotNull ServerPlayer player, @NotNull String title, @Nullable String subtitle) {
        title(player, text(title), subtitle != null ? text(subtitle) : null, 10, 70, 20);
    }

    /**
     * Send a title with custom timings.
     */
    public static void title(
            @NotNull ServerPlayer player,
            @NotNull Component title,
            @Nullable Component subtitle,
            int fadeIn, int stay, int fadeOut) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(title, "title must not be null");
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        if (subtitle != null) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    // -- Action bar -----------------------------------------------------------

    /**
     * Send an action bar message to a player.
     */
    public static void actionBar(@NotNull ServerPlayer player, @NotNull String text) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(text, "text must not be null");
        player.displayClientMessage(text(text), true);
    }

    /**
     * Send an action bar Component to a player.
     */
    public static void actionBar(@NotNull ServerPlayer player, @NotNull Component component) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(component, "component must not be null");
        player.displayClientMessage(component, true);
    }

    // -- Broadcast ------------------------------------------------------------

    /**
     * Broadcast a chat message to all players on the server.
     */
    public static void broadcastChat(@NotNull String message, @NotNull MinecraftServer server) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(server, "server must not be null");
        server.getPlayerList().broadcastSystemMessage(text(message), false);
    }

    /**
     * Broadcast a Component to all players on the server.
     */
    public static void broadcastChat(@NotNull Component component, @NotNull MinecraftServer server) {
        Objects.requireNonNull(component, "component must not be null");
        Objects.requireNonNull(server, "server must not be null");
        server.getPlayerList().broadcastSystemMessage(component, false);
    }

    /**
     * Broadcast a title to all players.
     */
    public static void broadcastTitle(
            @NotNull Component title, @Nullable Component subtitle,
            int fadeIn, int stay, int fadeOut,
            @NotNull MinecraftServer server) {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(server, "server must not be null");
        var packet = new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
        var titlePacket = new ClientboundSetTitleTextPacket(title);
        var subtitlePacket = subtitle != null ? new ClientboundSetSubtitleTextPacket(subtitle) : null;
        for (var player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
            player.connection.send(titlePacket);
            if (subtitlePacket != null) {
                player.connection.send(subtitlePacket);
            }
        }
    }

    /**
     * Broadcast an action bar to all players.
     */
    public static void broadcastActionBar(@NotNull String text, @NotNull MinecraftServer server) {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(server, "server must not be null");
        var component = text(text);
        for (var player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(component, true);
        }
    }

    // -- Sound ----------------------------------------------------------------

    /**
     * Play a sound to a player.
     */
    public static void playSound(@NotNull ServerPlayer player, @NotNull SoundEvent sound, float volume, float pitch) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(sound, "sound must not be null");
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(),
                volume, pitch, player.getRandom().nextLong()
        ));
    }

    // -- Component formatting -------------------------------------------------

    /**
     * Create a literal text Component.
     */
    public static @NotNull MutableComponent text(@NotNull String text) {
        return Component.literal(Objects.requireNonNull(text, "text must not be null"));
    }

    /**
     * Parse {@code &}-codes into a formatted Component.
     * Supports: {@code &0-9}, {@code &a-f}, {@code &k-o}, {@code &r}.
     */
    public static @NotNull MutableComponent format(@NotNull String text) {
        Objects.requireNonNull(text, "text must not be null");
        var output = Component.literal("");
        var current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                // Flush current literal
                if (!current.isEmpty()) {
                    output.append(Component.literal(current.toString()));
                    current.setLength(0);
                }
                char code = text.charAt(++i);
                var format = ChatFormatting.getByCode(code);
                if (format != null) {
                    output.append(Component.literal("").withStyle(format));
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            output.append(Component.literal(current.toString()));
        }
        return output;
    }

    // -- Named colour helpers -------------------------------------------------

    public static @NotNull MutableComponent red(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.RED);
    }

    public static @NotNull MutableComponent green(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.GREEN);
    }

    public static @NotNull MutableComponent blue(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.BLUE);
    }

    public static @NotNull MutableComponent gold(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.GOLD);
    }

    public static @NotNull MutableComponent yellow(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.YELLOW);
    }

    public static @NotNull MutableComponent aqua(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.AQUA);
    }

    public static @NotNull MutableComponent lightPurple(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    public static @NotNull MutableComponent white(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.WHITE);
    }

    public static @NotNull MutableComponent gray(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.GRAY);
    }

    public static @NotNull MutableComponent darkRed(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.DARK_RED);
    }

    public static @NotNull MutableComponent darkGreen(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.DARK_GREEN);
    }

    public static @NotNull MutableComponent darkBlue(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.DARK_BLUE);
    }

    public static @NotNull MutableComponent darkAqua(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.DARK_AQUA);
    }

    public static @NotNull MutableComponent darkGray(@NotNull String text) {
        return text(text).withStyle(ChatFormatting.DARK_GRAY);
    }
}
