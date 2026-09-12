package tollenaar.stephen.ItemSorter.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import tollenaar.stephen.ItemSorter.Core.ItemSorter;

public abstract class SubCommand {

	// The legacy section sign that Bukkit's deprecated ChatColor.COLOR_CHAR held.
	private static final char COLOR_CHAR = '\u00A7';

	protected ItemSorter plugin;

	public SubCommand(ItemSorter plugin) {
		this.plugin = plugin;
	}

	public abstract void onCommand(CommandSender sender, Command cmd, String label, String[] args);

	protected String convertToInvisibleString(String s) {
		StringBuilder hidden = new StringBuilder();
		for (char c : s.toCharArray())
			hidden.append(COLOR_CHAR).append(c);
		return hidden.toString();
	}
}
