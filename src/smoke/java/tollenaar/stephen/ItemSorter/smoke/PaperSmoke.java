package tollenaar.stephen.ItemSorter.smoke;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import tollenaar.stephen.ItemSorter.Core.*;
import java.util.*;
public final class PaperSmoke extends JavaPlugin {
    @Override public void onEnable() {
        getServer().getScheduler().runTask(this, () -> getServer().getWorlds().getFirst().getEntitiesByClass(ItemFrame.class).forEach(org.bukkit.entity.Entity::remove));
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                var plugin = (ItemSorter) getServer().getPluginManager().getPlugin("ItemSorter");
                var world = getServer().getWorlds().getFirst();
                world.getEntitiesByClass(ItemFrame.class).forEach(org.bukkit.entity.Entity::remove);
                world.setChunkForceLoaded(0, 0, true);
                for (int x = 6; x <= 10; x++) for (int y = 98; y <= 103; y++) for (int z = 6; z <= 10; z++) world.getBlockAt(x,y,z).setType(Material.AIR);
                var block = world.getBlockAt(8, 100, 8);
                block.setType(Material.HOPPER);
                var frame = world.spawn(new Location(world, 8.5, 100.5, 7.5), ItemFrame.class);
                frame.setFacingDirection(BlockFace.NORTH, true);
                plugin.getDatabase().saveHoppers(block.getLocation(), frame.getLocation());
                int id = (int) plugin.getDatabase().getSavedItemFrameByLocation(frame.getLocation(), "id");
                new HopperConfiguring(plugin).configureHopper(id, UUID.randomUUID(), Map.of("input_stone", List.of("stone")));
                world.getBlockAt(8, 101, 8).setType(Material.CHEST);
                var source = ((org.bukkit.block.Chest) world.getBlockAt(8, 101, 8).getState()).getInventory();
                source.setItem(0, new ItemStack(Material.DIRT, 8));
                source.setItem(1, new ItemStack(Material.STONE, 8));
                var key = new NamespacedKey("itemsorter", "itemsorter");
                String stored = frame.getItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (!stored.startsWith("{")) throw new AssertionError("New Paper book is not portable");
                var book = tollenaar.stephen.ItemSorter.Util.Server.Book.fromString(stored);
                var inventory = ((org.bukkit.block.Hopper) block.getState()).getInventory();
                if (!book.allowItem(inventory, new ItemStack(Material.STONE)) || book.allowItem(inventory, new ItemStack(Material.DIRT))) throw new AssertionError("Paper portable filter failed");
                System.out.println("ITEMSORTER_PAPER_SMOKE_FILTER_PASS");
                getServer().getScheduler().runTaskLater(this, () -> {
                    try {
                        if (source.getItem(0).getAmount() != 8 || inventory.contains(Material.DIRT)) throw new AssertionError("Actual Paper hopper accepted dirt");
                        source.setItem(0, null);
                        getServer().getScheduler().runTaskLater(this, () -> {
                            if (!inventory.contains(Material.STONE)) System.out.println("ITEMSORTER_PAPER_SMOKE_FAIL");
                            else System.out.println("ITEMSORTER_PAPER_SMOKE_TRANSFER_PASS");
                            getServer().shutdown();
                        }, 40L);
                        Thread.startVirtualThread(() -> {
                            try (var client = java.net.http.HttpClient.newHttpClient()) {
                                for (String route : List.of("HopperConfig", "editHopperConfig")) {
                                    var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:8766/" + route)).GET().build();
                                    if (client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode() != 403) throw new AssertionError("Unauthenticated Paper GET allowed");
                                }
                                var post = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:8766/postHopperConfig")).POST(java.net.http.HttpRequest.BodyPublishers.ofString("userCode=forged&frameID=" + id + "&input_dirt=dirt")).header("Content-Type", "application/x-www-form-urlencoded").build();
                                if (client.send(post, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode() != 403) throw new AssertionError("Unauthenticated Paper POST allowed");
                                System.out.println("ITEMSORTER_PAPER_SMOKE_HTTP_AUTH_PASS");
                            } catch (Throwable failure) { failure.printStackTrace(); System.out.println("ITEMSORTER_PAPER_SMOKE_FAIL"); }

                        });
                    } catch (Throwable failure) { failure.printStackTrace(); System.out.println("ITEMSORTER_PAPER_SMOKE_FAIL"); getServer().shutdown(); }
                }, 40L);
            } catch (Throwable failure) { failure.printStackTrace(); System.out.println("ITEMSORTER_PAPER_SMOKE_FAIL"); getServer().shutdown(); }
        }, 40L);
    }
}
