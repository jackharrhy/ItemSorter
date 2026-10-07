package tollenaar.stephen.ItemSorter.fabric;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import tollenaar.stephen.ItemSorter.Shared.FilterConfig;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FabricBooksTest {
    @Test void vanillaWrittenBookCarriesPaperCompatibleMetadataAndRealFilter() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var initializers = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS;
        var field = initializers.getClass().getDeclaredField("initializers");
        field.setAccessible(true);
        for (var item : List.of(Items.STONE, Items.DIRT, Items.WRITTEN_BOOK)) {
            var builder = net.minecraft.core.component.DataComponentMap.builder();
            for (Object entry : (List<?>) field.get(initializers)) {
                var key = entry.getClass().getDeclaredMethod("key");
                key.setAccessible(true);
                if (!key.invoke(entry).equals(item.builtInRegistryHolder().key())) continue;
                var run = entry.getClass().getDeclaredMethod("run", net.minecraft.core.component.DataComponentMap.Builder.class, net.minecraft.core.HolderLookup.Provider.class);
                run.setAccessible(true);
                run.invoke(entry, builder, net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
            }
            item.builtInRegistryHolder().bindComponents(builder.build());
        }
        var filter = FilterConfig.fromForm(Map.of("input_stone", List.of("stone")));
        var book = FabricBooks.create(filter);
        assertTrue(book.is(Items.WRITTEN_BOOK));
        assertFalse(book.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().isEmpty());
        assertEquals(filter.encode(), FabricBooks.value(book));
        assertTrue(FabricBooks.allows(FabricBooks.read(book), new ItemStack(Items.STONE)));
        assertFalse(FabricBooks.allows(FabricBooks.read(book), new ItemStack(Items.DIRT)));
        assertEquals(filter.encode(), book.get(DataComponents.CUSTOM_DATA).copyTag()
                .getCompoundOrEmpty("PublicBukkitValues").getStringOr("itemsorter:itemsorter", ""));
    }
}
