package tollenaar.stephen.ItemSorter.Util.Server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/** Guards the saved-configuration format. Runs without a server. */
class BookSerializationTest {

    @Test
    void savedConfigurationStillLoads() throws Exception {
        Book book = Book.fromString(BookFixtures.SAVED_CONFIGURATION);

        assertTrue(book.hasInputConfig(Material.STONE));
        assertFalse(book.hasInputConfig(Material.DIRT));
        assertTrue(book.isStrictMode());
        assertTrue(book.isLinkedBelow());
        assertFalse(book.hasPreventOverflow());
        assertTrue(book.hasRatio());
    }

    @Test
    void savedFormatIsUnchanged() throws Exception {
        // Loading and saving again must produce the identical bytes; any change
        // to Book's persisted fields fails here before it can reach a server.
        assertEquals(BookFixtures.SAVED_CONFIGURATION,
                Book.fromString(BookFixtures.SAVED_CONFIGURATION).toString());
    }
}
