package com.github.Jochyoua.MCSF;

import be.dezijwegel.configapi.ConfigAPI;
import be.dezijwegel.configapi.Settings;
import be.dezijwegel.configapi.utility.Logger;
import com.github.Jochyoua.MCSF.events.*;
import com.github.Jochyoua.MCSF.shared.MySQL;
import com.github.Jochyoua.MCSF.shared.Types;
import com.github.Jochyoua.MCSF.shared.Utils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MCSF extends JavaPlugin {
    public static Plugin plugin;
    public Utils utils;
    MySQL MySQL;
    ConfigAPI lang = null;


    public static MCSF getInstance() {
        return (MCSF) plugin;
    }

    public void loadLanguage() {
        String language = Types.Languages.getLanguage(getInstance());
        Settings settings = new Settings();
        settings.setSetting("reportMissingOptions", false);
        lang = new ConfigAPI("locales/" + language + ".yml", settings, this);
        lang.copyDefConfigIfNeeded();
        YamlConfiguration conf = lang.getLiveConfiguration();
        Map<String, Object> missingOptions = lang.getMissingOptions(conf, lang.getDefaultConfiguration());
        if (!missingOptions.isEmpty()) {
            for (Map.Entry<String, Object> missing : missingOptions.entrySet()) {
                conf.set(missing.getKey(), missing.getValue());
            }
            try {
                conf.save(new File(plugin.getDataFolder(), "locales/" + language + ".yml"));
                utils.debug("Missing options have been found in " + language + ".yml!");
                utils.debug("No fret, the default values have been filled.");
                utils.debug("Successfully saved " + language + ".yml!");
            } catch (IOException e) {
                Logger.log("Failed to save " + language + ".yml!:\n" + e.getMessage());
            }
        }
    }
    
    public void reloadLanguage() {
        if (lang == null)
            loadLanguage();
        YamlConfiguration conf = lang.getLiveConfiguration();
        if (!lang.getMissingOptions(conf, lang.getDefaultConfiguration()).isEmpty())
            loadLanguage(); // Reload the language file because some details are missing - Prevents nullpointerexception in some cases
        lang.reloadContents();
    }

    public YamlConfiguration getLanguage() {
        if (lang == null)
            reloadLanguage();
        YamlConfiguration conf = lang.getLiveConfiguration();
        if (!lang.getMissingOptions(conf, lang.getDefaultConfiguration()).isEmpty())
            reloadLanguage();
        return conf;
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
            if (Objects.requireNonNull(getConfig().getString("replacements.all")).equalsIgnoreCase("&c*&f") && !getConfig().getString("settings.filtering.replacement").equalsIgnoreCase("&c*&f")) {
                getConfig().set("replacements.all", getConfig().getString("settings.filtering.replacement"));
            }
        }
        saveConfig();
        loadLanguage();
        // Loop through each language file that isn't the selected one and add it to /locales/ folder
        for (Types.Languages language : Types.Languages.values()) {
            if (!(new File(getDataFolder(), "locales/" + language.name() + ".yml").exists()) && !Types.Languages.getLanguage(this).equalsIgnoreCase(language.name())) {
                Settings settings = new Settings();
                settings.setSetting("reportMissingOptions", false);
                ConfigAPI lang = new ConfigAPI("locales/" + language.name() + ".yml", settings, this);
                lang.copyDefConfigIfNeeded();
            }
        }
        new CommandEvents(this, MySQL, utils);
        new PlayerEvents(this, MySQL, utils);
        if (utils.supported("mysql")) {
            utils.send(Bukkit.getConsoleSender(), "MySQL brought to you by MySQL API: https://www.spigotmc.org/resources/mysql-api.23932/");
            if (!MySQL.isConnected())
                MySQL.connect();
        }
        if (!getConfig().getBoolean("settings.only filter players.enabled") && utils.supported("ProtocolLib"))
            new ProtocolLib(this, utils);
        if (getConfig().getBoolean("settings.filtering.punishments.punish players"))
            new PunishmentEvents(this, utils);
        if (getConfig().getBoolean("settings.filtering.filter checks.signcheck") && utils.supported("ProtocolLib"))
            new SignEvents(this, utils);
        if (utils.supported("DiscordSRV"))
            new DiscordEvents(this, utils);
        utils.reload();
        final String test = getConfig().getStringList("swears").get((new Random()).nextInt(getConfig().getStringList("swears").size()));
        String clean = utils.clean(test, true, false, Types.Filters.DEBUG);
        utils.debug("Running filter test for `" + test + "`; returns as: `" + clean + "`");
        if (getConfig().getBoolean("settings.console motd")) {
            utils.send(Bukkit.getConsoleSender(), String.join("\n", getLanguage().getStringList("variables.console motd")));
        }
        if (getConfig().getBoolean("settings.updating.check for updates")) {
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
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> Types.Languages.getLanguage(this)));
        utils.debug("Metrics is " + (metrics.isEnabled() ? "enabled; Disable" : "disabled; Enable") + " it through the global bStats config.");

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
}