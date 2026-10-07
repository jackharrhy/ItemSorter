package tollenaar.stephen.ItemSorter.Util.Server;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tollenaar.stephen.ItemSorter.Commands.CommandsHandler;
import tollenaar.stephen.ItemSorter.Core.ItemSorter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PortableCommandTest {
    @Test void operatorCanConvertHeldLegacyBooksForFabric() {
        var server = MockBukkit.mock();
        try {
            var player = server.addPlayer();
            player.setOp(true);
            var key = new NamespacedKey("itemsorter", "itemsorter");
            var item = new ItemStack(Material.WRITTEN_BOOK);
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, BookFixtures.SAVED_CONFIGURATION);
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            new CommandsHandler(mock(ItemSorter.class)).onCommand(player, mock(org.bukkit.command.Command.class), "itemsorter", new String[]{"portable"});
            assertTrue(player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING).startsWith("{"));
        } finally { MockBukkit.unmock(); }
    }
}
