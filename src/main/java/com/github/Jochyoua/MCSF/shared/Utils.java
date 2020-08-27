package com.github.Jochyoua.MCSF.shared;

import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.signcheck.SignUtils;
import net.jodah.expiringmap.ExpiringMap;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {
    MCSF plugin;
    MySQL MySQL;

    List<String> regex = new ArrayList<>();
    List<String> localSwears = new ArrayList<>();
    List<String> localWhitelist = new ArrayList<>();
    ExpiringMap<UUID, UUID> cooldowns;

    public Utils(MCSF plugin, MySQL mysql) {
        this.plugin = plugin;
        this.MySQL = mysql;
        cooldowns = ExpiringMap.builder()
                .expiration(plugin.getConfig().getInt("settings.cooldown", 5), TimeUnit.SECONDS)
                .build();
    }

    public
    static String color(String message) {
        StringBuffer rgbBuilder = new StringBuffer();
        Matcher rgbMatcher = Pattern.compile("(&)?&#([0-9a-fA-F]{6})").matcher(message);
        while (rgbMatcher.find()) {
            boolean isEscaped = (rgbMatcher.group(1) != null);
            if (!isEscaped) {
                try {
                    String hexCode = rgbMatcher.group(2);
                    if (hexCode.startsWith("#")) {
                        hexCode = hexCode.substring(1);
                    }
                    Color.decode("#" + hexCode);
                    StringBuilder assembledColorCode = new StringBuilder();
                    assembledColorCode.append("\u00a7x");
                    for (char curChar : hexCode.toCharArray()) {
                        assembledColorCode.append("\u00a7").append(curChar);
                    }
                    rgbMatcher.appendReplacement(rgbBuilder, assembledColorCode.toString());
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            rgbMatcher.appendReplacement(rgbBuilder, "&#$2");
        }
        rgbMatcher.appendTail(rgbBuilder);
        return rgbBuilder.toString();
    }

    public ExpiringMap<UUID, UUID> getCooldowns() {
        return this.cooldowns;
    }

    public void addUser(UUID id) {
        cooldowns.put(id, id);
    }

    public List<String> getSwears() {
        return this.localSwears;
    }

    public void setSwears(List<String> str) {
        this.localSwears = sortArray(str);
    }

    public List<String> sortArray(List<String> str) {
        return str.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getWhitelist() {
        return this.localWhitelist;
    }

    public void setWhitelist(List<String> str) {
        this.localWhitelist = sortArray(str);
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
            case "hex":
                statement = Integer.parseInt(Bukkit.getBukkitVersion().split("[.\\-]")[1]) >= 16;
                break;
            case "signcheck":
                statement = Integer.parseInt(Bukkit.getBukkitVersion().split("[.\\-]")[1]) >= 9;
                break;
            case "discordsrv":
                statement = plugin.getConfig().getBoolean("settings.discordSRV.enabled") && plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null;
                break;
            case "protocollib":
                statement = plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null;
                break;
            case "mysql":
                statement = plugin.getConfig().getBoolean("mysql.enabled");
                break;
        }
        return statement;
    }

    public Double getVersion() {
        String version;
        try {
            version = ((JSONObject) new JSONParser().parse(new Scanner(new URL("https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=54115").openStream()).nextLine())).get("current_version").toString();
        } catch (Exception ignored) {
            version = plugin.getDescription().getVersion();
        }
        return Double.parseDouble(version);
    }

    public boolean isUpToDate() {
        boolean isuptodate = true;
        try {
            isuptodate = !(Double.parseDouble(plugin.getDescription().getVersion()) < this.getVersion());
        } catch (NumberFormatException e) {
            send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.updatecheck")));
            e.printStackTrace();
        }
        return isuptodate;
    }

    private void signCheck(Player player) {
        if (!plugin.getConfig().getBoolean("settings.filtering.filter checks.signcheck") || !supported("SignCheck"))
            return;
        if (player == null) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        int distance;
        try {
            distance = player.getClientViewDistance();
        } catch (Exception e) {
            distance = Bukkit.getViewDistance();
        }
        List<Sign> nearbySigns = SignUtils.getNearbyTileEntities(player.getLocation(), distance, Sign.class);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendSignChange(sign.getLocation(), sign.getLines());
                });
            }
            for (Sign sign : second) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendSignChange(sign.getLocation(), sign.getLines());
                });
            }
            if (status(player.getUniqueId())) {
                debug("Filtering " + nearbySigns.size() + (nearbySigns.size() == 1 ? " sign" : " signs") + " for " + player.getName());
            } else {
                debug("Resetting " + nearbySigns.size() + (nearbySigns.size() == 1 ? " sign" : " signs") + " for " + player.getName());
            }
        });
    }

    // Filter methods

    public void showHelp(CommandSender sender) {
        StringBuilder message = new StringBuilder();
        for (String str : plugin.getLanguage().getStringList("variables.help")) {
            Matcher match = Pattern.compile("(?i)\\{PERMISSION=(.*?)}|(?i)<%PERMISSION=(.*?)%>", Pattern.DOTALL).matcher(str);
            String permission = null;
            while (match.find()) {
                permission = match.group(1);
            }
            if (!(permission == null)) {
                if (!sender.hasPermission(permission)) {
                    continue;
                } else {
                    str = str.replaceAll("(?i)\\{PERMISSION=(.*?)}|(?i)<%PERMISSION=(.*?)%>", "");
                }
            }
            message.append(str).append("\n");
        }
        send(sender, message.toString());
    }

    public void setTable(String table) {
        switch (table) {
            case "users":
                if (!MySQL.tableExists("users") || MySQL.countRows("users") == 0) {
                    StringBuilder omg = new StringBuilder();
                    for (final String ID : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                        String playername = plugin.getConfig().getString("users." + ID + ".playername");
                        boolean status = status(UUID.fromString(ID));
                        omg.append("('").append(ID).append("', '").append(playername).append("', '").append(status).append("'),");
                    }
                    MySQL.update("CREATE TABLE IF NOT EXISTS users (uuid varchar(255) UNIQUE, name varchar(255), status varchar(255));");
                    MySQL.update("INSERT INTO users (uuid,name,status) VALUES " + omg.toString().trim().substring(0, omg.length() - 1) + ";");
                }
                break;
            case "swears":
                if (!MySQL.tableExists("swears") || MySQL.countRows("swears") == 0) {
                    MySQL.update("CREATE TABLE IF NOT EXISTS swears (word varchar(255) UNIQUE);");
                    MySQL.stateSwears(sortArray(plugin.getConfig().getStringList("swears")));
                }
                break;
            case "whitelist":
                if (!MySQL.tableExists("whitelist") || MySQL.countRows("whitelist") == 0) {
                    MySQL.update("CREATE TABLE IF NOT EXISTS whitelist (word varchar(255) UNIQUE);");
                    MySQL.stateWhite(sortArray(plugin.getConfig().getStringList("whitelist")));
                }
                break;
            default:
                debug("No correct database was selected.");
                break;
        }
    }

    public void createTable(boolean reset) {
        if (plugin.getConfig().getStringList("swears").isEmpty()) {
            plugin.getConfig().set("swears", new String[]{"fuck", "shit"});
            plugin.saveConfig();
        }
        if (!MySQL.isConnected())
            MySQL.connect();
        if (MySQL.isConnected()) {
            MySQL.query("SET NAMES utf8");
            MySQL.query("SET CHARACTER SET utf8");
            if (reset) {
                MySQL.update("DROP TABLE IF EXISTS swears,users,whitelist;");
            }
            try {
                setTable("users");
                setTable("swears");
                setTable("whitelist");
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("Failed to set tables!:");
                e.printStackTrace();
            }

        } else {
            send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.failedtoconnect")));
        }
    }

    public boolean status(UUID ID) {
        plugin.reloadConfig();
        return plugin.getConfig().getBoolean("settings.filtering.force") || plugin.getConfig().getBoolean("users." + ID + ".enabled");
    }

    String random() {
        return UUID.randomUUID().toString();
    }

    public String escape(String str) {
        return Pattern.compile("[{}()\\[\\].+*?^$\\\\|]").matcher(str).replaceAll("\\$0");
    }

    public String clean(String string, boolean strip, boolean log, Types.Filters type) {
        plugin.reloadConfig();
        String replacement = plugin.getConfig().getString("settings.filtering.replacement");
        if (string != null) {
            reloadPattern();
            List<String> array = getRegex();
            Map<String, String> whitelist = new HashMap<>();
            if (plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
                String lstring = string.trim();
                if (type.equals(Types.Filters.SIGNS)) {
                    lstring = string.trim().replaceAll("(\\{\"extra\":\\[\\{\"text\":\"|\"}],\"text\":|}_\\{\"text\":|})", " ").trim().replaceAll("\"", "");
                } else {
                    lstring = string.replaceAll("[^\\p{L}0-9 ]+", " ").trim();
                }
                for (String str : lstring.split(" ")) {
                    if (type.equals(Types.Filters.ALL)) {
                        str = str.trim().replaceAll("[\"{}\\]]", "").replace(",text:", "");
                    }
                    if (getWhitelist().stream().anyMatch(str::equalsIgnoreCase)) {
                        String r;
                        if (!whitelist.containsKey(str)) {
                            r = random();
                        } else {
                            r = whitelist.get(str);
                        }
                        if (!whitelist.containsKey(str)) {
                            whitelist.put(str, r);
                        }
                        string = string.replaceAll("(\\b" + str + "\\b)", r);
                    }
                }
            }
            if (plugin.getConfig().getBoolean("custom_regex.enabled")) {
                for (String str : plugin.getConfig().getStringList("custom_regex.regex")) {
                    Matcher match = Pattern.compile("(?i)\\{TYPE=(.*?)}", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS).matcher(str);
                    str = str.replaceAll("(?i)\\{TYPE=(.*?)}", "").trim();
                    String found = null;
                    while (match.find()) {
                        found = match.group(1);
                    }
                    if (found == null) {
                        debug("Custom regex is missing {TYPE=} parameters. Adding it with the parameters ALL.");
                        if (!array.contains(str)) {
                            array.add(str);
                        }
                    } else {
                        if (found.contains(type.toString()) || found.contains("ALL")) {
                            if (!array.contains(str)) {
                                array.add(str);
                            }
                        }
                    }
                }
            }
            Pattern pattern = Pattern.compile(String.join("|", array), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
            Matcher matcher = pattern.matcher(string);
            StringBuffer out = new StringBuffer();
            int swearcount = plugin.getConfig().getInt("swearcount");
            while (matcher.find()) {
                try {
                    String[] arr = matcher.toMatchResult().toString().split("=");
                    String str = arr[arr.length - 1].replaceAll("[^\\p{L}0-9 ]+", " ").trim();
                    if (plugin.getConfig().getBoolean("settings.replace word for word")) {
                        replacement = (plugin.getConfig().isSet("replacements." + str) ? plugin.getConfig().getString("replacements." + str) :
                                (plugin.getConfig().isSet("replacements.all") ? plugin.getConfig().getString("replacements.all") : plugin.getConfig().getString("settings.filtering.replacement")));
                    }
                } catch (Exception e) {
                    debug("Could not register replace_word_for_words: " + e.getMessage());
                }
                if (replacement != null) {
                    replacement = ChatColor.translateAlternateColorCodes('&', replacement);
                }
                if (strip) {
                    replacement = ChatColor.stripColor(replacement);
                }
                String r = plugin.getConfig().getBoolean("settings.replace word for word") ? replacement : matcher.group(0).replaceAll("(?s).", replacement);
                if (type.equals(Types.Filters.DISCORD)) {
                    if (plugin.getConfig().getBoolean("settings.discordSRV.escape special chars.escape swears", true))
                        r = r.replaceAll("\\*", "\\\\*")
                                .replaceAll("_", "\\_")
                                .replaceAll("\\|", "\\\\|")
                                .replaceAll("~", "\\~")
                                .replaceAll("`", "\\\\`");
                    else if (plugin.getConfig().getBoolean("settings.discordSRV.escape special chars.escape entire message", false)) {
                        string = string.replaceAll("\\*", "\\\\*")
                                .replaceAll("_", "\\_")
                                .replaceAll("\\|", "\\\\|")
                                .replaceAll("~", "\\~")
                                .replaceAll("`", "\\\\`");
                    }
                    if (plugin.getConfig().getBoolean("settings.discordSRV.spoilers.enabled", false))
                        r = Objects.requireNonNull(plugin.getConfig().getString("settings.discordSRV.spoilers.template", "||{swear}||"))
                                .replaceAll("(?i)\\{swear}|(?i)%swear%", matcher.group(0));
                }
                matcher.appendReplacement(out, r);
                swearcount++;
            }
            if (log) {
                plugin.getConfig().set("swearcount", swearcount);
                plugin.saveConfig();
            }
            matcher.appendTail(out);
            string = out.toString();
            for (Map.Entry<String, String> str : whitelist.entrySet()) {
                string = string.replaceAll(str.getValue(), str.getKey());
            }
        }
        return string;
    }


    public boolean isclean(String str) {
        str = str.replaceAll("[^\\p{L}0-9 ]+", " ").trim();
        plugin.reloadConfig();
        reloadPattern();
        List<String> array = getRegex();
        if (plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
            for (String strs : str.split(" ")) {
                if (getWhitelist().stream().anyMatch(strs::equalsIgnoreCase)) {
                    str = str.replaceAll("(\\b" + strs + "\\b)", "");
                }
            }
        }
        if (plugin.getConfig().getBoolean("custom_regex.enabled"))
            for (String str2 : plugin.getConfig().getStringList("custom_regex.regex")) {
                str2 = str2.replaceAll("(?i)\\{TYPE=(.*?)}", "").trim();
                if (!array.contains(str2)) {
                    array.add(str2);
                }
            }
        return !Pattern.compile(String.join("|", array), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS).matcher(str).find();
    }

    public List<String> getUsers() {
        List<String> users = new ArrayList<>();
        for (final String ID : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
            users.add(plugin.getConfig().getString("users." + ID + ".playername"));
        }
        return users;
    }

    public boolean toggle(UUID ID) {
        plugin.reloadConfig();
        if (plugin.getConfig().getBoolean("settings.filtering.force"))
            return true;
        Boolean value = null;
        if (supported("mysql")) {
            if (!MySQL.isConnected()) {
                MySQL.connect();
            }
            if (MySQL.isConnected()) {
                setTable("users");
                if (!MySQL.exists("uuid", ID.toString(), "users")) {
                    value = plugin.getConfig().getBoolean("settings.filtering.default");
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
                        result = plugin.getConfig().getBoolean("settings.filtering.default");
                    }
                    value = !result;
                    MySQL.update("UPDATE users SET status='" + value + "' WHERE uuid='" + ID + "';");
                    plugin.getConfig().set("users." + ID + ".enabled", value);
                }
            }
        } else if (!plugin.getConfig().isSet("users." + ID + ".enabled")) { // If enabled value doesn't exist, set to default value
            value = plugin.getConfig().getBoolean("settings.filtering.default");
            plugin.getConfig().set("users." + ID + ".enabled", value);
        }
        if (value == null) {
            value = plugin.getConfig().getBoolean("users." + ID + ".enabled");
            plugin.getConfig().set("users." + ID + ".enabled", !value);
            value = !value;
            plugin.saveConfig();
        }
        plugin.saveConfig();
        Bukkit.getScheduler().runTask(plugin, () -> signCheck(Bukkit.getPlayer(ID)));
        return value;
    }

    public void reload() {
        if (supported("mysql")) {
            if (!MySQL.isConnected())
                MySQL.connect();
            if (MySQL.isConnected()) {
                ArrayList<String> swears = new ArrayList<>();
                ArrayList<String> whitelist = new ArrayList<>();
                ResultSet rs;
                try {
                    rs = MySQL.query("SELECT * FROM swears;");
                    while (rs.next()) {
                        swears.add(rs.getString("word"));
                    }
                    rs = MySQL.query("SELECT * FROM users");
                    while (rs.next()) {
                        String ID = rs.getObject("uuid").toString();
                        String name = rs.getObject("name").toString();
                        boolean status = Boolean.parseBoolean(rs.getObject("status").toString());
                        plugin.getConfig().set("users." + ID + ".enabled", status);
                        plugin.getConfig().set("users." + ID + ".playername", name);
                    }
                    rs = MySQL.query("SELECT * FROM whitelist");
                    while (rs.next()) {
                        whitelist.add(rs.getString("word"));
                    }
                } catch (Exception ignored) {
                }
                if (!swears.isEmpty()) {
                    plugin.getConfig().set("swears", swears);
                }
                if (!whitelist.isEmpty()) {
                    plugin.getConfig().set("whitelist", whitelist);
                }
                plugin.saveConfig();
            } else {
                send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.failedtoconnect")));
            }
        }
        reloadPattern();
    }

    public void debug(String str) {
        if (plugin.getConfig().getBoolean("settings.debug")) {
            String message = prepare(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.debug").replaceAll("(?i)\\{message}|(?i)%message%", str));
            send(Bukkit.getConsoleSender(), message);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                File file = new File(plugin.getDataFolder(), "/debug/log.txt");
                File dir = new File(plugin.getDataFolder(), "debug");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
                    bw.append("[").append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date())).append("] ").append(ChatColor.stripColor(message)).append("\n");
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void reloadPattern() {
        if ((getWhitelist().size() != plugin.getConfig().getStringList("whitelist").size())) {
            debug("Whitelist doesn't equal local paramters, filling variables.");
            setWhitelist(plugin.getConfig().getStringList("whitelist"));
            if (getWhitelist().isEmpty()) {
                send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure")
                        .replaceAll("(?i)\\{message}|(?i)%message%", "PATH `whitelist` is empty in config, please fix this ASAP; Using `class, hello` as placeholders"));
                setWhitelist(Arrays.asList("class", "hello"));
            }
        }
        if ((getSwears().size() != plugin.getConfig().getStringList("swears").size()) || getRegex().isEmpty()) {
            debug("localSwears doesn't equal config parameters or regex is empty, filling variables.");
            setSwears(plugin.getConfig().getStringList("swears"));
            if (getSwears().isEmpty()) {
                send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure")
                        .replaceAll("(?i)\\{message}|(?i)%message%", "PATH `swears` is empty in config, please fix this ASAP; Using `fuck, shit` as placeholders"));
                setSwears(Arrays.asList("fuck", "shit"));
            }
            regex.clear();
            List<String> duh = new ArrayList<>();
            for (String str : getSwears()) {
                if (!plugin.getConfig().getBoolean("settings.filtering.ignore special characters.enabled")) {
                    str = str.replaceAll("\\s+", "");
                    StringBuilder omg = new StringBuilder();
                    for (String str2 : str.split("")) {
                        //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+)
                        str2 = Pattern.quote(str2);
                        omg.append(str2).append("+\\s*");
                    }
                    duh.add(omg.substring(0, omg.toString().length() - 4) + "+");
                    if (plugin.getConfig().getBoolean("settings.filtering.filter reverse versions of swears", true)) {
                        StringBuilder omg2 = new StringBuilder();
                        for (String str3 : new StringBuilder(str).reverse().toString().split("")) {
                            str3 = Pattern.quote(str3);
                            omg2.append(str3).append("+\\s*");
                        }
                        duh.add(omg2.substring(0, omg2.toString().length() - 4) + "+");
                    }
                } else {
                    str = str.replaceAll("\\s+", "");
                    StringBuilder omg = new StringBuilder();
                    int length = str.length();
                    for (String str2 : str.split("")) {
                        length = length - 1;
                        //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+) = fuck
                        str2 = Pattern.quote(str2);
                        if (length <= 0) {
                            omg.append("(").append(str2).append("+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-"))).append("]+").append(str2).append("+)");
                        } else {
                            omg.append("(").append(str2).append("+\\s*+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-"))).append("]+").append(str2).append("+)");
                        }
                    }
                    duh.add(omg.toString());
                    if (plugin.getConfig().getBoolean("settings.filtering.filter reverse versions of swears", true)) {
                        StringBuilder omg2 = new StringBuilder();
                        int length2 = str.length();
                        for (String str3 : new StringBuilder(str).reverse().toString().split("")) {
                            length2 = length2 - 1;
                            str3 = Pattern.quote(str3);
                            if (length <= 0) {
                                omg2.append("(").append(str3).append("+\\s*+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-"))).append("]+").append(str3).append("+)");
                            } else {
                                omg2.append("(").append(str3).append("+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-"))).append("]+").append(str3).append("+)");
                            }
                        }
                        duh.add(omg2.toString());
                    }
                }
            }
            setRegex(duh.stream().sorted((s1, s2) -> s2.length() - s1.length())
                    .collect(Collectors.toList()));
        }
    }

    public String prepare(CommandSender player, String message) {
        message = message.replaceAll("(?i)\\{prefix}|(?i)%prefix%", Objects.requireNonNull(plugin.getLanguage().getString("variables.prefix")));
        message = message.replaceAll("(?i)\\{command}|(?i)%command%", "mcsf");
        message = message.replaceAll("(?i)\\{player}|(?i)%player%", player.getName());
        message = message.replaceAll("(?i)\\{current}|(?i)%current%", plugin.getDescription().getVersion());
        message = message.replaceAll("(?i)\\{version}|(?i)%version%", String.valueOf(getVersion()));
        message = message.replaceAll("(?i)\\{serverversion}|(?i)%serverversion%", plugin.getServer().getVersion());
        message = message.replaceAll("(?i)\\{swearcount}|(?i)%swearcount", Integer.toString(plugin.getConfig().getInt("swearcount")));
        message = message.replaceAll("(?i)\\{wordcount}|(?i)%wordcount%", Integer.toString(plugin.getConfig().getStringList("swears").size()));
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (supported("hex"))
            message = color(message);
        return message;
    }

    public void send(CommandSender sender, String message) {
        if ("".equals(message))
            return;
        message = prepare(sender, message);
        if (supported("hex"))
            sender.spigot().sendMessage(new TextComponent(message));
        else
            sender.sendMessage(message);
    }
}