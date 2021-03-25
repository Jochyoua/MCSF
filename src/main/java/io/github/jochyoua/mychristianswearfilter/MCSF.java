package io.github.jochyoua.mychristianswearfilter;

import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.events.CommandEvents;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
    private Boolean needsUpdate;

    public MCSF() {
        throw new AssertionError();
    }


    @Override
    public void onEnable() {

        // Sets the default configuration with header
        getConfig().options().header(
                "MCSF (My Christian Swear Filter) v" + getDescription().getVersion() + " by Jochyoua\n"
                        + "This is a toggleable swear filter for your players\n"
                        + "Resource: https://www.spigotmc.org/resources/54115/\n"
                        + "Github: https://www.github.com/Jochyoua/MCSF/\n"
                        + "Wiki: https://github.com/Jochyoua/MCSF/wiki");
        saveDefaultConfig();

        // Retrieves the currently set language
        language = Manager.FileManager.getLanguage(this);

        // Relocates old pre-existing data to the appropriate locations
        Manager.FileManager.relocateData(this);

        // Loads MySQL data if mysql is enabled
        hikariCP = new HikariCP(this, connector);

        FileConfiguration sql = Manager.FileManager.getFile(this, "sql");
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

        // Retrieves data from MySQL and sets it accordingly
        manager.reload();

        // Loads the MCSF commands with aliases
        new CommandEvents(manager);

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
        final List<String> swears = Manager.FileManager.getFile(this, "data/swears").getStringList("swears");
        if (!swears.isEmpty()) {
            final String test = swears.get((new SecureRandom()).nextInt(swears.size()));
            String clean = manager.clean(test, true, false, manager.reloadPattern(Types.Filters.BOTH), Types.Filters.DEBUG);
            manager.debug("Running filter test for `" + test + "`; returns as: `" + clean + "`");
        } else {
            manager.debug("Uh-oh! Swears seems to be empty.");
        }

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
        // Closes pre-existing mysql and sqlite connections
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