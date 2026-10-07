package tollenaar.stephen.ItemSorter.fabric;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;
public final class FabricHoppers {
    private FabricHoppers() {}
    private record RatioState(String value, tollenaar.stephen.ItemSorter.Util.Web.Ratio ratio) {}
    private static final java.util.Map<ItemFrame, RatioState> ratios = new java.util.WeakHashMap<>();
    private static ItemFrame junctionFrame(Container source) {
        if (!(source instanceof HopperBlockEntity hopper) || hopper.getLevel() == null
                || hopper.getBlockState().getValue(net.minecraft.world.level.block.HopperBlock.FACING) == net.minecraft.core.Direction.DOWN) return null;
        if (hopper.getLevel().getBlockEntity(hopper.getBlockPos().below()) instanceof HopperBlockEntity lower) return frame(lower);
        return null;
    }
    private static tollenaar.stephen.ItemSorter.Util.Web.Ratio ratio(ItemFrame frame) {
        String value = FabricBooks.value(frame.getItem());
        var filter = FabricBooks.read(frame.getItem());
        if (filter.firstRatio() == null) return null;
        var state = ratios.get(frame);
        if (state == null || !state.value().equals(value)) {
            state = new RatioState(value, new tollenaar.stephen.ItemSorter.Util.Web.Ratio(filter.firstRatio(), filter.secondRatio()));
            ratios.put(frame, state);
        }
        return state.ratio();
    }
    public static void moved(Container source, int count) {
        var frame = junctionFrame(source);
        if (frame == null) return;
        var ratio = ratio(frame);
        if (ratio != null) for (int i = 0; i < count; i++) ratio.addCount();
    }

    public static ItemFrame frame(HopperBlockEntity hopper) {
        if (hopper.getLevel() == null) return null;
        for (var frame : hopper.getLevel().getEntitiesOfClass(ItemFrame.class, new AABB(hopper.getBlockPos()).inflate(1))) {
            if (frame.getPos().relative(frame.getDirection().getOpposite()).equals(hopper.getBlockPos())
                    && FabricBooks.value(frame.getItem()) != null) return frame;
        }
        return null;
    }
    public static boolean allows(Container source, Container destination, ItemStack item) {
        var junction = junctionFrame(source);
        if (junction != null) {
            try {
                var ratio = ratio(junction);
                if (ratio != null) {
                    var upper = (HopperBlockEntity) source;
                    boolean below = destination instanceof HopperBlockEntity lower && lower.getBlockPos().equals(upper.getBlockPos().below());
                    if (ratio.isFirstSelector() == below) return false;
                }
            } catch (IllegalArgumentException invalid) { return false; }
        }
        if (!(destination instanceof HopperBlockEntity hopper)) return true;
        var frame = frame(hopper);
        if (frame == null) return true;
        try {
            var filter = FabricBooks.read(frame.getItem());
            if (source == null && !filter.strictMode()) return true;
            if (!FabricBooks.allows(filter, item)) {
                if (filter.linkedBelow() && hopper.getLevel().getBlockEntity(hopper.getBlockPos().below()) instanceof HopperBlockEntity lower
                        && frame(lower) != null) return allows(source, lower, item);
                return false;
            }
            if (filter.preventOverflow()) {
                var facing = hopper.getBlockState().getValue(net.minecraft.world.level.block.HopperBlock.FACING);
                var output = HopperBlockEntity.getContainerAt(hopper.getLevel(), hopper.getBlockPos().relative(facing));
                if (output == null) return false;
                for (int i = 0; i < output.getContainerSize(); i++) {
                    var stack = output.getItem(i);
                    if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, item)
                            && stack.getCount() + item.getCount() <= stack.getMaxStackSize()) return true;
                }
                return false;
            }
            return true;
        } catch (IllegalArgumentException invalid) { return false; }
    }
}
