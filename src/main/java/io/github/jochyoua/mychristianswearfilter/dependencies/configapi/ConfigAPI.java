package io.github.jochyoua.mychristianswearfilter.dependencies.configapi;

import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.utility.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class ConfigAPI {

    // Some terminology for the reader:
    //      Default config: The config file which is provided by the plugin (default)
    //      Live config: The config file on the server which server admins can edit

    // Expansion ideas:
    //      allow message customisation
    //      allow writing to the live config (ex: a new option) which should include its comments

    private JavaPlugin plugin;              // An instance of which plugin's config files are being managed
    private String fileName;                // The name of the config file being managed

    private Map<String, Object> contents;   // Will contain the loaded values

    private Settings settings = new Settings();          // An instance that handles all settings


    /**
     * Creates an instance of ConfigAPI which will manage your config files. Multiple files will require multiple instances
     * Some terminology used in the documentation:
     * Default config: The config file which is provided by the plugin (default)
     * Live config: The config file on the server which server admins can edit
     *
     * @param fileName the name of the file you wish to read (config.yml, lang.yml, etc.)
     * @param plugin   your plugin
     */
    public ConfigAPI(String fileName, JavaPlugin plugin) {

        initializeAPI(fileName, plugin);    // Perform default initialization

    }


    /**
     * Creates an instance of ConfigAPI which will manage your config files. Multiple files will require multiple instances
     * Some terminology used in the documentation:
     * Default config: The config file which is provided by the plugin (default)
     * Live config: The config file on the server which server admins can edit
     * This constructor starts by updating the settings
     *
     * @param fileName the name of the file you wish to read (config.yml, lang.yml, etc.)
     * @param settings the settings to be used by this API
     * @param plugin   your plugin
     */
    public ConfigAPI(String fileName, Settings settings, JavaPlugin plugin) {
        this.settings = settings;            // update the settings
        initializeAPI(fileName, plugin);     // Perform default initialization
    }


    /**
     * Contains all options for startup
     */
    private void initializeAPI(String fileName, JavaPlugin plugin) {
        this.plugin = plugin;           // Keep track of an instance of this plugin
        this.fileName = fileName;       // Store the filename of this config file

        contents = new HashMap<>();       // Create a Map that will contain all paths and values

        if (settings.getAutoLoadValues())             // Check if auto loading is enabled
        {
            reloadContents();                           // Load contents into memory
        }

        copyDefConfigIfNeeded();    // Checks if a live config is available, if not: a new default config file is generated
    }


    // ------------ //
    // Getting data //
    // ------------ //


    /**
     * Check if the given path exists
     *
     * @param path the path to be checked
     * @return a boolean, exists: true. Does not exist: false
     */
    public boolean contains(String path) {
        return contents.containsKey(path);
    }


    /**
     * Get the name of this file
     *
     * @return the file name (String)
     */
    public String getFileName() {
        return fileName;
    }


    /**
     * Get an Object at a given path
     * Subsections are indicated with a '.' just like they are for a ConfigurationSection
     *
     * @param path path
     * @return Object
     */
    public Object getObject(String path) {
        return contents.get(path);
    }


    /**
     * Get a setting from the config file
     * Subsections are indicated with a '.' just like they are for a ConfigurationSection
     *
     * @param path the path of the setting
     * @return an empty String when the option does not exist or the value is not of the requested type
     */
    public String getString(String path) {
        Object value = contents.get(path);

        if (value instanceof String) {
            return (String) value;
        } else {
            if (settings.getDoDebugLogging())
                Logger.log("Path " + path + " was not found in " + fileName + " while expecting a String (and found: " + value.getClass().getSimpleName() + ")");
            return "";
        }
    }


    /**
     * Get a setting from the config file
     * Subsections are indicated with a '.' just like they are for a ConfigurationSection
     *
     * @param path the path of the setting
     * @return zero (0) when the option does not exist or the value is not of the requested type
     */
    public int getInt(String path) {
        Object value = contents.get(path);

        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            if (settings.getDoDebugLogging())
                Logger.log("Path " + path + " was not found in " + fileName + " while expecting an Integer (and found: " + value.getClass().getSimpleName() + ")");
            return 0;
        }
    }


    /**
     * Get a setting from the config file
     * Subsections are indicated with a '.' just like they are for a ConfigurationSection
     *
     * @param path the path of the setting
     * @return false when the option does not exist or the value is not of the requested type
     */
    public boolean getBoolean(String path) {
        Object value = contents.get(path);

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            if (settings.getDoDebugLogging())
                Logger.log("Path " + path + " was not found in " + fileName + " while expecting a Boolean (and found: " + value.getClass().getSimpleName() + ")");
            return false;
        }
    }


    /**
     * Get a setting from the config file
     * Subsections are indicated with a '.' just like they are for a ConfigurationSection
     *
     * @param path the path of the setting
     * @return zero (0) when the option does not exist or the value is not of the requested type
     */
    public double getDouble(String path) {
        Object value = contents.get(path);

        if (value instanceof Double) {
            return (Double) value;
        } else {
            if (settings.getDoDebugLogging())
                Logger.log("Path " + path + " was not found in " + fileName + " while expecting a Double (and found: " + value.getClass().getSimpleName() + ")");
            return 0;
        }
    }


    /**
     * Get a setting from the config file
     * Subsections are indicated with a '.' just like they are for a ConfigurationSection
     *
     * @param path the path of the setting
     * @return zero (0) when the option does not exist or the value is not of the requested type
     */
    public long getLong(String path) {
        Object value = contents.get(path);

        if (value instanceof Long) {
            return (Long) value;
        } else {
            if (settings.getDoDebugLogging())
                Logger.log("Path " + path + " was not found in " + fileName + " while expecting a Long (and found: " + value.getClass().getSimpleName() + ")");
            return 0;
        }
    }


    /**
     * Get a setting from the config file
     * Subsections are indicated with a '.' just like they are for a ConfigurationSection
     *
     * @param path the path of the setting
     * @return null when the option does not exist or the value is not of the requested type
     */
    public ItemStack getItemStack(String path) {
        Object value = contents.get(path);

        if (value instanceof ItemStack) {
            return (ItemStack) value;
        } else {
            if (settings.getDoDebugLogging())
                Logger.log("Path " + path + " was not found in " + fileName + " while expecting an ItemStack (and found: " + value.getClass().getSimpleName() + ")");
            return null;
        }
    }


    // ------------------- //
    // Settings management //
    // ------------------- //


    /**
     * Get the object that handles all ConfigAPI settings
     *
     * @return the Settings object
     */
    public Settings getSettings() {
        return settings;
    }


    /**
     * Overwrite the used settings
     *
     * @param settings the new settings
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
    }


    // ------------------- //
    // Config file loading //
    // ------------------- //


    /**
     * Get a FileConfiguration containing all live configuration values
     *
     * @return a YamlConfiguration, created from the live config file
     */
    public YamlConfiguration getLiveConfiguration() {
        File file = new File(plugin.getDataFolder(), fileName);     // Get the file from the plugin's datafolder
        return YamlConfiguration.loadConfiguration(file);           // Create a YAMLConfiguration from the found file
    }


    /**
     * Create a FileConfiguration of the default config file within the resources folder of the jar file
     *
     * @return YamlConfiguration
     */
    public YamlConfiguration getDefaultConfiguration() {
        YamlConfiguration fileConfig = new YamlConfiguration();

        Reader defaultStream;
        try {
            defaultStream = new InputStreamReader(Objects.requireNonNull(plugin.getResource(fileName)), StandardCharsets.UTF_8);
            fileConfig = YamlConfiguration.loadConfiguration(defaultStream);
        } catch (Exception ex) {
            if (settings.getDoDebugLogging()) {
                Logger.log("An error occurred while loading the default config values (see below)");
                Logger.log(ex.getMessage());
            }
        }

        return fileConfig;
    }


    /**
     * Any missing options of the live config are added and set to their default value
     * This will be generated when this method is called so you might want to avoid repeating this call too many times
     *
     * @return YamlConfiguration (
     */
    public YamlConfiguration getConfiguration() {
        YamlConfiguration liveConfig = getLiveConfiguration();
        YamlConfiguration defConfig = getDefaultConfiguration();

        for (String key : defConfig.getKeys(true)) {
            if (!defConfig.isConfigurationSection(key)) {
                if (!liveConfig.contains(key)) {
                    liveConfig.set(key, defConfig.get(key));
                }
            }
        }

        return liveConfig;
    }


    // ------------------- //
    // Loading into memory //
    // ------------------- //


    /**
     * Load all values from both provided configurations into memory
     * The previous contents will be removed from memory
     *
     * @param liveConfig    should be a YamlConfiguration of the config on the server
     * @param defaultConfig should be a YamlConfiguration of the default config values. If null, no default values will be loaded. If not null, there will be no missing options
     */
    private void loadFromConfigurations(YamlConfiguration liveConfig, YamlConfiguration defaultConfig) {
        // If the default config should not be considered (for missing options): only load from the live values

        if (defaultConfig == null) {
            loadFromConfigurations(liveConfig);
            return;
        }

        // Empty the contents

        contents = new HashMap<>();

        // Load the new contents

        for (String path : defaultConfig.getKeys(true)) {
            if (!defaultConfig.isConfigurationSection(path)) {
                if (liveConfig.contains(path)) {
                    contents.put(path, liveConfig.get(path));
                } else {
                    contents.put(path, defaultConfig.get(path));
                }
            }
        }
    }


    /**
     * Load all values from the provided configuration into memory
     * Earlier loaded values will be deleted
     *
     * @param liveConfig should be a YamlConfiguration of the config on the server
     */
    private void loadFromConfigurations(YamlConfiguration liveConfig) {

        // Reset contents
        contents = new HashMap<>();

        // Load new contents
        for (String path : liveConfig.getKeys(true)) {
            if (!liveConfig.isConfigurationSection(path)) {
                contents.put(path, liveConfig.get(path));
            }
        }
    }


    /**
     * (re)Load all values from the live config
     * If loadDefaults is enabled, default values will be used wherever the live config is missing options
     * When reportMissingOptions is enabled:     missing options will be reported to the console
     * When reportRedundantOptions is enabled:   redundant options will be reported to the console
     * When reportNewConfig is enabled:          creating a new config will be reported to the console (meaning: when no live config exists yet)
     */
    public void reloadContents() {

        copyDefConfigIfNeeded();        // Copy a new config if none exists yet

        YamlConfiguration liveConfiguration;        // Live config values
        YamlConfiguration defConfiguration;  // Default config values

        // Get the live config
        liveConfiguration = getLiveConfiguration();

        // If missing/redundant options need to be reported on reload:
        if (settings.getReportMissingOptions())
            reportMissingOptions();

        // If enabled: add the default values to the live configuration where options are missing
        if (settings.getLoadDefaults()) {
            defConfiguration = getDefaultConfiguration();
            loadFromConfigurations(liveConfiguration, defConfiguration);
        } else {   // If defaults should not be loaded: only provide the live config
            loadFromConfigurations(liveConfiguration);
        }
    }


    // --------------- //
    // File management //
    // --------------- //


    /**
     * This method will reset the live config to the default config
     * ALL LIVE CONFIG CONTENTS WILL BE OVERWRITTEN!
     * If you only want to copy the default config if no live config is present, please use ConfigAPI#copyDefConfigIfNeeded()
     */
    public void forceDefaultConfig() {
        if (settings.getDoDebugLogging())
            Logger.log("Forcing default config! The live config will be overwritten");

        if (settings.getReportNewConfig())
            Logger.sendToConsole("Forcing a fresh " + fileName + " ...", plugin, settings.getUseColors() ? ChatColor.RED : null);

        plugin.saveResource(fileName, true);
    }


    /**
     * Checks whether a live config exists
     * If it does not exist, the default contents are copied and a message is sent to the console
     * If you want to force-copy the default config to the server, please use ConfigAPI#forceDefaultConfig()
     */
    public void copyDefConfigIfNeeded() {

        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            if (settings.getReportNewConfig())
                Logger.sendToConsole("Copying a new " + fileName + " ...", plugin, settings.getUseColors() ? ChatColor.GREEN : null);

            plugin.saveResource(fileName, false);       // saves if the file does not exist but will not overwrite

            //Make sure the Configuration is up to date. Otherwise missing options may be reported!
            //this.configuration = YamlConfiguration.loadConfiguration(file);
        }
    }


    // --------------- //
    // Missing options //
    // --------------- //


    /**
     * This option will find all missing options and provide the default value for each missing option in a Map
     * Missing options will NOT be reported to the console, even if enabled
     * To report missing options to the console, please use ConfigAPI#reportMissingOptions()
     *
     * @return A Map<String, Object> with all Strings being paths of missing options and the Object being its default value
     */
    public Map<String, Object> getMissingOptions() {
        return getMissingOptions(null, null);
    }


    /**
     * Get all options that are present in defaultConfig but NOT in liveConfig
     * By swapping defaultConfig and liveConfig, you can get all redundant options!
     * Both arguments are Nullable! If no value is given, they will be replaced by the contents of each respective file
     * -> liveConfig would be replaced by the contents of the config on the server
     * -> defaultConfig would be replaced by the contents of the default config file
     *
     * @param liveConfig    the YamlConfiguration with possible missing options
     * @param defaultConfig the YamlConfiguration which is assumed to be complete
     * @return a Map<String, Object> containing all missing options and
     */
    public Map<String, Object> getMissingOptions(YamlConfiguration liveConfig, YamlConfiguration defaultConfig) {
        Map<String, Object> missingOptions = new HashMap<>();

        // Get the current config on the server if none was provided

        if (liveConfig == null)
            liveConfig = getLiveConfiguration();

        // Get the current default config if none was provided

        if (defaultConfig == null)
            defaultConfig = getDefaultConfiguration();

        // Get the missing configuration options that are not configuration sections

        for (String path : defaultConfig.getKeys(true)) {
            if (!defaultConfig.isConfigurationSection(path)) {
                if (!liveConfig.contains(path)) {
                    missingOptions.put(path, defaultConfig.get(path));
                }
            }
        }

        return missingOptions;
    }


    /**
     * Compare the default file with the one on the server and report every missing option
     * This will also report all redundant options if this setting is enabled in Settings (default: false)
     */
    public void reportMissingOptions() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        YamlConfiguration liveConfig = getLiveConfiguration();
        YamlConfiguration defConfig = getDefaultConfiguration();

        Map<String, Object> missingOptions = getMissingOptions(liveConfig, defConfig);


        reportFaultyOptions(true, missingOptions);

        if (settings.getReportRedundantOptions()) {
            Map<String, Object> redundantOptions = getMissingOptions(defConfig, liveConfig);
            reportFaultyOptions(false, redundantOptions);
        }
    }


    /**
     * Reports all options in the map as either missing options or redundant options
     *
     * @param isMissing boolean that indicates whether the method will be reporting missing options or redundant options
     * @param options   Map<String, Object> which maps paths to values
     */
    private void reportFaultyOptions(boolean isMissing, Map<String, Object> options) {

        String keyword = isMissing ? "missing" : "redundant";

        if (options.size() > 0) {

            if (options.size() == 1)
                Logger.sendToConsole("A " + keyword + " option has been found in " + fileName + "!", plugin, settings.getUseColors() ? ChatColor.RED : null);
            else
                Logger.sendToConsole(options.size() + keyword + " options have been found in " + fileName + "!", plugin, settings.getUseColors() ? ChatColor.RED : null);

            if (isMissing) {
                Logger.sendToConsole("Please add the missing options manually or delete this file and perform a reload (/bs reload)", plugin, settings.getUseColors() ? ChatColor.RED : null);
                Logger.sendToConsole("The default values will be used until then", plugin, settings.getUseColors() ? ChatColor.RED : null);
            } else {
                Logger.sendToConsole("Redundant options won't have any effect. Feel free to delete them.", plugin, settings.getUseColors() ? ChatColor.RED : null);
            }
            YamlConfiguration defaultConfig = getDefaultConfiguration();

            for (Map.Entry<String, Object> entry : options.entrySet()) {
                StringBuilder path = new StringBuilder(entry.getKey());

                // Handle value
                Object value = entry.getValue();

                // Change formatting if the setting is a String
                if (value instanceof String)
                    value = "\"" + value + "\"";


                // Handle path
                String[] sections = path.toString().split("\\.");  // Split the path in its sections
                path = new StringBuilder();                                     // Reset the path variable

                for (int i = sections.length - 1; i > 0; i--)    // Loop each subsection in reversed order
                {
                    String part = sections[i];
                    path.append("'").append(part).append("'").append(" in section ");             // Print each section in reversed order
                }
                path.append("'").append(sections[0]).append("'");                           // Add the actual setting name

                // Send message
                if (isMissing)
                    Logger.sendToConsole("" + "Missing option: " + path + " with default value: " + value, plugin, settings.getUseColors() ? ChatColor.DARK_RED : null);
                else
                    Logger.sendToConsole("" + "Redundant option: " + path, plugin, settings.getUseColors() ? ChatColor.DARK_RED : null);
            }
        } else {
            Logger.sendToConsole("No " + keyword + " options were found in " + fileName + "!", plugin, settings.getUseColors() ? ChatColor.GREEN : null);
        }
    }
}