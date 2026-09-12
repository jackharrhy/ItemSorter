package tollenaar.stephen.ItemSorter.Core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import tollenaar.stephen.ItemSorter.Util.Server.EditConfig;
import tollenaar.stephen.ItemSorter.Util.Server.Frame;
import tollenaar.stephen.ItemSorter.Util.Text;

public class HopperConfiguring {
    private final Database database;
    private final ItemSorter plugin;

	// Registry lookups replacing the deprecated Enchantment.getByKey and
	// PotionEffectType.getByName. Values come from the web form, so fold the
	// case before building a key rather than letting NamespacedKey throw.
	private static Enchantment enchantment(String value) {
		return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
				.get(NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT)));
	}

	private static PotionEffectType potionEffect(String value) {
		return RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT)
				.get(NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT)));
	}

	public HopperConfiguring(ItemSorter plugin) {
		this.plugin = plugin;
		this.database = plugin.getDatabase();
	}

	public void configureHopper(int frameID, UUID player, Map<String, List<String>> formParams)
			throws UnsupportedEncodingException, IllegalArgumentException {
		Frame frame = Frame.getFRAME(frameID);
		ItemFrame fr = frame.getEntityFrame();

		if (fr == null) {
			throw new NullPointerException("Error trying to find the edited config item");
		}

		// loading the book materials
		Book book = new Book(frameID);

		for (String key : formParams.keySet()) {
			String value = formParams.get(key).get(0);
			if (key.contains("input_") && Material.matchMaterial(value) != null) {
				book.addInputConfig(Material.matchMaterial(value));
			} else if (key.contains("enchantment_") && enchantment(value) != null) {
				book.addEnchantment(enchantment(value));
			} else if (key.contains("potion_") && potionEffect(value) != null) {
				book.addPotion(potionEffect(value));
			}
		}
        book.setStrictMode(formParams.keySet().contains("strict_mode"));
        book.setPreventOverflow(formParams.keySet().contains("prevent_overflow"));
        book.setLinkedBelow(formParams.keySet().contains("linked_below"));

        if (formParams.keySet().contains("junction_ratio")) {
			int first = Integer.parseInt(formParams.get("firstRatio").get(0));
			int second = Integer.parseInt(formParams.get("secondRatio").get(0));

			if (first <= 0 || second <= 0) {
				throw new IllegalArgumentException("Can't have a 0 or negative ratio");
			}

			book.setRatio(first, second);
		} else {
			book.emptyRatio();
		}

		ItemStack replaceItem = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) replaceItem.getItemMeta();
		meta.setTitle("HopperConfiguration");
		meta.setAuthor("ItemSorter");

		meta.pages(Text.legacy(book.toPages()));

		// changing the item frame item
		NamespacedKey key = new NamespacedKey(plugin, "itemsorter");
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, book.toString());
		replaceItem.setItemMeta(meta);
		fr.setItem(replaceItem);
	}

	public void editConfigureHopper(UUID player, String bookValue, Map<String, List<String>> formParams)
			throws ClassNotFoundException, IOException, NullPointerException, NumberFormatException,
			IllegalArgumentException {

		// loading the book materials
		Player p = Bukkit.getPlayer(player);
		EditConfig editConfig = database.getSavedEdit(player);
		Book book = Book.getBook(bookValue);
		if (book == null) {
			book = Book.fromString(bookValue);
		}

		book.emptyInputConfig();
		for (String key : formParams.keySet()) {
			String value = formParams.get(key).get(0);
			if (key.contains("input_") && Material.matchMaterial(value) != null) {
				book.addInputConfig(Material.matchMaterial(value));
			} else if (key.contains("enchantment_") && enchantment(value) != null) {
				book.addEnchantment(enchantment(value));
			} else if (key.contains("potion_") && potionEffect(value) != null) {
				book.addPotion(potionEffect(value));
			}
		}
        book.setStrictMode(formParams.keySet().contains("strict_mode"));
        book.setPreventOverflow(formParams.keySet().contains("prevent_overflow"));
        book.setLinkedBelow(formParams.keySet().contains("linked_below"));

        if (formParams.keySet().contains("junction_ratio")) {
			int first = Integer.parseInt(formParams.get("firstRatio").get(0));
			int second = Integer.parseInt(formParams.get("secondRatio").get(0));

			if (first <= 0 || second <= 0) {
				throw new IllegalArgumentException("Can't have a 0 or negative ratio");
			}

			book.setRatio(first, second);
		} else {
			book.emptyRatio();
		}

		ItemStack replaceItem = new ItemStack(Material.WRITTEN_BOOK);

		BookMeta meta = (BookMeta) replaceItem.getItemMeta();
		meta.setTitle("HopperConfiguration");
		meta.setAuthor("ItemSorter");

		// changing the item frame item
		NamespacedKey key = new NamespacedKey(plugin, "itemsorter");
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, book.toString());

		meta.pages(Text.legacy(book.toPages()));
		
		// changing the item frame item
		replaceItem.setItemMeta(meta);
		// The player may have logged off since opening the edit page, and the slot may be empty.
		ItemStack editedItem = editConfig.isHopper() || p == null ? null
				: p.getInventory().getItem(editConfig.getSlot());
		if (editedItem != null && editedItem.getType() == Material.WRITTEN_BOOK
				&& bookValue.equals(editedItem.getItemMeta().getPersistentDataContainer().get(key,
						PersistentDataType.STRING))) {
			editedItem.setItemMeta(meta);
		} else if (editConfig.isHopper() && Frame.getFRAME(book.getFrameID()) != null
				&& Frame.getFRAME(book.getFrameID()).getEntityFrame() != null) {
			Frame.getFRAME(book.getFrameID()).getEntityFrame().setItem(replaceItem);
		} else {
			throw new NullPointerException("Error trying to find the edited config item");
		}
		book.addSelf(book.getFrameID());
		database.deleteEditHopper(player);
	}
}
