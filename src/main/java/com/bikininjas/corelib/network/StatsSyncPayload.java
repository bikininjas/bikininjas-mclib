package com.bikininjas.corelib.network;

import com.bikininjas.corelib.stats.PlayerStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Payload for syncing player stats from server to client for HUD display.
 * Also carries overlay display preferences (enabled state, visible fields bitmask).
 */
public record StatsSyncPayload(
        int deaths,
        int kills,
        int blocksBroken,
        int crafts,
        boolean overlayEnabled,
        int visibleFields
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("core_lib", "stats_sync");
    public static final Type<StatsSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, StatsSyncPayload> STREAM_CODEC =
            StreamCodec.ofMember(StatsSyncPayload::write, StatsSyncPayload::new);

    public StatsSyncPayload(@NotNull PlayerStats stats, boolean overlayEnabled, int visibleFields) {
        this(stats.deaths(), stats.kills(), stats.blocksBroken(), stats.crafts(), overlayEnabled, visibleFields);
    }

    private StatsSyncPayload(@NotNull RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readBoolean(), buf.readInt());
    }

    private void write(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeInt(deaths);
        buf.writeInt(kills);
        buf.writeInt(blocksBroken);
        buf.writeInt(crafts);
        buf.writeBoolean(overlayEnabled);
        buf.writeInt(visibleFields);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
