package com.github.Jochyoua.MCSF.checks;

import java.util.Arrays;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import com.github.Jochyoua.MCSF.Main;

public class signCheck implements Listener {
	public Main plugin;

	public signCheck(Main pl) {
		this.plugin = pl;
	}

	@EventHandler
	public void onSignPlace(SignChangeEvent event) {
		if (plugin.getConfig().getBoolean("signCheck")) {

			List<String> lines = Arrays.asList(event.getLines());
			StringBuilder str = new StringBuilder();
			lines.stream().forEach(s -> {
				str.append(s);
			});
			boolean checked = false;
			if (plugin.util.getSwears().stream().parallel()
					.anyMatch(org.apache.commons.lang3.StringUtils.stripAccents(str.toString()).toLowerCase()::contains)
					&& !checked) {
				plugin.util.sendMessage(event.getPlayer(), plugin.getConfig()
						.getString("messages.signCheckMessage"));
				event.setCancelled(true);
				event.getBlock().breakNaturally();
				checked = true;
			}
		}
	}
}
