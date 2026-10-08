package tollenaar.stephen.ItemSorter.Core;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import tollenaar.stephen.ItemSorter.Shared.FilterConfig;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import tollenaar.stephen.ItemSorter.Util.Server.EditConfig;
import tollenaar.stephen.ItemSorter.Util.Server.Frame;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HopperConfiguringTest {
    private static final int FRAME = 907;
    private static final NamespacedKey KEY = new NamespacedKey("itemsorter", "itemsorter");
    private ServerMock server;
    private Database database;
    private org.mockito.MockedStatic<Book> books;
    private HopperConfiguring editor;

    @BeforeEach void setup() {
        server = MockBukkit.mock();
        // Exercise parsing and saves; MockBukkit cannot render Adventure book pages.
        books = mockStatic(Book.class, CALLS_REAL_METHODS);
        books.when(() -> Book.fromPortable(any())).thenAnswer(call -> {
            Book book = spy((Book) call.callRealMethod());
            doAnswer(ignored -> writtenBook(book)).when(book).toItem(any());
            return book;
        });
        database = mock(Database.class);
        var plugin = mock(ItemSorter.class);
        when(plugin.namespace()).thenReturn("itemsorter");
        when(plugin.getDatabase()).thenReturn(database);
        editor = new HopperConfiguring(plugin);
    }

    @AfterEach void cleanup() {
        Book.removeBook(FRAME);
        Book.removeBook(-1);
        books.close();
        MockBukkit.unmock();
    }

    @Test void paperAliasesAndUnknownChoicesKeepTheirLegacyMeaning() {
        var form = Map.of("legacy_input_1", List.of("minecraft:STONE"),
                "input_2", List.of("not_a_material"),
                "old_enchantment_1", List.of("SHARPNESS"),
                "enchantment_2", List.of("not_an_enchantment"),
                "old_potion_1", List.of("SPEED"), "potion_2", List.of("not_an_effect"),
                "strict_mode", List.of("on"), "linked_below", List.of("on"),
                "prevent_overflow", List.of("on"));
        var filter = HopperConfiguring.parseBook(form).toPortable();
        assertEquals(List.of("stone"), filter.items());
        assertEquals(List.of("sharpness"), filter.enchantments());
        assertEquals(List.of("speed"), filter.potions());
        assertTrue(filter.strictMode());
        assertTrue(filter.linkedBelow());
        assertTrue(filter.preventOverflow());
    }

    @Test void invalidCreateDoesNotReplaceTheRegisteredBookOrFrame() {
        var previous = new Book(FRAME);
        previous.addInputConfig(Material.DIRT);
        var frame = mock(Frame.class);
        var entity = mock(ItemFrame.class);
        when(frame.getEntityFrame()).thenReturn(entity);
        try (var frames = mockStatic(Frame.class)) {
            frames.when(() -> Frame.getFRAME(FRAME)).thenReturn(frame);
            assertThrows(IllegalArgumentException.class,
                    () -> editor.configureHopper(FRAME, UUID.randomUUID(), invalidRatio()));
            assertSame(previous, Book.getBook(FRAME));
            verifyNoInteractions(entity);
        }
    }

    @Test void invalidEditLeavesTheHeldBookAndActiveFilterIntact() {
        var player = server.addPlayer();
        var previous = new Book(FRAME);
        previous.addInputConfig(Material.DIRT);
        previous.setStrictMode(true);
        var held = writtenBook(previous);
        player.getInventory().setItemInMainHand(held);
        String value = previous.toPortable().encode();
        when(database.getSavedEdit(player.getUniqueId())).thenReturn(new EditConfig(value, false, EquipmentSlot.HAND));
        assertThrows(IllegalArgumentException.class,
                () -> editor.editConfigureHopper(player.getUniqueId(), value, invalidRatio()));
        assertSame(previous, Book.getBook(FRAME));
        assertEquals(value, previous.toPortable().encode());
        assertEquals(held, player.getInventory().getItemInMainHand());
        verify(database, never()).deleteEditHopper(any());
    }

    @Test void successfulHeldEditWritesPortableOptionsAndPreservesStackAmount() throws Exception {
        var player = server.addPlayer();
        var previous = new Book(FRAME);
        previous.addInputConfig(Material.DIRT);
        previous.setStrictMode(true);
        var held = writtenBook(previous);
        held.setAmount(2);
        player.getInventory().setItemInMainHand(held);
        String value = previous.toPortable().encode();
        when(database.getSavedEdit(player.getUniqueId())).thenReturn(new EditConfig(value, false, EquipmentSlot.HAND));
        editor.editConfigureHopper(player.getUniqueId(), value, Map.of("input_1", List.of("STONE"),
                "junction_ratio", List.of("on"), "firstRatio", List.of("2"), "secondRatio", List.of("3")));
        var result = player.getInventory().getItemInMainHand();
        var filter = FilterConfig.decode(result.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING));
        assertEquals(2, result.getAmount());
        assertEquals(List.of("stone"), filter.items());
        assertFalse(filter.strictMode());
        assertEquals(2, filter.firstRatio());
        assertEquals(3, filter.secondRatio());
        assertEquals(filter, Book.getBook(FRAME).toPortable());
        verify(database).deleteEditHopper(player.getUniqueId());
    }

    @Test void changedHeldBookRejectsWithoutMutatingTheRegisteredFilter() {
        var player = server.addPlayer();
        var previous = new Book(FRAME);
        previous.addInputConfig(Material.DIRT);
        String value = previous.toPortable().encode();
        when(database.getSavedEdit(player.getUniqueId())).thenReturn(new EditConfig(value, false, EquipmentSlot.HAND));
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));
        assertThrows(IllegalArgumentException.class, () -> editor.editConfigureHopper(player.getUniqueId(), value,
                Map.of("input_1", List.of("stone"))));
        assertSame(previous, Book.getBook(FRAME));
        assertEquals(value, previous.toPortable().encode());
    }

    private static ItemStack writtenBook(Book book) {
        var item = new ItemStack(Material.WRITTEN_BOOK);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, book.toPortable().encode());
        item.setItemMeta(meta);
        return item;
    }

    private static Map<String, List<String>> invalidRatio() {
        return Map.of("input_1", List.of("stone"), "junction_ratio", List.of("on"),
                "firstRatio", List.of("0"), "secondRatio", List.of("3"));
    }
}
