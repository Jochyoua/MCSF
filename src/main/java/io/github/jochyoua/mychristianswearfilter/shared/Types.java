package io.github.jochyoua.mychristianswearfilter.shared;

import io.github.jochyoua.mychristianswearfilter.MCSF;

import java.util.Objects;

public class Types {

    public Types() {
        throw new AssertionError();
    }

    /**
     * Successful filtering types
     */
    public enum Filters {
        BOOKS,
        PLAYERS,
        ALL,
        DEBUG,
        DISCORD,
        GLOBAL,
        BOTH,
        OTHER
    }

    /**
     * Arguments manager, used for comparing permissions and current supported arguments
     */
    public enum Arguments {
        parse("MCSF.modify.parse"),
        version("MCSF.version"),
        reload("MCSF.modify.reload"),
        add("MCSF.modify.add"),
        whitelist("MCSF.modify.whitelist"),
        status("MCSF.use.status"),
        remove("MCSF.modify.remove"),
        reset("MCSF.modify.reset"),
        help("MCSF.use.help"),
        toggle("MCSF.use.toggle"),
        unset("MCSF.modify.unset"),
        global("MCSF.modify.global");

        String permission;

        Arguments(String s) {
            this.permission = s;
        }

        /**
         * This grabs the permission for a command
         *
         * @return the permission needed for a command
         */
        public String getPermission() {
            return permission;
        }
    }

    /**
     * Language manager, verifies the current language is supported
     */
    public enum Languages {
        en_us(), de_de(), es_es(), fr_fr();

        /**
         * Returns the current language string,
         *
         * @param plugin the providing plugin
         * @return the current language string
         */
        public static String getLanguage(MCSF plugin) {
            String lan = Objects.requireNonNull(plugin.getConfig().getString("settings.language")).replaceAll(".yml", "");
            try {
                Languages.valueOf(lan);
            } catch (IllegalArgumentException | NullPointerException exception) {
                plugin.getLogger().warning("Sorry but Language (" + lan + ") doesn't exist! Using default en_us.yml");
                lan = "en_us";
            }
            return lan;
        }
    }
}
