package io.github.jochyoua.mychristianswearfilter.shared;

import io.github.jochyoua.mychristianswearfilter.MCSF;

public class Types {

    /**
     * Successful filtering types
     */
    public enum Filters {
        BOOKS,
        SIGNS,
        PLAYERS,
        ALL,
        DEBUG,
        DISCORD,
        CUSTOM
    }

    /**
     * Arguments manager, used for comparing permissions and current supported arguments
     */
    public enum Arguments {
        parse("MCSF.modify"),
        version("MCSF.version"),
        reload("MCSF.modify"),
        add("MCSF.modify"),
        whitelist("MCSF.modify"),
        status("MCSF.use"),
        remove("MCSF.modify"),
        reset("MCSF.modify"),
        help("MCSF.use"),
        toggle("MCSF.toggle"),
        unset("MCSF.modify"),
        global("MCSF.modify");
        String permission;

        Arguments(String s) {
            this.permission = s;
        }

        public String getPermission() {
            return permission;
        }
    }

    /**
     * Language manager, verifies the current language is supported
     */
    public enum Languages {
        en_us(), de_de(), es_es(), fr_fr();

        public static String getLanguage(MCSF plugin) {
            String lan = plugin.getString("settings.language").replaceAll(".yml", "");
            try {
                Languages.valueOf(lan);
            } catch (Exception ignored) {
                plugin.getLogger().warning("Sorry but Language (" + lan + ") doesn't exist! Using default en_us.yml");
                plugin.getConfig().set("settings.language", "en_us");
                plugin.saveConfig();
                lan = "en_us";
            }
            return lan;
        }
    }
}
