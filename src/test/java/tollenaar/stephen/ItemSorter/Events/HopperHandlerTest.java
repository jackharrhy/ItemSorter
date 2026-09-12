package tollenaar.stephen.ItemSorter.Events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;

import tollenaar.stephen.ItemSorter.Core.Database;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import tollenaar.stephen.ItemSorter.Util.Server.Frame;

/**
 * Drives {@link HopperHandler} with real MockBukkit hoppers and a mocked
 * database. The configured hopper is looked up from the block below the source.
 */
class HopperHandlerTest {

    private WorldMock world;
    private Database database;
    private HopperHandler handler;
    private final List<Integer> books = new ArrayList<>();

    // Public so the Oracle Java extension doesn't report these as unused: it only
    // treats runnable @Test methods as used, not JUnit lifecycle methods.
    @BeforeEach
    public void setUp() {
        world = MockBukkit.mock().addSimpleWorld("world");
        database = mock(Database.class);
        handler = new HopperHandler(database);
    }

    @AfterEach
    public void tearDown() {
        books.forEach(Book::removeBook);
        MockBukkit.unmock();
    }

    private Block hopper(int x, int y, int z, BlockFace facing, boolean enabled) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.HOPPER);
        org.bukkit.block.data.type.Hopper data = (org.bukkit.block.data.type.Hopper) block.getBlockData();
        data.setFacing(facing);
        data.setEnabled(enabled);
        block.setBlockData(data);
        return block;
    }

    // A real server reports a container inventory at its block; MockBukkit reports
    // the world spawn instead. The handler depends on the real behaviour.
    private static Inventory inventory(Block block) {
        Inventory inventory = spy(((org.bukkit.block.Hopper) block.getState()).getInventory());
        doReturn(block.getLocation()).when(inventory).getLocation();
        return inventory;
    }

    /** Records the hopper in the mocked database, attached to an item frame. */
    private void saveHopper(Block block, int hopperId, int frameId) {
        Frame frame = mock(Frame.class);
        when(frame.getId()).thenReturn(frameId);
        when(database.getSavedHopperByLocation(block.getLocation(), "id")).thenReturn(hopperId);
        when(database.getSavedItemFrameByHopperID(hopperId, "id")).thenReturn(frame);
    }

    private Book book(int frameId) {
        books.add(frameId);
        return new Book(frameId);
    }

    private static InventoryMoveItemEvent move(Block from, Material item, Block to) {
        return new InventoryMoveItemEvent(inventory(from), new ItemStack(item), inventory(to), true);
    }

    @Test
    void itemOutsideTheFilterIsCancelled() {
        Block source = hopper(0, 64, 0, BlockFace.DOWN, true);
        hopper(0, 63, 0, BlockFace.DOWN, true);
        Block destination = hopper(1, 64, 0, BlockFace.DOWN, true);
        saveHopper(source.getRelative(BlockFace.DOWN), 1, 10);
        saveHopper(destination, 2, 20);
        book(10).addInputConfig(Material.DIRT);

        InventoryMoveItemEvent event = move(source, Material.STONE, destination);
        handler.onHopperInputEvent(event);

        assertTrue(event.isCancelled());
    }

    // Bug: handleJunction cast the Block itself to hopper block data, so this
    // threw ClassCastException instead of honouring the disabled hopper.
    @Test
    void junctionOutOfADisabledHopperIsCancelled() {
        Block source = hopper(0, 64, 0, BlockFace.EAST, false);
        hopper(0, 63, 0, BlockFace.DOWN, true);
        Block destination = hopper(1, 64, 0, BlockFace.DOWN, true);
        saveHopper(source.getRelative(BlockFace.DOWN), 1, 10);
        book(10);

        InventoryMoveItemEvent event = move(source, Material.STONE, destination);
        assertDoesNotThrow(() -> handler.onHopperInputEvent(event));

        assertTrue(event.isCancelled());
    }

    @Test
    void junctionOutOfAnEnabledHopperIsAllowed() {
        Block source = hopper(0, 64, 0, BlockFace.EAST, true);
        hopper(0, 63, 0, BlockFace.DOWN, true);
        Block destination = hopper(1, 64, 0, BlockFace.DOWN, true);
        saveHopper(source.getRelative(BlockFace.DOWN), 1, 10);
        book(10);

        InventoryMoveItemEvent event = move(source, Material.STONE, destination);
        assertDoesNotThrow(() -> handler.onHopperInputEvent(event));

        assertFalse(event.isCancelled());
        verify(database).getSavedItemFrameByHopperID(1, "id");
        verify(database, never()).getSavedHopperByLocation(destination.getLocation(), "id");
    }

    // Bug: a linked hopper whose frame has no loaded book made
    // handleLinkedHopperLogic dereference null.
    @Test
    void linkedHopperWithoutABookCancelsInsteadOfThrowing() {
        Block source = hopper(0, 64, 0, BlockFace.DOWN, true);
        Block below = hopper(0, 63, 0, BlockFace.DOWN, true);
        Block twoBelow = hopper(0, 62, 0, BlockFace.DOWN, true);
        Block destination = hopper(1, 64, 0, BlockFace.DOWN, true);
        saveHopper(below, 1, 10);
        saveHopper(destination, 2, 20);
        saveHopper(twoBelow, 3, 30);
        Book filter = book(10);
        filter.addInputConfig(Material.DIRT);
        filter.setLinkedBelow(true);

        InventoryMoveItemEvent event = move(source, Material.STONE, destination);
        assertDoesNotThrow(() -> handler.onHopperInputEvent(event));

        assertTrue(event.isCancelled());
        verify(database).getSavedItemFrameByHopperID(3, "id");
    }

    // Bug: reverseRatioStep dereferenced the destination's book without
    // checking that one was loaded.
    @Test
    void allowedItemIntoAHopperWithoutABookDoesNotThrow() {
        Block source = hopper(0, 64, 0, BlockFace.DOWN, true);
        hopper(0, 63, 0, BlockFace.DOWN, true);
        Block destination = hopper(1, 64, 0, BlockFace.DOWN, true);
        saveHopper(source.getRelative(BlockFace.DOWN), 1, 10);
        saveHopper(destination, 2, 20);
        book(10);

        InventoryMoveItemEvent event = move(source, Material.STONE, destination);
        assertDoesNotThrow(() -> handler.onHopperInputEvent(event));

        assertFalse(event.isCancelled());
        verify(database, times(2)).getSavedItemFrameByHopperID(2, "id");
    }
}
