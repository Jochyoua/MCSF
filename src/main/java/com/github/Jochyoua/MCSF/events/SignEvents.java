package com.github.Jochyoua.MCSF.events;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;
import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.shared.Types;
import com.github.Jochyoua.MCSF.shared.Utils;
import com.github.Jochyoua.MCSF.signcheck.SignPacketListener;
import com.github.Jochyoua.MCSF.signcheck.SignViewEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.logging.Level;

public class SignEvents implements Listener {
    MCSF plugin;
    Utils utils;

    public SignEvents(MCSF plugin, Utils utils) {
        this.plugin = plugin;
        this.utils = utils;
        if (!plugin.getConfig().getBoolean("settings.filtering.filter checks.signcheck")) {
            return;
        }
        if (utils.supported("signcheck")) {
            try {
                PacketListener pl = new SignPacketListener();
                ProtocolLibrary.getProtocolManager().addPacketListener(pl);
                if (ProtocolLibrary.getProtocolManager().getPacketListeners().contains(pl)) {
                    plugin.getServer().getPluginManager().registerEvents(this, plugin);
                }
            }catch(Exception e){
                FileConfiguration language = plugin.getLanguage();
                plugin.getLogger().log(Level.SEVERE, "Failure: {message}"
                        .replaceAll("(?i)\\{message}|(?i)%message%",
                                Objects.requireNonNull(language.getString("variables.error.execute_failure"))
                                        .replaceAll("(?i)\\{feature}", "Sign Filtering")), e);
                plugin.getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))));
            }
        } else {
            utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.unsupported")).replaceAll("(?i)\\{feature}", "Sign Filtering"));
        }
    }

    // Sign Events
    @EventHandler
    public void viewSign(SignViewEvent event) {
        if (plugin.getConfig().getBoolean("settings.filtering.filter checks.signcheck")) {
            if (!(event.getLine(0).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(1).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(2).equalsIgnoreCase("{\"text\":\"\"}")
                    && event.getLine(3).equalsIgnoreCase("{\"text\":\"\"}"))) {
                String lines = String.join("_", event.getLines());
                if (utils.status(event.getPlayer().getUniqueId())) {
                    if (!utils.isclean(lines)) {
                        lines = utils.clean(lines, true, false, Types.Filters.SIGNS);
                    }
                }
                event.setLines(lines.split("_"));
            }
        }
    }
}
