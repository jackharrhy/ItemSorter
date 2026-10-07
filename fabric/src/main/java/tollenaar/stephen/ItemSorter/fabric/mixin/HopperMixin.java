package tollenaar.stephen.ItemSorter.fabric.mixin;
import tollenaar.stephen.ItemSorter.fabric.FabricHoppers;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(HopperBlockEntity.class)
public class HopperMixin {
    @WrapMethod(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;")
    private static ItemStack filter(Container source, Container destination, ItemStack item, Direction direction, Operation<ItemStack> original) {
        if (!FabricHoppers.allows(source, destination, item)) return item;
        int count = item.getCount();
        var remaining = original.call(source, destination, item, direction);
        int moved = count - remaining.getCount();
        if (moved > 0) FabricHoppers.moved(source, moved);
        return remaining;
    }
}
