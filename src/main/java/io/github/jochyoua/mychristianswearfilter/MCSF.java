package io.github.jochyoua.mychristianswearfilter;

import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.events.*;
import io.github.jochyoua.mychristianswearfilter.shared.HikariCP.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.Utils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class MCSF extends JavaPlugin {
    public MCSF plugin;
    public Utils utils;
    ConfigAPI lang = null;
    HashMap<String, Integer> localSizes = new HashMap<>();
    private DatabaseConnector connector;

    @Override
    public void onEnable() {
        plugin = this;
        getConfig().options().header(
                "MCSF (My Christian Swear Filter) v" + getDescription().getVersion() + " by Jochyoua\n"
                        + "This is a toggleable swear filter for your players\n"
                        + "Resource: https://www.spigotmc.org/resources/54115/\n"
                        + "Github: https://www.github.com/Jochyoua/MCSF/\n"
                        + "Wiki: https://github.com/Jochyoua/MCSF/wiki");
        this.getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        FileConfiguration local = getFile("swears");
        if (!getConfig().getStringList("swears").isEmpty()) {
            getLogger().info("(CONFIG) Setting path `swears` into `data/swears.yml`");
            if (local.isSet("swears")) {
                if (!local.getStringList("swears").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("swears"));
                    Set<String> local2 = new HashSet<>(getConfig().getStringList("swears"));
                    local1.addAll(local2);
                    local.set("swears", local1);
                    saveFile(local, "swears");
                    getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/swears.yml` and removed path `swears`");
                }
            }
            getConfig().set("swears", null);
            saveConfig();
        }
        local = getFile("whitelist");
        if (!getConfig().getStringList("whitelist").isEmpty()) {
            getLogger().info("(CONFIG) Setting path `global` into `data/whitelist.yml`");
            if (local.isSet("whitelist")) {
                if (!local.getStringList("whitelist").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("whitelist"));
                    Set<String> local2 = new HashSet<>(getConfig().getStringList("whitelist"));
                    local1.addAll(local2);
                    local.set("whitelist", local1);
                    saveFile(local, "whitelist");
                    getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/whitelist.yml` and removed path `whitelist`");
                }
            }
            getConfig().set("whitelist", null);
            saveConfig();
        }
        local = getFile("global");
        if (!getConfig().getStringList("global").isEmpty()) {
            getLogger().info("(CONFIG) Setting path `global` into `data/global.yml`");
            if (local.isSet("global")) {
                if (!local.getStringList("global").isEmpty()) {
                    Set<String> local1 = new HashSet<>(local.getStringList("global"));
                    Set<String> local2 = new HashSet<>(getConfig().getStringList("global"));
                    local1.addAll(local2);
                    local.set("global", local1);
                    saveFile(local, "global");
                    getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/global.yml` and removed path `global`");
                }
            }
            getConfig().set("global", null);
            saveConfig();
        }
        if (getConfig().getBoolean("mysql.enabled")) {
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
        utils = new Utils(this, connector);

        if (plugin.getConfig().getBoolean("mysql.enabled"))
            try {
                utils.createTable(false);
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
                User user = new User(utils, UUID.fromString(ID));
                if (!user.exists()) {
                    user.create(getConfig().getString("users." + ID + ".playername"), getConfig().getBoolean("users." + ID + ".enabled"));
                }
            }
            getConfig().set("users", null);
        }
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
        utils.reload();
        new CommandEvents(utils);
        new PlayerEvents(utils);
        if (utils.supported("ProtocolLib"))
            new ProtocolLib(utils);
        if (getConfig().getBoolean("settings.filtering.punishments.punish players"))
            new PunishmentEvents(utils);
        if (getConfig().getBoolean("settings.filtering.filter checks.signcheck") && utils.supported("ProtocolLib"))
            new SignEvents(utils);
        if (utils.supported("DiscordSRV"))
            new DiscordEvents(utils);
        final List<String> swears = plugin.getFile("swears").getStringList("swears");
        if (!swears.isEmpty()) {
            final String test = swears.get((new Random()).nextInt(swears.size()));
            utils.reloadPattern();
            String clean = utils.clean(test, true, false, utils.getSwears(), Types.Filters.DEBUG);
            utils.debug("Running filter test for `" + test + "`; returns as: `" + clean + "`");
        } else {
            utils.debug("Uh-oh! Swears seems to be empty.");
        }
        if (getConfig().getBoolean("settings.console motd")) {
            utils.send(Bukkit.getConsoleSender(), String.join("\n", getLanguage().getStringList("variables.console motd")));
        }
        final Metrics metrics = new Metrics(this, 4345);
        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> Types.Languages.getLanguage(this)));

        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            if (getConfig().getBoolean("settings.updating.check for updates")) {
                utils.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.checking")));
                if (utils.needsUpdate()) {
                    utils.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.update_available")));
                    utils.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.update_link")));
                } else {
                    utils.send(Bukkit.getConsoleSender(), Objects.requireNonNull(getLanguage().getString("variables.updatecheck.no_new_version")));
                }
            }
            utils.debug("Metrics is " + (metrics.isEnabled() ? "enabled; Disable" : "disabled; Enable") + " it through the global bStats config.");
        }, 1L);

        if (Bukkit.getOnlinePlayers().size() != 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = new User(utils, player.getUniqueId());
                if (!user.exists()) {
                    user.toggle();
                }
                user.playerName(player.getName().toLowerCase());
                if (!user.playerName().equalsIgnoreCase(player.getName())) {
                    utils.debug("There was an issue saving " + player.getName() + "'s name to the config.");
                } else {
                    utils.debug("Successfully added " + player.getName() + "'s name to the config.");
                }
            }
        }
    }

    public YamlConfiguration getFile(String fileName) {
        ConfigAPI config;
        Settings settings = new Settings();
        settings.setSetting("reportMissingOptions", false);
        config = new ConfigAPI("data/" + fileName + ".yml", settings, this);
        config.copyDefConfigIfNeeded();
        return config.getLiveConfiguration();
    }

    public void saveFile(FileConfiguration file, String fileName) {
        try {
            file.save(new File(plugin.getDataFolder(), "data/" + fileName + ".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadLanguage() {
        String language = Types.Languages.getLanguage(this);
        Settings settings = new Settings();
        settings.setSetting("reportMissingOptions", false);
        lang = new ConfigAPI("locales/" + language + ".yml", settings, this);
        lang.copyDefConfigIfNeeded();
        YamlConfiguration conf = lang.getLiveConfiguration();
        Map<String, Object> missingOptions = lang.getMissingOptions(conf, lang.getDefaultConfiguration());
        /*
           Making sure that the Language file selected doesn't have missing values which would cause null pointer exceptions
         */
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
                getLogger().log(Level.WARNING, "Failed to save " + language + ".yml!:\n", e);
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

    public boolean reloadSQL() {
        if (getConfig().getBoolean("mysql.enabled")) {
            if (connector == null)
                connector = new DatabaseConnector(this);
            if (!connector.isWorking()) {
                getLogger().info("(MYSQL) Loading database info....");
                try {
                    String driverClass = getConfig().getString("mysql.driverClass");
                    String url = getConfig().getString("mysql.connection", "jdbc:mysql://{host}:{port}/{database}?useUnicode={unicode}&characterEncoding=utf8&autoReconnect=true&useSSL={ssl}").replaceAll("(?i)\\{host}|(?i)%host%", plugin.getString("mysql.host"))
                            .replaceAll("(?i)\\{port}|(?i)%port%", plugin.getString("mysql.port", "3306"))
                            .replaceAll("(?i)\\{database}|(?i)%database%", plugin.getString("mysql.database", "MCSF"))
                            .replaceAll("(?i)\\{unicode}|(?i)%unicode%", String.valueOf(plugin.getConfig().getBoolean("mysql.use_unicode", true)))
                            .replaceAll("(?i)\\{ssl}|(?i)%ssl%", String.valueOf(plugin.getConfig().getBoolean("mysql.ssl", false)));
                    String username = getConfig().getString("mysql.username");
                    String password = getConfig().getString("mysql.password");
                    int maxPoolSize = getConfig().getInt("mysql.maxPoolSize");
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
                getLogger().info("(MYSQL) The connection has been established! Plugin is working.");


            } else {
                getLogger().info("(MYSQL) Database is already working.");
            }
        }
        return true;
    }

    public void setLocal(String name, int size) {
        localSizes.put(name, size);
    }

    public String getString(String path) {
        String string = plugin.getConfig().getString(path);
        if (string == null)
            utils.debug("String is null, take caution!");
        return string;
    }

    public String getString(String path, String def) {
        String string = plugin.getConfig().getString(path, def);
        if (string == null) {
            utils.debug(path + " is null, take caution!");
            return def;
        }
        return string;
    }

    public int getLocal(String value) {
        return localSizes.getOrDefault(value, 0);
    }

    @Override
    public void onDisable() {
        saveConfig();
        utils.shutDown();
        Bukkit.getScheduler().cancelTasks(this);
    }
}