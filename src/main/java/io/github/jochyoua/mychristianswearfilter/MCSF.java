package io.github.jochyoua.mychristianswearfilter;

import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.events.*;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.hooks.PlaceholderAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

import static io.github.jochyoua.mychristianswearfilter.shared.Manager.FileManager.saveFile;

public class MCSF extends JavaPlugin {
    public MCSF plugin;
    public Manager manager;
    HashMap<String, Integer> localSizes = new HashMap<>();
    private DatabaseConnector connector;
    private YamlConfiguration language;

    public YamlConfiguration getLanguage() {
        return language;
    }

    public void reloadLanguage() {
        language = Manager.FileManager.getLanguage(this);
    }

    public Manager getManager() {
        return manager;
    }

    @Override
    public void onEnable() {
        plugin = this;
        getConfig().options().header(
                "MCSF (My Christian Swear Filter) v" + getDescription().getVersion() + " by Jochyoua\n"
                        + "This is a toggleable swear filter for your players\n"
                        + "Resource: https://www.spigotmc.org/resources/54115/\n"
                        + "Github: https://www.github.com/Jochyoua/MCSF/\n"
                        + "Wiki: https://github.com/Jochyoua/MCSF/wiki");
        saveDefaultConfig();
        saveConfig();
        language = Manager.FileManager.getLanguage(this);
        FileConfiguration sql = Manager.FileManager.getFile(plugin, "sql");
        if (getConfig().isSet("mysql")) {
            getLogger().info("(MYSQL) Setting mysql path into `data/sql.yml`");
            for (String key : getConfig().getConfigurationSection("mysql").getKeys(false)) {
                sql.set("mysql." + key, getConfig().get("mysql." + key));
            }
            saveFile(this, sql, "sql");
            getConfig().set("mysql", null);
            saveConfig();
        }
        FileConfiguration local = Manager.FileManager.getFile(this, "data/swears");
        if (!getConfig().getStringList("swears").isEmpty()) {
            getLogger().info("(CONFIG) Setting path `swears` into `data/swears.yml`");
            if (local.isSet("swears")) {
                if (!local.getStringList("swears").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("swears"));
                    Set<String> local2 = new HashSet<>(getConfig().getStringList("swears"));
                    local1.addAll(local2);
                    local.set("swears", local1);
                    saveFile(this, local, "data/swears");
                    getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/swears.yml` and removed path `swears`");
                }
            }
            getConfig().set("swears", null);
            saveConfig();
        }
        local = Manager.FileManager.getFile(this, "data/whitelist");
        if (!getConfig().getStringList("whitelist").isEmpty()) {
            getLogger().info("(CONFIG) Setting path `global` into `data/whitelist.yml`");
            if (local.isSet("whitelist")) {
                if (!local.getStringList("whitelist").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("whitelist"));
                    Set<String> local2 = new HashSet<>(getConfig().getStringList("whitelist"));
                    local1.addAll(local2);
                    local.set("whitelist", local1);
                    Manager.FileManager.saveFile(this, local, "data/whitelist");
                    getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/whitelist.yml` and removed path `whitelist`");
                }
            }
            getConfig().set("whitelist", null);
            saveConfig();
        }
        local = Manager.FileManager.getFile(this, "data/global");
        if (!getConfig().getStringList("global").isEmpty()) {
            getLogger().info("(CONFIG) Setting path `global` into `data/global.yml`");
            if (local.isSet("global")) {
                if (!local.getStringList("global").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("global"));
                    Set<String> local2 = new HashSet<>(getConfig().getStringList("global"));
                    local1.addAll(local2);
                    local.set("global", local1);
                    Manager.FileManager.getFile(this, "data/global");
                    getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/global.yml` and removed path `global`");
                }
            }
            getConfig().set("global", null);
            saveConfig();
        }
        if (sql.getBoolean("mysql.enabled")) {
            boolean load = false;
            if (connector == null)
                load = true;
            else if (!connector.isWorking())
                load = true;
            if (load) {
                if (reloadSQL()) {
                    getLogger().log(Level.INFO, "(MYSQL) Successfully initiated MySQL!");
                } else {
                    getLogger().log(Level.WARNING, "(MYSQL) Unsuccessfully initiated MySQL!");
                }
            }
        }
        manager = new Manager(this, connector);

        if (sql.getBoolean("mysql.enabled"))
            try {
                manager.createTable(false);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        if (getConfig().isSet("replacements.all")) {
            if (Objects.requireNonNull(getConfig().getString("replacements.all")).equalsIgnoreCase("&c*&f") && !Objects.requireNonNull(getConfig().getString("settings.filtering.replacement")).equalsIgnoreCase("&c*&f")) {
                getConfig().set("replacements.all", getConfig().getString("settings.filtering.replacement"));
            }
        }
        if (getConfig().isSet("users")) {
            getLogger().info("(CONFIG) Converting path `users` into `data/users.db`");
            for (String ID : getConfig().getConfigurationSection("users").getKeys(false)) {
                User user = new User(manager, UUID.fromString(ID));
                if (!user.exists()) {
                    user.create(getConfig().getString("users." + ID + ".playername"), getConfig().getBoolean("users." + ID + ".enabled"));
                }
            }
            getConfig().set("users", null);
            saveConfig();
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
        manager.reload();
        new CommandEvents(manager);
        new PlayerEvents(manager);
        if (manager.supported("ProtocolLib"))
            new ProtocolLib(manager);
        if (getConfig().getBoolean("settings.filtering.punishments.punish players"))
            new PunishmentEvents(manager);
        if (manager.supported("DiscordSRV"))
            new DiscordEvents(manager);
        final List<String> swears = Manager.FileManager.getFile(this, "data/swears").getStringList("swears");
        if (!swears.isEmpty()) {
            final String test = swears.get((new Random()).nextInt(swears.size()));
            manager.reloadPattern();
            String clean = manager.clean(test, true, false, manager.getSwears(), Types.Filters.DEBUG);
            manager.debug("Running filter test for `" + test + "`; returns as: `" + clean + "`");
        } else {
            manager.debug("Uh-oh! Swears seems to be empty.");
        }
        if (getConfig().getBoolean("settings.console motd")) {
            manager.send(Bukkit.getConsoleSender(), String.join("\n", getLanguage().getStringList("variables.console motd")));
        }
        if (getConfig().getBoolean("settings.enable placeholder api")) {
            new PlaceholderAPI(this).register();
        }
        final Metrics metrics = new Metrics(this, 4345);
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> Types.Languages.getLanguage(this)));

        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            if (getConfig().getBoolean("settings.updating.check for updates")) {
                manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.checking")));
                if (Manager.needsUpdate(plugin.getDescription().getVersion())) {
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.update_available")));
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.update_link")));
                } else {
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.no_new_version")));
                }
            }
            manager.debug("Metrics is " + (metrics.isEnabled() ? "enabled; Disable" : "disabled; Enable") + " it through the global bStats config.");
        }, 1L);
    }

    public boolean reloadSQL() {
        FileConfiguration sql = Manager.FileManager.getFile(this, "sql");
        if (sql.getBoolean("mysql.enabled")) {
            if (connector == null)
                connector = new DatabaseConnector(this);
            if (!connector.isWorking()) {
                getLogger().info("(MYSQL) Loading database info....");
                try {
                    String driverClass = sql.getString("mysql.driverClass");
                    String url = sql.getString("mysql.connection", "jdbc:mysql://{host}:{port}/{database}?useUnicode={unicode}&characterEncoding=utf8&autoReconnect=true&useSSL={ssl}").replaceAll("(?i)\\{host}|(?i)%host%", sql.getString("mysql.host"))
                            .replaceAll("(?i)\\{port}|(?i)%port%", sql.getString("mysql.port", "3306"))
                            .replaceAll("(?i)\\{database}|(?i)%database%", sql.getString("mysql.database", "MCSF"))
                            .replaceAll("(?i)\\{unicode}|(?i)%unicode%", String.valueOf(sql.getBoolean("mysql.use_unicode", true)))
                            .replaceAll("(?i)\\{ssl}|(?i)%ssl%", String.valueOf(sql.getBoolean("mysql.ssl", false)));
                    String username = sql.getString("mysql.username");
                    String password = sql.getString("mysql.password");
                    int maxPoolSize = sql.getInt("mysql.maxPoolSize");
                    getLogger().info("(MYSQL) Using URL: " + url);
                    connector.setInfo(
                            new DatabaseConnector.Info(
                                    driverClass,
                                    url,
                                    username,
                                    password,
                                    maxPoolSize
                            )
                    );
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    return false;
                }

                getLogger().info("(MYSQL) Trying a database connection....");
                try {
                    connector.tryFirstConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                getLogger().info("(MYSQL) The connection has been established!");
            }
        }
        return true;
    }

    public void setLocal(String name, int size) {
        localSizes.put(name, size);
    }

    public int getLocal(String value) {
        return localSizes.getOrDefault(value, 0);
    }

    @Override
    public void onDisable() {
        manager.shutDown();
        Bukkit.getScheduler().cancelTasks(this);
    }
}