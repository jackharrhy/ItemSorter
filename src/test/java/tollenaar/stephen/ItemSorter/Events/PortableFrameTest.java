package tollenaar.stephen.ItemSorter.Events;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tollenaar.stephen.ItemSorter.Core.*;
import tollenaar.stephen.ItemSorter.Shared.FilterConfig;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PortableFrameTest {
    @Test void fabricFrameIsRegisteredWhenItsEntitiesLoadOnPaper() throws Exception {
        var server = MockBukkit.mock();
        try {
            var world = server.addSimpleWorld("world");
            var location = new Location(world, 8.5, 100.5, 7.96875);
            world.getBlockAt(8, 100, 8).setType(Material.HOPPER);
            var item = new ItemStack(Material.WRITTEN_BOOK);
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey("itemsorter", "itemsorter"), PersistentDataType.STRING,
                    FilterConfig.fromForm(Map.of("input_stone", List.of("stone"))).encode());
            item.setItemMeta(meta);
            var frame = mock(ItemFrame.class);
            when(frame.getLocation()).thenReturn(location);
            when(frame.getAttachedFace()).thenReturn(BlockFace.SOUTH);
            when(frame.getItem()).thenReturn(item);
            var plugin = mock(ItemSorter.class);
            var database = mock(Database.class);
            when(plugin.getDatabase()).thenReturn(database);
            when(database.getSavedItemFrameByLocation(location, "id")).thenReturn(702);
            new HopperInteractHandler(plugin).onEntitiesLoad(new EntitiesLoadEvent(world.getChunkAt(0, 0), List.of(frame)));
            verify(database).saveHoppers(world.getBlockAt(8, 100, 8).getLocation(), location);
            assertTrue(Book.getBook(702).hasInputConfig(Material.STONE));
        } finally { Book.removeBook(702); MockBukkit.unmock(); }
    }
}
