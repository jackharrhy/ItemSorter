package tollenaar.stephen.ItemSorter.fabric;

import io.javalin.Javalin;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import tollenaar.stephen.ItemSorter.Shared.*;
import tollenaar.stephen.ItemSorter.Util.Web.Item;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class ItemSorterFabric implements DedicatedServerModInitializer {
    public static ItemSorterFabric instance;
    private final WebEditor.WebSessions sessions = new WebEditor.WebSessions();
    private record Target(ItemFrame frame, InteractionHand hand, String value) {}
    private final Map<UUID, Target> targets = new HashMap<>();
    private final Properties config = new Properties();
    private MinecraftServer server;
    private Javalin app;
    private Map<String, List<Item>> catalog;

    @Override public void onInitializeServer() {
        instance = this;
        ServerLifecycleEvents.SERVER_STARTED.register(this::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            sessions.clear();
            targets.clear();
            if (app != null) app.stop();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
            sessions.revoke(handler.player.getUUID());
            targets.remove(handler.player.getUUID());
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer p) || !(entity instanceof ItemFrame frame)
                    || !level.getBlockState(frame.getPos().relative(frame.getDirection().getOpposite())).is(Blocks.HOPPER)) return InteractionResult.PASS;
            if (frame.getItem().isEmpty() && player.getItemInHand(hand).is(Items.WRITABLE_BOOK)
                    || FabricBooks.value(frame.getItem()) != null) {
                try { sendLink(p, issueForFrame(p, frame), !frame.getItem().isEmpty()); }
                catch (IllegalArgumentException invalid) { p.sendSystemMessage(Component.literal("This book needs conversion on Paper with /itemsorter portable before use on Fabric.")); }
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer p) || FabricBooks.value(player.getItemInHand(hand)) == null) return InteractionResult.PASS;
            try {
                String value = FabricBooks.value(player.getItemInHand(hand));
                FilterConfig.decode(value);
                targets.put(p.getUUID(), new Target(null, hand, value));
                sendLink(p, sessions.issue(p.getUUID(), null, value), true);
            } catch (IllegalArgumentException invalid) { p.sendSystemMessage(Component.literal("Convert this book on Paper with /itemsorter portable first.")); }
            return InteractionResult.PASS;
        });
    }
    private void sendLink(ServerPlayer player, String token, boolean edit) {
        String url = config.getProperty("URL", "http://localhost:7001/")
                + (edit ? "editHopperConfig" : "HopperConfig") + "?token=" + token;
        player.sendSystemMessage(Component.literal("Click here to configure hopper sorting")
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(url)))));
    }
    public String issueForFrame(ServerPlayer player, ItemFrame frame) {
        String value = FabricBooks.value(frame.getItem());
        if (value != null) FilterConfig.decode(value);
        if (value == null && !frame.getItem().isEmpty()) throw new IllegalArgumentException("Frame occupied");
        targets.put(player.getUUID(), new Target(frame, null, value));
        return sessions.issue(player.getUUID(), frame.getId(), value);
    }
    private void start(MinecraftServer server) {
        this.server = server;
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("itemsorter.properties");
            if (Files.exists(path)) try (var reader = Files.newBufferedReader(path)) { config.load(reader); }
            else {
                config.setProperty("port", "7001");
                config.setProperty("host", "0.0.0.0");
                config.setProperty("URL", "http://localhost:7001/");
                config.setProperty("contextPath", "/");
                try (var writer = Files.newBufferedWriter(path)) { config.store(writer, "ItemSorter"); }
            }
            catalog = catalog();
            app = Javalin.create(c -> WebEditor.configure(c, config.getProperty("contextPath", "/")));
            for (boolean edit : List.of(false, true)) WebEditor.registerEditor(app, sessions,
                    edit ? "/editHopperConfig" : "/HopperConfig", edit ? "/postEditHopperConfig" : "/postHopperConfig", edit,
                    ctx -> {
                        var session = (WebEditor.WebSessions.Session) ctx.attribute("session");
                        ctx.attribute("attributes", catalog);
                        ctx.attribute("checkItems", onServer(() -> {
                            validate(session);
                            return (session.bookValue() == null ? FilterConfig.fromForm(Map.of()) : FilterConfig.decode(session.bookValue())).toItems();
                        }));
                    }, ctx -> {
                        var session = (WebEditor.WebSessions.Session) ctx.attribute("session");
                        Map<String, List<String>> form = ctx.attribute("form");
                        onServer(() -> {
                            var target = validate(session);
                            var filter = FilterConfig.fromForm(form);
                            validateChoices(filter);
                            var book = FabricBooks.create(filter);
                            if (target.frame() != null) target.frame().setItem(book);
                            else server.getPlayerList().getPlayer(session.player()).setItemInHand(target.hand(), book);
                            targets.remove(session.player());
                            return null;
                        });
                    });
            app.start(config.getProperty("host", "0.0.0.0"), Integer.parseInt(config.getProperty("port", "7001")));
        } catch (Exception failure) { throw new IllegalStateException("ItemSorter web editor could not start", failure); }
    }
    private Target validate(WebEditor.WebSessions.Session session) {
        var player = server.getPlayerList().getPlayer(session.player());
        var target = targets.get(session.player());
        if (player == null || target == null || !Objects.equals(target.value(), session.bookValue())) throw new IllegalArgumentException("Player or target changed");
        ItemStack item;
        if (target.frame() != null) {
            var frame = target.frame();
            if (frame.isRemoved() || frame.getId() != session.frameId()
                    || !frame.level().getBlockState(frame.getPos().relative(frame.getDirection().getOpposite())).is(Blocks.HOPPER)) throw new IllegalArgumentException("Frame changed");
            item = frame.getItem();
        } else item = player.getItemInHand(target.hand());
        if (target.value() == null ? !item.isEmpty() : !target.value().equals(FabricBooks.value(item))) throw new IllegalArgumentException("Book changed");
        return target;
    }
    private <T> T onServer(Callable<T> action) throws Exception {
        var task = new CompletableFuture<T>();
        server.execute(() -> {
            if (task.isCancelled()) return;
            try { task.complete(action.call()); }
            catch (Exception failure) { task.completeExceptionally(failure); }
        });
        try { return task.get(5, TimeUnit.SECONDS); }
        catch (ExecutionException failure) {
            if (failure.getCause() instanceof IllegalArgumentException invalid) throw invalid;
            throw failure;
        } finally { if (!task.isDone()) task.cancel(false); }
    }
    private void validateChoices(FilterConfig filter) {
        if (filter.items().size() > 100) throw new IllegalArgumentException("Too many items");
        for (String name : filter.items()) if (!BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(name)) || name.contains("spawn_egg")) throw new IllegalArgumentException("Unknown item");
        for (String name : filter.enchantments()) if (!server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).containsKey(Identifier.withDefaultNamespace(name))) throw new IllegalArgumentException("Unknown enchantment");
        for (String name : filter.potions()) if (!BuiltInRegistries.MOB_EFFECT.containsKey(Identifier.withDefaultNamespace(name))) throw new IllegalArgumentException("Unknown potion effect");
    }
    private Map<String, List<Item>> catalog() throws Exception {
        var items = new ArrayList<Item>();
        var root = FabricLoader.getInstance().getModContainer("itemsorter").orElseThrow().getRootPaths().getFirst();
        try (var paths = Files.list(root.resolve("web/images/block"))) {
            for (Path icon : paths.toList()) {
                String name = icon.getFileName().toString().replace(".png", "");
                var id = Identifier.tryParse(name);
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || name.contains("spawn_egg")) continue;
                items.add(webItem(name, BuiltInRegistries.ITEM.getValue(id).getDefaultMaxStackSize()));
            }
        }
        items.sort(Comparator.comparing(Item::getName));
        return Map.of("items", items, "enchantments", server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).keySet().stream().map(key -> webItem(key.getPath(), 1)).toList(),
                "potions", BuiltInRegistries.MOB_EFFECT.keySet().stream().map(key -> webItem(key.getPath(), 1)).toList());
    }
    private Item webItem(String name, int size) {
        return new Item(name.hashCode(), size, name, name.replace('_', ' '));
    }
}
