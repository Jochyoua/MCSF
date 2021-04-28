package io.github.jochyoua.mychristianswearfilter;

import io.github.jochyoua.mychristianswearfilter.commands.McsfCommand;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.listeners.ChatListener;
import io.github.jochyoua.mychristianswearfilter.listeners.InteractListener;
import io.github.jochyoua.mychristianswearfilter.listeners.JoinLeaveListener;
import io.github.jochyoua.mychristianswearfilter.listeners.PunishmentListener;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import static io.github.jochyoua.mychristianswearfilter.shared.Manager.debug;


@Getter
public class MCSF extends JavaPlugin {
    public static MCSF plugin;

    public final HashMap<String, Integer> localSizes = new HashMap<>();
    private HikariCP hikariCP;
    private DatabaseConnector connector;
    private YamlConfiguration language;
    private Manager manager;
    private io.github.jochyoua.mychristianswearfilter.shared.hooks.ProtocolLib ProtocolLib;
    private io.github.jochyoua.mychristianswearfilter.shared.hooks.PlaceholderAPI PlaceholderAPI;
    private io.github.jochyoua.mychristianswearfilter.shared.hooks.DiscordSRV DiscordSRV;
    private Boolean needsUpdate = false;
    private Boolean debug;

    public static MCSF getInstance() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        debug = getConfig().getBoolean("settings.debug");

        debug("Loading MCSF v" + getDescription().getVersion() + " on " + getServer().getVersion() + (Bukkit.getOnlinePlayers().isEmpty() ? " FRESH RUN" : " RELOADED"), false, Level.INFO);

        language = Manager.FileManager.getLanguage(this);

        FileConfiguration sql = Manager.FileManager.getFile(this, "sql");
        if (this.getConfig().isSet("mysql")) {
            debug("(MYSQL) Setting mysql path into `data/sql.yml`", true, Level.INFO);
            debug("Converting legacy sql", false, Level.INFO);
            for (String key : Objects.requireNonNull(this.getConfig().getConfigurationSection("mysql")).getKeys(false)) {
                sql.set("mysql." + key, this.getConfig().get("mysql." + key));
            }
            Manager.FileManager.saveFile(this, sql, "sql");
            this.getConfig().set("mysql", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        FileConfiguration local = Manager.FileManager.getFile(this, "data/swears");
        if (!this.getConfig().getStringList("swears").isEmpty()) {
            debug("(CONFIG) Setting path `swears` into `data/swears.yml`", true, Level.INFO);
            debug("Converting legacy swears", false, Level.INFO);
            if (local.isSet("swears")) {
                if (!local.getStringList("swears").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("swears"));
                    Set<String> local2 = new HashSet<>(this.getConfig().getStringList("swears"));
                    local1.addAll(local2);
                    local.set("swears", local1);
                    Manager.FileManager.saveFile(this, local, "data/swears");
                    debug("(CONFIG) Set " + local1.size() + " entries into `data/swears.yml` and removed path `swears`", true, Level.INFO);
                }
            }
            getConfig().set("swears", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        local = Manager.FileManager.getFile(this, "data/whitelist");
        if (!getConfig().getStringList("whitelist").isEmpty()) {
            debug("(CONFIG) Setting path `whitelist` into `data/whitelist.yml`", true, Level.INFO);
            debug("Converting legacy whitelist", false, Level.INFO);
            if (local.isSet("whitelist")) {
                if (!local.getStringList("whitelist").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("whitelist"));
                    Set<String> local2 = new HashSet<>(this.getConfig().getStringList("whitelist"));
                    local1.addAll(local2);
                    local.set("whitelist", local1);
                    Manager.FileManager.saveFile(this, local, "data/whitelist");
                    debug("(CONFIG) Set " + local1.size() + " entries into `data/whitelist.yml` and removed path `whitelist`", true, Level.INFO);
                }
            }
            this.getConfig().set("whitelist", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        local = Manager.FileManager.getFile(this, "data/global");
        if (!this.getConfig().getStringList("global").isEmpty()) {
            debug("(CONFIG) Setting path `global` into `data/global.yml`", true, Level.INFO);
            debug("Converting legacy global", false, Level.INFO);
            if (local.isSet("global")) {
                if (!local.getStringList("global").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("global"));
                    Set<String> local2 = new HashSet<>(this.getConfig().getStringList("global"));
                    local1.addAll(local2);
                    local.set("global", local1);
                    Manager.FileManager.saveFile(this, local, "data/global");
                    debug("(CONFIG) Set " + local1.size() + " entries into `data/global.yml` and removed path `global`", true, Level.INFO);
                }
            }
            this.getConfig().set("global", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        hikariCP = new HikariCP(this, connector);
        if (sql.getBoolean("mysql.enabled")) {
            boolean load = false;
            if (connector == null)
                load = true;
            else if (!connector.isWorking())
                load = true;
            if (load) {
                if (hikariCP.isEnabled()) {
                    debug("(MYSQL) Successfully initiated MySQL!", true, Level.INFO);
                    this.connector = hikariCP.getConnector();
                } else {
                    debug("(MYSQL) Unsuccessfully initiated MySQL!", true, Level.WARNING);
                }
            }
        }
        manager = new Manager(this, connector);

        if (getHikariCP().isEnabled())
            try {
                manager.createTable(false);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        if (getConfig().isSet("users")) {
            debug("(CONFIG) Converting path `users` into `data/users.db`", true, Level.INFO);
            debug("Converting legacy users", false, Level.INFO);
            for (String ID : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                User user = new User(manager, UUID.fromString(ID));
                if (!user.exists()) {
                    user.create(getConfig().getString("users." + ID + ".playername"), getConfig().getBoolean("users." + ID + ".enabled"));
                }
            }
            getConfig().set("users", null);
            Manager.FileManager.saveFile(this, getConfig(), "config");
        }

        for (Data.Languages language : Data.Languages.values()) {
            if (!(new File(getDataFolder(), "locales/" + language.name() + ".yml").exists()) && !Data.Languages.getLanguage(this).equalsIgnoreCase(language.name())) {
                Settings settings = new Settings();
                settings.setSetting("reportMissingOptions", false);
                ConfigAPI lang = new ConfigAPI("locales/" + language.name() + ".yml", settings, this);
                lang.copyDefConfigIfNeeded();
            }
        }


        manager.reload();

        new McsfCommand(this);

        new ChatListener(this);

        new InteractListener(this);

        new JoinLeaveListener(this);

        if (getConfig().getBoolean("settings.filtering.punishments.punish players"))
            new PunishmentListener(this);

        if (manager.supported("ProtocolLib")) {
            this.ProtocolLib = new ProtocolLib(this);
            this.ProtocolLib.register();
            if (this.getProtocolLib().isEnabled())
                debug("ProtocolLib support successfully enabled", false, Level.INFO);
        }
        if (manager.supported("PlaceholderAPI")) {
            this.PlaceholderAPI = new PlaceholderAPI(this);
            this.PlaceholderAPI.register();
            if (this.getPlaceholderAPI().isRegistered())
                debug("PlaceholderAPI support successfully enabled", false, Level.INFO);
        }
        if (manager.supported("DiscordSRV")) {
            this.DiscordSRV = new DiscordSRV(this);
            this.DiscordSRV.register();
            if (this.getDiscordSRV().isEnabled())
                debug("DiscordSRV support successfully enabled", false, Level.INFO);
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            final List<String> swears = Manager.FileManager.getFile(this, "data/swears").getStringList("swears");
            if (!swears.isEmpty()) {
                final String test = swears.get((new SecureRandom()).nextInt(swears.size()));
                String clean = manager.clean(test, true, manager.reloadPattern(Data.Filters.BOTH), Data.Filters.DEBUG);
                debug("Running filter test for `" + test + "`; returns as: `" + clean + "`", getDebug(), Level.INFO);
            } else {
                debug("Uh-oh! Swears seems to be empty.", true, Level.WARNING);
            }
        });

        final Metrics metrics = new Metrics(this, 4345);
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> Data.Languages.getLanguage(this)));

        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {

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
            debug("Metrics is " + (metrics.isEnabled() ? "enabled; Disable" : "disabled; Enable") + " it through the global bStats config.", getDebug(), Level.INFO);
        }, 1L);
    }


    @Override
    public void onDisable() {
        manager.shutDown();

        Bukkit.getScheduler().cancelTasks(this);
    }

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