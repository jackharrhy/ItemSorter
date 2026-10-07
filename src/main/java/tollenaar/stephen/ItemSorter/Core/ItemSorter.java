package tollenaar.stephen.ItemSorter.Core;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import tollenaar.stephen.ItemSorter.Util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import tollenaar.stephen.ItemSorter.Commands.CommandsHandler;
import tollenaar.stephen.ItemSorter.Events.HopperHandler;
import tollenaar.stephen.ItemSorter.Events.HopperInteractHandler;
import tollenaar.stephen.ItemSorter.Util.Server.Book;
import tollenaar.stephen.ItemSorter.Util.Server.EventExceptionHandler;
import tollenaar.stephen.ItemSorter.Util.Web.Attributes;
import tollenaar.stephen.ItemSorter.Util.Web.HopperItems;
import tollenaar.stephen.ItemSorter.Util.Web.Item;

public class ItemSorter extends JavaPlugin {

    private Database database;
    private HopperConfiguring hopperConfig;
    private FileConfiguration config;
    private static Javalin appItem;
    private static Attributes attributes;
    private EventExceptionHandler handler;
    private final WebSessions webSessions = new WebSessions();

    public WebSessions getWebSessions() { return webSessions; }

    @Override
    public void onEnable() {
        config = this.getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        handler = new EventExceptionHandler(this);
        database = new Database(this);
        hopperConfig = new HopperConfiguring(this);

        // registering events
        EventExceptionHandler.registerEvents(new HopperInteractHandler(this), this, handler);
        EventExceptionHandler.registerEvents(new HopperHandler(database), this, handler);

        getCommand("ItemSorter").setExecutor(new CommandsHandler(this));
        // Iterating over all the image files which corresponds to the items in
        // minecraft
        // Reading our own jar, whatever it got named (builds are ItemSorter-<mcversion>.jar)
        try (ZipFile jar = new ZipFile(getFile())) {
            Enumeration<? extends ZipEntry> entries = jar.entries();

            List<Item> tmp = new ArrayList<>();
            int unknown = 0;
            while (entries.hasMoreElements()) {
                ZipEntry en = entries.nextElement();
                if (en.getName().contains("images/block/") && en.getName().split("images/block/").length > 1) {
                    // Formatting name and getting the max itemstack size
                    // Filtering out disabled items as well
                    String name = en.getName().replace("itemsorter-web/images/block/", "").replace(".png", "");
                    boolean cont = false;
                    for (String disabled : config.getStringList("disabledItems")) {
                        if (name.contains(disabled)) {
                            cont = true;
                        }
                    }
                    if (!cont) {
                        try {
                            ItemStack t = new ItemStack(Material.matchMaterial(name));
                            tmp.add(new Item(en.hashCode(), t.getMaxStackSize(), name,
                                    Text.capitalizeFully(name.toLowerCase().replace("_", " "))));
                        } catch (Exception e) {
                            // The icon set covers newer Minecraft versions too, so a
                            // few names won't resolve here. Counted, not spammed.
                            unknown++;
                        }
                    }
                }
            }
            if (unknown > 0) {
                getLogger().log(Level.INFO, "{0} item icons skipped, unknown to this Minecraft version", unknown);
            }
            attributes = new Attributes(tmp, new ArrayList<>());
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, e.toString());
            // disabling this server
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, this::startWebServer);

        Bukkit.getScheduler().runTaskAsynchronously(this, database::load);
    }

    @Override
    public void onDisable() {
        webSessions.clear();
        try {
            appItem.stop();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "appItem stopped with errors");
        }
    }

    public void startWebServer() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ItemSorter.class.getClassLoader());
        appItem = Javalin.create(javalinConfig -> configureWebServer(javalinConfig, config));

        appItemInitalize();
        appItem.start(config.getString("host", "0.0.0.0"), config.getInt("port"));

        Thread.currentThread().setContextClassLoader(classLoader);
    }

    /**
     * Web server settings, kept apart from starting the server so tests can use
     * them. contextPath moves every page and asset under a prefix such as
     * "/itemsorter"; Javalin normalizes missing or doubled slashes itself.
     * 
     * @param javalinConfig - the Javalin configuration to fill in.
     * @param pluginConfig  - the plugin's config.yml.
     */
    static void configureWebServer(JavalinConfig javalinConfig, FileConfiguration pluginConfig) {
        javalinConfig.addStaticFiles("itemsorter-web", Location.CLASSPATH);
        javalinConfig.showJavalinBanner = false;
        javalinConfig.maxRequestSize = 30000L;
        javalinConfig.contextPath = pluginConfig.getString("contextPath", "/");
    }

    private void appItemInitalize() {
        Handler load = ctx -> {
            WebSessions.Session session = ctx.attribute("session");
            ctx.attribute("attributes", attributes);
            ctx.attribute("checkItems", onServerThread(() -> {
                validateSession(session);
                return session.bookValue() == null
                        ? new HopperItems(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false, false, false, null)
                        : Book.fromString(session.bookValue()).toItems();
            }));
        };
        Handler save = ctx -> {
            WebSessions.Session session = ctx.attribute("session");
            Map<String, List<String>> form = ctx.attribute("form");
            onServerThread(() -> {
                validateSession(session);
                if (session.bookValue() == null) {
                    hopperConfig.configureHopper(session.frameId(), session.player(), form);
                    database.deletePlayerWithFrame(session.player(), session.frameId());
                } else {
                    hopperConfig.editConfigureHopper(session.player(), session.bookValue(), form);
                }
                return null;
            });
        };
        registerEditor(appItem, webSessions, "/" + config.getString("initialPageResponse"), "/" + config.getString("postConfigResponse"), false, load, save);
        registerEditor(appItem, webSessions, "/" + config.getString("editPageResponse"), "/" + config.getString("postEditPageResponse"), true, load, save);

        appItem.get("/", ctx -> {
            ctx.attribute("response", "You're not supposed to be here!!");
            ctx.render("/itemsorter-web/response.html");
        });
    }

    public Database getDatabase() {
        return database;
    }

    static void registerEditor(Javalin app, WebSessions sessions, String getPath, String postPath,
            boolean edit, Handler load, Handler save) {
        tollenaar.stephen.ItemSorter.Shared.WebEditor.registerEditor(app, sessions, getPath, postPath, edit, load, save);
    }

    public static final class WebSessions extends tollenaar.stephen.ItemSorter.Shared.WebEditor.WebSessions {
        WebSessions() { super(); }
        WebSessions(java.util.function.LongSupplier now) { super(now); }
    }

    private void validateSession(WebSessions.Session session) {
        if (Bukkit.getPlayer(session.player()) == null) throw new IllegalArgumentException("Player disconnected");
        if (session.bookValue() == null) {
            if (!database.hasSavedPlayerWithItemFrame(session.player(), session.frameId()))
                throw new IllegalArgumentException("Configuration no longer active");
        } else {
            var edit = database.getSavedEdit(session.player());
            if (edit == null || !session.bookValue().equals(edit.getBookValue()))
                throw new IllegalArgumentException("Configuration no longer active");
        }
    }

    private <T> T onServerThread(Callable<T> action) throws Exception {
        var task = Bukkit.getScheduler().callSyncMethod(this, action);
        try {
            return task.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException failure) {
            if (failure.getCause() instanceof IllegalArgumentException invalid) throw invalid;
            throw failure;
        } finally {
            if (!task.isDone()) task.cancel(false);
        }
    }

    // private BufferedImage loadImage(InputStream file) {
    // BufferedImage buff = null;
    // try {
    // buff = ImageIO.read(file);
    // } catch (IOException e) {
    // Bukkit.getLogger().log(Level.SEVERE, e.toString());
    // return null;
    // }
    // return buff;
    //
    // }
}
