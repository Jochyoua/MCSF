package com.github.Jochyoua.MCSF.checks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import com.github.Jochyoua.MCSF.Main;

import net.md_5.bungee.api.ChatColor;

public class bookCheck implements Listener {
	Main plugin = null;

	public bookCheck(Main pl) {
		this.plugin = pl;
	}

	@EventHandler
	public void bookEvent(PlayerEditBookEvent event) {

		if (plugin.getConfig().getBoolean("bookCheck")) {
			Player player = event.getPlayer();
			BookMeta meta = event.getNewBookMeta();
			List<String> lines = new ArrayList<String>();
			boolean containsSwears = false;
			for (String string : meta.getPages()) {
				containsSwears = plugin.util.getSwears().stream().parallel()
						.anyMatch(org.apache.commons.lang3.StringUtils.stripAccents(string).toLowerCase()::contains);
				lines.add(org.apache.commons.lang3.StringUtils.stripAccents(plugin.util.filter(string)));
			}
			if (containsSwears)
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig()
						.getString("messages.bookCheckMessage").replaceAll("%prefix%", plugin.getConfig().getString("prefix"))));

			meta.setDisplayName(org.apache.commons.lang3.StringUtils.stripAccents(plugin.util.filter(meta.getTitle())));
			meta.setPages(lines);
			event.setNewBookMeta(meta);
		}

	}
}
