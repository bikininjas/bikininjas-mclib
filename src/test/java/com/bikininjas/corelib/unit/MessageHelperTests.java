package com.bikininjas.corelib.unit;

import com.bikininjas.corelib.message.MessageHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit5 unit tests for {@link MessageHelper}.
 * <p>
 * These tests cover only the logic that does not require a live Minecraft
 * server: structural guarantees (final class, private constructor), the
 * {@link MessageHelper#text} helper, the color helpers, and the legacy-code
 * parser {@link MessageHelper#format(String)}.
 * <p>
 * Methods that touch {@code ServerPlayer} or {@code MinecraftServer}
 * (chat/title/actionBar/broadcast) are not unit-tested here — they require a
 * running game and belong in NeoForge {@code GameTest}s. They are marked
 * {@link Disabled} with a note rather than omitted, so the intent is explicit.
 */
class MessageHelperTests {

    // ------------------------------------------------------------------
    // Structure
    // ------------------------------------------------------------------

    @Test
    void classIsFinal() {
        assertTrue(Modifier.isFinal(MessageHelper.class.getModifiers()),
                "MessageHelper must be a final utility class");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<MessageHelper> ctor = MessageHelper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                "MessageHelper constructor must be private");
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance(),
                "Private constructor should be invokable (no side effects)");
    }

    // ------------------------------------------------------------------
    // text()
    // ------------------------------------------------------------------

    @Test
    void textReturnsLiteralComponent() {
        Component c = MessageHelper.text("hello");
        assertEquals("hello", c.getString());
        assertTrue(c.getSiblings().isEmpty(), "literal component has no siblings");
    }

    @Test
    void textNullThrowsNpe() {
        assertThrows(NullPointerException.class, () -> MessageHelper.text(null));
    }

    // ------------------------------------------------------------------
    // Color helpers
    // ------------------------------------------------------------------

    @Test
    void redReturnsRedComponent() {
        Component c = MessageHelper.red("err");
        assertEquals("err", c.getString());
        assertColor(c, ChatFormatting.RED);
    }

    @Test
    void greenReturnsGreenComponent() {
        Component c = MessageHelper.green("ok");
        assertColor(c, ChatFormatting.GREEN);
    }

    @Test
    void blueReturnsBlueComponent() {
        assertColor(MessageHelper.blue("x"), ChatFormatting.BLUE);
    }

    @Test
    void goldReturnsGoldComponent() {
        assertColor(MessageHelper.gold("x"), ChatFormatting.GOLD);
    }

    @Test
    void grayReturnsGrayComponent() {
        assertColor(MessageHelper.gray("x"), ChatFormatting.GRAY);
    }

    @Test
    void whiteReturnsWhiteComponent() {
        assertColor(MessageHelper.white("x"), ChatFormatting.WHITE);
    }

    @Test
    void aquaReturnsAquaComponent() {
        assertColor(MessageHelper.aqua("x"), ChatFormatting.AQUA);
    }

    @Test
    void yellowReturnsYellowComponent() {
        assertColor(MessageHelper.yellow("x"), ChatFormatting.YELLOW);
    }

    @Test
    void lightPurpleReturnsLightPurpleComponent() {
        assertColor(MessageHelper.lightPurple("x"), ChatFormatting.LIGHT_PURPLE);
    }

    @Test
    void darkRedReturnsDarkRedComponent() {
        assertColor(MessageHelper.darkRed("x"), ChatFormatting.DARK_RED);
    }

    @Test
    void darkGreenReturnsDarkGreenComponent() {
        assertColor(MessageHelper.darkGreen("x"), ChatFormatting.DARK_GREEN);
    }

    @Test
    void darkBlueReturnsDarkBlueComponent() {
        assertColor(MessageHelper.darkBlue("x"), ChatFormatting.DARK_BLUE);
    }

    @Test
    void darkGrayReturnsDarkGrayComponent() {
        assertColor(MessageHelper.darkGray("x"), ChatFormatting.DARK_GRAY);
    }

    @Test
    void blackReturnsBlackComponent() {
        assertColor(MessageHelper.black("x"), ChatFormatting.BLACK);
    }

    @Test
    void colorHelpersNullThrowsNpe() {
        assertAll("every color helper rejects null",
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.red(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.green(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.blue(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.gold(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.gray(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.white(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.aqua(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.yellow(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.lightPurple(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.darkRed(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.darkGreen(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.darkBlue(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.darkGray(null)),
                () -> assertThrows(NullPointerException.class, () -> MessageHelper.black(null))
        );
    }

    // ------------------------------------------------------------------
    // format()
    // ------------------------------------------------------------------

    @Test
    void formatEmptyString() {
        Component c = MessageHelper.format("");
        assertEquals("", c.getString());
    }

    @Test
    void formatNoColorCodes() {
        Component c = MessageHelper.format("plain text");
        assertEquals("plain text", c.getString());
    }

    @Test
    void formatRedCodeProducesRed() {
        Component c = MessageHelper.format("&cred");
        assertEquals("red", c.getString());
        assertColor(c, ChatFormatting.RED);
    }

    @Test
    void formatGreenCodeProducesGreen() {
        Component c = MessageHelper.format("&agreen");
        assertEquals("green", c.getString());
        assertColor(c, ChatFormatting.GREEN);
    }

    @Test
    void formatMultipleCodes() {
        // &4 dark red, then &l bold decoration applied on top.
        Component c = MessageHelper.format("&4&lalert");
        assertEquals("alert", c.getString());
        // The whole string is a single styled segment (first sibling).
        List<Component> siblings = c.getSiblings();
        assertEquals(1, siblings.size(), "one styled segment expected");
        Component segment = siblings.get(0);
        assertColor(segment, ChatFormatting.DARK_RED);
        assertTrue(segment.getStyle().isBold(), "bold decoration should be set");
    }

    @Test
    void formatResetClearsStyle() {
        // &c red, then &r reset before "plain" -> plain has no color.
        Component c = MessageHelper.format("&cred&rplain");
        assertEquals("redplain", c.getString());
        // Two styled segments: "red" (red) and "plain" (reset / no color).
        List<Component> siblings = c.getSiblings();
        assertEquals(2, siblings.size(), "two styled segments expected");
        assertColor(siblings.get(0), ChatFormatting.RED);
        Component plain = siblings.get(1);
        assertNull(plain.getStyle().getColor(), "reset segment has no color");
    }

    @Test
    void formatEscapeProducesLiteralAmpersand() {
        Component c = MessageHelper.format("a && b");
        assertEquals("a & b", c.getString());
    }

    @Test
    void formatUnknownCodeLeftAsLiteral() {
        // &z is not a valid code -> kept as literal "&z".
        Component c = MessageHelper.format("&zfoo");
        assertEquals("&zfoo", c.getString());
    }

    @Test
    void formatNullThrowsNpe() {
        assertThrows(NullPointerException.class, () -> MessageHelper.format(null));
    }

    // ------------------------------------------------------------------
    // Runtime-dependent methods (require Minecraft server) — documented,
    // not executed in unit scope.
    // ------------------------------------------------------------------

    @Test
    @Disabled("requires Minecraft runtime: sends a system message to a ServerPlayer")
    void chatRequiresRuntime() {
        // MessageHelper.chat(player, "hi");
    }

    @Test
    @Disabled("requires Minecraft runtime: sends title packets to a ServerPlayer")
    void titleRequiresRuntime() {
        // MessageHelper.title(player, "T", "S");
    }

    @Test
    @Disabled("requires Minecraft runtime: displays action-bar on a ServerPlayer")
    void actionBarRequiresRuntime() {
        // MessageHelper.actionBar(player, "bar");
    }

    @Test
    @Disabled("requires Minecraft runtime: broadcasts via MinecraftServer.getPlayerList()")
    void broadcastRequiresRuntime() {
        // MessageHelper.broadcastChat("hi", server);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Extract the color of the first styled segment of a component, compared by
     * its RGB value against a {@link ChatFormatting} constant.
     * <p>
     * In Minecraft 1.21.1 {@link Style#getColor()} returns a {@link TextColor}
     * that is not {@code equals} to the originating {@link ChatFormatting}, so
     * we compare the underlying RGB {@code getValue()} instead.
     */
    private static int colorValueOf(Component c) {
        Style style = c.getStyle();
        if (style.getColor() != null) {
            return style.getColor().getValue();
        }
        for (Component sibling : c.getSiblings()) {
            if (sibling.getStyle().getColor() != null) {
                return sibling.getStyle().getColor().getValue();
            }
        }
        return -1;
    }

    /**
     * Assert that the first colored segment of {@code c} matches the RGB value
     * of the given {@link ChatFormatting} constant.
     */
    private static void assertColor(Component c, ChatFormatting expected) {
        int expectedRgb = TextColor.fromLegacyFormat(expected).getValue();
        assertEquals(expectedRgb, colorValueOf(c),
                () -> "expected color " + expected + " but got " + colorValueOf(c));
    }
}
