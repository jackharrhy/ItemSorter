package tollenaar.stephen.ItemSorter.Commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import tollenaar.stephen.ItemSorter.Core.ItemSorter;

public class CommandsHandler implements CommandExecutor {

    private static final Map<String, SubCommand> commands = new HashMap<>();

	public CommandsHandler(ItemSorter plugin) {
		commands.put("legacy", new Legacy(plugin));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length >= 1) {
			for (String s : commands.keySet()) {
				if (s.equals(args[0])) {
					commands.get(s).onCommand(sender, command, label, args);
					return true;
				}
			}
		}
		sender.sendMessage("Current commands supported: " + commands.keySet());
		return true;
	}

}
