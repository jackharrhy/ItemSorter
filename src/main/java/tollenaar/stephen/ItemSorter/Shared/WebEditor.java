package tollenaar.stephen.ItemSorter.Shared;
import java.security.SecureRandom;
import java.util.*;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
public final class WebEditor {
    private WebEditor() {}
    public static void configure(JavalinConfig config, String contextPath) {
        config.addStaticFiles("itemsorter-web", Location.CLASSPATH);
        config.showJavalinBanner = false;
        config.maxRequestSize = 30000L;
        config.contextPath = contextPath;
    }
    public static void registerEditor(Javalin app, WebSessions sessions, String getPath, String postPath,
            boolean edit, Handler load, Handler save) {
        app.get(getPath, ctx -> {
            ctx.header("Cache-Control", "no-store");
            ctx.header("Referrer-Policy", "no-referrer");
            String token = ctx.queryParam("token");
            WebSessions.Session session = sessions.get(token, edit);
            if (session == null) {
                ctx.status(403).result("Open a fresh link in game.");
                return;
            }
            ctx.attribute("session", session);
            try {
                load.handle(ctx);
            } catch (IllegalArgumentException invalid) {
                ctx.status(400).result("Configuration rejected. Open a fresh link in game.");
                return;
            }
            ctx.attribute("token", token);
            ctx.attribute("postAction", "." + postPath);
            ctx.render("/itemsorter-web/index.html");
        });
        app.post(postPath, ctx -> {
            ctx.header("Cache-Control", "no-store");
            ctx.header("Referrer-Policy", "no-referrer");
            WebSessions.Session session = sessions.consume(ctx.formParam("token"), edit);
            if (session == null) {
                ctx.status(403).result("Open a fresh link in game.");
                return;
            }
            Map<String, List<String>> form = new HashMap<>(ctx.formParamMap());
            for (String key : List.of("token", "userCode", "frameID", "bookValue", "configData")) form.remove(key);
            ctx.attribute("session", session);
            ctx.attribute("form", form);
            try {
                save.handle(ctx);
            } catch (IllegalArgumentException invalid) {
                ctx.status(400).result("Configuration rejected. Check your choices and open a fresh link in game.");
                return;
            }
            ctx.attribute("response", "Thank you. You can close this page now.");
            ctx.render("/itemsorter-web/response.html");
        });
    }

    public static class WebSessions {
        public record Session(UUID player, Integer frameId, String bookValue, long expiresAt) {}
        private final SecureRandom random = new SecureRandom();
        private final Map<String, Session> sessions = new HashMap<>();
        private final java.util.function.LongSupplier now;

        public WebSessions() { this(System::currentTimeMillis); }
        public WebSessions(java.util.function.LongSupplier now) { this.now = now; }

        public synchronized String issue(UUID player, Integer frameId, String bookValue) {
            sessions.values().removeIf(session -> session.player().equals(player) || session.expiresAt() <= now.getAsLong());
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            sessions.put(token, new Session(player, frameId, bookValue, now.getAsLong() + 600_000));
            return token;
        }

        public synchronized Session get(String token, boolean edit) {
            Session session = sessions.get(token);
            if (session != null && session.expiresAt() <= now.getAsLong()) {
                sessions.remove(token);
                return null;
            }
            return session != null && (session.bookValue() != null) == edit ? session : null;
        }

        public synchronized Session consume(String token, boolean edit) {
            Session session = get(token, edit);
            if (session != null) sessions.remove(token);
            return session;
        }

        public synchronized void revoke(UUID player) {
            sessions.values().removeIf(session -> session.player().equals(player));
        }

        public synchronized void clear() { sessions.clear(); }
    }

}
