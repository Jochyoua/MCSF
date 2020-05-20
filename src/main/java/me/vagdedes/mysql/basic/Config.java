package me.vagdedes.mysql.basic;

import com.github.Jochyoua.MCSF.Main;

public class Config {
    Main plugin;
    public Config(Main plugin){
        this.plugin = plugin;
    }
    public String getHost() {
        return get("mysql.host");
    }

    public String getUser() {
        return get("mysql.username");
    }

    public String getPassword() {
        return get("mysql.password");
    }

    public String getDatabase() {
        return get("mysql.database");
    }

    public String getPort() {
        return get("mysql.port");
    }

    public boolean getSSL() {
        return getBoolean("mysql.ssl");
    }

    private String get(String name) {
        return (name == null || !plugin.getConfig().contains(name)) ? "" : plugin.getConfig().getString(name);
    }

    private boolean getBoolean(String name) {
        return (name != null && plugin.getConfig().contains(name) && plugin.getConfig().getBoolean(name));
    }
}