package io.github.jochyoua.mychristianswearfilter;

import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.events.*;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.HikariCP;
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

public class MCSF extends JavaPlugin {
    private final HashMap<String, Integer> localSizes = new HashMap<>();
    public HikariCP hikariCP = null;
    private MCSF plugin;
    private Manager manager;
    private DatabaseConnector connector;
    private YamlConfiguration language;

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

        Manager.FileManager.relocateData(this);

        FileConfiguration sql = Manager.FileManager.getFile(plugin, "sql");
        if (sql.getBoolean("mysql.enabled")) {
            boolean load = false;
            if (connector == null)
                load = true;
            else if (!connector.isWorking())
                load = true;
            if (load) {
                hikariCP = new HikariCP(this, connector);
                if (hikariCP.isEnabled()) {
                    getLogger().log(Level.INFO, "(MYSQL) Successfully initiated MySQL!");
                    this.connector = hikariCP.getConnector();
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
            String clean = manager.clean(test, true, false, manager.reloadPattern(Types.Filters.BOTH), Types.Filters.DEBUG);
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
                    manager.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.updatecheck.update_available"));
                    manager.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.updatecheck.update_link"));
                } else {
                    manager.send(Bukkit.getConsoleSender(), getLanguage().getString("variables.updatecheck.no_new_version"));
                }
            }
            manager.debug("Metrics is " + (metrics.isEnabled() ? "enabled; Disable" : "disabled; Enable") + " it through the global bStats config.");
        }, 1L);
    }

    @Override
    public void onDisable() {
        manager.shutDown();
        Bukkit.getScheduler().cancelTasks(this);
    }

    /* Setters and getters */

    public void setLocal(String name, int size) {
        localSizes.put(name, size);
    }

    public int getLocal(String value) {
        return localSizes.getOrDefault(value, 0);
    }

    public YamlConfiguration getLanguage() {
        return language;
    }

    public void reloadLanguage() {
        language = Manager.FileManager.getLanguage(this);
    }

    public Manager getManager() {
        return manager;
    }

    public HikariCP getHikariCP() {
        return this.hikariCP;
    }
}