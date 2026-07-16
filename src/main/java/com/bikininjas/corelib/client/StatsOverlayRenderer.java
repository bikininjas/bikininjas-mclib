package com.bikininjas.corelib.client;

import com.bikininjas.corelib.network.NetworkHandler.StatsClientData;
import com.bikininjas.corelib.stats.StatsDisplayPrefs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Renders a semi-transparent stats HUD overlay on the right side of the screen.
 */
public final class StatsOverlayRenderer {

    private StatsOverlayRenderer() {
    }

    public static final class Renderer {
        private Renderer() {
        }

        @SubscribeEvent
        static void onRenderGui(@NotNull RenderGuiEvent.Post event) {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }

            if (!StatsClientData.isOverlayEnabled()) {
                return;
            }

            var stats = StatsClientData.getLatest();
            var graphics = event.getGuiGraphics();
            var font = mc.font;
            var width = mc.getWindow().getGuiScaledWidth();
            var height = mc.getWindow().getGuiScaledHeight();

            int x = width - 110;
            int y = height / 2 - 40;

            int bgHeight = 56;
            int lineY = y + 16;
            int color = 0xCCCCCC;

            boolean hasVisible = StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_DEATHS)
                    || StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_KILLS)
                    || StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_BLOCKS)
                    || StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_CRAFTS);

            if (!hasVisible) return;

            graphics.fill(x - 4, y - 4, x + 100, y + bgHeight, 0x88000000);
            graphics.drawString(font, "Stats", x + 30, y + 2, 0xFFFFFF);

            if (StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_DEATHS)) {
                graphics.drawString(font, "Deaths: " + stats.deaths(), x + 4, lineY, color);
                lineY += 10;
            }
            if (StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_KILLS)) {
                graphics.drawString(font, "Kills: " + stats.kills(), x + 4, lineY, color);
                lineY += 10;
            }
            if (StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_BLOCKS)) {
                graphics.drawString(font, "Blocks: " + stats.blocksBroken(), x + 4, lineY, color);
                lineY += 10;
            }
            if (StatsClientData.isFieldVisible(StatsDisplayPrefs.BIT_CRAFTS)) {
                graphics.drawString(font, "Crafts: " + stats.crafts(), x + 4, lineY, color);
            }
        }
    }
}
