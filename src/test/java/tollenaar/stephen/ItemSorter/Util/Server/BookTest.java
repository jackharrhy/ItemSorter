package tollenaar.stephen.ItemSorter.Util.Server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class BookTest {

    private static final int FRAME_ID = 100;

    private ServerMock server;

    // Public so the Oracle Java extension doesn't report these as unused: it only
    // treats runnable @Test methods as used, not JUnit lifecycle methods.
    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    public void tearDown() {
        Book.removeBook(FRAME_ID);
        MockBukkit.unmock();
    }

    private Inventory hopperInventory() {
        return server.createInventory(null, InventoryType.HOPPER);
    }

    @Test
    void emptyConfigurationAllowsAnyItem() {
        assertTrue(new Book(FRAME_ID).allowItem(hopperInventory(), new ItemStack(Material.STONE)));
    }

    @Test
    void inputConfigurationOnlyAllowsListedMaterials() {
        Book book = new Book(FRAME_ID);
        book.addInputConfig(Material.DIRT);

        assertTrue(book.allowItem(hopperInventory(), new ItemStack(Material.DIRT)));
        assertFalse(book.allowItem(hopperInventory(), new ItemStack(Material.STONE)));
    }

    @Test
    void preventOverflowRejectsInventoriesWithoutABlock() {
        Book book = new Book(FRAME_ID);
        book.setPreventOverflow(true);

        // On a real server an inventory that isn't a placed block has no location,
        // so the next container can't be checked. MockBukkit never returns null
        // here, hence the mock.
        assertFalse(book.allowItem(mock(Inventory.class), new ItemStack(Material.STONE)));
    }

    @Test
    void potionSavedUnderItsOldUppercaseNameStillMatches() throws Exception {
        Book book = Book.fromString(BookFixtures.SAVED_CONFIGURATION);

        assertTrue(book.hasPotion(List.of(new PotionEffect(PotionEffectType.SPEED, 200, 0))));
        assertFalse(book.hasPotion(List.of(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0))));
    }
}
