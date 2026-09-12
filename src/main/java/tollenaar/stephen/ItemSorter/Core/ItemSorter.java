package tollenaar.stephen.ItemSorter.Core;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.javalin.Javalin;
import io.javalin.http.Context;
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
                    String name = en.getName().replace("web/images/block/", "").replace(".png", "");
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
                                    WordUtils.capitalizeFully(name.toLowerCase().replace("_", " "))));
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
        try {
            appItem.stop();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "appItem stopped with errors");
        }
    }

    private static void addSoftwareLibrary(File file)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, MalformedURLException {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), new Object[] { file.toURI().toURL() });
    }

    public void startWebServer() {

        // try {
        // addSoftwareLibrary(new File(getDataFolder().getAbsoluteFile() +
        // File.separator + "lib" + File.separator
        // + "websocket-server-9.4.46.v20220331.jar"));
        // } catch (Exception e) {
        // Bukkit.getLogger().log(Level.SEVERE, e.toString());
        // }
        //
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ItemSorter.class.getClassLoader());
        appItem = Javalin.create(config -> {
            config.addStaticFiles("web", Location.CLASSPATH);
            config.showJavalinBanner = false;
            config.maxRequestSize = 30000L;
        }).start(config.getInt("port"));

        appItemInitalize();

        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private void appItemInitalize() {
        appItem.post("/" + config.getString("postConfigResponse"), (Context ctx) -> {
            try {
                String userCode = ctx.formParam("userCode");
                int frameID = Integer.parseInt(ctx.formParam("frameID"));
                if (database.hasSavedPlayerWithItemFrame(UUID.fromString(userCode), frameID)) {
                    hopperConfig.configureHopper(frameID, UUID.fromString(userCode), ctx.formParamMap());
                    ctx.attribute("response", "Thank you. You can close this page now.");
                    database.deletePlayerWithFrame(UUID.fromString(userCode), frameID);
                } else {
                    ctx.attribute("response", "Conflicting data while posting your configuration set up.");
                }
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                ctx.attribute("response", "Internal server error while posting your configuration set up. ("
                        + e.toString().replace("java.lang.", "") + ")");
            }
            ctx.render("/web/response.html");
        });

        appItem.get("/" + config.getString("initialPageResponse"), ctx -> {
            try {
                String userCode = ctx.queryParam("userCode");
                int frameID = Integer.parseInt(ctx.queryParam("frameID"));
                ctx.attribute("userCode", userCode);
                ctx.attribute("frameID", frameID);
                ctx.attribute("attributes", attributes);
                ctx.attribute("postAction", "./" + config.getString("postConfigResponse"));
                ctx.attribute("checkItems",
                        new HopperItems(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false, false, false,
                                null));
                ctx.render("/web/index.html");
                if (!database.hasSavedPlayerWithItemFrame(UUID.fromString(userCode), frameID)) {
                    ctx.attribute("response", "You're not supposed to be here!!");
                    ctx.render("/web/response.html");
                }

            } catch (NumberFormatException ex) {
                ctx.attribute("response", "You're not supposed to be here!!");
                ctx.render("/web/response.html");
            }

        });

        appItem.get("/" + config.getString("editPageResponse"), ctx -> {
            try {
                String user = ctx.queryParam("configData");
                String bookValue = database.getSavedEdit(UUID.fromString(user)).getBookValue();
                HopperItems checkItems = Book.fromString(bookValue).toItems();
                ctx.attribute("bookValue", bookValue);
                ctx.attribute("userCode", user);
                ctx.attribute("attributes", attributes);
                ctx.attribute("postAction", "./" + config.getString("postEditPageResponse"));
                ctx.attribute("checkItems", checkItems);
                ctx.render("/web/index.html");
                if (!database.hasSavedPlayer(UUID.fromString(user))) {
                    ctx.attribute("response", "You're not supposed to be here!!");
                    ctx.render("/web/response.html");
                }

            } catch (IOException | ClassNotFoundException e) {
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                ctx.attribute("response", "Internal server error while posting your configuration set up. ("
                        + e.toString().replace("java.lang.", "") + ")");
                ctx.render("/web/response.html");
            }

        });

        appItem.post("/" + config.getString("postEditPageResponse"), ctx -> {
            try {
                String bookValue = ctx.formParam("bookValue");
                String userCode = ctx.formParam("userCode");
                if (database.hasSavedPlayer(UUID.fromString(userCode))) {
                    hopperConfig.editConfigureHopper(UUID.fromString(userCode), bookValue, ctx.formParamMap());
                    ctx.attribute("response", "Thank you. You can close this page now.");
                    database.deleteEditHopper(UUID.fromString(userCode));
                } else {
                    ctx.attribute("response", "Conflicting data while posting your configuration set up.");
                }
            } catch (IOException | ClassNotFoundException | IllegalArgumentException | NullPointerException e) {
                Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
                ctx.attribute("response", "Internal server error while posting your configuration set up. ("
                        + e.toString().replace("java.lang.", "") + ")");
            }
            ctx.render("/web/response.html");
        });

        appItem.get("/", ctx -> {
            ctx.attribute("response", "You're not supposed to be here!!");
            ctx.render("/web/response.html");
        });
    }

    public Database getDatabase() {
        return database;
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
