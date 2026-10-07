package tollenaar.stephen.ItemSorter.Commands;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.ItemFrame;
import tollenaar.stephen.ItemSorter.Core.ItemSorter;
import tollenaar.stephen.ItemSorter.Util.Server.Book;

public class CommandsHandler implements CommandExecutor {
    private final Legacy legacy;
    public CommandsHandler(ItemSorter plugin) { legacy = new Legacy(plugin); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equals("legacy")) {
            legacy.onCommand(sender, command, label, args);
            return true;
        }
        if (args.length == 1 && args[0].equals("portable")) {
            if (!sender.isOp()) { sender.sendMessage("Only operators can convert books."); return true; }
            int count = 0;
            var key = new NamespacedKey("itemsorter", "itemsorter");
            try {
                for (var player : Bukkit.getOnlinePlayers()) {
                    var contents = player.getInventory().getContents();
                    for (int i = 0; i < contents.length; i++) if (Book.makePortable(contents[i], key)) {
                        player.getInventory().setItem(i, contents[i]);
                        count++;
                    }
                }
                for (var world : Bukkit.getWorlds()) for (var entity : world.getEntities()) {
                    if (entity instanceof ItemFrame frame) {
                        var item = frame.getItem();
                        if (Book.makePortable(item, key)) { frame.setItem(item); count++; }
                    }
                }
                sender.sendMessage("Converted " + count + " loaded books. Unloaded chunks, offline players and stored books need separate conversion before migration.");
            } catch (java.io.IOException | ClassNotFoundException | IllegalArgumentException invalid) {
                sender.sendMessage("Conversion stopped at an unreadable book; " + count + " books converted.");
            }
            return true;
        }
        sender.sendMessage("Current commands supported: legacy, portable");
        return true;
    }
}
