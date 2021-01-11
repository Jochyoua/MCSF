package io.github.jochyoua.mychristianswearfilter.dependencies.configapi;

import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.utility.Logger;

import java.util.HashMap;
import java.util.Map;

public class Settings {

    // This class is used to store the settings of ConfigAPI

    private Map<String, Boolean> settings;  // This Map will contain all settings of ConfigAPI

    /**
     * This constructor will initialize the default values
     * |------------------------|-------|-----
     * | Setting name           | Value | Description
     * | ---------------------- | ------| -----
     * | doDebugLogging         | false | Allows debugging messages to be sent to troubleshoot a config file and the API usage. This is meant as a developer tool and logs a message when an issue occurs
     * | reportMissingOptions   | true  | On each config file reload, report missing options to the console (useful for server owners)
     * | reportRedundantOptions | false | On each config file reload, Report options which are in the live config but not in the default config
     * | reportNewConfig        | true  | On each config file reload, report the action of copying a new config file
     * | useColors              | true  | Choose whether console output should be coloured
     * | autoLoadValues         | true  | Choose to load all config contents upon creating an instance. Setting this to false will be slightly more performant if other settings need to be set first.
     * | loadDefaults           | true  | Load default values (from the default config) where live config values are missing
     * |------------------------| ------| -----
     */
    public Settings() {

        settings = new HashMap<String, Boolean>();

        // Logging settings
        settings.put("dodebuglogging", false);          // Allows debugging messages to be sent to troubleshoot a config file and the API usage. This is meant as a developer tool and logs a message when an issue occurs
        settings.put("reportmissingoptions", true);     // On each config file reload, report missing options to the console (useful for server owners)
        settings.put("reportredundantoptions", false);  // On each config file reload, Report options which are in the live config but not in the default config
        settings.put("reportnewconfig", true);          // On each config file reload, report the action of copying a new config file
        settings.put("usecolors", true);                // Choose whether console output should be coloured

        // Internal working settings
        settings.put("autoloadvalues", true);  // Choose to load all config contents upon creating an instance. Setting this to false will be slightly more performant if other settings need to be set first.
        settings.put("loaddefaults", true);    // Load default values (from the default config) where live config values are missing
    }


    // ----------------------- //
    // Settings Map management //
    // ----------------------- //


    /**
     * Get a specific setting
     * If the requested option does not exist, false will be returned
     * <p>
     * |------------------------| -----
     * | Setting name           | Description
     * | ---------------------- | -----
     * | doDebugLogging         | Allows debugging messages to be sent to troubleshoot a config file and the API usage. This is meant as a developer tool and logs a message when an issue occurs
     * | reportMissingOptions   | On each config file reload, report missing options to the console (useful for server owners)
     * | reportRedundantOptions | On each config file reload, Report options which are in the live config but not in the default config
     * | reportNewConfig        | On each config file reload, report the action of copying a new config file
     * | useColors              | Choose whether console output should be coloured
     * | autoLoadValues         | Choose to load all config contents upon creating an instance. Setting this to false will be slightly more performant if other settings need to be set first.
     * | loadDefaults           | Load default values (from the default config) where live config values are missing
     * |------------------------| -----
     *
     * @param setting the name of the setting (see table above)
     * @return a boolean: true (enabled) or false (disabled/non existing option)
     */
    public boolean getSetting(String setting) {
        String lowered = setting.toLowerCase();

        if (settings.containsKey(lowered))
            return settings.get(lowered);
        else if (settings.get("dodebuglogging"))
            Logger.log("Error while getting an option: " + setting + " does not exist! False has been returned.");

        return false;
    }


    /**
     * Update a specific setting to a new state. The table below contains all default values
     * <p>
     * |------------------------|-------|-----
     * | Setting name           | Value | Description
     * | ---------------------- | ------| -----
     * | doDebugLogging         | false | Allows debugging messages to be sent to troubleshoot a config file and the API usage. This is meant as a developer tool and logs a message when an issue occurs
     * | reportMissingOptions   | true  | On each config file reload, report missing options to the console (useful for server owners)
     * | reportRedundantOptions | false | On each config file reload, Report options which are in the live config but not in the default config
     * | reportNewConfig        | true  | On each config file reload, report the action of copying a new config file
     * | useColors              | true  | Choose whether console output should be coloured
     * | autoLoadValues         | true  | Choose to load all config contents upon creating an instance. Setting this to false will be slightly more performant if other settings need to be set first.
     * | loadDefaults           | true  | Load default values (from the default config) where live config values are missing
     * |------------------------| ------| -----
     *
     * @param setting   the option that will be edited
     * @param isEnabled the new value of the option
     */
    public void setSetting(String setting, boolean isEnabled) {


        String lowered = setting.toLowerCase();

        if (settings.containsKey(lowered)) {
            settings.put(lowered, isEnabled);
        } else if (settings.get("dodebuglogging")) {
            Logger.log("Error while setting an option: " + setting + " does not exist! This action will be ignored.");
        }
    }


    /**
     * Update multiple settings at once. This saves some programming work when working with multiple configuration files which need the same settings
     * Missing options will keep their previous value and redundant (non-existing) options will be ignored. If unsure about the available settings, check the table below.
     * <p>
     * |------------------------| -----
     * | Setting name           | Description
     * | ---------------------- | -----
     * | doDebugLogging         | Allows debugging messages to be sent to troubleshoot a config file and the API usage. This is meant as a developer tool and logs a message when an issue occurs
     * | reportMissingOptions   | On each config file reload, report missing options to the console (useful for server owners)
     * | reportRedundantOptions | On each config file reload, Report options which are in the live config but not in the default config
     * | reportNewConfig        | On each config file reload, report the action of copying a new config file
     * | useColors              | Choose whether console output should be coloured
     * | autoLoadValues         | Choose to load all config contents upon creating an instance. Setting this to false will be slightly more performant if other settings need to be set first.
     * | loadDefaults           | Load default values (from the default config) where live config values are missing
     * |------------------------| -----
     *
     * @param newSettings a Map that contains the setting's name and its boolean value
     */
    public void setSettings(Map<String, Boolean> newSettings) {
        for (Map.Entry<String, Boolean> pair : newSettings.entrySet()) {
            setSetting(pair.getKey().toLowerCase(), pair.getValue());
        }
    }


    /**
     * Gets all current settings. All options are shown below
     * <p>
     * |------------------------| -----
     * | Setting name           | Description
     * | ---------------------- | -----
     * | doDebugLogging         | Allows debugging messages to be sent to troubleshoot a config file and the API usage. This is meant as a developer tool and logs a message when an issue occurs
     * | reportMissingOptions   | On each config file reload, report missing options to the console (useful for server owners)
     * | reportRedundantOptions | On each config file reload, Report options which are in the live config but not in the default config
     * | reportNewConfig        | On each config file reload, report the action of copying a new config file
     * | useColors              | Choose whether console output should be coloured
     * | autoLoadValues         | Choose to load all config contents upon creating an instance. Setting this to false will be slightly more performant if other settings need to be set first.
     * | loadDefaults           | Load default values (from the default config) where live config values are missing
     * |------------------------| -----
     *
     * @return A Map where the key is the setting's name and the value is its setting (true, false)
     */
    public Map<String, Boolean> getSettings() {
        return settings;
    }


    /**
     * This should only be enabled if debugging is required during development
     * When enabled, ConfigAPI will send several debug messages to the console
     * Default: false
     *
     * @param setting a boolean (true: enable feature - false: disable feature)
     */
    public void setDoDebugLogging(boolean setting) {
        setSetting("doDebugLogging", setting);
    }


    /**
     * Get whether debug logging is enabled
     *
     * @return boolean of the debugging state
     */
    public boolean getDoDebugLogging() {
        return getSetting("doDebugLogging");
    }


    /**
     * When enabled, missing options will be reported to the console on each reload
     * Default: true
     *
     * @param setting a boolean (true: enable feature - false: disable feature)
     */
    public void setReportMissingOptions(boolean setting) {
        setSetting("reportMissingOptions", setting);
    }


    /**
     * Get whether missing options are being reported to the console
     *
     * @return boolean of the setting
     */
    public boolean getReportMissingOptions() {
        return getSetting("reportMissingOptions");
    }


    /**
     * Choose whether redundant options should be reported to the console
     * Default: false
     *
     * @param setting a boolean (true: enable feature - false: disable feature)
     */
    public void setReportRedundantOptions(boolean setting) {
        setSetting("reportRedundantOptions", setting);
    }


    /**
     * Get wheter redundant options are being reported to the console
     *
     * @return boolean of this setting
     */
    public boolean getReportRedundantOptions() {
        return getSetting("reportRedundantOptions");
    }


    /**
     * Set whether or not the console should be notified if the default config is copied to the live config
     * Default: true
     *
     * @param setting a boolean (true: enable feature - false: disable feature)
     */
    public void setReportNewConfig(boolean setting) {
        setSetting("reportNewConfig", setting);
    }


    /**
     * Get whether or not creating a new config is being reported to the console
     *
     * @return a boolean of this setting
     */
    public boolean getReportNewConfig() {
        return getSetting("reportNewConfig");
    }


    /**
     * This setting determines if output should be coloured
     * Positive messages would be green while negative messages would be red
     *
     * @param setting a boolean. True: colored. False: default text (white)
     */
    public void setUseColors(boolean setting) {
        setSetting("useColors", setting);
    }


    /**
     * This setting determines if output should be coloured
     * Positive messages would be green while negative messages would be red
     *
     * @return A boolean. True: colored. False: default text (white)
     */
    public boolean getUseColors() {
        return getSetting("useColors");
    }


    /**
     * This option allows for config files to be loaded automatically upon instance creation
     * When you want to change other settings BEFORE loading values, you should disable this feature in the constructor to reduce valuable processor time
     * This method is only provided for completeness
     * Set to true by default to allow this API to work out of the box
     * Default: true
     *
     * @param setting a boolean (true: enable feature - false: disable feature)
     */
    public void setAutoLoadValues(boolean setting) {
        setSetting("autoLoadValues", setting);
    }


    /**
     * Get whether the config contents are loaded on object creation
     *
     * @return a boolean of this setting
     */
    public boolean getAutoLoadValues() {
        return getSetting("autoLoadValues");
    }

    /**
     * Choose whether to load the default values (from the default config) where the live config is missing options
     *
     * @param setting a boolean (true: enable feature - false: disable feature)
     */
    public void setLoadDefaults(boolean setting) {
        setSetting("loadDefaults", setting);
    }


    /**
     * Get whether the default values are being used for missing options in the live config
     *
     * @return a boolean of this setting
     */
    public boolean getLoadDefaults() {
        return getSetting("loadDefaults");
    }


    /**
     * This method will print all settings to the console
     * The description will not be included
     * Logger#log() will be used, regardless of 'doDebugLogging'
     */
    public void printSettings() {
        Logger.log("|------------------------|-------|");
        Logger.log("| Setting name           | Value |");
        Logger.log("|------------------------|-------|");

        for (Map.Entry<String, Boolean> setting : settings.entrySet()) {
            String name = setting.getKey();
            String logString = "| " + name;

            for (int i = name.length(); i < 23; i++) {
                logString += " ";
            }

            boolean value = setting.getValue();
            logString += "| " + value;
            logString += value ? "  |" : " |";

            Logger.log(logString);
        }

        Logger.log("|------------------------|-------|");
    }
}
