package io.github.jochyoua.mychristianswearfilter.shared;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.HikariCP;
import me.clip.placeholderapi.PlaceholderAPI;
import net.jodah.expiringmap.ExpiringMap;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Manager {
    public Map<UUID, Integer> userFlags = new HashMap<>();
    MCSF plugin;
    DatabaseConnector connector;

    List<String> regex = new ArrayList<>();

    List<String> globalRegex = new ArrayList<>();

    List<String> localSwears = new ArrayList<>();

    List<String> localWhitelist = new ArrayList<>();

    List<String> globalSwears = new ArrayList<>();


    FileConfiguration sql;

    ExpiringMap<UUID, UUID> cooldowns;

    Connection connection;

    Connection userConnection;

    Map<String, String> whitelistMap = new HashMap<>();

    /**
     * Instantiates a new Manager class
     *
     * @param plugin    the providing plugin
     * @param connector the mysql connector
     */
    public Manager(MCSF plugin, DatabaseConnector connector) {
        this.plugin = plugin;
        this.connector = connector;
        sql = Manager.FileManager.getFile(plugin, "sql");
        reloadUserData();
        if (plugin.getHikariCP().isEnabled())
            try {
                this.connection = connector.getConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        cooldowns = ExpiringMap.builder()
                .expiration(plugin.getConfig().getInt("settings.cooldown", 5), TimeUnit.SECONDS)
                .build();
    }

    /**
     * Gets the current version according to spigot
     *
     * @return the version
     */
    public static Double getVersion(String version) {
        try {
            version = ((JSONObject) new JSONParser().parse(new Scanner(new URL("https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=54115").openStream()).nextLine())).get("current_version").toString();
        } catch (ParseException | IOException ignored) {
        }

        return Double.parseDouble(version);
    }

    /**
     * Checks to see if the plugin needs an update,
     *
     * @return the boolean
     */
    public static boolean needsUpdate(String version) {
        return (Double.parseDouble(version) < getVersion(version));
    }

    /**
     * This method colors the string with hex, only works on 1.16.x+
     *
     * @param message the message to be colored
     * @return the colored string
     */
    public static String color(String message) {
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

    /**
     * Distinctly sorts an arraylist
     *
     * @param str the arraylist to be sorted
     * @return the sorted list
     */
    public static List<String> sortArray(List<String> str) {
        return str.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns a random UUID
     *
     * @return the string
     */
    public static String random() {
        return UUID.randomUUID().toString();
    }

    /**
     * Reloads the user sql database
     */
    public void reloadUserData() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "data/users.db";
        if (userConnection == null) {
            try {
                userConnection = DriverManager.getConnection(url);
                plugin.getLogger().info("Connection to the SQLite database has been established!");
            } catch (SQLException throwables) {
                userConnection = null;
                plugin.getLogger().warning(String.format("Unable to connect to the SQLite database!\n%s", throwables.getMessage()));
            }
        }
        try {
            PreparedStatement ps = userConnection.prepareStatement(HikariCP.Query.USERS.create);
            ps.execute();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Closes SQL connections, called when the plugin is disabled.
     */
    public void shutDown() {
        try {
            userConnection.close();
            if (supported("mysql"))
                connector.getConnection().close();
        } catch (SQLException ignored) {
        }
    }

    public void putWhitelistMap(String str1, String str2) {
        whitelistMap.put(str1, str2);
    }

    /**
     * Gets the user cooldowns
     *
     * @return the cooldowns
     */
    public ExpiringMap<UUID, UUID> getCooldowns() {
        return this.cooldowns;
    }

    /**
     * Adds user to the cooldown
     *
     * @param id the playerid
     */
    public void addUser(UUID id) {
        cooldowns.put(id, id);
    }

    /**
     * Gets the local saved swears
     *
     * @return the swears list
     */
    public List<String> getSwears() {
        return this.localSwears;
    }

    public void setSwears(List<String> str) {
        this.localSwears = sortArray(str);
    }

    /**
     * Gets the local whitelist
     *
     * @return the whitelist
     */
    public List<String> getWhitelist() {
        return this.localWhitelist;
    }

    public void setWhitelist(List<String> str) {
        this.localWhitelist = sortArray(str);
    }

    /**
     * Gets the local global list
     *
     * @return the global
     */
    public List<String> getGlobal() {
        return this.globalSwears;
    }

    public void setGlobal(List<String> str) {
        this.globalSwears = sortArray(str);
    }

    /**
     * Gets the local regex for swears
     *
     * @return the regex
     */
    public List<String> getRegex() {
        return this.regex;
    }

    public void setRegex(List<String> str) {
        this.regex = str;
    }

    public Map<String, String> getWhitelistMap() {
        return whitelistMap;
    }

    /**
     * Checks to see if a feature is enabled
     *
     * @param string the feature to be checked
     * @return boolean if the feature is supported
     */
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
                statement = plugin.getConfig().getBoolean("settings.discordSRV.enabled") && (plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null);
                break;
            case "protocollib":
                statement = plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null;
                break;
            case "mysql":
                statement = plugin.getHikariCP().isEnabled();
                break;
            case "placeholderapi":
                statement = (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) && plugin.getConfig().getBoolean("settings.enable placeholder api");
        }
        return statement;
    }

    /**
     * Count rows in a mysql database
     *
     * @param table the table
     * @return the size of the rows
     */
    public int countRows(String table) {
        if (!plugin.getHikariCP().isEnabled())
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

    /**
     * Gets the MYSQL connection
     *
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    public DatabaseConnector getConnector() {
        return connector;
    }

    /**
     * Gets the providing plugin
     *
     * @return the provider
     */
    public MCSF getProvider() {
        return plugin;
    }

    /**
     * Shows help to the user
     *
     * @param sender the sender
     */
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

    /**
     * Sets a table based on the correct information if the table is empty in mysql
     *
     * @param table the table
     */
    public void setTable(String table) {
        if (!plugin.getHikariCP().isEnabled())
            return;
        try {
            switch (table) {
                case "global":
                    if (countRows("global") == 0) {
                        try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.GLOBAL.create)) {
                            ps.execute();
                        }
                        FileConfiguration local = Manager.FileManager.getFile(plugin, "data/global");
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
                    if (countRows("users") == 0) {
                        debug("(MySQL) attempting to insert user data");
                        try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.create)) {
                            ps.execute();
                        }
                        List<String> user_list = new ArrayList<>();
                        StringBuilder query = new StringBuilder("INSERT IGNORE INTO users(uuid, name, status) VALUES ");
                        PreparedStatement userData = userConnection.prepareStatement("SELECT * FROM users");
                        ResultSet rs = userData.executeQuery();
                        while (rs.next()) {
                            UUID ID = UUID.fromString(rs.getString("uuid"));
                            String name = rs.getString("name");
                            boolean status = rs.getBoolean("status");
                            user_list.add(ID.toString());
                            user_list.add(name);
                            user_list.add(String.valueOf(status));
                            query.append("(?,?,?), ");
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
                    if (countRows("swears") == 0) {
                        debug("(MySQL) attempting to insert swear data");
                        try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.SWEARS.create)) {
                            ps.execute();
                        }
                        List<String> s = Manager.FileManager.getFile(plugin, "data/swears").getStringList("swears");
                        StringBuilder query = new StringBuilder("INSERT IGNORE INTO swears(word) VALUES (?)");
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
                    if (countRows("whitelist") == 0) {
                        debug("(MySQL) attempting to insert whitelist data");
                        try (PreparedStatement ps = connection.prepareStatement(HikariCP.Query.WHITELIST.create)) {
                            ps.execute();
                        }
                        FileConfiguration local = Manager.FileManager.getFile(plugin, "data/whitelist");
                        List<String> s = local.getStringList("whitelist");

                        local = Manager.FileManager.getFile(plugin, "data/swears");
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
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Creates table based on the correct data, if reset is true the mysql tables are forcefully reset.
     *
     * @param reset the reset
     * @throws SQLException the sql exception
     */
    public void createTable(boolean reset) throws SQLException {
        if (plugin.getHikariCP().isEnabled()) {
            FileConfiguration local = Manager.FileManager.getFile(plugin, "data/swears");
            if (local.getStringList("swears").isEmpty()) {
                local.set("swears", new String[]{"fuck", "shit"});
                Manager.FileManager.saveFile(plugin, local, "data/swears");
            }
            if (plugin.getHikariCP().isEnabled()) {
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

    /**
     * Filtering but filters twice if server operator wishes for it.
     * <p>
     * This function is essentially a forward for filter(String string, boolean strip, boolean log, List<String> array, Types.Filters type)
     *
     * @param string the string to be cleared
     * @param strip  if the string should be stripped of all chatcolor
     * @param log    if this should count as a logged swear
     * @param array  the array of regex strings to be checked for
     * @param type   the type of filter this is
     * @return the cleaned string
     */
    public String clean(String string, boolean strip, boolean log, List<String> array, Types.Filters type) {
        if (plugin.getConfig().getBoolean("settings.filtering.double filtering") && type != Types.Filters.DISCORD)
            return filter(filter(string, strip, false, array, type), strip, log, array, type);
        return filter(string, strip, log, array, type);
    }

    /**
     * Filters string
     *
     * @param string the string to be cleared
     * @param strip  if the string should be stripped of all chatcolor
     * @param log    if this should count as a logged swear
     * @param array  the array of regex strings to be checked for
     * @param type   the type of filter this is
     * @return the cleaned string
     */
    public String filter(String string, boolean strip, boolean log, List<String> array, Types.Filters type) {
        if (array.isEmpty() || string == null) {
            return string;
        }
        String replacement = plugin.getConfig().getString("settings.filtering.replacement");
        if (plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
            String lstring = string.replaceAll("[^\\p{L}0-9 ]+", " ").trim();
            for (String str : lstring.split(" ")) {
                str = str.replaceAll("[^\\p{L}0-9 ]+", " ").trim();
                if (getWhitelist().stream().distinct().collect(Collectors.toList()).stream().anyMatch(str::equalsIgnoreCase)) {
                    String r = getWhitelistMap().get(str);
                    if (r == null) {
                        r = random();
                        putWhitelistMap(str, r);
                    }
                    string = string.replaceAll("(\\b" + str + "\\b)", r);
                }
            }
        }
        Pattern pattern = Pattern.compile(String.join("|", array), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
        Matcher matcher = pattern.matcher(string);
        StringBuffer out = new StringBuffer();
        int swearcount = plugin.getConfig().getInt("swearcount");
        while (matcher.find()) {
            if (!matcher.group(0).trim().isEmpty()) {
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
                if (replacement == null)
                    return string;
                replacement = ChatColor.translateAlternateColorCodes('&', replacement);
                if (strip) {
                    replacement = ChatColor.stripColor(replacement);
                }
                String r = plugin.getConfig().getBoolean("settings.replace word for word") ? replacement : matcher.group(0).trim().replaceAll("(?s).", replacement);
                if (type.equals(Types.Filters.DISCORD) && plugin.getConfig().getBoolean("settings.discordSRV.spoilers.enabled")) {
                    if (plugin.getConfig().getBoolean("settings.discordSRV.escape special chars.escape entire message", false)) {
                        string = string.replaceAll("\\*", "\\\\*")
                                .replaceAll("_", "\\_")
                                .replaceAll("\\|", "\\\\|")
                                .replaceAll("~", "\\~")
                                .replaceAll("`", "\\\\`");
                    }
                    r = Objects.requireNonNull(plugin.getConfig().getString("settings.discordSRV.spoilers.template", "||{swear}||"));
                    r = r.replaceAll("(?i)\\{swear}|(?i)%swear%", (plugin.getConfig().getBoolean("settings.discordSRV.escape special chars.escape swears", true) ? matcher.group(0).trim().replaceAll("\\*", "\\\\*")
                            .replaceAll("_", "\\_")
                            .replaceAll("\\|", "\\\\|")
                            .replaceAll("~", "\\~")
                            .replaceAll("`", "\\\\`") : matcher.group(0).trim()));
                }
                r = r.replaceAll("\\*", "\\\\*")
                        .replaceAll("_", "\\_")
                        .replaceAll("\\|", "\\\\|")
                        .replaceAll("~", "\\~")
                        .replaceAll("`", "\\\\`");
                matcher.appendReplacement(out, r);
                swearcount++;
            }
        }
        if (log) {
            plugin.getConfig().set("swearcount", swearcount);
            plugin.saveConfig();
        }
        matcher.appendTail(out);
        string = out.toString();
        for (Map.Entry<String, String> str : getWhitelistMap().entrySet()) {
            string = string.replaceAll(str.getValue(), str.getKey());
        }
        return string;
    }

    /**
     * Gets global regex.
     *
     * @return the global regex
     */
    public List<String> getGlobalRegex() {
        return this.globalRegex;
    }

    private void setGlobalRegex(List<String> collect) {
        this.globalRegex = collect;
    }

    /**
     * Checks to see if the string is clean
     *
     * @param string the suspected string
     * @param array  the array of strings to check for
     * @return if the string is clean or not
     */
    public boolean isclean(String string, List<String> array) {
        if (array.isEmpty())
            return true;
        string = string.replaceAll("[^\\p{L}0-9 ]+", " ").trim();
        if (plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
            for (String strs : string.split(" ")) {
                if (getWhitelist().stream().distinct().collect(Collectors.toList()).stream().anyMatch(string::equalsIgnoreCase)) {
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

    /**
     * Gets the current users in the database
     *
     * @param name if true, only names are returned. If false, only UUIDs are returned.
     * @return the list of user's name or uuid
     */
    public List<String> getUsers(boolean name) {
        List<String> users = new ArrayList<>();
        try {
            PreparedStatement ps = userConnection.prepareStatement("SELECT * FROM users");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (name)
                    users.add(rs.getString("name"));
                else
                    users.add(rs.getString("uuid"));
            }
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return users;
    }

    /**
     * Gets user SQL connection
     *
     * @return the user connection
     */
    public Connection getUserConnection() {
        return userConnection;
    }

    /**
     * Reloads the MYSQL Data
     */
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
                    User user = new User(this, UUID.fromString(ID));
                    String name;
                    if (!(rs2.getString("name") == null)) {
                        name = rs2.getString("name");
                    } else {
                        name = "undefined";
                    }
                    boolean status = rs2.getBoolean("status");
                    if (user.exists()) {
                        user.set(status);
                        user.playerName(name);
                    } else {
                        user.create(name, status);
                    }
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
            FileConfiguration local = Manager.FileManager.getFile(plugin, "data/swears");
            local.set("swears", swears);
            Manager.FileManager.saveFile(plugin, local, "data/swears");
            setSwears(swears);
            local = Manager.FileManager.getFile(plugin, "data/whitelist");
            local.set("whitelist", whitelist);
            Manager.FileManager.saveFile(plugin, local, "data/whitelist");
            setWhitelist(whitelist);
            local = Manager.FileManager.getFile(plugin, "data/global");
            local.set("global", global);
            Manager.FileManager.saveFile(plugin, local, "data/global");
            setGlobal(global);
        }
        reloadPattern(Types.Filters.OTHER);
    }

    /**
     * Debugs string, if settings.debug is false nothing is outputed to console but are still saved in /logs/debug.txt
     *
     * @param str the str
     */
    public void debug(String str) {
        String message = prepare(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.debug").replaceAll("(?i)\\{message}|(?i)%message%", str));
        if (plugin.getConfig().getBoolean("settings.debug")) {
            send(Bukkit.getConsoleSender(), message);
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "/logs/debug.txt");
            File dir = new File(plugin.getDataFolder(), "logs");
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    plugin.getLogger().info("Failed to create directory " + dir.getParent());
                }
            }
            if (!file.exists()) {
                try {
                    if (!file.createNewFile()) {
                        plugin.getLogger().info("Failed to create file " + file.getName());
                    }
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

    /**
     * Reload the current patterns for Global, Swears and whitelist.
     */
    public List<String> reloadPattern(Types.Filters type) {
        List<String> value = new ArrayList<>();
        FileConfiguration local = Manager.FileManager.getFile(plugin, "data/whitelist");
        if ((local.getStringList("whitelist").size() != plugin.getLocal("whitelist")) && plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
            debug("Whitelist doesn't equal local parameters, filling variables.");
            setWhitelist(local.getStringList("whitelist"));
            if (local.getStringList("whitelist").isEmpty()) {
                send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure")
                        .replaceAll("(?i)\\{message}|(?i)%message%", "File /data/whitelist.yml is empty, please fix this ASAP; Using `class, hello` as placeholders"));
                setWhitelist(Arrays.asList("class", "hello"));
            }
            plugin.setLocal("whitelist", local.getStringList("whitelist").size());
        }
        local = Manager.FileManager.getFile(plugin, "data/global");
        if ((local.getStringList("global").size() != plugin.getLocal("global")) || (getGlobalRegex().isEmpty() && !local.getStringList("global").isEmpty())) {
            debug("globalSwears doesn't equal config parameters or regex is empty, filling variables.");
            List<String> s = local.getStringList("global");
            setGlobal(s);
            List<String> duh = generateRegex(s);
            setGlobalRegex(duh.stream().sorted((s1, s2) -> s2.length() - s1.length())
                    .collect(Collectors.toList()));
            local.set("global", s);
            Manager.FileManager.saveFile(plugin, local, "data/global");
            plugin.setLocal("global", s.size());
        }
        local = Manager.FileManager.getFile(plugin, "data/swears");
        if ((local.getStringList("swears").size() != plugin.getLocal("swears")) || (getRegex().isEmpty() && !local.getStringList("swears").isEmpty())) {
            debug("localSwears doesn't equal config parameters or regex is empty, filling variables.");
            List<String> s = local.getStringList("swears");
            local.set("swears", s);
            Manager.FileManager.saveFile(plugin, local, "data/swears");
            setSwears(s);
            if (s.isEmpty()) {
                send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure")
                        .replaceAll("(?i)\\{message}|(?i)%message%", "File /data/swears.yml, please fix this ASAP; Using `fuck, shit` as placeholders"));
                setSwears(Arrays.asList("fuck", "shit"));
            }
            regex.clear();
            List<String> duh = generateRegex(s);
            setRegex(duh.stream().sorted((s1, s2) -> s2.length() - s1.length())
                    .collect(Collectors.toList()));
            plugin.setLocal("swears", s.size());
        }

        if (type == Types.Filters.BOTH) {
            value.addAll(getGlobalRegex());
            value.addAll(getRegex());
        } else if (type == Types.Filters.GLOBAL) {
            value.addAll(getGlobalRegex());
        }
        return value;
    }

    public List<String> generateRegex(List<String> s) {
        List<String> duh = new ArrayList<>();
        for (String str : s) {
            if (str.toLowerCase().startsWith("regex:")) {
                try {
                    duh.add(Pattern.compile(str.replaceAll("regex:", "")).pattern());
                } catch (Exception ignored) {
                }
                continue;
            }
            if (!plugin.getConfig().getBoolean("settings.filtering.ignore special characters.enabled")) {
                str = str.replaceAll("\\s+", "");
                StringBuilder omg = new StringBuilder();
                for (String str2 : str.split("")) {
                    //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+)
                    omg.append(str2).append("+\\s*");
                }
                duh.add(omg.substring(0, omg.toString().length() - 4) + "+");
                if (plugin.getConfig().getBoolean("settings.filtering.filter reverse versions of swears", true)) {
                    StringBuilder omg2 = new StringBuilder();
                    for (String str3 : new StringBuilder(str).reverse().toString().split("")) {
                        omg2.append(str3).append("+\\s*");
                    }
                    duh.add(omg2.substring(0, omg2.toString().length() - 4) + "+");
                }
            } else {
                StringBuilder omg = new StringBuilder();
                String quote = Pattern.quote(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-").replaceAll("\"", "\\\""));
                int length = str.length();
                for (String str2 : str.split("")) {
                    length = length - 1;
                    str2 = Pattern.quote(str2);
                    //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+)                                                                               = fuck
                    //(f+\s*+|f+[!@#$%^&*()_+-"]+\s*+)(u+\s*+|[!@#$%^&*()_+-"]+u+\s*+)(c+\s*+|[!@#$%^&*()_+-"]+c+\s*+)(k+\s*+|[!@#$%^&*()_+-"]+k+\s*+)   = fuck with special chars
                    if (length <= 0) { // length is the end
                        omg.append("(").append(str2).append("+|([").append(quote).append("]|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+").append(str2).append(")");
                    } else if (length == str.length() - 1) { // length is the beginning
                        omg.append("(?i)(").append(str2).append("+\\s*+|").append(str2).append("+\\s*+([").append(quote).append("]+\\s*+|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+)");
                    } else { // length is somewhere inbetween
                        omg.append("(").append(str2).append("+\\s*+|([").append(quote).append("]+\\s*+|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+").append(str2).append("+\\s*+)");
                    }
                }
                duh.add(omg.toString().trim());
                if (plugin.getConfig().getBoolean("settings.filtering.filter reverse versions of swears", true)) {
                    StringBuilder omg2 = new StringBuilder();
                    String str2 = new StringBuilder(str).reverse().toString();
                    str2 = Pattern.quote(str2);
                    int length2 = str2.length();
                    for (String str3 : str2.split("")) {
                        length2 = length2 - 1;
                        str3 = Pattern.quote(str3);
                        //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+)                                                                               = fuck
                        //(f+\s*+|f+[!@#$%^&*()_+-"]+\s*+)(u+\s*+|[!@#$%^&*()_+-"]+u+\s*+)(c+\s*+|[!@#$%^&*()_+-"]+c+\s*+)(k+\s*+|[!@#$%^&*()_+-"]+k+\s*+)   = fuck with special chars
                        if (length2 <= 0) { // length is the end
                            omg2.append("(").append(str3).append("+|([").append(quote).append("]|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+").append(str3).append(")");
                        } else if (length2 == str2.length() - 1) { // length is the beginning
                            omg2.append("(?i)(").append(str3).append("+\\s*+|").append(str3).append("+\\s*+([").append(quote).append("]+\\s*+|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+)");
                        } else { // length is somewhere inbetween
                            omg2.append("(").append(str3).append("+\\s*+|([").append(quote).append("]+\\s*+|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+").append(str3).append("+\\s*+)");
                        }
                    }
                    duh.add(omg2.toString().trim());
                }
            }
        }
        return duh;
    }

    /**
     * Prepares string to be sent to the client
     *
     * @param sender  the sender
     * @param message the message to be prepared
     * @return the prepared string
     */
    public String prepare(CommandSender sender, String message) {
        message = ChatColor.translateAlternateColorCodes('&', message.replaceAll("(?i)\\{prefix}|(?i)%prefix%", Objects.requireNonNull(plugin.getLanguage().getString("variables.prefix")))
                .replaceAll("(?i)\\{command}|(?i)%command%", "mcsf")
                .replaceAll("(?i)\\{player}|(?i)%player%", sender.getName())
                .replaceAll("(?i)\\{current}|(?i)%current%", plugin.getDescription().getVersion())
                .replaceAll("(?i)\\{version}|(?i)%version%", String.valueOf(getVersion(plugin.getDescription().getVersion())))
                .replaceAll("(?i)\\{serverversion}|(?i)%serverversion%", plugin.getServer().getVersion())
                .replaceAll("(?i)\\{swearcount}|(?i)%swearcount", Integer.toString(plugin.getConfig().getInt("swearcount")))
                .replaceAll("(?i)\\{wordcount}|(?i)%wordcount%", Integer.toString(Manager.FileManager.getFile(plugin, "data/swears").getStringList("swears").size())));
        if (supported("PlaceholderAPI") && sender instanceof Player)
            message = PlaceholderAPI.setPlaceholders((Player) sender, message);
        return supported("hex") ? color(message) : message;
    }

    /**
     * Send message to client
     *
     * @param sender  the sender
     * @param message the message
     */
    public void send(CommandSender sender, String message) {
        if (message.isEmpty())
            return;
        message = prepare(sender, message);
        if (supported("hex"))
            sender.spigot().sendMessage(new TextComponent(message));
        else
            sender.sendMessage(message);
    }

    public static class JSONUtil {
        /**
         * @author DarkSeraphim
         **/
        private static final StringBuilder JSON_BUILDER = new StringBuilder("{\"text\":\"\",\"extra\":[");

        private static final int RETAIN = "{\"text\":\"\",\"extra\":[".length();
        private static final StringBuilder STYLE = new StringBuilder();

        public static String toJSON(String message) {
            if (message == null || message.isEmpty())
                return null;
            message = JSONObject.escape(message);
            if (JSON_BUILDER.length() > RETAIN)
                JSON_BUILDER.delete(RETAIN, JSON_BUILDER.length());
            String[] parts = message.split(Character.toString(ChatColor.COLOR_CHAR));
            boolean first = true;
            String colour = null;
            String format = null;
            boolean ignoreFirst = !parts[0].isEmpty() && ChatColor.getByChar(parts[0].charAt(0)) != null;
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }

                String newStyle = null;
                if (!ignoreFirst) {
                    newStyle = getStyle(part.charAt(0));
                } else {
                    ignoreFirst = false;
                }

                if (newStyle != null) {
                    part = part.substring(1);
                    if (newStyle.startsWith("\"c"))
                        colour = newStyle;
                    else
                        format = newStyle;
                }
                if (!part.isEmpty()) {
                    if (first)
                        first = false;
                    else {
                        JSON_BUILDER.append(",");
                    }
                    JSON_BUILDER.append("{");
                    if (colour != null) {
                        JSON_BUILDER.append(colour);
                        colour = null;
                    }
                    if (format != null) {
                        JSON_BUILDER.append(format);
                        format = null;
                    }
                    JSON_BUILDER.append(String.format("\"text\":\"%s\"", part));
                    JSON_BUILDER.append("}");
                }
            }
            return JSON_BUILDER.append("]}").toString();
        }

        private static String getStyle(char colour) {
            if (STYLE.length() > 0)
                STYLE.delete(0, STYLE.length());
            switch (colour) {
                case 'k':
                    return "\"obfuscated\": true,";
                case 'l':
                    return "\"bold\": true,";
                case 'm':
                    return "\"strikethrough\": true,";
                case 'n':
                    return "\"underlined\": true,";
                case 'o':
                    return "\"italic\": true,";
                case 'r':
                    return "\"reset\": true,";
                default:
                    break;
            }
            ChatColor cc = ChatColor.getByChar(colour);
            if (cc == null)
                return null;
            return STYLE.append("\"color\":\"").append(cc.name().toLowerCase()).append("\",").toString();
        }
    }

    public static class FileManager {
        public static YamlConfiguration getLanguage(MCSF plugin) {
            String language = Types.Languages.getLanguage(plugin);
            Settings settings = new Settings();
            settings.setSetting("reportMissingOptions", false);
            ConfigAPI lang = new ConfigAPI("locales/" + language + ".yml", settings, plugin);
            lang.copyDefConfigIfNeeded();
            YamlConfiguration conf = lang.getLiveConfiguration();
            Map<String, Object> missingOptions = lang.getMissingOptions(conf, lang.getDefaultConfiguration());
            if (!missingOptions.isEmpty()) {
                for (Map.Entry<String, Object> missing : missingOptions.entrySet()) {
                    conf.set(missing.getKey(), missing.getValue());
                }
            }
            return conf;
        }

        public static YamlConfiguration getFile(MCSF plugin, String fileName) {
            ConfigAPI config;
            Settings settings = new Settings();
            settings.setSetting("reportMissingOptions", false);
            config = new ConfigAPI(fileName + ".yml", settings, plugin);
            config.copyDefConfigIfNeeded();
            return config.getLiveConfiguration();
        }

        public static void saveFile(MCSF plugin, FileConfiguration file, String fileName) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    file.save(new File(plugin.getDataFolder(), fileName + ".yml"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        public static void relocateData(MCSF plugin) {
            FileConfiguration sql = Manager.FileManager.getFile(plugin, "sql");
            if (plugin.getConfig().isSet("mysql")) {
                plugin.getLogger().info("(MYSQL) Setting mysql path into `data/sql.yml`");
                for (String key : plugin.getConfig().getConfigurationSection("mysql").getKeys(false)) {
                    sql.set("mysql." + key, plugin.getConfig().get("mysql." + key));
                }
                Manager.FileManager.saveFile(plugin, sql, "sql");
                plugin.getConfig().set("mysql", null);
                plugin.saveConfig();
            }
            FileConfiguration local = Manager.FileManager.getFile(plugin, "data/swears");
            if (!plugin.getConfig().getStringList("swears").isEmpty()) {
                plugin.getLogger().info("(CONFIG) Setting path `swears` into `data/swears.yml`");
                if (local.isSet("swears")) {
                    if (!local.getStringList("swears").isEmpty()) {
                        Set<String> local1 = new HashSet<>(local.getStringList("swears"));
                        Set<String> local2 = new HashSet<>(plugin.getConfig().getStringList("swears"));
                        local1.addAll(local2);
                        local.set("swears", local1);
                        Manager.FileManager.saveFile(plugin, local, "data/swears");
                        plugin.getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/swears.yml` and removed path `swears`");
                    }
                }
                plugin.getConfig().set("swears", null);
                plugin.saveConfig();
            }
            local = Manager.FileManager.getFile(plugin, "data/whitelist");
            if (!plugin.getConfig().getStringList("whitelist").isEmpty()) {
                plugin.getLogger().info("(CONFIG) Setting path `global` into `data/whitelist.yml`");
                if (local.isSet("whitelist")) {
                    if (!local.getStringList("whitelist").isEmpty()) {
                        Set<String> local1 = new HashSet<>(local.getStringList("whitelist"));
                        Set<String> local2 = new HashSet<>(plugin.getConfig().getStringList("whitelist"));
                        local1.addAll(local2);
                        local.set("whitelist", local1);
                        Manager.FileManager.saveFile(plugin, local, "data/whitelist");
                        plugin.getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/whitelist.yml` and removed path `whitelist`");
                    }
                }
                plugin.getConfig().set("whitelist", null);
                plugin.saveConfig();
            }
            local = Manager.FileManager.getFile(plugin, "data/global");
            if (!plugin.getConfig().getStringList("global").isEmpty()) {
                plugin.getLogger().info("(CONFIG) Setting path `global` into `data/global.yml`");
                if (local.isSet("global")) {
                    if (!local.getStringList("global").isEmpty()) {
                        Set<String> local1 = new HashSet<>(local.getStringList("global"));
                        Set<String> local2 = new HashSet<>(plugin.getConfig().getStringList("global"));
                        local1.addAll(local2);
                        local.set("global", local1);
                        Manager.FileManager.saveFile(plugin, local, "data/global");
                        plugin.getLogger().info("(CONFIG) Set " + local1.size() + " entries into `data/global.yml` and removed path `global`");
                    }
                }
                plugin.getConfig().set("global", null);
                plugin.saveConfig();
            }
        }
    }
}