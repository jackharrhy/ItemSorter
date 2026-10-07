package tollenaar.stephen.ItemSorter.Shared;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FilterConfigTest {
    @Test void webChoicesSurvivePortableBookRoundTripAndRejectOtherMaterials() {
        var filter = FilterConfig.fromForm(Map.of("input_stone", List.of("stone"), "strict_mode", List.of("on")));
        var restored = FilterConfig.decode(filter.encode());
        assertTrue(restored.strictMode());
        assertTrue(restored.allows("stone", Set.of(), List.of()));
        assertFalse(restored.allows("dirt", Set.of(), List.of()));
        assertEquals(List.of("stone"), restored.toItems().getItems());
    }
    @Test void ratioAndMetadataFiltersArePreserved() {
        var filter = FilterConfig.fromForm(Map.of("enchantment_sharpness", List.of("sharpness"),
                "potion_speed", List.of("speed"), "strict_mode", List.of("on"),
                "junction_ratio", List.of("on"), "firstRatio", List.of("2"), "secondRatio", List.of("3")));
        assertEquals(2, filter.firstRatio());
        assertEquals(3, FilterConfig.decode(filter.encode()).secondRatio());
        assertTrue(filter.allows("diamond_sword", Set.of("sharpness"), List.of()));
        assertFalse(filter.allows("diamond_sword", Set.of("sharpness", "smite"), List.of()));
        assertTrue(filter.allows("potion", Set.of(), List.of("SPEED")));
        assertFalse(filter.allows("potion", Set.of(), List.of("slowness")));
        assertThrows(IllegalArgumentException.class, () -> FilterConfig.fromForm(Map.of("junction_ratio", List.of("on"), "firstRatio", List.of("0"), "secondRatio", List.of("3"))));
    }
    @Test void damagedOrUnsupportedPortableMetadataIsRejectedWithoutCrashingHoppers() {
        for (String value : List.of("null", "not-json", "{}", "{\"version\":2,\"items\":[],\"enchantments\":[],\"potions\":[]}",
                "{\"version\":1,\"items\":[],\"enchantments\":[],\"potions\":[],\"firstRatio\":0,\"secondRatio\":1}")) {
            assertThrows(IllegalArgumentException.class, () -> FilterConfig.decode(value));
        }
    }
    @Test void missingJunctionValuesRejectTheRequestRatherThanThrowingNullPointer() {
        assertThrows(IllegalArgumentException.class, () -> FilterConfig.fromForm(Map.of("junction_ratio", List.of("on"))));
    }
}
