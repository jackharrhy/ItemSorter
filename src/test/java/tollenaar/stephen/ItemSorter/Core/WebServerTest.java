package tollenaar.stephen.ItemSorter.Core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;

/** Starts the web server with the plugin's own settings, on a free port. */
class WebServerTest {

    private Javalin app;

    // Public so the Oracle Java extension doesn't report it as unused: it only
    // treats runnable @Test methods as used, not JUnit lifecycle methods.
    @AfterEach
    public void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    private void start(YamlConfiguration pluginConfig) {
        app = Javalin.create(javalinConfig -> ItemSorter.configureWebServer(javalinConfig, pluginConfig)).start(0);
        // Registered after start, the same way the plugin adds its pages.
        app.get("/HopperConfig", ctx -> ctx.result("page"));
    }

    private int status(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void servesFromTheRootByDefault() throws Exception {
        start(new YamlConfiguration());

        assertEquals(200, status("/HopperConfig"));
        assertEquals(200, status("/css/page.css"));
    }

    @Test
    void servesPagesAndAssetsUnderTheContextPath() throws Exception {
        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("contextPath", "/itemsorter");
        start(pluginConfig);

        assertEquals(200, status("/itemsorter/HopperConfig"));
        assertEquals(200, status("/itemsorter/css/page.css"));
        assertEquals(404, status("/HopperConfig"));
        assertEquals(404, status("/css/page.css"));
    }
}
