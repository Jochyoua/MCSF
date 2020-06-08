package com.github.Jochyoua.MCSF.events;

import com.comphenix.protocol.ProtocolLibrary;
import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import com.github.Jochyoua.MCSF.signcheck.SignPacketListener;
import com.github.Jochyoua.MCSF.signcheck.SignViewEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class SignEvents implements Listener {
    Main plugin;
    Utils utils;

    public SignEvents(Main plugin, Utils utils) {
        this.plugin = plugin;
        this.utils = utils;
        if (!plugin.getConfig().getBoolean("settings.signcheck")) {
            return;
        }
        if (!utils.supported("SignCheck")) {
            utils.send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.unsupported")).replace("%feature%", "sign filtering"));
            return;
        }

            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            ProtocolLibrary.getProtocolManager().addPacketListener(new SignPacketListener());
    }

    // Sign Events
    @EventHandler
    public void viewSign(SignViewEvent event) {
        if (plugin.getConfig().getBoolean("settings.signcheck") && utils.status(event.getPlayer().getUniqueId())) {
            List<String> lines = new ArrayList<>();
            for (String line : event.getLines()) {
                if (!utils.isclean(line)) {
                    line = utils.clean(line, false, true);
                }
                lines.add(line);
            }
            event.setLines(lines.toArray(new String[0]));
        }
    }
}
