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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        if (getConfig().getBoolean("settings.discordSRV")) {
            getServer().getPluginManager().registerEvents(new Listener() {
                @Subscribe
                public void discordMessageProcessed(
                        final github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent event) {
                    event.setProcessedMessage(clean(event.getProcessedMessage()).replaceAll("\\*", "\\" + "\\*"));
                }
            }, this);
        }
        getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void Join(PlayerJoinEvent e) {
                Player player = e.getPlayer();
                if (!getConfig().isSet("users." + player.getUniqueId() + ".enabled"))
                    toggle(player.getUniqueId());
                getConfig().set("users." + player.getUniqueId() + ".playername", player.getName());
                saveConfig();
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
                if (MySQL.isConnected()) {
                    if (!SQL.tableExists("swears")) {
                        createTable();
                    }
                    ArrayList<String> array = new ArrayList<>();
                    try {
                        ResultSet rs = MySQL.query("SELECT * FROM swears;");
                        while (rs.next()) {
                            array.add((String) (rs.getObject("word")));
                        }
                    } catch (Exception ignored) {
                    }
                    getConfig().set("swears", array);
                    saveConfig();
                    reloadConfig();
                }
            } else {
                send(Bukkit.getConsoleSender(), getConfig().getString("variables.failure").replace("%message%", "MySQL api is installed but settings.mysql is currently false."));
            }
        } else if (getConfig().getBoolean("settings.mysql")) {
            send(Bukkit.getConsoleSender(), getConfig().getString("variables.failure").replace("%message%", "Make sure you have the MySQL API installed!: https://www.spigotmc.org/resources/mysql-api.23932/"));
        }
        if (getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
            DiscordSRV.api.subscribe(this);
        }
        Objects.requireNonNull(getCommand("mcsf")).setExecutor((sender, command, s, args) -> {
            if (args.length != 0) {
                switch (args[0].toLowerCase()) {
                    case "help":
                    default:
                        for (String str : getConfig().getStringList("variables.help"))
                            send(sender, str);
                        break;
                    case "toggle":
                        boolean value = false;
                        if (args.length == 1) {
                            if (!(sender instanceof Player)) {
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                        sender.getName() + " is not a valid player"));
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
                            if (!sender.hasPermission("MCSF.modify")) {
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.noperm")));
                                break;
                            } else if (sender.hasPermission("MCSF.bypass")) {
                                UUID targetid = null;
                                for (final String key : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                                    if (Objects.requireNonNull(getConfig().getString("users." + key + ".playername")).equalsIgnoreCase(args[1])) {
                                        targetid = UUID.fromString(key);
                                    }
                                }
                                value = toggle(targetid);
                                if (getConfig().getBoolean("settings.log"))
                                    send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.targetToggle"))
                                            .replaceAll("%value%", value ? "enabled" : "disabled").replaceAll("%target%", args[1]));
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.targetToggle")).replace("%target%", args[1]).replace("%value%", (value ? "enabled" : "disabled")));

                                break;
                            }
                            if (getConfig().getBoolean("settings.force")) {
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.disabled")));
                                break;
                            }
                            UUID targetid = null;
                            for (final String key : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                                if (Objects.requireNonNull(getConfig().getString("users." + key + ".playername")).equalsIgnoreCase(args[1])) {
                                    targetid = UUID.fromString(key);
                                }
                            }
                            if (targetid == null) {
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                        args[1] + " is not a valid player"));
                                break;
                            } else {
                                getConfig().set("users." + targetid + "enabled", !getConfig().getBoolean("users." + targetid + ".enabled"));
                                saveConfig();
                                if (getConfig().getBoolean("settings.log"))
                                    send(Bukkit.getConsoleSender(), Objects.requireNonNull(getConfig().getString("variables.targetToggle"))
                                            .replaceAll("%value%", getConfig().getBoolean("users." + targetid + ".enabled") ? "enabled" : "disabled").replaceAll("%target%", args[1]));
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.targetToggle")).replace("%target", args[1]).replace("%value%", (getConfig().getBoolean("users." + targetid + ".enabled") ? "enabled" : "disabled")));
                            }
                        }
                        break;
                    case "reload":
                        if (args.length != 1) {
                            for (String str : getConfig().getStringList("variables.help"))
                                send(sender, str);
                            break;
                        }
                        if (!sender.hasPermission("MCSF.modify")) {
                            send(sender, getConfig().getString("variables.noperm"));
                            break;
                        }
                        saveConfig();
                        reloadConfig();
                        send(sender, Objects.requireNonNull(getConfig().getString("variables.success")).replace("%message%", "Plugin successfully reloaded"));
                        break;
                    case "status":
                        if (args.length == 1) {
                            if (!(sender instanceof Player)) {
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                        sender.getName() + " is not a valid player"));
                                break;
                            }
                            if (getConfig().getBoolean("settings.force")) {
                                value = true;
                            } else {
                                value = status(((Player) sender).getUniqueId());
                            }
                            send(sender, Objects.requireNonNull(getConfig().getString("variables.status")).replace("%target%", sender.getName()).replace("%value%", (value ? "enabled" : "disabled")));
                            break;
                        } else {
                            if (!sender.hasPermission("MCSF.status")) {
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.noperm")));
                                break;
                            }
                            UUID targetid = null;
                            for (final String key : Objects.requireNonNull(getConfig().getConfigurationSection("users")).getKeys(false)) {
                                if (Objects.requireNonNull(getConfig().getString("users." + key + ".playername")).equalsIgnoreCase(args[1])) {
                                    targetid = UUID.fromString(key);
                                }
                            }
                            if (targetid == null) {
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.failure")).replace("%message%",
                                        args[1] + " is not a valid player"));
                                break;
                            } else {
                                if (getConfig().getBoolean("settings.force")) {
                                    value = true;
                                } else {
                                    value = status(targetid);
                                }
                                send(sender, Objects.requireNonNull(getConfig().getString("variables.status")).replace("%target%", args[1]).replace("%value%", (value ? "enabled" : "disabled")));
                            }
                        }
                        break;
                    case "add":
                    case "remove":
                        if (sender.hasPermission("MCSF.modify") && getConfig().getBoolean("settings.mysql")) {
                            if (!SQL.tableExists("swears")) {
                                createTable();
                            }
                            if (args.length != 2) {
                                send(sender, getConfig().getString("variables.failure").replace("%message%", "Not enough arguments."));
                                break;
                            }
                            String word = args[1].toLowerCase();
                            List<String> localList = getConfig().getStringList("swears");
                            if (args[0].equalsIgnoreCase("add")) {
                                if (SQL.exists("word", word, "swears")) {
                                    send(sender, getConfig().getString("variables.failure").replace("%message%", "That word already exists in the database."));
                                    break;
                                }
                                localList.add(word);
                                SQL.insertData("word", "'" + word + "'", "swears");
                                send(sender, getConfig().getString("variables.success").replace("%message%", "Word has been added."));
                            } else {
                                if (!SQL.exists("word", word, "swears")) {
                                    send(sender, getConfig().getString("variables.failure").replace("%message%", "That word doesn't exists in the database."));
                                    break;
                                }
                                localList.remove(word);
                                SQL.deleteData("word", "=", word, "swears");
                                send(sender, getConfig().getString("variables.success").replace("%message%", "Word has been removed."));
                            }
                            getConfig().set("swears", localList);
                            saveConfig();
                            reloadConfig();
                        } else {
                            send(sender, getConfig().getString("variables.disabled"));
                        }
                        break;
                }
            } else {
                for (String str : getConfig().getStringList("variables.help"))
                    send(sender, str);
            }
            return false;
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                UUID ID = player.getUniqueId();
                try {
                    PacketContainer packet = event.getPacket();
                    StructureModifier<WrappedChatComponent> chatComponents = packet.getChatComponents();
                    for (WrappedChatComponent component : chatComponents.getValues()) {
                        if (getConfig().getBoolean("settings.force") || status(ID)) {
                            reloadConfig();
                            if (!isclean(component.getJson())) {
                                component.setJson(clean(component.getJson()));
                                chatComponents.write(0, component);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void createTable() {
        reloadConfig();
        if (MySQL.isConnected()) {
            SQL.createTable("swears", "word varchar(255)");
            StringBuilder omg = new StringBuilder();
            for (final String str : getConfig().getStringList("swears")) {
                omg.append("('").append(str.trim()).append("'),");
            }
            boolean query = MySQL.update("INSERT INTO swears (word) VALUES "
                    + omg.toString().trim().substring(0, omg.length() - 1).replace("(''),".toLowerCase(), "") + ";");
            send(Bukkit.getConsoleSender(), !query ? getConfig().getString("variables.success").replace("%message%", "Failed to update SQL. Check connection.") : getConfig().getString("variables.failure").replace("%message%", "Successfully created table."));
        }
    }

    public boolean status(UUID ID) {
        reloadConfig();
        return getConfig().getBoolean("users." + ID + ".enabled");
    }

    public String clean(String string) {
        reloadConfig();
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
        return string;
    }

    public boolean isclean(String str) {
        reloadConfig();
        return getConfig().getStringList("swears").stream().parallel().noneMatch((str).toLowerCase()::contains);
    }

    @Override
    public void onDisable() {
        if (getServer().getPluginManager().getPlugin("DiscordSRV") != null)
            DiscordSRV.api.unsubscribe(this);
        if (getServer().getPluginManager().getPlugin("MySQL") != null) {
            if (getConfig().getBoolean("settings.mysql") && MySQL.isConnected()) {
                MySQL.disconnect();
                send(Bukkit.getConsoleSender(), getConfig().getString("variables.success").replace("%message%", "Successfully disconnected from database."));
            }
        }
    }

    public void send(CommandSender player, String message) {
        message = message.replaceAll("%prefix%", Objects.requireNonNull(getConfig().getString("variables.prefix")));
        message = message.replaceAll("%player%", player.getName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public boolean toggle(UUID ID) {
        reloadConfig();
        boolean value;
        if (!getConfig().isSet("users." + ID + ".enabled")) { // If enabled value doesn't exist, set to default value
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
