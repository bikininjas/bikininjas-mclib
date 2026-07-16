package com.bikininjas.corelib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Client-side HUD overlay that renders a translucent stats panel
 * on the right side of the screen.
 * <p>
 * The overlay is drawn during {@link RenderGuiEvent.Post} and respects
 * the visibility and field-preference flags synced from the server.
 * Auto-sizing: the panel width expands to fit the longest stat line.
 * <p>
 * Registered on the NeoForge event bus from {@code CoreLib} via
 * {@code FMLClientSetupEvent}.
 */
public final class StatsOverlayRenderer {

    private static final int BG_COLOR         = 0x88000000; // semi-transparent black
    private static final int HEADER_COLOR     = 0xFFDAA520; // gold
    private static final int DEATH_COLOR      = 0xFFFF5555; // red
    private static final int KILL_COLOR       = 0xFFFFAA00; // orange
    private static final int BLOCK_COLOR      = 0xFFAAAAAA; // gray
    private static final int CRAFT_COLOR      = 0xFFFF55FF; // light purple
    private static final int FIELD_TEXT_COLOR = 0xFFFFFFFF; // white
    private static final int MARGIN           = 4;          // px

    private StatsOverlayRenderer() {}

    @SubscribeEvent
    static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!StatsClientData.isVisible()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiGraphics gg = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // ── Build lines ────────────────────────────
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("\u00a76\u00a7lStats:");  // gold bold header

        var fields = StatsClientData.getVisibleFields();
        if (fields.contains("deaths"))
            lines.add("\u00a7c\u2620 Deaths: \u00a7f" + StatsClientData.getDeaths());
        if (fields.contains("kills"))
            lines.add("\u00a7e\u2694 Kills: \u00a7f" + StatsClientData.getKills());
        if (fields.contains("blocksBroken"))
            lines.add("\u00a77\u26CF Blocks Broken: \u00a7f" + StatsClientData.getBlocksBroken());
        if (fields.contains("crafts"))
            lines.add("\u00a7d\uD83D\uDD28 Crafts: \u00a7f" + StatsClientData.getCrafts());

        if (lines.size() <= 1) {
            return; // header only, nothing to show
        }

        // ── Layout ─────────────────────────────────
        int lineHeight = font.lineHeight + 1;
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }
        width += MARGIN * 2;
        int height = lines.size() * lineHeight + MARGIN * 2;

        int x = screenWidth - width - MARGIN;
        int y = screenHeight / 2 - height / 2;
        int x2 = x + width;
        int y2 = y + height;

        // ── Draw background ─────────────────────────
        gg.fill(x, y, x2, y2, BG_COLOR);

        // ── Draw text ───────────────────────────────
        int textY = y + MARGIN;
        for (String line : lines) {
            int color;
            if (line.startsWith("\u00a76")) {
                color = HEADER_COLOR;
            } else if (line.startsWith("\u00a7c")) {
                color = DEATH_COLOR;
            } else if (line.startsWith("\u00a7e")) {
                color = KILL_COLOR;
            } else if (line.startsWith("\u00a77")) {
                color = BLOCK_COLOR;
            } else if (line.startsWith("\u00a7d")) {
                color = CRAFT_COLOR;
            } else {
                color = FIELD_TEXT_COLOR;
            }
            // Strip the § code prefix we used for routing, draw the rest
            String display = line.substring(2);
            gg.drawString(font, display, x + MARGIN, textY, color);
            textY += lineHeight;
        }
    }
}
