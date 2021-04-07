package io.github.jochyoua.mychristianswearfilter;

import io.github.jochyoua.mychristianswearfilter.commands.McsfCommand;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.events.PlayerEvents;
import io.github.jochyoua.mychristianswearfilter.events.PunishmentEvents;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.HikariCP;
import io.github.jochyoua.mychristianswearfilter.shared.hooks.DiscordSRV;
import io.github.jochyoua.mychristianswearfilter.shared.hooks.PlaceholderAPI;
import io.github.jochyoua.mychristianswearfilter.shared.hooks.ProtocolLib;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;


@Getter
public class MCSF extends JavaPlugin {
    private final HashMap<String, Integer> localSizes = new HashMap<>();
    private HikariCP hikariCP;
    private DatabaseConnector connector;
    private YamlConfiguration language;
    private Manager manager;
    private io.github.jochyoua.mychristianswearfilter.shared.hooks.ProtocolLib ProtocolLib;
    private io.github.jochyoua.mychristianswearfilter.shared.hooks.PlaceholderAPI PlaceholderAPI;
    private io.github.jochyoua.mychristianswearfilter.shared.hooks.DiscordSRV DiscordSRV;
    private Boolean needsUpdate = false;


    @Override
    public void onEnable() {

        // Sets the default configuration
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        // Retrieves the currently set language
        language = Manager.FileManager.getLanguage(this);

        // Removes old sql information from config.yml and sorts it into sql.yml
        FileConfiguration sql = Manager.FileManager.getFile(this, "sql");
        if (this.getConfig().isSet("mysql")) {
            this.getLogger().info("(MYSQL) Setting mysql path into `data/sql.yml`");
            for (String key : Objects.requireNonNull(this.getConfig().getConfigurationSection("mysql")).getKeys(false)) {
                sql.set("mysql." + key, this.getConfig().get("mysql." + key));
            }
            Manager.FileManager.saveFile(this, sql, "sql");
            this.getConfig().set("mysql", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        // Removes old swear information from config.yml and sorts it into data/swears.yml
        FileConfiguration local = Manager.FileManager.getFile(this, "data/swears");
        if (!this.getConfig().getStringList("swears").isEmpty()) {
            this.getLogger().info("(CONFIG) Setting path `swears` into `data/swears.yml`");
            if (local.isSet("swears")) {
                if (!local.getStringList("swears").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("swears"));
                    Set<String> local2 = new HashSet<>(this.getConfig().getStringList("swears"));
                    local1.addAll(local2);
                    local.set("swears", local1);
                    Manager.FileManager.saveFile(this, local, "data/swears");
                    this.getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/swears.yml` and removed path `swears`");
                }
            }
            getConfig().set("swears", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        // Removes old whitelist information from config.yml and sorts it into data/whitelist.yml
        local = Manager.FileManager.getFile(this, "data/whitelist");
        if (!getConfig().getStringList("whitelist").isEmpty()) {
            getLogger().info("(CONFIG) Setting path `whitelist` into `data/whitelist.yml`");
            if (local.isSet("whitelist")) {
                if (!local.getStringList("whitelist").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("whitelist"));
                    Set<String> local2 = new HashSet<>(this.getConfig().getStringList("whitelist"));
                    local1.addAll(local2);
                    local.set("whitelist", local1);
                    Manager.FileManager.saveFile(this, local, "data/whitelist");
                    this.getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/whitelist.yml` and removed path `whitelist`");
                }
            }
            this.getConfig().set("whitelist", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        // Removes old global information from config.yml and sorts it into data/global.yml
        local = Manager.FileManager.getFile(this, "data/global");
        if (!this.getConfig().getStringList("global").isEmpty()) {
            this.getLogger().info("(CONFIG) Setting path `global` into `data/global.yml`");
            if (local.isSet("global")) {
                if (!local.getStringList("global").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("global"));
                    Set<String> local2 = new HashSet<>(this.getConfig().getStringList("global"));
                    local1.addAll(local2);
                    local.set("global", local1);
                    Manager.FileManager.saveFile(this, local, "data/global");
                    this.getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/global.yml` and removed path `global`");
                }
            }
            this.getConfig().set("global", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        // Loads MySQL data if mysql is enabled
        hikariCP = new HikariCP(this, connector);
        if (sql.getBoolean("mysql.enabled")) {
            boolean load = false;
            if (connector == null)
                load = true;
            else if (!connector.isWorking())
                load = true;
            if (load) {
                if (hikariCP.isEnabled()) {
                    getLogger().log(Level.INFO, "(MYSQL) Successfully initiated MySQL!");
                    this.connector = hikariCP.getConnector();
                } else {
                    getLogger().log(Level.WARNING, "(MYSQL) Unsuccessfully initiated MySQL!");
                }
            }
        }
        manager = new Manager(this, connector);

        // Sets the correct data for tables
        if (getHikariCP().isEnabled())
            try {
                manager.createTable(false);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        // Relocates old pre-existing user data to the appropriate locations
        if (getConfig().isSet("users")) {
            getLogger().info("(CONFIG) Converting path `users` into `data/users.db`");
            for (String ID : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                User user = new User(manager, UUID.fromString(ID));
                if (!user.exists()) {
                    user.create(getConfig().getString("users." + ID + ".playername"), getConfig().getBoolean("users." + ID + ".enabled"));
                }
            }
            getConfig().set("users", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        // Loop through each language file that isn't the selected one and add it to /locales/ folder
        for (Types.Languages language : Types.Languages.values()) {
            if (!(new File(getDataFolder(), "locales/" + language.name() + ".yml").exists()) && !Types.Languages.getLanguage(this).equalsIgnoreCase(language.name())) {
                Settings settings = new Settings();
                settings.setSetting("reportMissingOptions", false);
                ConfigAPI lang = new ConfigAPI("locales/" + language.name() + ".yml", settings, this);
                lang.copyDefConfigIfNeeded();
            }
        }

        // Retrieves data from MySQL and sets it accordingly
        manager.reload();

        // Loads the MCSF commands with aliases
        new McsfCommand(this);

        // Loads all the player related events
        new PlayerEvents(manager);

        // Punishes players if their messages, created signs or books contain swears
        if (getConfig().getBoolean("settings.filtering.punishments.punish players"))
            new PunishmentEvents(manager);

        // Loading plugin hooks
        if (manager.supported("ProtocolLib")) {
            this.ProtocolLib = new ProtocolLib(this);
            this.ProtocolLib.register();
        }
        if (manager.supported("PlaceholderAPI")) {
            this.PlaceholderAPI = new PlaceholderAPI(this);
            this.PlaceholderAPI.register();
        }
        if (manager.supported("DiscordSRV")) {
            this.DiscordSRV = new DiscordSRV(this);
            this.DiscordSRV.register();
        }

        // Debugs and checks to make sure that strings are correctly being filtered
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            final List<String> swears = Manager.FileManager.getFile(this, "data/swears").getStringList("swears");
            if (!swears.isEmpty()) {
                final String test = swears.get((new SecureRandom()).nextInt(swears.size()));
                String clean = manager.clean(test, true, false, manager.reloadPattern(Types.Filters.BOTH), Types.Filters.DEBUG);
                manager.debug("Running filter test for `" + test + "`; returns as: `" + clean + "`");
            } else {
                manager.debug("Uh-oh! Swears seems to be empty.");
            }
        });

        // Basic metrics data, includes currently set language
        final Metrics metrics = new Metrics(this, 4345);
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> Types.Languages.getLanguage(this)));

        // This code notifies the server operator if an update is needed and if metrics is enabled
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {

            // Verifies this is the latest version of MCSF
            if (getConfig().getBoolean("settings.updating.check for updates")) {
                manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.checking")));
                if (Manager.getVersion() != 0.0 && Manager.getVersion() > Double.parseDouble(getDescription().getVersion())) {
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.update_available")));
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.update_link")));
                    needsUpdate = true;
                } else {
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.no_new_version")));
                }
            }
            manager.debug("Metrics is " + (metrics.isEnabled() ? "enabled; Disable" : "disabled; Enable") + " it through the global bStats config.");
        }, 1L);
    }

    @Override
    public void onDisable() {
        // Closes pre-existing mysql and sqlite connectionsman
        manager.shutDown();

        // Terminates all pre-existing tasks that are still running
        Bukkit.getScheduler().cancelTasks(this);
    }

    /* Setters and getters */

    public void setLocal(String name, int size) {
        localSizes.put(name, size);
    }

    public int getLocal(String value) {
        return localSizes.getOrDefault(value, 0);
    }

    public void reloadLanguage() {
        language = Manager.FileManager.getLanguage(this);
    }
}