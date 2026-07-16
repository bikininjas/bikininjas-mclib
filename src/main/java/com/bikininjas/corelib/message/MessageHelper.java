package com.bikininjas.corelib.message;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.Map;
import java.util.Objects;

/**
 * Static helpers for sending chat messages, titles, subtitles and action-bar
 * text to players, plus a small set of formatting utilities built on top of
 * Minecraft's {@link Component} API.
 * <p>
 * Every method is static. The class cannot be instantiated. No method depends
 * on a live game registry, so the pure formatting helpers ({@link #text},
 * {@link #red}, {@link #format}, ...) are safe to call from unit tests, while
 * the player/server-bound methods require a running Minecraft server.
 * <p>
 * Color helpers return a {@link Component} styled with the matching
 * {@link ChatFormatting}. The {@link #format(String)} helper parses legacy
 * {@code &}-prefixed color/format codes (e.g. {@code &c}, {@code &l}) and
 * supports {@code &&} as an escape for a literal ampersand.
 */
public final class MessageHelper {

    /** Default title fade-in duration, in ticks. */
    public static final int DEFAULT_FADE_IN = 10;
    /** Default title stay duration, in ticks. */
    public static final int DEFAULT_STAY = 70;
    /** Default title fade-out duration, in ticks. */
    public static final int DEFAULT_FADE_OUT = 20;

    private MessageHelper() {
        // Utility class — do not instantiate.
    }

    // ---------------------------------------------------------------------
    // Chat
    // ---------------------------------------------------------------------

    /**
     * Send a plain-text system message to a single player.
     *
     * @param player  the target player (must not be {@code null})
     * @param message the raw message text (must not be {@code null})
     * @throws NullPointerException if {@code player} or {@code message} is {@code null}
     */
    public static void chat(ServerPlayer player, String message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        player.sendSystemMessage(Component.literal(message));
    }

    /**
     * Send a pre-built {@link Component} system message to a single player.
     *
     * @param player  the target player (must not be {@code null})
     * @param message the message component (must not be {@code null})
     * @throws NullPointerException if {@code player} or {@code message} is {@code null}
     */
    public static void chat(ServerPlayer player, Component message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        player.sendSystemMessage(message);
    }

    // ---------------------------------------------------------------------
    // Title / Subtitle
    // ---------------------------------------------------------------------

    /**
     * Display a title and subtitle to a single player with custom timing.
     *
     * @param player   the target player (must not be {@code null})
     * @param title    the title text (must not be {@code null})
     * @param subtitle the subtitle text (must not be {@code null})
     * @param fadeIn   fade-in duration, in ticks
     * @param stay     stay duration, in ticks
     * @param fadeOut  fade-out duration, in ticks
     * @throws NullPointerException if {@code player}, {@code title} or {@code subtitle} is {@code null}
     */
    public static void title(ServerPlayer player, String title, String subtitle,
                             int fadeIn, int stay, int fadeOut) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(subtitle, "subtitle");
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
    }

    /**
     * Display a title and subtitle to a single player using default timing
     * ({@value #DEFAULT_FADE_IN}/{@value #DEFAULT_STAY}/{@value #DEFAULT_FADE_OUT}).
     *
     * @param player   the target player (must not be {@code null})
     * @param title    the title text (must not be {@code null})
     * @param subtitle the subtitle text (must not be {@code null})
     * @throws NullPointerException if {@code player}, {@code title} or {@code subtitle} is {@code null}
     */
    public static void title(ServerPlayer player, String title, String subtitle) {
        title(player, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Display a pre-built title and subtitle {@link Component} to a single player
     * using default timing.
     *
     * @param player   the target player (must not be {@code null})
     * @param title    the title component (must not be {@code null})
     * @param subtitle the subtitle component (must not be {@code null})
     * @throws NullPointerException if {@code player}, {@code title} or {@code subtitle} is {@code null}
     */
    public static void title(ServerPlayer player, Component title, Component subtitle) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(subtitle, "subtitle");
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT));
    }

    // ---------------------------------------------------------------------
    // Action bar
    // ---------------------------------------------------------------------

    /**
     * Display a text message in the player's action bar (the line above the
     * hotbar).
     *
     * @param player the target player (must not be {@code null})
     * @param text   the action-bar text (must not be {@code null})
     * @throws NullPointerException if {@code player} or {@code text} is {@code null}
     */
    public static void actionBar(ServerPlayer player, String text) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(text, "text");
        player.displayClientMessage(Component.literal(text), true);
    }

    /**
     * Display a pre-built {@link Component} in the player's action bar.
     *
     * @param player the target player (must not be {@code null})
     * @param text   the action-bar component (must not be {@code null})
     * @throws NullPointerException if {@code player} or {@code text} is {@code null}
     */
    public static void actionBar(ServerPlayer player, Component text) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(text, "text");
        player.displayClientMessage(text, true);
    }

    // ---------------------------------------------------------------------
    // Broadcast (explicit server reference)
    // ---------------------------------------------------------------------

    /**
     * Broadcast a plain-text system message to every player on the server.
     *
     * @param message the message text (must not be {@code null})
     * @param server  the running {@link MinecraftServer} (must not be {@code null})
     * @throws NullPointerException if {@code message} or {@code server} is {@code null}
     */
    public static void broadcastChat(String message, MinecraftServer server) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(server, "server");
        PlayerList players = server.getPlayerList();
        players.broadcastSystemMessage(Component.literal(message), false);
    }

    /**
     * Broadcast a pre-built {@link Component} system message to every player.
     *
     * @param message the message component (must not be {@code null})
     * @param server  the running {@link MinecraftServer} (must not be {@code null})
     * @throws NullPointerException if {@code message} or {@code server} is {@code null}
     */
    public static void broadcastChat(Component message, MinecraftServer server) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(server, "server");
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /**
     * Broadcast a title and subtitle to every player on the server with custom
     * timing.
     *
     * @param title    the title text (must not be {@code null})
     * @param subtitle the subtitle text (must not be {@code null})
     * @param fadeIn   fade-in duration, in ticks
     * @param stay     stay duration, in ticks
     * @param fadeOut  fade-out duration, in ticks
     * @param server   the running {@link MinecraftServer} (must not be {@code null})
     * @throws NullPointerException if any argument is {@code null}
     */
    public static void broadcastTitle(String title, String subtitle,
                                       int fadeIn, int stay, int fadeOut,
                                       MinecraftServer server) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(subtitle, "subtitle");
        Objects.requireNonNull(server, "server");
        Component titleComponent = Component.literal(title);
        Component subtitleComponent = Component.literal(subtitle);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        }
    }

    /**
     * Broadcast an action-bar message to every player on the server.
     *
     * @param text   the action-bar text (must not be {@code null})
     * @param server the running {@link MinecraftServer} (must not be {@code null})
     * @throws NullPointerException if {@code text} or {@code server} is {@code null}
     */
    public static void broadcastActionBar(String text, MinecraftServer server) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(server, "server");
        Component component = Component.literal(text);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(component, true);
        }
    }

    // ---------------------------------------------------------------------
    // Format helpers
    // ---------------------------------------------------------------------

    /**
     * Wrap raw text into a plain {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return a literal {@link Component} containing {@code text}
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component text(String text) {
        return Component.literal(Objects.requireNonNull(text, "text"));
    }

    /**
     * Build a red-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component red(String text) {
        return colored(text, ChatFormatting.RED);
    }

    /**
     * Build a green-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component green(String text) {
        return colored(text, ChatFormatting.GREEN);
    }

    /**
     * Build a blue-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component blue(String text) {
        return colored(text, ChatFormatting.BLUE);
    }

    /**
     * Build a gold-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component gold(String text) {
        return colored(text, ChatFormatting.GOLD);
    }

    /**
     * Build a gray-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component gray(String text) {
        return colored(text, ChatFormatting.GRAY);
    }

    /**
     * Build a white-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component white(String text) {
        return colored(text, ChatFormatting.WHITE);
    }

    /**
     * Build an aqua-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component aqua(String text) {
        return colored(text, ChatFormatting.AQUA);
    }

    /**
     * Build a yellow-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component yellow(String text) {
        return colored(text, ChatFormatting.YELLOW);
    }

    /**
     * Build a light-purple-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component lightPurple(String text) {
        return colored(text, ChatFormatting.LIGHT_PURPLE);
    }

    /**
     * Build a dark-red-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component darkRed(String text) {
        return colored(text, ChatFormatting.DARK_RED);
    }

    /**
     * Build a dark-green-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component darkGreen(String text) {
        return colored(text, ChatFormatting.DARK_GREEN);
    }

    /**
     * Build a dark-blue-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component darkBlue(String text) {
        return colored(text, ChatFormatting.DARK_BLUE);
    }

    /**
     * Build a dark-gray-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component darkGray(String text) {
        return colored(text, ChatFormatting.DARK_GRAY);
    }

    /**
     * Build a black-colored {@link Component}.
     *
     * @param text the text (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component black(String text) {
        return colored(text, ChatFormatting.BLACK);
    }

    /**
     * Apply a single {@link ChatFormatting} color to literal text.
     *
     * @param text  the text (must not be {@code null})
     * @param color the formatting/color to apply (must not be {@code null})
     * @return the styled component
     * @throws NullPointerException if {@code text} or {@code color} is {@code null}
     */
    private static Component colored(String text, ChatFormatting color) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(color, "color");
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color));
    }

    /**
     * Parse legacy {@code &}-prefixed color and format codes into a styled
     * {@link Component}.
     * <p>
     * Supported codes (see {@link #LEGACY_CODES}): {@code &4} dark red,
     * {@code &c} red, {@code &6} gold, {@code &e} yellow, {@code &a} green,
     * {@code &b} aqua, {@code &d} light purple, {@code &f} white, {@code &7}
     * gray, {@code &8} dark gray, {@code &l} bold, {@code &o} italic,
     * {@code &n} underline, {@code &m} strikethrough, {@code &k} obfuscated,
     * {@code &r} reset. A literal ampersand is written as {@code &&}.
     *
     * @param text the text containing legacy codes (must not be {@code null})
     * @return a {@link Component} with the parsed styling applied
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static Component format(String text) {
        Objects.requireNonNull(text, "text");

        MutableComponent result = Component.literal("");
        Style current = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '&') {
                    // Escaped ampersand -> literal '&'.
                    buffer.append('&');
                    i++; // consume the second '&'
                    continue;
                }
                ChatFormatting formatting = LEGACY_CODES.get(next);
                if (formatting != null) {
                    flush(result, buffer, current);
                    current = applyFormatting(current, formatting);
                    i++; // consume the code char
                    continue;
                }
            }
            buffer.append(c);
        }
        flush(result, buffer, current);

        return result;
    }

    /**
     * Append the buffered text as a single styled segment, then clear the
     * buffer. No-op when the buffer is empty.
     */
    private static void flush(MutableComponent result, StringBuilder buffer, Style style) {
        if (buffer.length() == 0) {
            return;
        }
        result.append(Component.literal(buffer.toString()).withStyle(style));
        buffer.setLength(0);
    }

    /**
     * Merge a {@link ChatFormatting} into the current {@link Style}.
     * <p>
     * {@code &r} (reset) clears all formatting; color codes set the color,
     * decoration codes toggle the matching decoration.
     */
    private static Style applyFormatting(Style style, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            return Style.EMPTY;
        }
        if (formatting.isColor()) {
            return style.withColor(formatting);
        }
        return style.applyFormat(formatting);
    }

    /**
     * Map of legacy {@code &}-code characters to their {@link ChatFormatting}.
     */
    private static final Map<Character, ChatFormatting> LEGACY_CODES = buildLegacyCodes();

    private static Map<Character, ChatFormatting> buildLegacyCodes() {
        Map<Character, ChatFormatting> map = new java.util.HashMap<>();
        map.put('4', ChatFormatting.DARK_RED);
        map.put('c', ChatFormatting.RED);
        map.put('6', ChatFormatting.GOLD);
        map.put('e', ChatFormatting.YELLOW);
        map.put('a', ChatFormatting.GREEN);
        map.put('b', ChatFormatting.AQUA);
        map.put('d', ChatFormatting.LIGHT_PURPLE);
        map.put('f', ChatFormatting.WHITE);
        map.put('7', ChatFormatting.GRAY);
        map.put('8', ChatFormatting.DARK_GRAY);
        map.put('0', ChatFormatting.BLACK);
        map.put('l', ChatFormatting.BOLD);
        map.put('o', ChatFormatting.ITALIC);
        map.put('n', ChatFormatting.UNDERLINE);
        map.put('m', ChatFormatting.STRIKETHROUGH);
        map.put('k', ChatFormatting.OBFUSCATED);
        map.put('r', ChatFormatting.RESET);
        return java.util.Collections.unmodifiableMap(map);
    }
}
