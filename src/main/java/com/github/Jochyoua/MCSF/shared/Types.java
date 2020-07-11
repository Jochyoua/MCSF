package com.github.Jochyoua.MCSF.shared;

import com.github.Jochyoua.MCSF.MCSF;
import org.bukkit.Bukkit;

public class Types {
    public enum Filters {
        BOOKS, SIGNS, PLAYERS, ALL, DEBUG;
    }

    public enum Arguments {
        version("MCSF.version"), add("MCSF.modify"), whitelist("MCSF.modify"), status("MCSF.use"), remove("MCSF.modify"), reset("MCSF.modify"), reload("MCSF.modify"), help("MCSF.use"), toggle("MCSF.toggle"), unset("MCSF.modify");
        String permission;

        Arguments(String s) {
            this.permission = s;
        }
        public String getPermission(){
            return permission;
        }
    }

    public enum Languages {
        en_us(), de_de(), es_es(), fr_fr();

        public static String getLanguage(MCSF plugin) {
            String lan = plugin.getConfig().getString("settings.language").replaceAll(".yml", "");
            try {
                Languages.valueOf(lan);
            } catch (Exception ignored) {
                Bukkit.getConsoleSender().sendMessage("[MCSF]: Sorry but Language (" + lan + ") doesn't exist! Using default en_us.yml");
                plugin.getConfig().set("settings.language", "en_us");
                plugin.saveConfig();
                lan = "en_us";
            }
            return lan;
        }
    }
}
