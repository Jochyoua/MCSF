package io.github.jochyoua.mychristianswearfilter.shared;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.ConfigAPI;
import io.github.jochyoua.mychristianswearfilter.dependencies.configapi.Settings;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.HikariCP;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.jodah.expiringmap.ExpiringMap;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.jochyoua.mychristianswearfilter.shared.Types.Filters.RELOAD;


@Setter
@Getter
public class Manager {
    private static FileHandler fileHandler;

    static {
        try {
            fileHandler = new FileHandler(MCSF.getInstance().getDataFolder()
                    + File.separator + "logs" + File.separator + "debug.log");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    Pattern swearPattern;
    Pattern globalPattern;
    Pattern bothPattern;

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
     * Gets the current version according to Github
     *
     * @return the version
     */
    public static double getVersion() {
        double version = 0.0;
        try (Scanner scanner = new Scanner(new URL("https://api.github.com/repos/Jochyoua/MCSF/releases/latest").openStream())) {
            version = new Gson().fromJson(scanner.nextLine(), JsonElement.class).getAsJsonObject().get("tag_name").getAsDouble();
            scanner.close();
        } catch (IOException e) {
            try (Scanner scanner = new Scanner(new URL("https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=54115").openStream())) {
                version = new Gson().fromJson(scanner.nextLine(), JsonElement.class).getAsJsonObject().get("current_version").getAsDouble();
                scanner.close();
            } catch (IOException ignored) {
            }
        }
        return version;
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
     * Debugs string, if settings.debug is false nothing is shown to console but are still saved in /logs/debug.log
     *
     * @param str           the string to be debugged
     * @param consoleOutput should the output be shown to console?
     * @param level         what level should this debug be? > Only applies to file debugging
     */
    public static void debug(String str, boolean consoleOutput, Level level) {
        StackTraceElement st = Thread.currentThread().getStackTrace()[2];
        Logger debug = Logger.getLogger(st.getClassName() + ":" + st.getLineNumber() + " " + Thread.currentThread().getStackTrace()[2].getMethodName());
        for (Handler handler : debug.getHandlers()) {
            debug.removeHandler(handler);
        }
        try {
            fileHandler.setFormatter(new SimpleFormatter() {
                final String format = "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%n";

                public String format(LogRecord record) {
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(record.getMillis()), ZoneId.systemDefault());
                    String source;
                    source = record.getLoggerName();
                    String message = formatMessage(record);
                    return String.format(format,
                            zdt,
                            source,
                            record.getLoggerName(),
                            record.getLevel().getLocalizedName(),
                            message);
                }
            });

            debug.addHandler(fileHandler);
            debug.setUseParentHandlers(false);
            debug.setLevel(level);

            debug.log(level, str + "\n");

            if (consoleOutput) {
                // Output to console
                Bukkit.getLogger().log(level, "[MCSF Debug] " + str);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
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
                debug("Connection to the SQLite database has been established!", true, Level.INFO);
            } catch (SQLException throwables) {
                userConnection = null;
                debug(String.format("Unable to connect to the SQLite database!\n%s", throwables.getMessage()), true, Level.WARNING);
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
            fileHandler.close();
            if (supported("mysql"))
                connector.getConnection().close();
        } catch (SQLException ignored) {
        }
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
            case "discordsrv":
                statement = plugin.getConfig().getBoolean("settings.discordSRV.enabled") && (plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null);
                break;
            case "protocollib":
                statement = plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null;
                break;
            case "mysql":
                statement = Manager.FileManager.getFile(plugin, "sql").getBoolean("mysql.enabled") && plugin.getHikariCP().isEnabled();
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
        int i = 0;
        if (plugin.getHikariCP().isEnabled()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + table);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    i++;
                ps.close();
                rs.close();
            } catch (Exception ignored) {
            }
        }
        return i;
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
                            debug("Cannot set global data because it is empty!", true, Level.WARNING);
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
                        debug("(MySQL) attempting to insert user data", plugin.getDebug(), Level.INFO);
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
                        rs.close();
                        if (user_list.isEmpty()) {
                            debug("Cannot set user data because it is empty!", true, Level.WARNING);
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
                        debug("(MySQL) attempting to insert swear data", plugin.getDebug(), Level.INFO);
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
                        debug("(MySQL) attempting to insert whitelist data", plugin.getDebug(), Level.INFO);
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
                    debug("No correct database was selected.", plugin.getDebug(), Level.INFO);
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
                    debug("Failed to set tables!: " + e.getMessage(), false, Level.WARNING);
                }
            } else {
                debug("Failed to use MYSQL, is it configured correctly?", true, Level.WARNING);
            }
        }
    }

    /**
     * Filtering but filters twice if server operator wishes for it.
     * <p>
     * This function is essentially a forward for filter(String string, boolean strip, boolean log, List<String> array, Types.Filters type)
     *
     * @param string  the string to be cleared
     * @param strip   if the string should be stripped of all chat color
     * @param pattern the pattern to filter by
     * @param type    the type of filter this is
     * @return the cleaned string
     */
    public String clean(String string, boolean strip, Pattern pattern, Types.Filters type) {
        if (plugin.getConfig().getBoolean("settings.filtering.double filtering") && type != Types.Filters.DISCORD)
            return filter(filter(string, strip, pattern, type), strip, pattern, type);
        return filter(string, strip, pattern, type);
    }

    /**
     * Filters string
     *
     * @param string the string to be cleared
     * @param strip  if the string should be stripped of all chat color
     * @param type   the type of filter this is
     * @return the cleaned string
     */
    public String filter(String string, boolean strip, Pattern pattern, Types.Filters type) {
        if (string == null) {
            return null;
        }
        String replacement = plugin.getConfig().getString("settings.filtering.replacement");
        Matcher matcher = pattern.matcher(string);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            if (getLocalWhitelist().contains(matcher.group(0).trim().toLowerCase()))
                continue;
            if (matcher.group(1) != null)
                if (getLocalWhitelist().contains(matcher.group(1).trim().toLowerCase()))
                    continue;
            try {
                String[] arr = matcher.toMatchResult().toString().split("=");
                String str = arr[arr.length - 1].replaceAll("[^\\p{L}0-9 ]+", " ").trim();
                if (plugin.getConfig().getBoolean("settings.filtering.replace word for word")) {
                    replacement = (plugin.getConfig().isSet("replacements." + str) ? plugin.getConfig().getString("replacements." + str) :
                            (plugin.getConfig().isSet("replacements.all") ? plugin.getConfig().getString("replacements.all") : plugin.getConfig().getString("settings.filtering.replacement")));
                }
            } catch (Exception e) {
                debug("Could not register replace_word_for_words: " + e.getMessage(), true, Level.WARNING);
            }
            if (replacement == null)
                return string;
            replacement = ChatColor.translateAlternateColorCodes('&', replacement);
            if (strip) {
                replacement = ChatColor.stripColor(replacement);
            }
            String r = plugin.getConfig().getBoolean("settings.filtering.replace word for word") ? replacement : matcher.group(0).trim().replaceAll("(?s).", replacement);
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
        }
        matcher.appendTail(out);
        string = out.toString();

        return string;
    }

    private String getWhitelistRegex() {
        List<String> whitelistRegex = new ArrayList<>();
        for (String str : getLocalWhitelist()) {
            whitelistRegex.add("(?i)\\b" + str + "\\b");
        }
        return String.join("|", whitelistRegex);
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
     * @param string  the suspected string
     * @param pattern the pattern to check for
     * @return if the string is clean or not
     */
    public boolean isclean(String string, Pattern pattern) {
        string = string.replaceAll("[^\\p{L}0-9 ]+", " ").trim();
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            if (getLocalWhitelist().contains(matcher.group(0).trim().toLowerCase()))
                continue;
            if (matcher.group(1) != null)
                if (getLocalWhitelist().contains(matcher.group(1).trim().toLowerCase()))
                    continue;
            return false;
        }
        return true;
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
            rs.close();
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
            setLocalSwears(swears);
            local = Manager.FileManager.getFile(plugin, "data/whitelist");
            local.set("whitelist", whitelist);
            Manager.FileManager.saveFile(plugin, local, "data/whitelist");
            setLocalWhitelist(whitelist);
            local = Manager.FileManager.getFile(plugin, "data/global");
            local.set("global", global);
            Manager.FileManager.saveFile(plugin, local, "data/global");
            setGlobalSwears(global);
        }
        reloadPattern(Types.Filters.OTHER);
    }

    /**
     * Reload the current patterns for Global, Swears and whitelist.
     */
    public Pattern reloadPattern(Types.Filters type) {
        boolean update = false;
        FileConfiguration local = Manager.FileManager.getFile(plugin, "data/whitelist");
        if (type == RELOAD)
            plugin.localSizes.clear();
        if ((local.getStringList("whitelist").size() != plugin.getLocal("whitelist"))) {
            debug("Whitelist doesn't equal local parameters, filling variables.", plugin.getDebug(), Level.INFO);
            List<String> whitelist = local.getStringList("whitelist");
            whitelist.add("text");
            whitelist.add("color");
            whitelist.add("extra");
            whitelist.add("italic");
            for (ChatColor c : ChatColor.values()) {
                whitelist.add(c.toString());
            }
            setLocalWhitelist(whitelist);
            plugin.setLocal("whitelist", local.getStringList("whitelist").size());
            if (plugin.getLocal("swears") != 0 || plugin.getLocal("global") != 0) {
                debug("Forcefully updating swear data & global data to prevent false positives", plugin.getDebug(), Level.INFO);
                plugin.setLocal("swears", 0);
                plugin.setLocal("global", 0);
            }
        }
        local = Manager.FileManager.getFile(plugin, "data/global");
        if ((local.getStringList("global").size() != plugin.getLocal("global")) || (getGlobalRegex().isEmpty() && !local.getStringList("global").isEmpty())) {
            debug("globalSwears doesn't equal config parameters or regex is empty, filling variables.", plugin.getDebug(), Level.INFO);
            List<String> s = local.getStringList("global");
            setGlobalSwears(s);
            List<String> duh = generateRegex(s);
            setGlobalRegex(duh.stream().sorted((s1, s2) -> s2.length() - s1.length())
                    .collect(Collectors.toList()));
            local.set("global", s);
            Manager.FileManager.saveFile(plugin, local, "data/global");
            plugin.setLocal("global", s.size());
            update = true;
            setGlobalPattern(Pattern.compile("(" + getWhitelistRegex() + ")|" + String.join("|", duh), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS));
        }
        local = Manager.FileManager.getFile(plugin, "data/swears");
        if ((local.getStringList("swears").size() != plugin.getLocal("swears")) || (getRegex().isEmpty() && !local.getStringList("swears").isEmpty())) {
            debug("localSwears doesn't equal config parameters or regex is empty, filling variables.", plugin.getDebug(), Level.INFO);
            List<String> s = local.getStringList("swears");
            local.set("swears", s);
            Manager.FileManager.saveFile(plugin, local, "data/swears");
            setLocalSwears(s);
            if (s.isEmpty()) {
                send(Bukkit.getConsoleSender(), Objects.requireNonNull(plugin.getLanguage().getString("variables.failure"))
                        .replaceAll("(?i)\\{message}|(?i)%message%", "File /data/swears.yml, please fix this ASAP; Using `fuck, shit` as placeholders"));
                setLocalSwears(Arrays.asList("fuck", "shit"));
            }
            regex.clear();
            List<String> duh = generateRegex(s);
            setRegex(duh.stream().sorted((s1, s2) -> s2.length() - s1.length())
                    .collect(Collectors.toList()));
            plugin.setLocal("swears", s.size());
            update = true;
            setSwearPattern(Pattern.compile("(" + getWhitelistRegex() + ")|" + String.join("|", duh), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS));
        }
        if (update) {
            debug("Updating pattern for both regex!", plugin.getDebug(), Level.INFO);
            List<String> patt = new ArrayList<>();
            patt.addAll(getGlobalRegex());
            patt.addAll(getRegex());
            setBothPattern(Pattern.compile("(" + getWhitelistRegex() + ")|" + String.join("|", patt), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS));
        }
        if (type == Types.Filters.BOTH) {
            return getBothPattern();
        } else if (type == Types.Filters.GLOBAL) {
            return getGlobalPattern();
        }
        return getSwearPattern();
    }

    public List<String> generateRegex(List<String> s) {
        List<String> duh = new ArrayList<>();
        for (String str : s) {
            if (str.toLowerCase().startsWith("regex:")) {
                try {
                    duh.add(Pattern.compile(str.replaceAll("regex:", "")).pattern());
                } catch (Exception ignored) {
                    debug("Pattern is invalid: " + str.replaceAll("regex:", ""), true, Level.WARNING);
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
                String quote = Pattern.quote(Objects.requireNonNull(plugin.getConfig().getString("settings.filtering.ignore special characters.characters to ignore", "!@#$%^&*()_+-")).replaceAll("\"", "\\\""));
                int length = str.length();
                for (String str2 : str.split("")) {
                    length -= 1;
                    str2 = Pattern.quote(str2);
                    //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+)                                                                               = fuck
                    //(f+\s*+|f+[!@#$%^&*()_+-"]+\s*+)(u+\s*+|[!@#$%^&*()_+-"]+u+\s*+)(c+\s*+|[!@#$%^&*()_+-"]+c+\s*+)(k+\s*+|[!@#$%^&*()_+-"]+k+\s*+)   = fuck with special chars
                    if (length <= 0) { // length is the end
                        omg.append("(").append(str2).append("+|([").append(quote).append("]|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+").append(str2).append(")");
                    } else if (length == str.length() - 1) { // length is the beginning
                        omg.append("(?i)(").append(str2).append("+\\s*+|").append(str2).append("+\\s*+([").append(quote).append("]+\\s*+|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+)");
                    } else { // length is somewhere in between
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
                        length2--;
                        str3 = Pattern.quote(str3);
                        //(f+\s*+)(u+\s*+|-+u+\s*+)(c+\s*+|-+c+\s*+)(k+|-+k+)                                                                               = fuck
                        //(f+\s*+|f+[!@#$%^&*()_+-"]+\s*+)(u+\s*+|[!@#$%^&*()_+-"]+u+\s*+)(c+\s*+|[!@#$%^&*()_+-"]+c+\s*+)(k+\s*+|[!@#$%^&*()_+-"]+k+\s*+)   = fuck with special chars
                        if (length2 <= 0) { // length is the end
                            omg2.append("(").append(str3).append("+|([").append(quote).append("]|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+").append(str3).append(")");
                        } else if (length2 == str2.length() - 1) { // length is the beginning
                            omg2.append("(?i)(").append(str3).append("+\\s*+|").append(str3).append("+\\s*+([").append(quote).append("]+\\s*+|((§|&)[0-9A-FK-OR]|(§|&)))+\\s*+)");
                        } else { // length is somewhere in between
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
                .replaceAll("(?i)\\{version}|(?i)%version%", String.valueOf(getVersion()))
                .replaceAll("(?i)\\{serverversion}|(?i)%serverversion%", plugin.getServer().getVersion())
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
            try {
                file.save(new File(plugin.getDataFolder(), fileName + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}