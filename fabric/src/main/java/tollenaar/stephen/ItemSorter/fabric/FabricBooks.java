package tollenaar.stephen.ItemSorter.fabric;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import tollenaar.stephen.ItemSorter.Shared.FilterConfig;

public final class FabricBooks {
    private FabricBooks() {}
    public static String value(ItemStack book) {
        if (!book.is(Items.WRITTEN_BOOK)) return null;
        var data = book.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag().getCompoundOrEmpty("PublicBukkitValues")
                .getString("itemsorter:itemsorter").orElse(null);
    }
    public static FilterConfig read(ItemStack book) {
        String value = value(book);
        if (value == null) return null;
        return FilterConfig.decode(value);
    }
    public static ItemStack create(FilterConfig filter) {
        var book = new ItemStack(Items.WRITTEN_BOOK);
        var values = new CompoundTag();
        values.putString("itemsorter:itemsorter", filter.encode());
        var data = new CompoundTag();
        data.put("PublicBukkitValues", values);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        String page = "Material Filter: " + filter.items() + "\nEnchantment Filter: " + filter.enchantments()
                + "\nPotion Filter: " + filter.potions() + "\nStrict Mode: " + filter.strictMode()
                + "\nPrevent Overflow: " + filter.preventOverflow() + "\nLinked Below: " + filter.linkedBelow()
                + "\nRatio: " + (filter.firstRatio() == null ? "None" : filter.firstRatio() + ":" + filter.secondRatio());
        var pages = new ArrayList<Filterable<Component>>();
        for (int i = 0; i < page.length(); i += 220) pages.add(Filterable.passThrough(Component.literal(page.substring(i, Math.min(i + 220, page.length())))));
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("HopperConfiguration"),
                "ItemSorter", 0, pages, true));
        return book;
    }
    public static boolean allows(FilterConfig filter, ItemStack item) {
        var enchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet()
                .stream().map(holder -> holder.unwrapKey().orElseThrow().identifier().getPath()).collect(Collectors.toSet());
        var effects = item.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).customEffects()
                .stream().map(effect -> effect.getEffect().unwrapKey().orElseThrow().identifier().getPath()).toList();
        return filter.allows(BuiltInRegistries.ITEM.getKey(item.getItem()).getPath(), enchantments, effects);
    }
}
