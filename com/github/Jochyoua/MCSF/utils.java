package com.github.Jochyoua.MCSF;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class utils {
	Main plugin = null;

	public utils(Main pl) {
		this.plugin = pl;
	}

	public List<String> getSwears() {
		return plugin.getConfig().getStringList("swearList");
	}

	public String filter(String str) {
		ArrayList<String> badWords = new ArrayList<>();
		for (int start = 0; start < str.length(); start++) {
			for (int offset = 1; offset < (str.length() + 1 - start); offset++) {
				String wordToCheck = str.substring(start, start + offset);
				if (getSwears().contains(wordToCheck.toLowerCase())) {
					badWords.add(wordToCheck);
				}
			}
		}
		for (String s : badWords) {
			String replacement = s.replaceAll("(?s).", "*");
			str = str.replaceAll("(?i)" + s, replacement);
		}
		return str;
	}

	public boolean enabled(Player player) {
		return plugin.getConfig().getBoolean("data.players." + player.getUniqueId().toString());
	}

	public boolean toggle(Player player) {
		FileConfiguration config = plugin.getConfig();
		String uuid = player.getUniqueId().toString();
		if (!config.isSet("data.players." + uuid)) {
			config.set("data.players." + uuid, true);
			plugin.saveConfig();
			return true;
		} else {
			boolean toReturn = (config.getBoolean("data.players." + uuid) ? false : true);
			config.set("data.players." + uuid, toReturn);
			plugin.saveConfig();
			return toReturn;
		}
	}

	public boolean addData(String string) {
		plugin.reloadConfig();
		boolean found = false;
		FileConfiguration config = plugin.getConfig();
		List<String> list = config.getStringList("swearList");
		for (String word : list) {
			if (word.equalsIgnoreCase(string)) {
				found = true;
			}
		}
		boolean success = false;
		if (!found) {
			list.add(string);
			config.set("swearList", list);
			plugin.saveConfig();
			success = true;
		}
		return success;
	}

	public boolean removeData(String string) {
		plugin.reloadConfig();
		FileConfiguration config = plugin.getConfig();
		List<String> list = config.getStringList("swearList");
		boolean success = list.remove(string);
		config.set("swearList", list);
		plugin.saveConfig();
		return success;
	}

	public void generateswearList() {
		FileConfiguration config = plugin.getConfig();
		if (config.getStringList("swearList").isEmpty()) {
			try {
				URL url = new URL(
						"https://docs.google.com/spreadsheets/d/1hIEi2YG3ydav1E06Bzf2mQbGZ12kh2fe4ISgLg_UBuM/export?format=csv");
				String string = IOUtils.toString(url.openStream(), "UTF-8");
				String[] content = string.split(",");
				List<String> list = new ArrayList<String>();
				for (String str : content) {
					str = str.replaceAll("\\r\\n", "");
					str = str.replaceAll("\"", "");
					str = str.replaceAll("'", "");
					if (Pattern.matches("[a-zA-Z]+", str)) {
						list.add(str.trim());
					}
				}
				list.sort(String::compareToIgnoreCase);
				config.set("swearList", list);
				plugin.saveConfig();
			} catch (UnknownHostException | MalformedURLException e) {
				plugin.getLogger().log(Level.SEVERE, "Could not download sample words:", e);
				this.addData("exampleword");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		plugin.reloadConfig();
		int length = plugin.getConfig().getStringList("swearList").size();
		plugin.getLogger().log(Level.INFO,
				"loaded " + length + " word"+(length != 1 ? 's' : "" )+" into the swear list.");

	}

	public void setupConfig() {
		FileConfiguration config = plugin.getConfig();
		List<String> list = new ArrayList<>();
		list.add("&e&m----&f&e[&fMCSF HELP&e]&m----");
		list.add("&e&l/mcsf help:");
		list.add("  &o&7This command outputs the plugin help");
		list.add("&e&l/mcsf toggle:");
		list.add("  &o&7This command toggles the swear filter");
		list.add("&e&l/mcsf modify <add/remove> <word>:");
		list.add("  &o&7This command modifies the swear list");
		list.add("&e&l/mcsf reload:");
		list.add("  &o&7This command reloads the plugin");
		config.options()
				.header("\"Do or don't do, that is the question\"\nMCSF(My Christian Swear Filter) by Jochyoua, v"
						+ plugin.getDescription().getVersion());
		config.addDefault("prefix", "[MCSF]");
		config.addDefault("messages.toggleMessage", "&e%prefix% &fYour swear filter has been %value%.");
		config.addDefault("messages.reloadMessage", "&e%prefix% &fThe Plugin has been reloaded");
		config.addDefault("messages.addedMessage", "&e%prefix% &fData has been %value% added.");
		config.addDefault("messages.removedMessage",
				"&e%prefix% &fData has been %value% removed.");
		config.addDefault("messages.incorrectPermissionMessage",
				"&e%prefix% &fYou lack the permission `&b%permission%&f`.");
		config.addDefault("messages.invalidSyntaxMessage", "&e%prefix% &fYou have used an invalid syntax.");
		config.addDefault("messages.signCheckMessage",
				"&e%prefix% &fPlease refrain from using swear words in your sign.");
		config.addDefault("messages.bookCheckMessage",
				"&e%prefix% &fPlease refrain from using swear words in your book.");
		config.addDefault("messages.helpMessages", list);
		config.addDefault("signCheck", true);
		config.addDefault("bookCheck", true);
		config.options().copyDefaults(true);
		config.options().copyHeader(true);
		plugin.saveConfig();
	}

	public void sendMessage(CommandSender player, String message) {
		message = message.replaceAll("%prefix%", plugin.getConfig().getString("prefix"));
		message = message.replaceAll("%player%", player.getName());
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

}