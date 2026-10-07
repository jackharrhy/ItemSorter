# ItemSorter — Paper and Fabric 26.2

Server-side hopper filtering and a token-authorized web editor. Vanilla 26.2 clients need no mod.

## Build

Use Java 26 for Gradle's toolchain (both artifacts target Java 25), Maven 3.9.9 and the included Gradle wrapper:

```sh
python3 tools/restore-icons.py
mvn clean package
./gradlew :fabric:clean :fabric:build :fabric:smokeJar --no-daemon
```

Generated icons are restored from a checksum-pinned upstream release, not committed. Both jars include them.

- Paper: `output/ItemSorter-26.2.jar`, installed under `plugins/`.
- Fabric: `fabric/build/libs/ItemSorter-Fabric-26.2.jar`, installed under `mods/`, with Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2.
- Do not install the thin or smoke jars on a normal server.

The Fabric runtime jar privately relocates Javalin, Kotlin, Jetty/servlet and the
Thymeleaf stack (including OGNL/Javassist), merges relocated service descriptors,
and leaves Minecraft-owned Gson/SLF4J out. This allows other mods such as
Storefront to bundle different web-library versions in the same classloader.
The Storefront repository's `dev/combined-web-classpath.py` checks both shipped
jars together over HTTP in both classpath orders, without launching Minecraft.

## Use

Attach an item frame to a hopper. Right-click its empty frame with a book and quill to receive a clickable editor link. Select filters, submit, and the frame receives a vanilla written configuration book. Right-click a configured frame or held configuration book for editing. Keep the player online while saving; links expire after ten minutes, are single-use on submission, and are replaced by the next link for that player.

Paper configuration lives in `plugins/ItemSorter/config.yml`; Fabric creates `config/itemsorter.properties`. Both support `host`, `port`, `URL`, and `contextPath`. Set `host=127.0.0.1` (YAML `host: '127.0.0.1'`) behind a local reverse proxy; set URL to the externally reachable editor base ending in `/`. For a path prefix, include it in both URL and contextPath. Keep editor links private: they are bearer credentials. Existing Paper route names and disabled-items configuration remain supported; Fabric uses the standard route names, a 100-material limit, and excludes spawn eggs.

Material/enchantment/custom-potion-effect choices, strict dropped-item filtering, overflow prevention, linked-lower filters and junction ratios are supported. Vanilla hopper cooldowns, redstone and inventory movement still apply. Paper can stop trying the remaining chest slots when a transfer is canceled: move the rejected stack out of the first slot to observe subsequent allowed stacks.

## Existing books and data

New books on either platform store version-1 JSON under the same `itemsorter:itemsorter` key. Fabric uses vanilla custom data's `PublicBukkitValues` compound; Paper uses its persistent-data API. Written book pages remain readable by vanilla clients.

Paper still reads its historical Java-serialized books. Before moving existing books to Fabric, an operator must run `/itemsorter portable` on Paper: this converts books in online player inventories and loaded item frames. It does **not** traverse unloaded chunks, offline players, chest contents or nested containers. Load/move those books into supported locations and convert them separately. Keep a verified backup before any real migration. Fabric rejects unconverted legacy filters rather than silently interpreting them.

Fabric discovers filters directly from attached frame books; it does not import Paper's SQLite cache. On returning to Paper, loaded tagged frame books are registered through entity-load events. This is book-metadata interoperability, not permission to open production worlds on another implementation; world-layout migration must be handled separately.

## Tests

`mvn test` retains all original security/regression tests and adds portable-book/filter/registration coverage. `./gradlew :fabric:test` runs the shared actual-HTTP editor tests and Fabric vanilla-book tests. Tests cover unauthorized GET/POST, token expiry/replay, hostile browser identity fields, and malformed configuration rejection.

Isolated smoke sources are in `src/smoke` (Paper) and `fabric/src/smoke` (Fabric). They are destructive to their **test** world and shut down their test server: never install them in production. Run only against separate fresh scratch worlds with game/editor bound to `127.0.0.1:25676` and `127.0.0.1:8766`, sequentially. Fabric smoke asserts actual Minecraft transfer/mixin behavior, overflow, linking, ratios and authorized HTTP save/replay against a real frame. Paper smoke checks generated portable metadata, tick-driven chest-to-hopper filtering and unauthenticated HTTP rejection. Human vanilla-client clicks, survival book consumption/duplication, restart persistence, two-player interaction and cross-platform copied-world migrations still require player-level verification.
