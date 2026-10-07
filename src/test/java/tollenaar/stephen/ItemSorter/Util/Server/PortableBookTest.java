package tollenaar.stephen.ItemSorter.Util.Server;
import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import static org.junit.jupiter.api.Assertions.*;
class PortableBookTest {
    @Test void legacyBookCanMoveToFabricAndReturnWithoutLosingItsFilters() throws Exception {
        var old = Book.fromString(BookFixtures.SAVED_CONFIGURATION);
        var portable = old.toPortable();
        assertEquals(2, portable.firstRatio());
        assertTrue(portable.strictMode());
        assertTrue(portable.linkedBelow());
        assertEquals(java.util.List.of("stone"), portable.items());
        var returned = Book.fromString(portable.encode());
        assertTrue(returned.hasInputConfig(Material.STONE));
        assertFalse(returned.hasInputConfig(Material.DIRT));
        assertTrue(returned.isStrictMode());
        assertTrue(returned.isLinkedBelow());
        assertEquals(old.toItems().getPotions(), returned.toItems().getPotions());
    }
    @Test void portableBookIdentityFindsItsRegisteredFrame() {
        var book = new Book(701);
        book.addInputConfig(Material.STONE);
        try { assertSame(book, Book.getBook(book.toPortable().encode())); }
        finally { Book.removeBook(701); }
    }
    @Test void paperMetadataConversionKeepsTheExistingKeyAndRejectsUnrelatedBooks() throws Exception {
        org.mockbukkit.mockbukkit.MockBukkit.mock();
        try {
            var key = new org.bukkit.NamespacedKey("itemsorter", "itemsorter");
            var item = new org.bukkit.inventory.ItemStack(Material.WRITTEN_BOOK);
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, BookFixtures.SAVED_CONFIGURATION);
            item.setItemMeta(meta);
            assertTrue(Book.makePortable(item, key));
            String stored = item.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
            assertTrue(stored.startsWith("{"));
            assertEquals(java.util.List.of("stone"), tollenaar.stephen.ItemSorter.Shared.FilterConfig.decode(stored).items());
            assertFalse(Book.makePortable(new org.bukkit.inventory.ItemStack(Material.WRITTEN_BOOK), key));
        } finally { org.mockbukkit.mockbukkit.MockBukkit.unmock(); }
    }
}
