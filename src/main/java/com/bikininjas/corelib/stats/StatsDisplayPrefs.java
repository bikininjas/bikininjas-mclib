package com.bikininjas.corelib.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages per-player HUD overlay preferences for stats display.
 * <p>
 * Preferences are persisted in the player's persistent data.
 */
public final class StatsDisplayPrefs {

    /** Available field keys. */
    public static final String FIELD_DEATHS = "deaths";
    public static final String FIELD_KILLS = "kills";
    public static final String FIELD_BLOCKS_BROKEN = "blocksBroken";
    public static final String FIELD_CRAFTS = "crafts";

    /** Bitmask constants for network sync. */
    public static final int BIT_DEATHS = 1;
    public static final int BIT_KILLS = 2;
    public static final int BIT_BLOCKS = 4;
    public static final int BIT_CRAFTS = 8;
    public static final int VISIBLE_ALL = 15;

    private static final Set<String> ALL_FIELDS = Set.of(FIELD_DEATHS, FIELD_KILLS, FIELD_BLOCKS_BROKEN, FIELD_CRAFTS);
    private static final String TAG_ENABLED = "stats_overlay_enabled";
    private static final String TAG_VISIBLE_FIELDS = "stats_visible_fields";

    private StatsDisplayPrefs() {
    }

    /**
     * Toggle the stats overlay for a player on/off.
     */
    public static void toggle(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        var data = player.getPersistentData();
        boolean current = data.getBoolean(TAG_ENABLED);
        data.putBoolean(TAG_ENABLED, !current);
    }

    /**
     * Set the visible fields for the player.
     *
     * @param fields a set containing any of {@link #FIELD_DEATHS}, {@link #FIELD_KILLS},
     *               {@link #FIELD_BLOCKS_BROKEN}, {@link #FIELD_CRAFTS}
     */
    public static void setVisibleFields(@NotNull ServerPlayer player, @NotNull Set<String> fields) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(fields, "fields must not be null");
        var valid = new HashSet<>(fields);
        valid.retainAll(ALL_FIELDS);
        var tag = new CompoundTag();
        int i = 0;
        for (var field : valid) {
            tag.putString(Integer.toString(i++), field);
        }
        player.getPersistentData().put(TAG_VISIBLE_FIELDS, tag);
    }

    /**
     * Get the currently visible fields for a player.
     */
    public static @NotNull Set<String> getVisibleFields(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        var tag = player.getPersistentData().getCompound(TAG_VISIBLE_FIELDS);
        var result = new HashSet<String>();
        for (int i = 0; tag.contains(Integer.toString(i)); i++) {
            var field = tag.getString(Integer.toString(i));
            if (ALL_FIELDS.contains(field)) {
                result.add(field);
            }
        }
        if (result.isEmpty()) {
            return ALL_FIELDS; // default: all visible
        }
        return result;
    }

    /**
     * Check if the stats overlay is enabled for a player (default: enabled).
     */
    public static boolean isEnabled(@NotNull ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        var data = player.getPersistentData();
        return !data.contains(TAG_ENABLED) || data.getBoolean(TAG_ENABLED);
    }

    /**
     * Convert a set of field keys to a bitmask for network sync.
     */
    public static int toBitmask(@NotNull Set<String> fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        int mask = 0;
        if (fields.contains(FIELD_DEATHS)) mask |= BIT_DEATHS;
        if (fields.contains(FIELD_KILLS)) mask |= BIT_KILLS;
        if (fields.contains(FIELD_BLOCKS_BROKEN)) mask |= BIT_BLOCKS;
        if (fields.contains(FIELD_CRAFTS)) mask |= BIT_CRAFTS;
        return mask;
    }

    /**
     * Convert a bitmask back to a set of field keys.
     */
    public static @NotNull Set<String> fromBitmask(int bitmask) {
        var result = new HashSet<String>();
        if ((bitmask & BIT_DEATHS) != 0) result.add(FIELD_DEATHS);
        if ((bitmask & BIT_KILLS) != 0) result.add(FIELD_KILLS);
        if ((bitmask & BIT_BLOCKS) != 0) result.add(FIELD_BLOCKS_BROKEN);
        if ((bitmask & BIT_CRAFTS) != 0) result.add(FIELD_CRAFTS);
        return result;
    }
}
