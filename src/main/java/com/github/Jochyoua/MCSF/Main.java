package com.github.Jochyoua.MCSF;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.Jochyoua.MCSF.events.*;
import github.scarsz.discordsrv.DiscordSRV;
import me.vagdedes.mysql.database.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.UUID;

public class Main extends JavaPlugin {
    public static Plugin plugin;
    MySQL MySQL;
    Utils utils;

    public static Main getInstance() {
        return (Main) plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        MySQL = new MySQL(this);
        utils = new Utils(this, MySQL);
        saveDefaultConfig();
        getConfig().options().header(
                "MCSF (My Christian Swear Filter) v" + getDescription().getVersion() + " by Jochyoua\n"
                        + "This is a toggleable swear filter for your players\n"
                        + "Resource: https://www.spigotmc.org/resources/54115/\n"
                        + "Github: https://www.github.com/Jochyoua/MCSF/\n");
        getConfig().options().copyDefaults(true);
        saveConfig();
        new DiscordEvents(this, utils);
        new CommandEvents(this, MySQL, utils);
        new PlayerEvents(this, utils, MySQL);
        if (getConfig().getBoolean("settings.mysql")) {
            utils.send(Bukkit.getConsoleSender(), "MySQL brought to you by MySQL API: https://www.spigotmc.org/resources/mysql-api.23932/");
            if (!MySQL.isConnected())
                MySQL.connect();
            if (MySQL.isConnected()) {
                if (!MySQL.tableExists("swears") || MySQL.countRows("swears") == 0 || !MySQL.tableExists("users") || MySQL.countRows("users") == 0) {
                    utils.createTable(false);
                }
                utils.reload();
                utils.debug("MySQL has been enabled & information has been set");
            } else {
                utils.send(Bukkit.getConsoleSender(), getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.failedtoconnect")));
            }
        }
        new PunishmentEvents(this, utils);
        new SignEvents(this, utils);

        if (!getConfig().getBoolean("settings.only_filter_players")) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.CHAT) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    Player player = event.getPlayer();
                    UUID ID = player.getUniqueId();
                    PacketContainer packet = event.getPacket();
                    StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
                    for (WrappedChatComponent component : chatComponents.getValues()) {
                        if (getConfig().getBoolean("settings.force") || utils.status(ID)) {
                            reloadConfig();
                            if (component != null) {
                                if (!component.getJson().isEmpty()) {
                                    if (!utils.isclean(component.getJson())) {
                                        String string = utils.clean(component.getJson(), false, true);
                                        if (string == null) {
                                            return;
                                        }
                                        component.setJson(string);
                                        chatComponents.write(0, component);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }

        utils.reload();
        final String test = getConfig().getStringList("swears").get((new Random()).nextInt(getConfig().getStringList("swears").size()));
        String clean = utils.clean(test, true, false);
        utils.debug("Running filter test for `" + test + "`; returns as: `" + clean + "`");
        if (getConfig().getBoolean("settings.console_motd")) {
            utils.send(Bukkit.getConsoleSender(), String.join("\n", getConfig().getStringList("variables.console_motd")));
        }
        if (getConfig().getBoolean("settings.check_for_updates")) {
            utils.send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.checking"));
            if (!utils.isUpToDate()) {
                utils.send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.update_available"));
                utils.send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.update_link"));
            } else {
                utils.send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.no_new_version"));
            }
        }
        if (getConfig().getBoolean("settings.metrics")) {
            final Metrics metrics = new Metrics(this);
            utils.debug("Metrics is " + (metrics.isEnabled() ? "enabled" : "disabled"));
        }
    }

    @Override
    public void onDisable() {
        if (MySQL.isConnected())
            MySQL.disconnect();
        if (utils.supported("DiscordSRV"))
            DiscordSRV.api.unsubscribe(this);
    }
}