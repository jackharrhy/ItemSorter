package tollenaar.stephen.ItemSorter.Shared;

import com.google.gson.Gson;
import java.util.*;
import tollenaar.stephen.ItemSorter.Util.Web.HopperItems;
import tollenaar.stephen.ItemSorter.Util.Web.Ratio;

public record FilterConfig(int version, List<String> items, List<String> enchantments, List<String> potions,
        boolean strictMode, boolean preventOverflow, boolean linkedBelow, Integer firstRatio, Integer secondRatio) {
    private static final Gson JSON = new Gson();
    public FilterConfig {
        if (version != 1) throw new IllegalArgumentException("Unsupported filter version");
        if (items == null || enchantments == null || potions == null) throw new IllegalArgumentException("Missing filters");
        if ((firstRatio == null) != (secondRatio == null)
                || firstRatio != null && (firstRatio <= 0 || secondRatio <= 0)) throw new IllegalArgumentException("Invalid ratio");
        items = List.copyOf(items);
        enchantments = List.copyOf(enchantments);
        potions = List.copyOf(potions);
    }
    public static FilterConfig fromForm(Map<String, List<String>> form) {
        var items = new ArrayList<String>();
        var enchantments = new ArrayList<String>();
        var potions = new ArrayList<String>();
        for (var entry : form.entrySet()) {
            if (entry.getValue().isEmpty()) throw new IllegalArgumentException("Empty choice");
            // These are UI section toggles, not selected registry entries.
            if (entry.getKey().equals("enchantment_blocker") || entry.getKey().equals("potion_blocker")) continue;
            String value = entry.getValue().getFirst().toLowerCase(Locale.ROOT);
            if (entry.getKey().startsWith("input_")) items.add(value);
            else if (entry.getKey().startsWith("enchantment_")) enchantments.add(value);
            else if (entry.getKey().startsWith("potion_")) potions.add(value);
        }
        Integer first = null, second = null;
        if (form.containsKey("junction_ratio")) {
            if (!form.containsKey("firstRatio") || !form.containsKey("secondRatio")) throw new IllegalArgumentException("Missing ratio");
            first = Integer.valueOf(form.get("firstRatio").getFirst());
            second = Integer.valueOf(form.get("secondRatio").getFirst());
        }
        return new FilterConfig(1, items, enchantments, potions, form.containsKey("strict_mode"),
                form.containsKey("prevent_overflow"), form.containsKey("linked_below"), first, second);
    }
    public boolean allows(String item, Set<String> itemEnchantments, List<String> itemEffects) {
        if (!items.isEmpty() && !items.contains(item)) return false;
        if (!itemEnchantments.isEmpty() && !enchantments.isEmpty() && !matchesEnchantments(itemEnchantments)) return false;
        return itemEffects.isEmpty() || potions.isEmpty() || matchesPotions(itemEffects);
    }
    public boolean matchesEnchantments(Set<String> names) {
        return !Collections.disjoint(enchantments, names) && (!strictMode || enchantments.containsAll(names));
    }
    // Legacy Paper books contain uppercase effect names.
    public boolean matchesPotions(List<String> effects) {
        return effects.stream().anyMatch(effect -> potions.stream().anyMatch(effect::equalsIgnoreCase));
    }
    public String encode() { return JSON.toJson(this); }
    public static FilterConfig decode(String value) {
        try {
            var filter = JSON.fromJson(value, FilterConfig.class);
            if (filter == null) throw new IllegalArgumentException("Missing filter");
            return filter;
        } catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid filter", invalid); }
    }
    public HopperItems toItems() {
        return new HopperItems(items, enchantments, potions, strictMode, preventOverflow, linkedBelow,
                firstRatio == null ? null : new Ratio(firstRatio, secondRatio));
    }
}
