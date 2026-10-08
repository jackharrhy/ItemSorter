package tollenaar.stephen.ItemSorter.Core;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import tollenaar.stephen.ItemSorter.Shared.FilterConfig;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import tollenaar.stephen.ItemSorter.Util.Server.Frame;

public class HopperConfiguring {
    private final Database database;
    private final NamespacedKey key;

    public HopperConfiguring(ItemSorter plugin) {
        database = plugin.getDatabase();
        key = new NamespacedKey(plugin, "itemsorter");
    }

    // Preserve Paper's legacy field names, material aliases and ignored unknown
    // registry choices. The shared parser owns checkbox and ratio semantics.
    static Book parseBook(Map<String, List<String>> form) {
        var translated = new LinkedHashMap<String, List<String>>();
        int choice = 0;
        for (var entry : form.entrySet()) {
            if (entry.getValue().isEmpty()) throw new IllegalArgumentException("Empty choice");
            String name = entry.getKey();
            String value = entry.getValue().getFirst();
            if (name.contains("input_") && Material.matchMaterial(value) != null) {
                translated.put("input_" + choice++, List.of(Material.matchMaterial(value).name()));
            } else if (name.contains("enchantment_")) {
                var enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                        .get(NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT)));
                if (enchantment != null) translated.put("enchantment_" + choice++, List.of(enchantment.getKey().getKey()));
            } else if (name.contains("potion_")) {
                var potion = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT)
                        .get(NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT)));
                if (potion != null) translated.put("potion_" + choice++, List.of(potion.getKey().getKey()));
            } else if (!name.contains("input_")) {
                translated.put(name, entry.getValue());
            }
        }
        return Book.fromPortable(FilterConfig.fromForm(translated));
    }

    public void configureHopper(int frameID, UUID player, Map<String, List<String>> form) {
        Frame frame = Frame.getFRAME(frameID);
        ItemFrame entity = frame == null ? null : frame.getEntityFrame();
        if (entity == null) throw new IllegalArgumentException("Frame no longer available");

        Book book = parseBook(form);
        entity.setItem(book.toItem(key));
        book.addSelf(frameID);
    }

    public void editConfigureHopper(UUID player, String bookValue, Map<String, List<String>> form)
            throws ClassNotFoundException, IOException {
        var p = Bukkit.getPlayer(player);
        var edit = database.getSavedEdit(player);
        if (p == null || edit == null || !bookValue.equals(edit.getBookValue())) {
            throw new IllegalArgumentException("Configuration no longer active");
        }
        Book previous = Book.getBook(bookValue);
        if (previous == null) previous = Book.fromString(bookValue);
        Book replacement = parseBook(form);
        ItemStack item = replacement.toItem(key);

        if (edit.isHopper()) {
            Frame frame = Frame.getFRAME(previous.getFrameID());
            ItemFrame entity = frame == null ? null : frame.getEntityFrame();
            if (entity == null) throw new IllegalArgumentException("Frame no longer available");
            entity.setItem(item);
        } else {
            ItemStack held = p.getInventory().getItem(edit.getSlot());
            if (held == null || held.getType() != Material.WRITTEN_BOOK
                    || !bookValue.equals(held.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING))) {
                throw new IllegalArgumentException("Book changed");
            }
            held.setItemMeta(item.getItemMeta());
        }
        replacement.addSelf(previous.getFrameID());
        database.deleteEditHopper(player);
    }
}
