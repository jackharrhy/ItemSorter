package tollenaar.stephen.ItemSorter.Util.Web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import tollenaar.stephen.ItemSorter.Util.Text;

public class Attributes {

    private final List<Item> items;
    private final List<Image> containers;
    private final List<Item> enchantments;
    private final List<Item> potions;

    public Attributes(List<Item> items, List<Image> containers) {
        Collections.sort(items, (a, b) -> {
            return a.getName().compareTo(b.getName());
        });
        this.items = items;
        this.containers = containers;

        this.enchantments = new ArrayList<>();
        for (Enchantment ent : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
            enchantments.add(new Item(ent.hashCode(), 1, ent.getKey().getKey(),
                    Text.capitalizeFully(ent.getKey().getKey().replace("_", " "))));
        }

        this.potions = new ArrayList<>();
        for (PotionEffectType pot : RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT)) {
            potions.add(new Item(pot.hashCode(), 1, pot.getKey().getKey(),
                    Text.capitalizeFully(pot.getKey().getKey().replace("_", " "))));
        }
    }

    public List<Item> getItems() {
        return items;
    }

    public List<Image> getContainers() {
        return containers;
    }

    public List<Item> getEnchantments() {
        return enchantments;
    }

    public List<Item> getPotions() {
        return potions;
    }

}
