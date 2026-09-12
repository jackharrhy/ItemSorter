package tollenaar.stephen.ItemSorter.Core;

import tollenaar.stephen.ItemSorter.Core.ItemSorter.WebSessions;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WebSessionsTest {
    @Test void concurrentSubmissionsHaveOnlyOneWinner() throws Exception {
        WebSessions sessions = new WebSessions();
        String token = sessions.issue(UUID.randomUUID(), 12, null);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var calls = new java.util.ArrayList<java.util.concurrent.Callable<Boolean>>();
            for (int i = 0; i < 32; i++) calls.add(() -> sessions.consume(token, false) != null);
            int winners = 0;
            for (var result : executor.invokeAll(calls)) if (result.get()) winners++;
            assertEquals(1, winners);
        }
    }
    @Test void disconnectAndShutdownRevokeLinks() {
        WebSessions sessions = new WebSessions();
        UUID player = UUID.randomUUID();
        String token = sessions.issue(player, 12, null);
        sessions.revoke(player);
        assertNull(sessions.get(token, false));
        token = sessions.issue(player, 12, null);
        sessions.clear();
        assertNull(sessions.get(token, false));
    }
    @Test void linksExpireAfterTenMinutesAndSubmissionsAreSingleUse() {
        var now = new java.util.concurrent.atomic.AtomicLong(1000);
        WebSessions sessions = new WebSessions(now::get);
        UUID player = UUID.randomUUID();
        String token = sessions.issue(player, 12, null);
        now.addAndGet(600_000);
        assertNull(sessions.get(token, false));
        assertNull(sessions.consume(token, false));
        token = sessions.issue(player, 12, null);
        assertNull(sessions.consume(token, true));
        assertNotNull(sessions.consume(token, false));
        assertNull(sessions.consume(token, false));
    }
    @Test void aNewLinkRevokesThePreviousLinkButNotOtherPlayers() {
        WebSessions sessions = new WebSessions();
        UUID player = UUID.randomUUID();
        String old = sessions.issue(player, 12, null);
        String other = sessions.issue(UUID.randomUUID(), 13, null);
        String edit = sessions.issue(player, null, "server-owned-book");
        assertNull(sessions.get(old, false));
        assertNotNull(sessions.get(other, false));
        assertNull(sessions.get(edit, false));
        assertEquals("server-owned-book", sessions.get(edit, true).bookValue());
    }
    @Test void tokenIsAnOpaqueReferenceToServerOwnedState() {
        WebSessions sessions = new WebSessions();
        UUID player = UUID.randomUUID();
        String token = sessions.issue(player, 12, null);
        assertNotNull(token);
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(player, sessions.get(token, false).player());
        assertEquals(12, sessions.get(token, false).frameId());
        assertNull(sessions.get(player.toString(), false));
    }
}
