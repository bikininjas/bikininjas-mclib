package com.bikininjas.corelib.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatsSyncPayloadTest {

    @Test
    void streamCodecRoundTripPreservesAllFields() {
        var original = new StatsSyncPayload(3, 17, 42, 8, true, 0b1011);

        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        StatsSyncPayload.STREAM_CODEC.encode(buf, original);
        var decoded = StatsSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original, decoded);
        assertEquals(3, decoded.deaths());
        assertEquals(17, decoded.kills());
        assertEquals(42, decoded.blocksBroken());
        assertEquals(8, decoded.crafts());
        assertTrue(decoded.overlayEnabled());
        assertEquals(0b1011, decoded.visibleFields());
    }

    @Test
    void streamCodecRoundTripWithZeroAndFalseValues() {
        var original = new StatsSyncPayload(0, 0, 0, 0, false, 0);

        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        StatsSyncPayload.STREAM_CODEC.encode(buf, original);
        var decoded = StatsSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original, decoded);
        assertFalse(decoded.overlayEnabled());
        assertEquals(0, decoded.visibleFields());
    }

    @Test
    void streamCodecRoundTripWithNegativeFields() {
        var original = new StatsSyncPayload(-5, -10, -1, -99, true, -7);

        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        StatsSyncPayload.STREAM_CODEC.encode(buf, original);
        var decoded = StatsSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original, decoded);
    }

    @Test
    void payloadFromPlayerStatsCarriesStatsFields() {
        var stats = new com.bikininjas.corelib.stats.PlayerStats(1, 2, 3, 4);
        var payload = new StatsSyncPayload(stats, true, 5);

        assertEquals(1, payload.deaths());
        assertEquals(2, payload.kills());
        assertEquals(3, payload.blocksBroken());
        assertEquals(4, payload.crafts());
        assertTrue(payload.overlayEnabled());
        assertEquals(5, payload.visibleFields());
    }

    @Test
    void typeReturnsExpectedResourceLocation() {
        assertEquals(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("core_lib", "stats_sync"),
                StatsSyncPayload.TYPE.id()
        );
        assertSame(StatsSyncPayload.TYPE, new StatsSyncPayload(0, 0, 0, 0, false, 0).type());
    }
}
