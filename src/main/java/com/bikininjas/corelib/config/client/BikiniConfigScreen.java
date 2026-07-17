package com.bikininjas.corelib.config.client;

import com.bikininjas.corelib.config.BikiniConfigRegistry;
import com.bikininjas.corelib.config.ConfigOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game GUI for managing Bikini mod configurations.
 * Shows mod list on the left, options for the selected mod on the right.
 * Scrolls vertically if there are too many options.
 */
public class BikiniConfigScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int EDIT_BOX_WIDTH = 60;
    private static final int SCROLL_SPEED = 10;

    private final List<String> modIds;
    private final List<List<ConfigOption>> modOptions;
    private int selectedModIndex;
    private int scrollOffset;

    public BikiniConfigScreen() {
        super(Component.literal("Bikini Config"));
        this.modIds = new ArrayList<>(BikiniConfigRegistry.getRegisteredMods());
        this.modOptions = new ArrayList<>();
        for (var modId : modIds) {
            modOptions.add(new ArrayList<>(BikiniConfigRegistry.getOptions(modId)));
        }
    }

    @Override
    protected void init() {
        super.init();
        int y = 40 - scrollOffset;

        for (int i = 0; i < modIds.size(); i++) {
            final int idx = i;
            var name = BikiniConfigRegistry.getModDisplayName(modIds.get(i));
            addRenderableWidget(Button.builder(
                    Component.literal(idx == selectedModIndex ? "§a▶ " + name : "  " + name),
                    btn -> { selectedModIndex = idx; scrollOffset = 0; rebuildWidgets(); }
            ).pos(10, y).size(BUTTON_WIDTH, BUTTON_HEIGHT).build());
            y += BUTTON_HEIGHT + 4;
        }

        if (!modIds.isEmpty() && selectedModIndex < modOptions.size()) {
            y = 40 - scrollOffset;
            int i = 0;
            for (var option : modOptions.get(selectedModIndex)) {
                if (y + BUTTON_HEIGHT > this.height - 40) break; // off-screen
                addOptionWidget(option, y);
                y += BUTTON_HEIGHT + 4;
                i++;
            }

            // Scroll buttons if needed
            if (scrollOffset > 0) {
                addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
                    scrollOffset = Math.max(0, scrollOffset - SCROLL_SPEED);
                    rebuildWidgets();
                }).pos(this.width / 2 - 10, 38).size(20, 15).build());
            }
            if (y > this.height - 40) {
                addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
                    scrollOffset += SCROLL_SPEED;
                    rebuildWidgets();
                }).pos(this.width / 2 - 10, this.height - 55).size(20, 15).build());
            }
        }

        addRenderableWidget(Button.builder(
                Component.literal("Close"), btn -> onClose()
        ).pos(this.width / 2 - 50, this.height - 28).size(100, BUTTON_HEIGHT).build());
    }

    private void addOptionWidget(ConfigOption option, int y) {
        switch (option.type()) {
            case BOOL, ENUM -> {
                var label = option.displayName().copy().append(": ").append(String.valueOf(option.currentValue()));
                addRenderableWidget(Button.builder(label, b -> {
                    Object newValue = toggleValue(option);
                    BikiniConfigRegistry.updateValue(option.modId(), option.key(), newValue);
                    rebuildWidgets();
                }).pos(this.width / 2 + 10, y).size(BUTTON_WIDTH, BUTTON_HEIGHT).build());
            }
            case INT, FLOAT -> {
                var label = option.displayName().copy().append(": ");
                int labelX = this.width / 2 + 10;
                int editX = labelX + font.width(label.getString()) + 4;
                var editBox = new EditBox(font, editX, y, EDIT_BOX_WIDTH, BUTTON_HEIGHT, Component.empty());
                editBox.setValue(String.valueOf(option.currentValue()));
                editBox.setResponder(text -> {
                    try {
                        Number val = option.type() == ConfigOption.OptionType.INT
                                ? Integer.parseInt(text) : Float.parseFloat(text);
                        BikiniConfigRegistry.updateValue(option.modId(), option.key(), val);
                    } catch (NumberFormatException ignored) {
                    }
                });
                addRenderableWidget(editBox);
            }
        }
    }

    private Object toggleValue(ConfigOption option) {
        return switch (option.type()) {
            case BOOL -> !(boolean) option.currentValue();
            case ENUM -> {
                var values = option.enumValues();
                var current = (String) option.currentValue();
                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(current)) {
                        yield values[(i + 1) % values.length];
                    }
                }
                yield values[0];
            }
            default -> option.currentValue();
        };
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        gui.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        gui.drawString(font, "§6§lBikini Mods", 10, 25, 0xFFFFFF);
        if (!modIds.isEmpty() && selectedModIndex < modIds.size()) {
            gui.drawString(font, "§6§l" + BikiniConfigRegistry.getModDisplayName(modIds.get(selectedModIndex)),
                    width / 2 + 10, 25, 0xFFFFFF);
            // Draw labels for INT/FLOAT options (EditBox doesn't show name)
            int idx = 0;
            for (var option : modOptions.get(selectedModIndex)) {
                int y = 40 + idx * (BUTTON_HEIGHT + 4) - scrollOffset;
                if (y + BUTTON_HEIGHT > this.height - 40) break;
                if (y < 35) { idx++; continue; }
                if (option.type() == ConfigOption.OptionType.INT
                        || option.type() == ConfigOption.OptionType.FLOAT) {
                    gui.drawString(font, option.displayName().getString() + ":",
                            width / 2 + 10, y + 5, 0xFFFFFF);
                }
                idx++;
            }
        }
    }

    @Override
    public void onClose() {
        BikiniConfigRegistry.saveConfig();
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
