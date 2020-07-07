package com.github.Jochyoua.MCSF;

import be.dezijwegel.configapi.ConfigAPI;
import be.dezijwegel.configapi.Settings;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.Jochyoua.MCSF.events.CommandEvents;
import com.github.Jochyoua.MCSF.events.PlayerEvents;
import com.github.Jochyoua.MCSF.events.PunishmentEvents;
import com.github.Jochyoua.MCSF.events.SignEvents;
import com.github.Jochyoua.MCSF.shared.Filters;
import com.github.Jochyoua.MCSF.shared.Metrics;
import com.github.Jochyoua.MCSF.shared.MySQL;
import com.github.Jochyoua.MCSF.shared.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class MCSF extends JavaPlugin {
    public static Plugin plugin;
    MySQL MySQL;
    Utils utils;
    ConfigAPI config = null;

    public static MCSF getInstance() {
        return (MCSF) plugin;
    }

    public void loadLanguage() {
        String language = getConfig().getString("settings.language", "en_us").replaceAll(".yml", "");
        Settings settings = new Settings();
        settings.setSetting("reportMissingOptions", false);
        config = new ConfigAPI("locales/" + language + ".yml", settings, this);
        config.copyDefConfigIfNeeded();
        YamlConfiguration conf = config.getLiveConfiguration();
        Map<String, Object> missingOptions = config.getMissingOptions(conf, config.getDefaultConfiguration());
        if (!missingOptions.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("There are some missing options! Filling them in now!");
            for (Map.Entry<String, Object> missing : missingOptions.entrySet()) {
                conf.set(missing.getKey(), missing.getValue());
            }
            try {
                conf.save(new File(plugin.getDataFolder(), "locales/" + language + ".yml"));
            } catch (IOException ignored) {
            }
        }
    }

    public void reloadLanguage() {
        if (config == null) {
            loadLanguage();
        }
        config.reloadContents();
    }

    public YamlConfiguration getLanguage() {
        if (config == null) {
            reloadConfig();
        }
        return config.getLiveConfiguration();
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
        if (getConfig().isSet("replacements.all")) {
            if (getConfig().getString("replacements.all").equalsIgnoreCase("&c*&f") && !getConfig().getString("settings.replacement").equalsIgnoreCase("&c*&f")) {
                getConfig().set("replacements.all", getConfig().getString("settings.replacement"));
            }
        }
        saveConfig();
        loadLanguage();
        new CommandEvents(this, MySQL, utils);
        new PlayerEvents(this, MySQL, utils);
        if (utils.supported("mysql")) {
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
                utils.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%}", getLanguage().getString("variables.error.failedtoconnect")));
            }
        }
        if (getConfig().getBoolean("settings.punish_players"))
            new PunishmentEvents(this, utils);
        if (getConfig().getBoolean("settings.signcheck"))
            new SignEvents(this, utils);

        if (!getConfig().getBoolean("settings.only_filter_players")) {
            try {
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
                                            String string = utils.clean(component.getJson(), false, true, Filters.ALL);
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
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, getLanguage().getString("variables.failure")
                        .replaceAll("(?i)\\{message}|(?i)%message%",
                                getLanguage().getString("variables.error.execute_failure")
                                        .replaceAll("(?i)\\{feature}|(?i)%feature%", "Chat Filtering (ProtocolLib)")), e);
                plugin.getLogger().log(Level.INFO, getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", getLanguage().getString("variables.error.execute_failure_link")));
            }
        }

        utils.reload();
        final String test = getConfig().getStringList("swears").get((new Random()).nextInt(getConfig().getStringList("swears").size()));
        String clean = utils.clean(test, true, false, Filters.DEBUG);
        utils.debug("Running filter test for `" + test + "`; returns as: `" + clean + "`");
        if (getConfig().getBoolean("settings.console_motd")) {
            utils.send(Bukkit.getConsoleSender(), String.join("\n", getLanguage().getStringList("variables.console_motd")));
        }
        if (getConfig().getBoolean("settings.check_for_updates")) {
            Bukkit.getScheduler().runTask(this, () -> {
                utils.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.updatecheck.checking"));
                if (!utils.isUpToDate()) {
                    utils.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.updatecheck.update_available"));
                    utils.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.updatecheck.update_link"));
                } else {
                    utils.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.updatecheck.no_new_version"));
                }
            });
        }
        final Metrics metrics = new Metrics(this, 4345);
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> getConfig().getString("settings.language", "en_us")));
        utils.debug("Metrics is " + (metrics.isEnabled() ? getLanguage().getString("variables.activated") : getLanguage().getString("variables.deactivated")) + "\nDisable it through the global bStats config.");
        //Reloaded plugin check:
        if (Bukkit.getOnlinePlayers().size() != 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getConfig().set("users." + player.getUniqueId() + ".playername", player.getName().toLowerCase());
                plugin.saveConfig();
                if (!plugin.getConfig().isSet("users." + player.getUniqueId() + ".playername")) {
                    utils.debug("There was an issue saving " + player.getName() + "'s name to the config.");
                } else {
                    utils.debug("Successfully added " + player.getName() + "'s name to the config.");
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (MySQL.isConnected())
            MySQL.disconnect();
    }
}