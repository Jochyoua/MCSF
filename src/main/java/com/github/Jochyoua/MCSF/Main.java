package com.github.Jochyoua.MCSF;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import me.vagdedes.mysql.basic.Config;
import me.vagdedes.mysql.database.MySQL;
import me.vagdedes.mysql.database.SQL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import xyz.upperlevel.spigot.book.BookUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends JavaPlugin {
    String version;
    {
        try {
            version = ((JSONObject) new JSONParser().parse(new Scanner(new URL("https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=54115").openStream()).nextLine())).get("current_version").toString();
        } catch (ParseException | IOException ignored) {
            version = getDescription().getVersion();
        }
    }

    public void reload() {
        if (getConfig().getBoolean("settings.mysql")) {
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
                    getConfig().set("users." + ID + ".enabled", status);
                    getConfig().set("users." + ID + ".playername", name);
                }
            } catch (Exception ignored) {
            }
            getConfig().set("swears", array);
            saveConfig();
        }
        reloadConfig();
    }

    private void debug(String str) {
        if (getConfig().getBoolean("settings.debug"))
            send(Bukkit.getConsoleSender(), getConfig().getString("variables.debug").replace("%message%", str));
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().header(
                "MCSF (My Christian Swear Filter) v" + getDescription().getVersion() + " by Jochyoua\n"
                        + "This is a toggleable swear filter for your players\n"
                        + "Resource: https://www.spigotmc.org/resources/54115/\n"
                        + "Github: https://www.github.com/Jochyoua/MCSF/\n");
        getConfig().options().copyDefaults(true);
        saveConfig();
        if (getConfig().getBoolean("settings.discordSRV")) {
            getServer().getPluginManager().registerEvents(new Listener() {
                @Subscribe
                public void discordMessageProcessed(
                        final github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent event) {
                    event.setProcessedMessage(clean(event.getProcessedMessage()).replaceAll("\\*", "\\" + "\\*"));
                }
            }, this);
            debug("Registered DiscordSRV event");
        }
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void serverCommand(ServerCommandEvent e) {
                String command = e.getCommand().split(" ")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (command.equalsIgnoreCase(getConfig().getString("variables.command")) || command.equalsIgnoreCase("mcsf")) {
                    e.setCancelled(true);
                    CommandSender sender = e.getSender();
                    List<String> args = new LinkedList<>(Arrays.asList(e.getCommand().split(" ")));
                    args.remove(0);
                    registerCommand(sender, args);
                }
            }

            @EventHandler
            public void playerCommand(PlayerCommandPreprocessEvent e) {
                String command = e.getMessage().split(" ")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (command.equalsIgnoreCase(getConfig().getString("variables.command")) || command.equalsIgnoreCase("mcsf")) {
                    e.setCancelled(true);
                    Player sender = e.getPlayer();
                    List<String> args = new LinkedList<>(Arrays.asList(e.getMessage().split(" ")));
                    args.remove(0);
                    registerCommand(sender, args);
                }
            }

            private void registerCommand(CommandSender sender, List<String> args) {
                reloadConfig();
                if (!args.isEmpty()) {
                    switch (args.get(0).toLowerCase()) {
                        case "help":
                        default:
                            showHelp(sender);
                            break;
                        case "toggle":
                            boolean value;
                            if (args.size() == 1 && (sender instanceof Player)) {
                                if (!sender.hasPermission("MCSF.toggle")) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.noperm")));
                                    break;
                                }
                                if (sender.hasPermission("MCSF.bypass")) {
                                    value = toggle(((Player) sender).getUniqueId());
                                    if (getConfig().getBoolean("settings.log"))
                                        send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.targetToggle"))
                                                .replaceAll("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.toggle"))
                                            .replaceAll("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));

                                    break;
                                }
                                if (getConfig().getBoolean("settings.force")) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.disabled")));
                                    break;
                                }
                                value = toggle(((Player) sender).getUniqueId());
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.toggle"))
                                        .replaceAll("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));
                                if (getConfig().getBoolean("settings.log"))
                                    send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.targetToggle"))
                                            .replaceAll("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));
                            } else {
                                if (args.size() != 2) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                            getConfig().getString("variables.error.invalidtarget").replace("%target%", sender.getName())));
                                    break;
                                }
                                if (!sender.hasPermission("MCSF.modify")) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.noperm")));
                                    break;
                                } else if (sender.hasPermission("MCSF.bypass")) {
                                    UUID targetid = null;
                                    for (final String key : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                                        if (Objects.requireNonNull(getConfig().getString("users." + key + ".playername")).equalsIgnoreCase(args.get(1))) {
                                            targetid = UUID.fromString(key);
                                        }
                                    }
                                    value = toggle(targetid);
                                    if (getConfig().getBoolean("settings.log"))
                                        send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.targetToggle"))
                                                .replaceAll("%value%", value ? "enabled" : "disabled").replaceAll("%target%", args.get(1)));
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.targetToggle")).replace("%target%", args.get(1)).replace("%value%", (value ? "enabled" : "disabled")));

                                    break;
                                }
                                if (getConfig().getBoolean("settings.force")) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.disabled")));
                                    break;
                                }
                                UUID targetid = null;
                                for (final String key : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                                    if (Objects.requireNonNull(getConfig().getString("users." + key + ".playername")).equalsIgnoreCase(args.get(1))) {
                                        targetid = UUID.fromString(key);
                                    }
                                }
                                if (targetid == null) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                            getConfig().getString("variables.error.invalidtarget").replace("%target%", args.get(1))));
                                    break;
                                } else {
                                    toggle(targetid);
                                    if (getConfig().getBoolean("settings.log"))
                                        send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.targetToggle"))
                                                .replaceAll("%value%", status(targetid) ? "enabled" : "disabled").replaceAll("%target%", args.get(1)));
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.targetToggle")).replace("%target", args.get(1)).replace("%value%", (status(targetid) ? "enabled" : "disabled")));
                                }
                            }
                            break;
                        case "reload":
                            if (!sender.hasPermission("MCSF.modify")) {
                                send(sender, getConfig().getString("variables.noperm"));
                                break;
                            }
                            reload();
                            send(sender, Objects.requireNonNull(getConfig().getString("variables.success")).replace("%message%", getConfig().getString("variables.successful.reloaded")));
                            break;
                        case "status":
                            if (args.size() == 1 && (sender instanceof Player)) {
                                if (getConfig().getBoolean("settings.force")) {
                                    value = true;
                                } else {
                                    value = status(((Player) sender).getUniqueId());
                                }
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.status")).replace("%target%", sender.getName()).replace("%value%", (value ? "enabled" : "disabled")));
                                break;
                            } else {
                                if (args.size() != 2) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                            getConfig().getString("variables.error.invalidtarget").replace("%target%", sender.getName())));
                                    break;
                                }
                                if (!sender.hasPermission("MCSF.status")) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.noperm")));
                                    break;
                                }
                                UUID targetid = null;
                                for (final String key : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                                    if (Objects.requireNonNull(getConfig().getString("users." + key + ".playername")).equalsIgnoreCase(args.get(1))) {
                                        targetid = UUID.fromString(key);
                                    }
                                }
                                if (targetid == null) {
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                            getConfig().getString("variables.error.invalidtarget").replace("%target%", args.get(1))));
                                    break;
                                } else {
                                    if (getConfig().getBoolean("settings.force")) {
                                        value = true;
                                    } else {
                                        value = status(targetid);
                                    }
                                    send(sender, Objects.requireNonNull(getConfig().getString("variables.status")).replace("%target%", args.get(1)).replace("%value%", (value ? "enabled" : "disabled")));
                                }
                            }
                            break;
                        case "version":
                            if (sender.hasPermission("MCSF.version")) {
                                for (String str : getConfig().getStringList("variables.version")) {
                                    send(sender, str);
                                }
                            }else{
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.noperm")));
                            }
                            break;
                        case "add":
                        case "remove":
                            if (sender.hasPermission("MCSF.modify")) {
                                if (args.size() != 2) {
                                    send(sender, getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.incorrectargs")));
                                    break;
                                }
                                String word = args.get(1).toLowerCase();
                                if (Pattern.compile("[`,./';+=\\-\\\\~#@*%(){}<>\\[\\]|\"_^]").matcher(word).find() || word.length() == 1) {
                                    send(sender, getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.illegalcharacters")));
                                    debug(sender.getName() + " has attempted to add `" + word + "` but it contains illegal characters");
                                    break;
                                }
                                List<String> swears = getConfig().getStringList("swears");
                                switch (args.get(0)) {
                                    case "add":
                                        if (getConfig().getBoolean("settings.mysql")) {
                                            if (SQL.exists("word", word, "swears")) {
                                                send(sender, getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.alreadyexists")));
                                                break;
                                            }
                                            if (!swears.contains(word))
                                                swears.add(word);
                                            SQL.insertData("word", "'" + word + "'", "swears");
                                            debug(sender.getName() + " has added `" + word + "` to the database");
                                            send(sender, getConfig().getString("variables.success").replace("%message%", getConfig().getString("variables.successful.added")));
                                        } else {
                                            boolean modified = false;
                                            if (!swears.contains(word))
                                                modified = true;
                                            if (modified) {
                                                swears.add(word);
                                                debug(sender.getName() + " has added `" + word + "` to the config");
                                                send(sender, getConfig().getString("variables.success").replace("%message%", getConfig().getString("variables.successful.added")));
                                            } else {
                                                send(sender, getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.alreadyexists")));
                                            }
                                        }
                                        break;
                                    case "remove":
                                        if (getConfig().getBoolean("settings.mysql")) {
                                            if (!SQL.exists("word", word, "swears")) {
                                                send(sender, getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.doesntexist")));
                                                break;
                                            }
                                            swears.remove(word);
                                            SQL.deleteData("word", "=", word, "swears");
                                            debug(sender.getName() + " has removed `" + word + "` from database");
                                            send(sender, getConfig().getString("variables.success").replace("%message%", getConfig().getString("variables.successful.removed")));
                                        } else {
                                            boolean modified = swears.remove(word);
                                            if (modified) {
                                                debug(sender.getName() + " has removed `" + word + "` from config");
                                                send(sender, getConfig().getString("variables.success").replace("%message%", getConfig().getString("variables.successful.removed")));
                                            } else {
                                                send(sender, getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.doesntexists")));
                                            }
                                        }
                                        break;
                                    default:
                                        send(sender, getConfig().getString("variables.failure").replace("%message%", getConfig().getString("variables.error.incorrectargs")));
                                        break;
                                }
                                getConfig().set("swears", swears);
                                saveConfig();
                            } else {
                                send(sender, getConfig().getString("variables.noperm"));
                            }
                            break;
                    }
                } else {
                    showHelp(sender);
                }
            }

            @EventHandler
            public void Join(PlayerJoinEvent e) {
                Player player = e.getPlayer();
                getConfig().set("users." + player.getUniqueId() + ".playername", player.getName());
                if (!getConfig().isSet("users." + player.getUniqueId() + ".enabled"))
                    toggle(player.getUniqueId());
                if (getConfig().getBoolean("settings.mysql")) {
                    MySQL.update("UPDATE users SET name='" + player.getName() + "' WHERE uuid='" + player.getUniqueId() + "';");
                }
                debug("Player " + player.getName() + "'s swear filter is " + (status(player.getUniqueId()) ? "enabled" : "disabled"));
                saveConfig();
            }

            @SuppressWarnings("deprecation")
            @EventHandler
            public void openBook(PlayerInteractEvent e) {
                Player player = e.getPlayer();
                if (getConfig().getBoolean("settings.bookcheck") && status(player.getUniqueId())) {
                    if (player.getItemInHand().getType() == Material.WRITTEN_BOOK && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
                        e.setCancelled(true);
                        ItemStack book = player.getInventory().getItemInHand();
                        BookMeta meta = (BookMeta) book.getItemMeta();
                        int slot = player.getInventory().getHeldItemSlot();
                        ItemStack old = player.getInventory().getItem(slot);
                        ItemStack newbook = new ItemStack(Material.WRITTEN_BOOK);
                        BookMeta newmeta = (BookMeta) newbook.getItemMeta();
                        for (String page : meta.getPages())
                            newmeta.addPage(isclean(page) ? page : ChatColor.stripColor(clean(page))); // Have to strip colors for book or the pages would mess up pretty badly..
                        newmeta.setAuthor("Server");
                        newmeta.setTitle("Example Title");
                        newbook.setItemMeta(newmeta);
                        player.getInventory().setItem(slot, newbook);
                        BookUtil.openPlayer(player, newbook);
                        player.getInventory().setItem(slot, old);
                    }
                }
            }
        }, this);
        if (getServer().getPluginManager().getPlugin("MySQL") != null) {
            send(Bukkit.getConsoleSender(), "MySQL brought to you by MySQL API: https://www.spigotmc.org/resources/mysql-api.23932/");
            if (getConfig().getBoolean("settings.mysql")) {
                Config.setHost(getConfig().getString("mysql.host"));
                Config.setUser(getConfig().getString("mysql.username"));
                Config.setPassword(getConfig().getString("mysql.password"));
                Config.setDatabase(getConfig().getString("mysql.database"));
                Config.setPort(getConfig().getString("mysql.port"));
                MySQL.connect();
                if (!SQL.tableExists("swears") || !SQL.tableExists("users")) {
                    createTable(false);
                }
                reload();
                debug("MySQL has been enabled & information has been set");
            } else {
                send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%", "MySQL api is installed but settings.mysql is currently false."));
            }
        } else if (getConfig().getBoolean("settings.mysql")) {
            send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%", "Make sure you have the MySQL API installed!: https://www.spigotmc.org/resources/mysql-api.23932/"));
        }
        if (getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
            DiscordSRV.api.subscribe(this);
            debug("DiscordSRV events have been subscribed to");
        }
        if (getConfig().getBoolean("settings.metrics")) {
            final Metrics metrics = new Metrics(this);
            debug("Metrics is " + (metrics.isEnabled() ? "enabled" : "disabled"));
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                UUID ID = player.getUniqueId();
                PacketContainer packet = event.getPacket();
                StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
                for (WrappedChatComponent component : chatComponents.getValues()) {
                    if (getConfig().getBoolean("settings.force") || status(ID)) {
                        reloadConfig();
                        if (component != null) {
                            if (!component.getJson().isEmpty()) {
                                if (!isclean(component.getJson())) {
                                    String string = clean(component.getJson());
                                    if (string == null) {
                                        return;
                                    }
                                    component.setJson(string);
                                    chatComponents.write(0, component);
                                }
                            }
                        }
                    }
                }


            }
        });
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (getConfig().getBoolean("settings.check_for_updates")) {
                send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.checking"));
                if ((Double.parseDouble(getDescription().getVersion()) < Double.parseDouble(version))) {
                    send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.update_available"));
                    send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.update_link"));
                } else {
                    send(Bukkit.getConsoleSender(), getConfig().getString("variables.updatecheck.no_new_version"));
                }
            }

        }, 0L, 432000L);
    }

    public void showHelp(CommandSender sender) {
        for (String str : getConfig().getStringList("variables.help")) {
            Matcher match = Pattern.compile("<%PERMISSION=(.*?)%>", Pattern.DOTALL).matcher(str);
            String permission = null;
            while (match.find()) {
                permission = match.group(1);
            }
            if (!(permission == null)) {
                if (!sender.hasPermission(permission)) {
                    continue;
                } else {
                    str = str.replaceAll("<%PERMISSION=(.*?)%>", "");
                }
            }
            send(sender, str);
        }
    }

    public void createTable(boolean reset) {
        if (getConfig().getStringList("swears").isEmpty()) {
            getConfig().set("swears", new String[]{"fuck", "shit"});
            saveConfig();
        }
        StringBuilder omg = new StringBuilder();
        for (final String str : getConfig().getStringList("swears")) {
            omg.append("('").append(str.trim()).append("'),");
        }
        StringBuilder omg2 = new StringBuilder();
        for (final String ID : getConfig().getConfigurationSection("users").getKeys(false)) {
            String playername = getConfig().getString("users." + ID + ".playername");
            boolean status = status(UUID.fromString(ID));
            omg2.append("('").append(ID).append("', '").append(playername).append("', '").append(status).append("'),");
        }
        if (reset) {
            MySQL.update("DROP TABLE IF EXISTS swears;DROP TABLE IF EXISTS users;");
        }
        if (!SQL.tableExists("swears") || countRows("swears") == 0) {
            MySQL.update("DROP TABLE IF EXISTS swears;");
            MySQL.update("CREATE TABLE IF NOT EXISTS swears (word varchar(255) UNIQUE)");
            boolean query = MySQL.update("INSERT INTO swears (word) VALUES "
                    + omg.toString().trim().substring(0, omg.length() - 1).replace("(''),".toLowerCase(), "") + ";");
        }
        if (!SQL.tableExists("users") || countRows("users") == 0) {
            MySQL.update("DROP TABLE IF EXISTS users;");
            MySQL.update("CREATE TABLE IF NOT EXISTS users (uuid varchar(255) UNIQUE, name varchar(255), status varchar(255))");
            MySQL.update("INSERT INTO users (uuid,name,status) VALUES " + omg2.toString().trim().substring(0, omg2.length() - 1) + ";");
        }
    }

    public int countRows(String table) {
        int i = 0;
        if (table == null)
            return i;
        ResultSet rs = MySQL.query("SELECT * FROM " + table + ";");
        try {
            while (rs.next())
                i++;
        } catch (Exception ignored) {
        }
        return i;
    }

    public boolean status(UUID ID) {
        reloadConfig();
        return getConfig().getBoolean("users." + ID + ".enabled");
    }

    public String clean(String string) {
        reloadConfig();
        if (string != null) {
            if (getConfig().getBoolean("settings.experimental_regex")) {
                List<String> regex = new ArrayList<>();
                for (String str : getConfig().getStringList("swears")) {
                    StringBuilder omg = new StringBuilder();
                    for (String str2 : str.split("")) {
                        omg.append(str2).append("+\\s*");
                    }
                    regex.add(omg.toString().substring(0, omg.toString().length() - 4) + "+");
                }
                for (String r : regex) {
                    String clean = r.replace("+\\s*", "");
                    clean = clean.substring(0, clean.length() - 1);
                    Pattern pattern = Pattern.compile(r, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
                    Matcher matcher = pattern.matcher(string);
                    String replacement = ChatColor.translateAlternateColorCodes('&', clean.replaceAll("(?s).", getConfig().getString("settings.replacement")));
                    string = matcher.replaceAll(replacement);
                }

            } else {
                final List<String> delete = new ArrayList<>();
                for (int start = 0; start < string.length(); start++) {
                    for (int offset = 1; offset < (string.length() + 1 - start); offset++) {
                        final String wordToCheck = string.substring(start, start + offset);
                        if (getConfig().getStringList("swears").contains(wordToCheck.toLowerCase())) {
                            delete.add(wordToCheck);
                        }
                    }
                }
                for (final String s : delete) {
                    final String replacement = s.replaceAll("(?s).", ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getConfig().getString("settings.replacement"))));
                    string = string.replaceAll(String.format("(?i)%s", s), replacement);
                }
            }
        }
        return string;
    }

    public boolean isclean(String str) {
        reloadConfig();
        if (getConfig().getBoolean("settings.experimental_regex") || getConfig().getStringList("swears").isEmpty()) {
            return false;
        }
        return getConfig().getStringList("swears").stream().parallel().noneMatch((str).toLowerCase()::contains);
    }

    @Override
    public void onDisable() {
        if (getServer().getPluginManager().getPlugin("DiscordSRV") != null)
            DiscordSRV.api.unsubscribe(this);
    }

    public void send(CommandSender player, String message) {
        message = message.replaceAll("%prefix%", Objects.requireNonNull(getConfig().getString("variables.prefix")));
        message = message.replaceAll("%command%", getConfig().getString("variables.command"));
        message = message.replaceAll("%player%", player.getName());
        message = message.replaceAll("%current%", getDescription().getVersion());
        message = message.replaceAll("%version%", version);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public boolean toggle(UUID ID) {
        reloadConfig();
        boolean value;
        if (getConfig().getBoolean("settings.mysql")) {
            if (!SQL.exists("uuid", ID.toString(), "users")) {
                value = getConfig().getBoolean("settings.default");
                MySQL.update("INSERT INTO users (uuid,status) VALUES ('" + ID + "','" + value + "')");
                getConfig().set("users." + ID + ".enabled", value);
                saveConfig();
                return value;
            } else {
                ResultSet rs = MySQL.query("SELECT status FROM users WHERE uuid='" + ID + "'");
                boolean result = false;
                try {
                    while (rs.next()) {
                        result = Boolean.parseBoolean(rs.getString("status"));
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    result = getConfig().getBoolean("settings.default");
                }
                value = !result;
                MySQL.update("UPDATE users SET status='" + value + "' WHERE uuid='" + ID + "';");
                getConfig().set("users." + ID + ".enabled", value);
                saveConfig();
                return value;
            }
        } else if (!getConfig().isSet("users." + ID + ".enabled")) { // If enabled value doesn't exist, set to default value
            value = getConfig().getBoolean("settings.default");
            getConfig().set("users." + ID + ".enabled", value);
            saveConfig();
            return value;
        }
        value = getConfig().getBoolean("users." + ID + ".enabled");
        getConfig().set("users." + ID + ".enabled", !value);
        saveConfig();
        return !value;
    }
}