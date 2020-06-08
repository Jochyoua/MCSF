package com.github.Jochyoua.MCSF;

import com.github.Jochyoua.MCSF.signcheck.SignUtils;
import me.vagdedes.mysql.database.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {
    Main plugin;
    MySQL MySQL;
    List<String> regex = new ArrayList<>();
    List<String> localSwears = new ArrayList<>();

    public Utils(Main plugin, MySQL mysql) {
        this.plugin = plugin;
        this.MySQL = mysql;
    }

    public List<String> getSwears() {
        return this.localSwears;
    }

    public void setSwears(List<String> str) {
        this.localSwears = str;
    }

    public List<String> getRegex() {
        return this.regex;
    }

    public void setRegex(List<String> str) {
        this.regex = str;
    }

    public boolean supported(String string) {
        boolean statement = false;
        switch (string.toLowerCase()) {
            case "signcheck":
                statement = !(plugin.getServer().getVersion().contains("1.8") ||
                        plugin.getServer().getVersion().contains("1.9"));
                break;
            case "discordsrv":
                statement = plugin.getConfig().getBoolean("settings.discordSRV") && plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null;
                break;
        }
        return statement;
    }

    public Double getVersion() {
        String version;
        try {
            version = ((JSONObject) new JSONParser().parse(new Scanner(new URL("https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=54115").openStream()).nextLine())).get("current_version").toString();
        } catch (ParseException | IOException ignored) {
            version = plugin.getDescription().getVersion();
        }
        return Double.parseDouble(version);
    }

    public boolean isUpToDate() {
        return !(Double.parseDouble(plugin.getDescription().getVersion()) < this.getVersion());
    }

    private void signCheck(Player player) {
        if (!plugin.getConfig().getBoolean("settings.signcheck") || !supported("SignCheck"))
            return;
        if (player == null) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        List<Sign> nearbySigns = SignUtils.getNearbyTileEntities(player.getLocation(), Bukkit.getViewDistance(), Sign.class);
        if (nearbySigns.size() == 0) {
            return;
        }
        List<Sign> first = new ArrayList<>();
        List<Sign> second = new ArrayList<>();
        int size = nearbySigns.size();
        for (int i = 0; i < size / 2; i++)
            first.add(nearbySigns.get(i));
        for (int i = size / 2; i < size; i++)
            second.add(nearbySigns.get(i));
        for (Sign sign : first) {
            player.sendSignChange(sign.getLocation(), sign.getLines());
        }
        for (Sign sign : second) {
            player.sendSignChange(sign.getLocation(), sign.getLines());
        }
        if (status(player.getUniqueId())) {
            debug("Filtering " + nearbySigns.size() + (nearbySigns.size() == 1 ? " sign" : " signs") + " for " + player.getName());
        } else {
            debug("Resetting " + nearbySigns.size() + (nearbySigns.size() == 1 ? " sign" : " signs") + " for " + player.getName());
        }
    }

    public void showHelp(CommandSender sender) {
        StringBuilder message = new StringBuilder();
        for (String str : plugin.getConfig().getStringList("variables.help")) {
            Matcher match = Pattern.compile("(?i)<%PERMISSION=(.*?)%>", Pattern.DOTALL).matcher(str);
            String permission = null;
            while (match.find()) {
                permission = match.group(1);
            }
            if (!(permission == null)) {
                if (!sender.hasPermission(permission)) {
                    continue;
                } else {
                    str = str.replaceAll("(?i)<%PERMISSION=(.*?)%>", "");
                }
            }
            message.append(str).append("\n");
        }
        send(sender, message.toString());
    }

    public void createTable(boolean reset) {
        if (plugin.getConfig().getStringList("swears").isEmpty()) {
            plugin.getConfig().set("swears", new String[]{"fuck", "shit"});
            plugin.saveConfig();
        }
        StringBuilder omg = new StringBuilder();
        for (final String ID : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
            String playername = plugin.getConfig().getString("users." + ID + ".playername");
            boolean status = status(UUID.fromString(ID));
            omg.append("('").append(ID).append("', '").append(playername).append("', '").append(status).append("'),");
        }
        if (!MySQL.isConnected())
            MySQL.connect();
        if (MySQL.isConnected()) {
            MySQL.query("SET NAMES utf8");
            MySQL.query("SET CHARACTER SET utf8");
            if (reset) {
                MySQL.update("DROP TABLE IF EXISTS swears,users;");
            }
            if (!MySQL.tableExists("swears") || MySQL.countRows("swears") == 0) {
                MySQL.update("CREATE TABLE IF NOT EXISTS swears (word varchar(255) UNIQUE);");
                MySQL.stateSwears(plugin.getConfig().getStringList("swears"));
            }
            if (!MySQL.tableExists("users") || MySQL.countRows("users") == 0) {
                MySQL.update("CREATE TABLE IF NOT EXISTS users (uuid varchar(255) UNIQUE, name varchar(255), status varchar(255));");
                MySQL.update("INSERT INTO users (uuid,name,status) VALUES " + omg.toString().trim().substring(0, omg.length() - 1) + ";");
            }
        } else {
            send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.failedtoconnect")));
        }
    }

    // Filter methods

    public boolean status(UUID ID) {
        plugin.reloadConfig();
        return plugin.getConfig().getBoolean("settings.force") ? plugin.getConfig().getBoolean("settings.force") : plugin.getConfig().getBoolean("users." + ID + ".enabled");
    }

    public String clean(String string, boolean strip, boolean log) {
        plugin.reloadConfig();
        String replacement = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.replacement"));
        if (strip) {
            replacement = ChatColor.stripColor(replacement);
        }
        if (string != null) {
            reloadPattern();
            Pattern pattern = Pattern.compile(String.join("|", getRegex()), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
            Matcher matcher = pattern.matcher(string);
            StringBuffer out = new StringBuffer();
            int swearcount = plugin.getConfig().getInt("swearcount");
            while (matcher.find()) {
                String r = plugin.getConfig().getBoolean("settings.replace_word_for_word") ? replacement : matcher.group(0).replaceAll("(?s).", replacement);
                matcher.appendReplacement(out, r);
                swearcount++;
            }
            if (log) {
                plugin.getConfig().set("swearcount", swearcount);
                plugin.saveConfig();
            }
            matcher.appendTail(out);
            string = out.toString();
        }
        return string;
    }

    public String escape(String str) {
        return Pattern.compile("[{}()\\[\\].+*?^$\\\\|]").matcher(str).replaceAll("\\$0");
    }

    public boolean isclean(String str) {
        plugin.reloadConfig();
        reloadPattern();
        return !Pattern.compile(String.join("|", regex), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS).matcher(str).find();
    }

    public boolean toggle(UUID ID) {
        plugin.reloadConfig();
        Boolean value = null;
        if (plugin.getConfig().getBoolean("settings.mysql")) {
            if (!MySQL.isConnected()) {
                MySQL.connect();
            }
            if (MySQL.isConnected()) {
                if (!MySQL.exists("uuid", ID.toString(), "users")) {
                    value = plugin.getConfig().getBoolean("settings.default");
                    MySQL.update("INSERT INTO users (uuid,status) VALUES ('" + ID + "','" + value + "')");
                    plugin.getConfig().set("users." + ID + ".enabled", value);
                } else {
                    ResultSet rs = MySQL.query("SELECT status FROM users WHERE uuid='" + ID + "'");
                    boolean result = false;
                    try {
                        while (rs.next()) {
                            result = Boolean.parseBoolean(rs.getString("status"));
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        result = plugin.getConfig().getBoolean("settings.default");
                    }
                    value = !result;
                    MySQL.update("UPDATE users SET status='" + value + "' WHERE uuid='" + ID + "';");
                    plugin.getConfig().set("users." + ID + ".enabled", value);
                }
            }
        } else if (!plugin.getConfig().isSet("users." + ID + ".enabled")) { // If enabled value doesn't exist, set to default value
            value = plugin.getConfig().getBoolean("settings.default");
            plugin.getConfig().set("users." + ID + ".enabled", value);
        }
        if (value == null) {
            value = plugin.getConfig().getBoolean("users." + ID + ".enabled");
            plugin.getConfig().set("users." + ID + ".enabled", !value);
            value = !value;
            plugin.saveConfig();
        }
        plugin.saveConfig();
        signCheck(Bukkit.getPlayer(ID));
        return value;
    }


    public void reload() {
        if (plugin.getConfig().getBoolean("settings.mysql")) {
            if (!MySQL.isConnected())
                MySQL.connect();
            if (MySQL.isConnected()) {
                ArrayList<String> array = new ArrayList<>();
                ResultSet rs;
                try {
                    rs = MySQL.query("SELECT * FROM swears;");
                    while (rs.next()) {
                        array.add(rs.getString("word"));
                    }
                    rs = MySQL.query("SELECT * FROM users");
                    while (rs.next()) {
                        String ID = rs.getObject("uuid").toString();
                        String name = rs.getObject("name").toString();
                        boolean status = Boolean.parseBoolean(rs.getObject("status").toString());
                        plugin.getConfig().set("users." + ID + ".enabled", status);
                        plugin.getConfig().set("users." + ID + ".playername", name);
                    }
                } catch (Exception ignored) {
                }
                if (!array.isEmpty()) {
                    plugin.getConfig().set("swears", array);
                }
                plugin.saveConfig();
            } else {
                send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.failedtoconnect")));
            }
        }
        plugin.reloadConfig();
        reloadPattern();
    }


    public void debug(String str) {
        if (plugin.getConfig().getBoolean("settings.debug"))
            send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.debug").replace("%message%", str));
    }

    private void reloadPattern() {
        if ((getSwears().size() != plugin.getConfig().getStringList("swears").size()) || getRegex().isEmpty()) {
            debug("localSwears doesn't equal config parameters or regex is empty, filling variables.");
            setSwears(plugin.getConfig().getStringList("swears"));
            if (getSwears().isEmpty()) {
                send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.failure")
                        .replaceAll("%message%", "PATH `swears` is empty in config, please fix this ASAP; Using `fuck, shit` as placeholders"));
                setSwears(Arrays.asList("fuck", "shit"));
            }
            regex.clear();
            List<String> duh = new ArrayList<>();
            for (String str : getSwears()) {
                str = str.replaceAll("\\s+","");
                StringBuilder omg = new StringBuilder();
                for (String str2 : str.split("")) {
                    str2 = escape(str2);
                    omg.append(str2).append("+\\s*");
                }
                duh.add(omg.toString().substring(0, omg.toString().length() - 4) + "+");
            }
            setRegex(duh.stream().sorted((s1, s2) -> s2.length() - s1.length())
                    .collect(Collectors.toList()));
        }
    }

    public String prepare(CommandSender player, String message) {
        message = message.replaceAll("(?i)%prefix%", Objects.requireNonNull(plugin.getConfig().getString("variables.prefix")));
        message = message.replaceAll("(?i)%command%", Objects.requireNonNull(plugin.getConfig().getString("variables.command")));
        message = message.replaceAll("(?i)%player%", player.getName());
        message = message.replaceAll("(?i)%current%", plugin.getDescription().getVersion());
        message = message.replaceAll("(?i)%version%", String.valueOf(getVersion()));
        message = message.replaceAll("(?i)%serverversion%", plugin.getServer().getVersion());
        message = message.replaceAll("(?i)%swearcount%", Integer.toString(plugin.getConfig().getInt("swearcount")));
        message = message.replaceAll("(?i)%wordcount%", Integer.toString(plugin.getConfig().getStringList("swears").size()));
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void send(CommandSender player, String message) {
        if ("".equals(message))
            return;
        message = prepare(player, message);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
