package tollenaar.stephen.ItemSorter.fabric;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import tollenaar.stephen.ItemSorter.Shared.FilterConfig;
import java.util.*;
public class SmokeTest implements DedicatedServerModInitializer {
    private HopperBlockEntity hopper;
    private int ticks;
    private int readyTick;
    @Override public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                var level = server.overworld();
                var pos = new BlockPos(8, 100, 8);
                level.setChunkForced(0, 0, true);
                level.getChunkAt(pos);
                for (int x = 6; x <= 10; x++) for (int y = 98; y <= 103; y++) for (int z = 6; z <= 10; z++) level.setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState());
                hopper = (HopperBlockEntity) level.getBlockEntity(pos);
                level.setBlockAndUpdate(pos.below(), Blocks.HOPPER.defaultBlockState());
            } catch (Throwable failure) { failure.printStackTrace(); }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            ++ticks;
            if (readyTick == 0 && server.overworld().areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(0, 0))) {
                readyTick = ticks;
                var level = server.overworld();
                var pos = hopper.getBlockPos();
                var filter = FilterConfig.fromForm(Map.of("input_stone", List.of("stone")));
                for (var p : List.of(pos, pos.below())) {
                    var frame = new ItemFrame(level, p.relative(Direction.NORTH), Direction.NORTH);
                    frame.setItem(FabricBooks.create(filter));
                    if (!level.addFreshEntity(frame)) throw new AssertionError("Could not add smoke frame");
                }
            }
            if (ticks > 400 && readyTick == 0) { System.out.println("ITEMSORTER_SMOKE_FAIL: chunk not ready"); server.halt(false); }
            if (readyTick == 0 || ticks != readyTick + 20) return;
            try {

                var rejected = HopperBlockEntity.addItem(new net.minecraft.world.SimpleContainer(5), hopper, new ItemStack(Items.DIRT), Direction.UP);
                if (rejected.isEmpty()) throw new AssertionError("Configured hopper accepted dirt");
                var accepted = HopperBlockEntity.addItem(new net.minecraft.world.SimpleContainer(5), hopper, new ItemStack(Items.STONE), Direction.UP);
                if (!accepted.isEmpty() || !hopper.getItem(0).is(Items.STONE)) throw new AssertionError("Configured hopper rejected stone");
                System.out.println("ITEMSORTER_SMOKE_FILTER_PASS");
                hopper.clearContent();
                var level = hopper.getLevel();
                level.setBlockAndUpdate(hopper.getBlockPos().below(), Blocks.CHEST.defaultBlockState());
                var chest = (net.minecraft.world.Container) level.getBlockEntity(hopper.getBlockPos().below());
                for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.DIRT, 64));
                FabricHoppers.frame(hopper).setItem(FabricBooks.create(FilterConfig.fromForm(Map.of("input_stone", List.of("stone"), "prevent_overflow", List.of("on")))));
                var overflow = HopperBlockEntity.addItem(new net.minecraft.world.SimpleContainer(5), hopper, new ItemStack(Items.STONE), Direction.UP);
                if (overflow.isEmpty()) throw new AssertionError("Overflow accepted into full output chest");
                chest.setItem(0, ItemStack.EMPTY);
                if (!HopperBlockEntity.addItem(new net.minecraft.world.SimpleContainer(5), hopper, new ItemStack(Items.STONE), Direction.UP).isEmpty()) throw new AssertionError("Overflow rejected available output space");
                System.out.println("ITEMSORTER_SMOKE_OVERFLOW_PASS");
                hopper.clearContent();
                level.setBlockAndUpdate(hopper.getBlockPos().below(), Blocks.HOPPER.defaultBlockState());
                FabricHoppers.frame(hopper).setItem(FabricBooks.create(FilterConfig.fromForm(Map.of("input_dirt", List.of("dirt"), "linked_below", List.of("on")))));
                if (!HopperBlockEntity.addItem(new net.minecraft.world.SimpleContainer(5), hopper, new ItemStack(Items.STONE), Direction.UP).isEmpty()) throw new AssertionError("Linked lower filter did not accept stone");
                System.out.println("ITEMSORTER_SMOKE_LINKED_PASS");
                hopper.clearContent();
                var upperPos = hopper.getBlockPos().above();
                level.setBlockAndUpdate(upperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
                level.setBlockAndUpdate(upperPos.east(), Blocks.CHEST.defaultBlockState());
                var upper = (HopperBlockEntity) level.getBlockEntity(upperPos);
                var side = (net.minecraft.world.Container) level.getBlockEntity(upperPos.east());
                FabricHoppers.frame(hopper).setItem(FabricBooks.create(FilterConfig.fromForm(Map.of("input_stone", List.of("stone"), "junction_ratio", List.of("on"), "firstRatio", List.of("2"), "secondRatio", List.of("1")))));
                for (int i = 0; i < 2; i++) if (!HopperBlockEntity.addItem(upper, side, new ItemStack(Items.STONE), Direction.WEST).isEmpty()) throw new AssertionError("Ratio rejected side turn");
                if (HopperBlockEntity.addItem(upper, side, new ItemStack(Items.STONE), Direction.WEST).isEmpty()) throw new AssertionError("Ratio failed to switch below");
                if (!HopperBlockEntity.addItem(upper, hopper, new ItemStack(Items.STONE), Direction.UP).isEmpty()) throw new AssertionError("Ratio rejected below turn");
                if (!HopperBlockEntity.addItem(upper, side, new ItemStack(Items.STONE), Direction.WEST).isEmpty()) throw new AssertionError("Ratio failed to return side");
                System.out.println("ITEMSORTER_SMOKE_RATIO_PASS");
                var player = new net.minecraft.server.level.ServerPlayer(server, server.overworld(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "Smoke"), net.minecraft.server.level.ClientInformation.createDefault());
                var playerField = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
                playerField.setAccessible(true);
                ((Map<UUID, net.minecraft.server.level.ServerPlayer>) playerField.get(server.getPlayerList())).put(player.getUUID(), player);
                var target = FabricHoppers.frame(hopper);
                target.setItem(ItemStack.EMPTY);
                String token = ItemSorterFabric.instance.issueForFrame(player, target);
                Thread.startVirtualThread(() -> {
                    try (var client = java.net.http.HttpClient.newHttpClient()) {
                        String base = "http://127.0.0.1:8766";
                        var get = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/HopperConfig?token=" + token)).GET().build();
                        var page = client.send(get, java.net.http.HttpResponse.BodyHandlers.ofString());
                        if (page.statusCode() != 200 || !page.body().contains(token) || !page.body().contains("images/block/stone.png")) throw new AssertionError("Fabric editor did not render authenticated item catalog: " + page.statusCode());
                        var post = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/postHopperConfig"))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("token=" + token + "&input_dirt=dirt&userCode=forged&frameID=999&bookValue=forged")).build();
                        if (client.send(post, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode() != 200) throw new AssertionError("Fabric editor save failed");
                        if (client.send(post, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode() != 403) throw new AssertionError("Fabric editor replay accepted");
                        server.submit(() -> {
                            if (!FabricBooks.read(target.getItem()).items().equals(List.of("dirt"))) throw new AssertionError("Fabric web save did not update actual frame");
                            System.out.println("ITEMSORTER_SMOKE_WEB_PASS");
                        }).get();
                    } catch (Throwable failure) { failure.printStackTrace(); System.out.println("ITEMSORTER_SMOKE_FAIL"); }
                    server.halt(false);
                });
            } catch (Throwable failure) { failure.printStackTrace(); System.out.println("ITEMSORTER_SMOKE_FAIL"); server.halt(false); }
        });
    }
}
