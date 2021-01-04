package io.github.Jochyoua.MyChristianSwearFilter.shared;

import io.github.Jochyoua.MyChristianSwearFilter.MCSF;
import io.github.Jochyoua.MyChristianSwearFilter.shared.HikariCP.DatabaseConnector;
import io.github.Jochyoua.MyChristianSwearFilter.shared.HikariCP.HikariCP;
import io.github.Jochyoua.MyChristianSwearFilter.signcheck.SignUtils;
import lombok.SneakyThrows;
import net.jodah.expiringmap.ExpiringMap;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    MCSF plugin;
    DatabaseConnector connector;
    List<String> regex = new ArrayList<>();
    List<String> globalRegex = new ArrayList<>();
    List<String> localSwears = new ArrayList<>();
    List<String> localWhitelist = new ArrayList<>();
    List<String> globalSwears = new ArrayList<>();
    List<String> json = new ArrayList<>();
    ExpiringMap<UUID, UUID> cooldowns;
    Connection connection;

    public Utils(MCSF plugin, DatabaseConnector connector) {
        this.plugin = plugin;
        this.connector = connector;
        if (plugin.getConfig().getBoolean("mysql.enabled"))
            try {
                this.connection = connector.getConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        json.add("extra");
        json.add("italic");
        json.add("text");
        json.add("color");
        for (ChatColor c : ChatColor.values()) {
            json.add(c.toString());
        }
        List<String> revjson = new ArrayList<>();
        for (String j : json) {
            revjson.add(new StringBuffer(j).reverse().toString());
        }
        json.addAll(revjson);
        cooldowns = ExpiringMap.builder()
                .expiration(plugin.getConfig().getInt("settings.cooldown", 5), TimeUnit.SECONDS)
                .build();
    }

    public String color(String message) {
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

    public List<String> getGlobal() {
        return this.globalSwears;
    }

    public void setGlobal(List<String> str) {
        this.globalSwears = sortArray(str);
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

    public boolean tableExists(String table) {
        return true;
    }

    public int countRows(String table) {
        if (!plugin.getConfig().getBoolean("mysql.enabled"))
            return 0;
        int i = 0;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + table);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                i++;
            ps.close();
        } catch (Exception ignored) {
        }
        return i;
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

    public void signCheck(UUID ID) {
        Player player = (Player) Bukkit.getOfflinePlayer(ID);
        if (!plugin.getConfig().getBoolean("settings.filtering.filter checks.signcheck") || !supported("SignCheck"))
            return;
        if (player == null) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        int distance = Bukkit.getViewDistance();
        List<Sign> nearbySigns = SignUtils.getNearbyTileEntities(player.getLocation(), distance, Sign.class);
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
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendSignChange(sign.getLocation(), sign.getLines());
            }, 20);
        }
        for (Sign sign : second) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendSignChange(sign.getLocation(), sign.getLines());
            }, 20);
        }
        if (status(player.getUniqueId())) {
            debug("Filtering " + nearbySigns.size() + (nearbySigns.size() == 1 ? " sign" : " signs") + " for " + player.getName());
        } else {
            debug("Resetting " + nearbySigns.size() + (nearbySigns.size() == 1 ? " sign" : " signs") + " for " + player.getName());
        }
    }

    // Filter methods

    public void showHelp(CommandSender sender) {
        StringBuilder message = new StringBuilder();
        int length = plugin.getLanguage().getStringList("variables.help").size();
        for (String str : plugin.getLanguage().getStringList("variables.help")) {
            length = length - 1;
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
            if (length <= 0) { // length is the end
                message.append(str);
            } else { // length is not the end
                message.append(str).append("\n");
            }
        }
        send(sender, message.toString());
    }

    @SneakyThrows
    public void setTable(String table) {
        if (!plugin.getConfig().getBoolean("mysql.enabled"))
            return;
        switch (table) {
            case "global":
                if (!tableExists("global") || countRows("global") == 0) {
                    try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.GLOBAL.create)) {
                        ps.execute();
                        ps.close();
                    }
                    FileConfiguration local = plugin.getFile("global");
                    List<String> s = local.getStringList("global");
                    if (s.isEmpty()) {
                        debug("Cannot set global data because it is empty!");
                        break;
                    }
                    StringBuilder query = new StringBuilder("INSERT INTO global(word) VALUES (?)");
                    for (int i = 0; i < s.size() - 1; i++) {
                        query.append(", (?)");
                    }
                    PreparedStatement ps = connection.prepareStatement(query.toString());
                    for (int i = 0; i < s.size(); i++) {
                        ps.setString(i + 1, s.get(i));
                    }
                    ps.execute();
                    ps.close();
                }
                break;
            case "users":
                if (!tableExists("users") || countRows("users") == 0) {
                    debug("(MySQL) attempting to insert user data");
                    try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.create)) {
                        ps.execute();
                        ps.close();
                    }
                    List<String> user_list = new ArrayList<>();

                    StringBuilder query = new StringBuilder("INSERT INTO users(uuid, name, status) VALUES ");
                    for (final String ID : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                        String playername = plugin.getConfig().getString("users." + ID + ".playername");
                        boolean status = status(UUID.fromString(ID));
                        user_list.add(ID);
                        user_list.add(playername);
                        user_list.add(String.valueOf(status));
                        query.append("(?,?,?), ");
                        /*try (
                             PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.insert)) {
                            ps.setString(1, ID);
                            ps.setString(2, playername);
                            ps.setBoolean(3, status);
                            ps.setBoolean(4, status);
                            ps.execute();
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }*/
                    }
                    if (user_list.isEmpty()) {
                        debug("Cannot set user data because it is empty!");
                        break;
                    }
                    PreparedStatement ps = connection.prepareStatement(query.toString().trim().substring(0, query.toString().trim().length() - 1));
                    for (int i = 0; i < user_list.size(); i++) {
                        ps.setString(i + 1, user_list.get(i));
                    }
                    ps.execute();
                    ps.close();
                }
                break;
            case "swears":
                if (!tableExists("swears") || countRows("swears") == 0) {
                    debug("(MySQL) attempting to insert swear data");
                    try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.SWEARS.create)) {
                        ps.execute();
                        ps.close();
                    }
                    List<String> s = plugin.getFile("swears").getStringList("swears");
                    s.removeAll(json);
                    StringBuilder query = new StringBuilder("INSERT INTO swears(word) VALUES (?)");
                    for (int i = 0; i < s.size() - 1; i++) {
                        query.append(", (?)");
                    }
                    PreparedStatement ps = connection.prepareStatement(query.toString());
                    for (int i = 0; i < s.size(); i++) {
                        ps.setString(i + 1, s.get(i));
                    }
                    ps.execute();
                    ps.close();
                }
                break;
            case "whitelist":
                if (!tableExists("whitelist") || countRows("whitelist") == 0) {
                    debug("(MySQL) attempting to insert whitelist data");
                    try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.WHITELIST.create)) {
                        ps.execute();
                        ps.close();
                    }
                    FileConfiguration local = plugin.getFile("whitelist");
                    List<String> s = local.getStringList("whitelist");

                    local = plugin.getFile("swears");
                    s.removeAll(local.getStringList("swears"));
                    StringBuilder query = new StringBuilder("INSERT INTO whitelist(word) VALUES (?)");
                    for (int i = 0; i < s.size() - 1; i++) {
                        query.append(", (?)");
                    }
                    PreparedStatement ps = connection.prepareStatement(query.toString());
                    for (int i = 0; i < s.size(); i++) {
                        ps.setString(i + 1, s.get(i));
                    }
                    ps.execute();
                    ps.close();
                }
                break;
            default:
                debug("No correct database was selected.");
                break;
        }
    }

    public void createTable(boolean reset) throws SQLException {
        if (plugin.getConfig().getBoolean("mysql.enabled")) {
            plugin.reloadConfig();
            FileConfiguration local = plugin.getFile("swears");
            if (local.getStringList("swears").isEmpty()) {
                local.set("swears", new String[]{"fuck", "shit"});
                plugin.saveFile(local, "swears");
            }
            if (plugin.reloadSQL()) {
                connector.execute("SET NAMES utf8");
                connector.execute("SET CHARACTER SET utf8");
                if (reset) {
                    connector.execute("DROP TABLE IF EXISTS swears,users,whitelist,global;");
                }
                try {
                    setTable("users");
                    setTable("swears");
                    setTable("whitelist");
                    setTable("global");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failure: {message}"
                            .replaceAll("(?i)\\{message}|(?i)%message%",
                                    Objects.requireNonNull(plugin.getLanguage().getString("variables.error.execute_failure"))
                                            .replaceAll("(?i)\\{feature}", "MYSQL tables")), e);
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Failed to use MYSQL, is it configured correctly?");
            }
        }
    }

    public boolean status(UUID ID) {
        return plugin.getConfig().getBoolean("settings.filtering.force") || plugin.getConfig().getBoolean("users." + ID + ".enabled");
    }

    String random() {
        return UUID.randomUUID().toString();
    }

    public List<String> getBoth() {
        List<String> array = new ArrayList<>();
        array.addAll(regex);
        array.addAll(globalRegex);
        return array;
    }

    public String escape(String str) {
        return Pattern.compile("[{}()\\[\\].+*?\"'^$\\\\|]").matcher(str).replaceAll("\\$0");
    }

    public String clean(String string, boolean strip, boolean log, List<String> array, Types.Filters type) {
        if (array.isEmpty())
            return string;
        String replacement = plugin.getConfig().getString("settings.filtering.replacement");
        if (string != null) {
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
                    if (Stream.concat(getWhitelist().stream(), json.stream()).distinct().collect(Collectors.toList()).stream().anyMatch(str::equalsIgnoreCase)) {
                        String r;
                        if (!whitelist.containsKey(str)) {
                            r = random();
                            while (!isclean(r, getBoth())) {
                                debug("UUID value (" + r + ") for whitelisting is unclean and has been re-generated.");
                                r = random();
                            }
                        } else {
                            r = whitelist.get(str);
                        }
                        if (!whitelist.containsKey(str)) {
                            whitelist.put(str, r);
                        }
                        string = Pattern.compile("(\\b" + str + "\\b)").matcher(string).replaceAll(r);
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
                    try {
                        if (plugin.getConfig().getBoolean("settings.discordSRV.spoilers.enabled", false))
                            r = Objects.requireNonNull(plugin.getConfig().getString("settings.discordSRV.spoilers.template", "||{swear}||"))
                                    .replaceAll("(?i)\\{swear}|(?i)%swear%", (plugin.getConfig().getBoolean("settings.discordSRV.escape special chars.escape swears", true) ? matcher.group(0).replaceAll("\\*", "\\\\*")
                                            .replaceAll("_", "\\_")
                                            .replaceAll("\\|", "\\\\|")
                                            .replaceAll("~", "\\~")
                                            .replaceAll("`", "\\\\`") : matcher.group(0)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                r = r.replaceAll("\\*", "\\\\*")
                        .replaceAll("_", "\\_")
                        .replaceAll("\\|", "\\\\|")
                        .replaceAll("~", "\\~")
                        .replaceAll("`", "\\\\`");
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
                string = Pattern.compile(str.getValue()).matcher(string).replaceAll(str.getKey());
            }
        }
        return string;
    }

    public List<String> getGlobalRegex() {
        return this.globalRegex;
    }

    private void setGlobalRegex(List<String> collect) {
        this.globalRegex = collect;
    }

    public boolean isclean(String string, List<String> array) {
        if (array.isEmpty())
            return true;
        string = string.replaceAll("[^\\p{L}0-9 ]+", " ").trim();
        reloadPattern();
        if (plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
            for (String strs : string.split(" ")) {
                if (Stream.concat(getWhitelist().stream(), json.stream()).distinct().collect(Collectors.toList()).stream().anyMatch(string::equalsIgnoreCase)) {
                    string = string.replaceAll("(\\b" + strs + "\\b)", "");
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
        return !Pattern.compile(String.join("|", array), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS).matcher(string).find();
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
            setTable("users");
            boolean exists = false;
            try {
                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.exists);
                ps.setString(1, ID.toString());
                if (ps.executeQuery().next()) {
                    exists = true;
                }
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                exists = false;
            }
            if (!exists) {
                value = plugin.getConfig().getBoolean("settings.filtering.default");
                try (
                        PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.insert)) {
                    ps.setString(1, ID.toString());
                    ps.setString(2, "placeholder");
                    ps.setBoolean(3, value);
                    ps.setBoolean(4, value);
                    ps.execute();
                    ps.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            } else {
                boolean result = false;
                try (
                        PreparedStatement ps = connection.prepareStatement("SELECT status FROM users WHERE uuid=?")) {
                    ps.setString(1, String.valueOf(ID));
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result = rs.getBoolean("status");
                    }
                    ps.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    result = plugin.getConfig().getBoolean("settings.filtering.default");
                }
                value = !result;
                try (
                        PreparedStatement ps = connection.prepareStatement("UPDATE users SET status=? WHERE uuid=?")) {
                    ps.setBoolean(1, value);
                    ps.setString(2, String.valueOf(ID));
                    ps.execute();
                    ps.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            plugin.getConfig().set("users." + ID + ".enabled", value);
            plugin.saveConfig();
        } else if (!plugin.getConfig().isSet("users." + ID + ".enabled")) { // If enabled value doesn't exist, set to default value
            value = plugin.getConfig().getBoolean("settings.filtering.default");
            plugin.getConfig().set("users." + ID + ".enabled", value);
        }
        if (value == null) {
            value = plugin.getConfig().getBoolean("users." + ID + ".enabled");
            plugin.getConfig().set("users." + ID + ".enabled", !value);
            value = !value;
        }
        plugin.saveConfig();
        Bukkit.getScheduler().runTask(plugin, () -> signCheck(ID));
        return value;
    }

    public void reload() {
        if (supported("mysql")) {
            ArrayList<String> swears = new ArrayList<>();
            ArrayList<String> whitelist = new ArrayList<>();
            ArrayList<String> global = new ArrayList<>();
            setTable("swears");
            setTable("users");
            setTable("whitelist");
            setTable("global");
            try (Connection connection = connector.getConnection()) {
                ResultSet rs1 = connection.prepareStatement("SELECT * FROM swears;").executeQuery();
                while (rs1.next()) {
                    swears.add(rs1.getString("word"));
                }
                rs1.close();
                ResultSet rs2 = connection.prepareStatement("SELECT * FROM users;").executeQuery();
                while (rs2.next()) {
                    String ID = rs2.getString("uuid");
                    String name;
                    if (!(rs2.getString("name") == null)) {
                        name = rs2.getString("name");
                    } else {
                        name = "undefined";
                    }
                    boolean status = rs2.getBoolean("status");
                    plugin.getConfig().set("users." + ID + ".enabled", status);
                    plugin.getConfig().set("users." + ID + ".playername", name);
                    plugin.saveConfig();
                }
                rs2.close();
                ResultSet rs3 = connection.prepareStatement("SELECT * FROM whitelist;").executeQuery();
                while (rs3.next()) {
                    whitelist.add(rs3.getString("word"));
                }
                rs3.close();
                ResultSet rs4 = connection.prepareStatement("SELECT * FROM global;").executeQuery();
                while (rs4.next()) {
                    global.add(rs4.getString("word"));
                }
                rs4.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            FileConfiguration local = plugin.getFile("swears");
            local.set("swears", swears);
            plugin.saveFile(local, "swears");
            setSwears(swears);

            local = plugin.getFile("whitelist");
            local.set("whitelist", whitelist);
            plugin.saveFile(local, "whitelist");
            setWhitelist(whitelist);

            local = plugin.getFile("global");
            local.set("global", global);
            plugin.saveFile(local, "global");
            setGlobal(global);
        }
        reloadPattern();
    }

    public void debug(String str) {
        String message = prepare(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.debug").replaceAll("(?i)\\{message}|(?i)%message%", str));
        if (plugin.getConfig().getBoolean("settings.debug")) {
            send(Bukkit.getConsoleSender(), message);
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "/logs/debug.txt");
            File dir = new File(plugin.getDataFolder(), "logs");
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

    public void reloadPattern() {
        FileConfiguration local = plugin.getFile("whitelist");
        if ((getWhitelist().size() != local.getStringList("whitelist").size())) {
            debug("Whitelist doesn't equal local parameters, filling variables.");
            local = plugin.getFile("whitelist");
            setWhitelist(local.getStringList("whitelist"));
            if (getWhitelist().isEmpty()) {
                send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure")
                        .replaceAll("(?i)\\{message}|(?i)%message%", "PATH `whitelist` is empty in config, please fix this ASAP; Using `class, hello` as placeholders"));
                setWhitelist(Arrays.asList("class", "hello"));
            }
        }
        local = plugin.getFile("global");
        if ((getGlobal().size() != local.getStringList("global").size()) || getGlobalRegex().isEmpty()) {
            debug("globalSwears doesn't equal config parameters or regex is empty, filling variables.");
            local = plugin.getFile("global");
            List<String> s = local.getStringList("global");
            s.removeAll(json);
            setGlobal(s);
            globalRegex.clear();
            List<String> duh = new ArrayList<>();
            for (String str : s) {
                StringBuilder omg = new StringBuilder();
                int length = str.length();
                for (String str2 : str.split("")) {
                    length = length - 1;
                    //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+) = fuck
                    str2 = Pattern.quote(str2);
                    if (length <= 0) { // length is the end
                        omg.append("(").append(str2).append("+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+").append(str2).append("+)");
                    } else if (length == str.length() - 1) { // length is the beginning
                        omg.append("(").append(str2).append("+|").append(str2).append("+[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+)");
                    } else { // length is somewhere inbetween
                        omg.append("(").append(str2).append("+\\s*+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+").append(str2).append("+)");
                    }
                }
                duh.add(omg.toString());
            }
            setGlobalRegex(duh.stream().sorted((s1, s2) -> s2.length() - s1.length())
                    .collect(Collectors.toList()));
            local.set("global", s);
            plugin.saveFile(local, "global");
        }
        local = plugin.getFile("swears");
        if ((getSwears().size() != local.getStringList("swears").size()) || getRegex().isEmpty()) {
            debug("localSwears doesn't equal config parameters or regex is empty, filling variables.");
            List<String> s = local.getStringList("swears");
            s.removeAll(json);
            local.set("swears", s);
            plugin.saveFile(local, "swears");
            setSwears(s);
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
                        if (length <= 0) { // length is the end
                            omg.append("(").append(str2).append("+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+").append(str2).append("+)");
                        } else if (length == str.length() - 1) { // length is the beginning
                            omg.append("(").append(str2).append("+|").append(str2).append("+[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+)");
                        } else { // length is somewhere inbetween
                            omg.append("(").append(str2).append("+\\s*+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+").append(str2).append("+)");
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
                                omg2.append("(").append(str3).append("+\\s*+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+").append(str3).append("+)");
                            } else {
                                omg2.append("(").append(str3).append("+|[").append(Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replace("\"", "\\\""))).append("]+").append(str3).append("+)");
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
        message = ChatColor.translateAlternateColorCodes('&', message.replaceAll("(?i)\\{prefix}|(?i)%prefix%", Objects.requireNonNull(plugin.getLanguage().getString("variables.prefix")))
                .replaceAll("(?i)\\{command}|(?i)%command%", "mcsf")
                .replaceAll("(?i)\\{player}|(?i)%player%", player.getName())
                .replaceAll("(?i)\\{current}|(?i)%current%", plugin.getDescription().getVersion())
                .replaceAll("(?i)\\{version}|(?i)%version%", String.valueOf(getVersion()))
                .replaceAll("(?i)\\{serverversion}|(?i)%serverversion%", plugin.getServer().getVersion())
                .replaceAll("(?i)\\{swearcount}|(?i)%swearcount", Integer.toString(plugin.getConfig().getInt("swearcount")))
                .replaceAll("(?i)\\{wordcount}|(?i)%wordcount%", Integer.toString(plugin.getFile("swears").getStringList("swears").size())));
        return supported("hex") ? color(message) : message;
    }

    public void send(CommandSender sender, String message) {
        if (message.isEmpty())
            return;
        message = prepare(sender, message);
        if (supported("hex"))
            sender.spigot().sendMessage(new TextComponent(message));
        else
            sender.sendMessage(message);
    }
}