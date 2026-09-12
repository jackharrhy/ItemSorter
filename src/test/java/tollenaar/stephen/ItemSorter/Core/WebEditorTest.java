package tollenaar.stephen.ItemSorter.Core;

import tollenaar.stephen.ItemSorter.Core.ItemSorter.WebSessions;
import tollenaar.stephen.ItemSorter.Util.Web.HopperItems;
import io.javalin.http.Handler;

import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.bukkit.configuration.file.YamlConfiguration;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WebEditorTest {
    @Test void rejectedSaveDoesNotLeakDetailsOrAllowReplay() throws Exception {
        WebSessions sessions = new WebSessions();
        String token = sessions.issue(UUID.randomUUID(), 1, null);
        Javalin app = Javalin.create(c -> c.showJavalinBanner = false);
        ItemSorter.registerEditor(app, sessions, "/HopperConfig", "/postHopperConfig", false,
            ctx -> {}, ctx -> { throw new IllegalArgumentException("internal-detail"); });
        app.start("127.0.0.1", 0);
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/postHopperConfig"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("token=" + token)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(400, response.statusCode());
            assertFalse(response.body().contains("internal-detail"));
            assertEquals(403, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally { app.stop(); }
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "/item-sorter"})
    void validLinksRenderAndSubmitOnlyServerOwnedIdentityOnce(String contextPath) throws Exception {
        WebSessions sessions = new WebSessions();
        UUID player = UUID.randomUUID();
        String token = sessions.issue(player, null, "server-book-not-browser-data");
        var writes = new java.util.concurrent.atomic.AtomicInteger();
        var config = new YamlConfiguration();
        config.set("contextPath", contextPath);
        Javalin app = Javalin.create(c -> ItemSorter.configureWebServer(c, config));
        ItemSorter.registerEditor(app, sessions, "/editHopperConfig", "/postEditHopperConfig", true, ctx -> {
            WebSessions.Session session = ctx.attribute("session");
            assertEquals(player, session.player());
            ctx.attribute("attributes", Map.of("items", List.of(), "enchantments", List.of(), "potions", List.of()));
            ctx.attribute("checkItems", new HopperItems(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), false, false, false, null));
        }, ctx -> {
            WebSessions.Session session = ctx.attribute("session");
            Map<String, List<String>> form = ctx.attribute("form");
            assertEquals(player, session.player());
            assertEquals("server-book-not-browser-data", session.bookValue());
            assertFalse(form.containsKey("bookValue"));
            assertFalse(form.containsKey("userCode"));
            assertFalse(form.containsKey("frameID"));
            assertEquals(List.of("stone"), form.get("input_stone"));
            writes.incrementAndGet();
        });
        app.start("127.0.0.1", 0);
        try (HttpClient client = HttpClient.newHttpClient()) {
            String base = "http://127.0.0.1:" + app.port() + contextPath;
            var get = HttpRequest.newBuilder(URI.create(base + "/editHopperConfig?token=" + token)).GET().build();
            var page = client.send(get, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains(token));
            assertFalse(page.body().contains("bookValue"));
            assertFalse(page.body().contains("server-book-not-browser-data"));
            assertTrue(page.body().contains("./postEditHopperConfig"));
            assertEquals("no-store", page.headers().firstValue("Cache-Control").orElseThrow());
            assertEquals("no-referrer", page.headers().firstValue("Referrer-Policy").orElseThrow());
            var post = HttpRequest.newBuilder(URI.create(base + "/postEditHopperConfig"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("token=" + token + "&bookValue=forged&userCode=forged&frameID=999&input_stone=stone")).build();
            assertEquals(200, client.send(post, HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(403, client.send(post, HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(1, writes.get());
        } finally { app.stop(); }
    }
    @Test void legacyIdentifiersCannotAuthorizeAnyRoute() throws Exception {
        WebSessions sessions = new WebSessions();
        Javalin app = Javalin.create(c -> c.showJavalinBanner = false);
        Handler denied = ctx -> { throw new AssertionError("Unauthorized request"); };
        ItemSorter.registerEditor(app, sessions, "/HopperConfig", "/postHopperConfig", false, denied, denied);
        ItemSorter.registerEditor(app, sessions, "/editHopperConfig", "/postEditHopperConfig", true, denied, denied);
        app.start("127.0.0.1", 0);
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String path : List.of("/HopperConfig", "/editHopperConfig")) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path + "?userCode=" + UUID.randomUUID() + "&frameID=1")).GET().build();
                assertEquals(403, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
            }
            for (String path : List.of("/postHopperConfig", "/postEditHopperConfig")) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("userCode=" + UUID.randomUUID() + "&frameID=1&bookValue=untrusted")).build();
                assertEquals(403, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
            }
        } finally { app.stop(); }
    }
}
