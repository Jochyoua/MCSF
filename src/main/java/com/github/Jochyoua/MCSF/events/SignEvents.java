package com.github.Jochyoua.MCSF.events;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;
import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import com.github.Jochyoua.MCSF.signcheck.SignPacketListener;
import com.github.Jochyoua.MCSF.signcheck.SignViewEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SignEvents implements Listener {
    Main plugin;
    Utils utils;

    public SignEvents(Main plugin, Utils utils) {
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
            utils.send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.unsupported")).replace("%feature%", "Sign Filtering"));
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
                        lines = utils.clean(lines, true, false);
                    }
                }
                event.setLines(lines.split("_"));
            }
        }
    }
}
