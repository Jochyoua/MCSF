package com.github.Jochyoua.MCSF.events;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;
import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.shared.Filters;
import com.github.Jochyoua.MCSF.shared.Utils;
import com.github.Jochyoua.MCSF.signcheck.SignPacketListener;
import com.github.Jochyoua.MCSF.signcheck.SignViewEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SignEvents implements Listener {
    MCSF plugin;
    Utils utils;

    public SignEvents(MCSF plugin, Utils utils) {
        this.plugin = plugin;
        this.utils = utils;
        if (!plugin.getConfig().getBoolean("settings.signcheck")) {
            return;
        }
        if (utils.supported("signcheck")) {
            PacketListener pl = new SignPacketListener();
            ProtocolLibrary.getProtocolManager().addPacketListener(pl);
            if (ProtocolLibrary.getProtocolManager().getPacketListeners().contains(pl)) {
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
            }
        } else {
            utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.unsupported")).replaceAll("(?i)\\{feature}", "Sign Filtering"));
        }
    }

    // Sign Events
    @EventHandler
    public void viewSign(SignViewEvent event) {
        if (plugin.getConfig().getBoolean("settings.signcheck")) {
            if (!(event.getLine(0).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(1).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(2).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(3).equalsIgnoreCase("{\"text\":\"\"}"))) {
                String lines = String.join("_", event.getLines());
                if (utils.status(event.getPlayer().getUniqueId())) {
                    if (!utils.isclean(lines)) {
                        lines = utils.clean(lines, true, false, Filters.SIGNS);
                    }
                }
                event.setLines(lines.split("_"));
            }
        }
    }
}
