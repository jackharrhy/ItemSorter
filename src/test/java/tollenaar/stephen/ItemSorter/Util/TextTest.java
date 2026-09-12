package tollenaar.stephen.ItemSorter.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;

class TextTest {

    @Test
    void capitalizeFullyTitleCasesEachWord() {
        assertEquals("Stone Brick Slab", Text.capitalizeFully("stone brick slab"));
        assertEquals("Netherite Sword", Text.capitalizeFully("NETHERITE SWORD"));
        assertEquals("  Multiple   Spaces  ", Text.capitalizeFully("  multiple   spaces  "));
    }

    @Test
    void capitalizeFullyPassesThroughNullAndEmpty() {
        assertNull(Text.capitalizeFully(null));
        assertEquals("", Text.capitalizeFully(""));
    }

    @Test
    void legacyTextSurvivesAComponentRoundTrip() {
        String legacy = "\u00a7aHello \u00a7lWorld";
        assertEquals(legacy, Text.legacy(Text.legacy(legacy)));
    }

    @Test
    void legacyListConvertsEveryLine() {
        List<Component> lines = Text.legacy(List.of("first", "second"));

        assertEquals(2, lines.size());
        assertEquals("first", Text.legacy(lines.get(0)));
        assertEquals("second", Text.legacy(lines.get(1)));
    }
}
