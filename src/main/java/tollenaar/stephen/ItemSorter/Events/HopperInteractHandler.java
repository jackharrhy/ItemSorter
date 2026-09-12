package tollenaar.stephen.ItemSorter.Events;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import tollenaar.stephen.ItemSorter.Core.Database;
import tollenaar.stephen.ItemSorter.Core.ItemSorter;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import tollenaar.stephen.ItemSorter.Util.Text;

public class HopperInteractHandler implements Listener {
    private final Database database;
    private final ItemSorter plugin;

	public HopperInteractHandler(ItemSorter plugin) {
		this.plugin = plugin;
		this.database = this.plugin.getDatabase();
	}

	// handling configuring of the frames
	@EventHandler
	public void onHopperInteractEvent(PlayerInteractEntityEvent event) {

		if (event.getRightClicked().getType() == EntityType.ITEM_FRAME
				&& database.hasSavedItemFrame(event.getRightClicked().getLocation())) {

			ItemFrame frame = (ItemFrame) event.getRightClicked();
			int frameID = (int) database.getSavedItemFrameByLocation(event.getRightClicked().getLocation(), "id");

			if (frame.getItem().getType() == Material.AIR) {
				// getting to configure
				if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.WRITABLE_BOOK) {
					database.savePlayer(event.getPlayer().getUniqueId(), frameID);

                    String token = plugin.getWebSessions().issue(event.getPlayer().getUniqueId(), frameID, null);
					String url = plugin.getConfig().getString("URL")
							+ plugin.getConfig().getString("initialPageResponse") + "?token=" + token;

					event.getPlayer().sendMessage(Component.text("Click here to configure the hopper sorting")
							.clickEvent(ClickEvent.openUrl(url)));

					// when replacing a configured book into an item frame
				} else if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.WRITTEN_BOOK) {
					BookMeta meta = (BookMeta) event.getPlayer().getInventory().getItemInMainHand().getItemMeta();
					NamespacedKey key = new NamespacedKey(plugin, "itemsorter");
					PersistentDataContainer container = meta.getPersistentDataContainer();
					if (container.has(key, PersistentDataType.STRING)) {
						String bookValue = container.get(key, PersistentDataType.STRING);
						try {
							Book b = Book.fromString(bookValue);
							b.addSelf(frameID);

						} catch (ClassNotFoundException | IOException e) {
							// no need for logging, if the item is a written
							// book and has lore, but is not
							// from this plugin it can throw this error.
						}
					}
				}
				// forcing the book to open to the player
			} else if (frame.getItem().getType() == Material.WRITTEN_BOOK) {
				BookMeta metaFrame = (BookMeta) frame.getItem().getItemMeta();
				NamespacedKey key = new NamespacedKey(plugin, "itemsorter");
				PersistentDataContainer container = metaFrame.getPersistentDataContainer();
				if (container.has(key, PersistentDataType.STRING)) {
					String bookValue = container.get(key, PersistentDataType.STRING);

					try {
						Player player = event.getPlayer();
						Book book = Book.fromString(bookValue);

						ItemStack replaceItem = new ItemStack(Material.WRITTEN_BOOK);
						BookMeta meta = (BookMeta) replaceItem.getItemMeta();
						meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, book.toString());
						meta.setTitle("HopperConfiguration");
						meta.setAuthor("ItemSorter");

						meta.pages(Text.legacy(book.toPages()));

                        database.savePlayer(player.getUniqueId(), bookValue, true, null);
                        String token = plugin.getWebSessions().issue(player.getUniqueId(), null, bookValue);
						Component editPage = Component.text("To edit the configuration click here.")
								.clickEvent(ClickEvent.openUrl(plugin.getConfig().getString("URL")
										+ plugin.getConfig().getString("editPageResponse") + "?token=" + token));
						meta.addPages(editPage);
						replaceItem.setItemMeta(meta);
						player.openBook(replaceItem);
						event.setCancelled(true);
                    } catch (ClassNotFoundException | IOException e) {
						// no need for logging, if the item is a written book
						// and has lore, but is not
						// from this plugin it can throw this error.
					}
				}
			}
		}
	}

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        plugin.getWebSessions().revoke(event.getPlayer().getUniqueId());
        database.deleteEditHopper(event.getPlayer().getUniqueId());
    }

	@EventHandler
	public void onItemFramePlaceEvent(HangingPlaceEvent event) {
		if (event.getEntity().getType() == EntityType.ITEM_FRAME && event.getBlock().getType() == Material.HOPPER) {
			database.saveHoppers(event.getBlock().getLocation(), event.getEntity().getLocation());
		}
	}

	// handling the hopper break
	@EventHandler
	public void onHopperBreakEvent(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) {
			database.deleteHopper(event.getBlock().getLocation());
		}
	}

	// handling the item frame break event

	@EventHandler
	public void onFrameBreakEvent(HangingBreakEvent event) {
		if (database.hasSavedItemFrame(event.getEntity().getLocation())) {
			database.deleteFrame(event.getEntity().getLocation());
		}
	}

	// handling for when the player removes an item out of the item frame
	@EventHandler
	public void onItemRemoveEvent(EntityDamageByEntityEvent event) {

		// filtering player and item frame
		if (event.getDamager().getType() == EntityType.PLAYER && event.getEntityType() == EntityType.ITEM_FRAME
				&& database.hasSavedItemFrame(event.getEntity().getLocation())) {

			if (database.hasSavedPlayerWithItemFrame(event.getDamager().getUniqueId(),
					(int) database.getSavedItemFrameByLocation(event.getEntity().getLocation(), "id"))) {
				database.deletePlayerWithFrame(event.getDamager().getUniqueId(),
						(int) database.getSavedItemFrameByLocation(event.getEntity().getLocation(), "id"));
			}

			Book.removeBook((int) database.getSavedItemFrameByLocation(event.getEntity().getLocation(), "id"));
		}
	}

	@EventHandler
	public void onHopperEditEvent(PlayerInteractEvent event) {
		ItemStack item = event.getItem();
		// Physical interactions, like stepping on a pressure plate, have no hand.
		EquipmentSlot hand = event.getHand();
		if (item != null && hand != null && item.getType() == Material.WRITTEN_BOOK) {
			BookMeta meta = (BookMeta) item.getItemMeta();
			NamespacedKey key = new NamespacedKey(plugin, "itemsorter");
			PersistentDataContainer container = meta.getPersistentDataContainer();
			if (container.has(key, PersistentDataType.STRING)) {
				String bookValue = container.get(key, PersistentDataType.STRING);
				try {
					Player player = event.getPlayer();
					Book book = Book.fromString(bookValue);
					meta.pages(Text.legacy(book.toPages()));

                    database.savePlayer(player.getUniqueId(), bookValue, false, hand);
                    String token = plugin.getWebSessions().issue(player.getUniqueId(), null, bookValue);
					Component editPage = Component.text("To edit the configuration click here.")
							.clickEvent(ClickEvent.openUrl(plugin.getConfig().getString("URL")
									+ plugin.getConfig().getString("editPageResponse") + "?token=" + token));

					meta.addPages(editPage);
					item.setItemMeta(meta);
					player.getInventory().setItem(hand, item);
				} catch (ClassNotFoundException | IOException e) {
					// no need for logging, if the item is a written book and h
					// lore, but is n
					// from this plugin it can throw this erro
				}

			}

		}
	}
}
