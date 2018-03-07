package com.github.Jochyoua.MCSF;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class MCSFCommand implements CommandExecutor {
	Main plugin = null;

	public MCSFCommand(Main pl) {
		this.plugin = pl;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {
		plugin.reloadConfig();
		if (cmd.getName().equalsIgnoreCase("MCSF")) {
			if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
				if (!sender.hasPermission("MCSF.help")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("messages.incorrectPermissionMessage")
									.replaceAll("%prefix%", plugin.getConfig().getString("prefix"))
									.replaceAll("%permission%", "MCSF.help")));
				}
				for(String string : plugin.getConfig().getStringList("messages.helpMessages")) {
					plugin.util.sendMessage(sender, string);
				}
				return true;
			}
			if (args[0].equalsIgnoreCase("toggle") && sender instanceof Player) {
				if (!sender.hasPermission("MCSF.toggle")) {
					plugin.util.sendMessage(sender, plugin.getConfig().getString("messages.incorrectPermissionMessage")
							.replaceAll("%permission%", "MCSF.toggle"));
					return true;
				}
				boolean value = plugin.util.toggle((Player) sender);
				String replacement = (value ? "enabled" : "disabled");
				plugin.util.sendMessage(sender,
						plugin.getConfig().getString("messages.toggleMessage").replaceAll("%value%", replacement));
				return true;
			}
			if (args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("MCSF.reload")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("messages.incorrectPermissionMessage")
									.replaceAll("%prefix%", plugin.getConfig().getString("prefix"))
									.replaceAll("%permission%", "MCSF.reload")));
					return true;
				}
				plugin.reloadConfig();
				plugin.util.generateswearList();
				plugin.util.sendMessage(sender, plugin.getConfig().getString("messages.reloadMessage"));
				return true;
			}
			if (args[0].equalsIgnoreCase("modify")) {
				if (args.length == 3) {
					if (!sender.hasPermission("MCSF.modify")) {
						plugin.util.sendMessage(sender,
								plugin.getConfig().getString("messages.incorrectPermissionMessage")
										.replaceAll("%permission%", "MCSF.modify"));
						return true;
					}
					if (args[1].equalsIgnoreCase("add")) {
						String replacement = (plugin.util.addData(args[2]) ? "successfully" : "unsuccessfully");
						plugin.util.sendMessage(sender, plugin.getConfig().getString("messages.addedMessage")
								.replaceAll("%value%", replacement));
						plugin.reloadConfig();
						return true;
					} else if (args[1].equalsIgnoreCase("remove")) {
						String replacement = (plugin.util.removeData(args[2]) ? "successfully" : "unsuccessfully");
						plugin.util.sendMessage(sender, plugin.getConfig().getString("messages.removedMessage")
								.replaceAll("%value%", replacement));
						plugin.reloadConfig();
						return true;
					}
				}
			}
		}
		plugin.util.sendMessage(sender, plugin.getConfig().getString("messages.invalidSyntaxMessage"));
		return true;
	}
}