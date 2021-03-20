package io.github.jochyoua.mychristianswearfilter.events;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.IllegalArgumentException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.HikariCP;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandEvents {
    MCSF plugin;
    DatabaseConnector connector;
    Connection connection;
    Manager manager;


    public CommandEvents(Manager manager) {
        this.plugin = manager.getProvider();
        if (manager.supported("mysql")) {
            this.connector = manager.getConnector();
            this.connection = manager.getConnection();
        }
        this.manager = manager;
        PluginCommand cmd = plugin.getCommand("mcsf");
        try {
            if (cmd != null) {
                cmd.setTabCompleter((sender, command, s, args) -> {
                    final List<String> completions = new ArrayList<>();
                    if (args.length == 1) {
                        List<Types.Arguments> arguments = new ArrayList<>();
                        for (Types.Arguments a : Types.Arguments.values()) {
                            if (sender.hasPermission(a.getPermission())) {
                                arguments.add(a);
                            }
                        }
                        StringUtil.copyPartialMatches(args[0], Stream.of(arguments.toArray(new Types.Arguments[0]))
                                .map(Enum::name)
                                .collect(Collectors.toList()), completions);
                    }
                    if (args.length >= 2) {
                        if (!sender.hasPermission("MCSF.modify"))
                            return null;
                        switch (args[0]) {
                            case "remove":
                                List<String> swears = manager.getSwears();
                                StringUtil.copyPartialMatches(args[1], swears, completions);
                                break;
                            case "reset":
                                StringUtil.copyPartialMatches(args[1], Collections.singletonList("confirm"), completions);
                                break;
                            case "whitelist":
                                List<String> white = manager.getWhitelist();
                                StringUtil.copyPartialMatches(args[1], white, completions);
                                break;
                            case "global":
                                List<String> global = manager.getGlobal();
                                StringUtil.copyPartialMatches(args[1], global, completions);
                                break;
                            case "status":
                            case "toggle":
                            case "unset":
                                List<String> users = manager.getUsers(true);
                                StringUtil.copyPartialMatches(args[1], users, completions);
                                break;
                        }
                    }

                    Collections.sort(completions);
                    return completions;
                });
                cmd.setExecutor((sender, command, s, args) -> {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (!(plugin.getConfig().getInt("settings.cooldown") <= 0) && !sender.hasPermission("MCSF.bypass")) {
                            if (!manager.getCooldowns().containsKey(player.getUniqueId())) {
                                manager.addUser(player.getUniqueId());
                            } else {
                                long duration = manager.getCooldowns().getExpectedExpiration(player.getUniqueId());
                                duration = TimeUnit.MILLISECONDS.toSeconds(duration);
                                if (duration != 0) {
                                    manager.send(player, plugin.getLanguage().getString("variables.cooldown").replaceAll("(?i)\\{duration}|(?i)%duration%", String.valueOf(duration)));
                                    return true;
                                }
                            }
                        }
                    }
                    registerCommand(sender, Arrays.asList(args));
                    return true;
                });
            }
        } catch (Exception e) {
            manager.debug("Command failed to execute: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommand(CommandSender sender, List<String> args) {
        List<String> finalArgs = args.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!sender.hasPermission("MCSF.use")) {
                    throw new NoPermissionException(plugin.getLanguage());
                }
                if (!finalArgs.isEmpty()) {
                    if (plugin.getConfig().isSet("settings.command args enabled." + finalArgs.get(0))) {
                        if (!plugin.getConfig().getBoolean("settings.command args enabled." + finalArgs.get(0))) {
                            throw new CommandDisabledException(plugin.getLanguage());
                        }
                    }
                    switch (finalArgs.get(0).toLowerCase()) {
                        case "help":
                        default:
                            if (!plugin.getConfig().getBoolean("settings.command args enabled.help")) {
                                throw new CommandDisabledException(plugin.getLanguage());
                            }
                            manager.showHelp(sender);
                            break;
                        case "reload":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            sender.sendMessage("Reload succeeded!");
                            if (manager.supported("mysql")) {
                                sender.sendMessage("Reloading databases:");
                                try {
                                    plugin.getHikariCP().reload();
                                    if (plugin.getHikariCP().isEnabled())
                                        throw new Exception("Database was reloaded incorrectly");
                                    sender.sendMessage("Successfully reloaded database information!");
                                } catch (Exception e) {
                                    sender.sendMessage("Failed to reload database: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            sender.sendMessage("Resetting Swears, Global swears and whitelist:");
                            try {
                                manager.reloadPattern(Types.Filters.OTHER);
                                sender.sendMessage("Successfully reloaded swear, global swears and whitelist information!");
                            } catch (Exception e) {
                                sender.sendMessage("Failed to reload: " + e.getMessage());
                                e.printStackTrace();
                            }
                            sender.sendMessage("Reloading configuration data:");
                            try {
                                plugin.reloadLanguage();
                                sender.sendMessage("Successfully reloaded configuration information!");
                            } catch (Exception e) {
                                sender.sendMessage("Failed to reload configuration data: " + e.getMessage());
                                e.printStackTrace();
                            }
                            break;
                        case "global":
                            manager.setTable("global");
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            if (finalArgs.size() != 2) {
                                throw new IllegalArgumentException(plugin.getLanguage());
                            }
                            String glo = finalArgs.get(1).toLowerCase();
                            if (glo.startsWith("regex:")) {
                                try {
                                    Pattern.compile(glo.replaceAll("regex:", ""));
                                } catch (Exception e) {
                                    manager.send(sender, new FailureException(plugin.getLanguage(), "The provided regex is invalid.").getMessage());
                                    break;
                                }
                            }
                            FileConfiguration local = Manager.FileManager.getFile(plugin, "data/global");
                            List<String> global = local.getStringList("global");
                            if (manager.supported("mysql")) {
                                if (!connector.isWorking())
                                    plugin.getHikariCP().reload();
                                boolean sqlexists = false;
                                try {

                                    PreparedStatement ps = connection.prepareStatement(HikariCP.Query.GLOBAL.exists);
                                    ps.setString(1, glo);
                                    if (ps.executeQuery().next()) {
                                        sqlexists = true;
                                    }
                                    ps.close();
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                    sqlexists = false;
                                }
                                if (sqlexists && global.contains(glo)) {
                                    global.remove(glo);
                                    try (
                                            PreparedStatement ps = connection.prepareStatement(HikariCP.Query.GLOBAL.delete)) {
                                        ps.setString(1, glo);
                                        ps.execute();
                                        ps.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                } else {
                                    if (!global.contains(glo))
                                        global.add(glo);
                                    if (!sqlexists) {
                                        try (
                                                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.GLOBAL.insert)) {
                                            ps.setString(1, glo);
                                            ps.execute();
                                            ps.close();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            } else {
                                if (global.contains(glo)) {
                                    global.remove(glo);
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                } else {
                                    if (!global.contains(glo))
                                        global.add(glo);
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            }
                            local = Manager.FileManager.getFile(plugin, "data/global");
                            local.set("global", global);
                            Manager.FileManager.saveFile(plugin, local, "data/global");
                            break;
                        case "parse":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            if (args.size() == 1 || args.size() == 2) {
                                throw new IllegalArgumentException(plugin.getLanguage());
                            }
                            boolean state;
                            switch (args.get(1)) {
                                case "true":
                                case "enable":
                                case "enabled":
                                case "1":
                                    state = true;
                                    break;
                                case "false":
                                case "disabled":
                                case "disable":
                                case "0":
                                    state = false;
                                    break;
                                default:
                                    throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.invalidtype").replaceAll("(?i)\\{arg}|(?i)%arg%", args.get(1)).replaceAll("(?i)\\{type}|(?i)%type%", "boolean"));
                            }
                            StringBuilder message = new StringBuilder();
                            for (int i = 2; i < args.size(); i++) {
                                String arg = args.get(i) + " ";
                                message.append(arg);
                            }
                            manager.send(sender, plugin.getLanguage().getString("variables.parse").replaceAll("(?i)\\{message}|(?i)%message%", manager.clean(message.toString(), false, false, state ? manager.reloadPattern(Types.Filters.BOTH) : manager.reloadPattern(Types.Filters.GLOBAL), Types.Filters.DEBUG)));
                            break;
                        case "whitelist":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            if (!plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
                                throw new CommandDisabledException(plugin.getLanguage());
                            }
                            if (finalArgs.size() != 2) {
                                throw new IllegalArgumentException(plugin.getLanguage());
                            }
                            String white = finalArgs.get(1).toLowerCase();
                            local = Manager.FileManager.getFile(plugin, "data/whitelist");
                            List<String> whitelist = local.getStringList("whitelist");
                            if (manager.supported("mysql")) {
                                if (!connector.isWorking())
                                    plugin.getHikariCP().reload();
                                boolean sqlexists = false;
                                try {

                                    PreparedStatement ps = connection.prepareStatement(HikariCP.Query.WHITELIST.exists);
                                    ps.setString(1, white);
                                    if (ps.executeQuery().next()) {
                                        sqlexists = true;
                                    }
                                    ps.close();
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                    sqlexists = false;
                                }
                                if (sqlexists && whitelist.contains(white)) {
                                    whitelist.remove(white);
                                    try (
                                            PreparedStatement ps = connection.prepareStatement(HikariCP.Query.WHITELIST.delete)) {
                                        ps.setString(1, white);
                                        ps.execute();
                                        ps.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                } else {
                                    if (!whitelist.contains(white))
                                        whitelist.add(white);
                                    if (!sqlexists) {
                                        try (
                                                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.WHITELIST.insert)) {
                                            ps.setString(1, white);
                                            ps.execute();
                                            ps.close();
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            } else {
                                if (whitelist.contains(white)) {
                                    whitelist.remove(white);
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                } else {
                                    if (!whitelist.contains(white))
                                        whitelist.add(white);
                                    manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            }
                            local.set("whitelist", whitelist);
                            Manager.FileManager.saveFile(plugin, local, "data/whitelist");
                            break;
                        case "unset":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            if (finalArgs.size() == 1) {
                                throw new IllegalArgumentException(plugin.getLanguage());
                            }
                            UUID targetid = null;
                            if (manager.supported("mysql"))
                                for (final String key : manager.getUsers(false)) {
                                    User user = new User(manager, UUID.fromString(key));
                                    if (user.exists()) {
                                        if (user.playerName().equalsIgnoreCase(finalArgs.get(1))) {
                                            targetid = UUID.fromString(key);
                                        } else if (user.getId().toString().equalsIgnoreCase(finalArgs.get(1))) {
                                            targetid = UUID.fromString(key);
                                        }
                                    }
                                }
                            if (targetid == null) {
                                throw new FailureException(plugin.getLanguage(),
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                            } else {
                                try (
                                        PreparedStatement ps = manager.getUserConnection().prepareStatement(HikariCP.Query.USERS.delete)) {
                                    ps.setString(1, targetid.toString());
                                    ps.execute();
                                    ps.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                if (manager.supported("mysql"))
                                    try (
                                            PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.delete)) {
                                        ps.setString(1, targetid.toString());
                                        ps.execute();
                                        ps.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                            }
                            if (!new User(manager, targetid).exists()) {
                                manager.send(sender,
                                        Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).
                                                replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed_player"))).
                                                replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                            } else {
                                throw new FailureException(plugin.getLanguage(),
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                            }
                            break;
                        case "toggle":
                            boolean value;
                            if (finalArgs.size() == 1 && (sender instanceof Player)) {
                                User user = new User(manager, ((Player) sender).getUniqueId());
                                if (!user.exists()) {
                                    user.create(sender.getName(), plugin.getConfig().getBoolean("settings.filtering.default"));
                                }
                                if (!sender.hasPermission("MCSF.toggle")) {
                                    throw new NoPermissionException(plugin.getLanguage());
                                }
                                if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                    throw new CommandDisabledException(plugin.getLanguage());
                                }
                                if (sender.hasPermission("MCSF.bypass")) {
                                    value = user.toggle();
                                    if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                        manager.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").
                                                replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                    manager.send(sender, plugin.getLanguage().getString("variables.toggle")
                                            .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));

                                    break;
                                }
                                value = user.toggle();
                                manager.send(sender, plugin.getLanguage().getString("variables.toggle")
                                        .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                    manager.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle")
                                            .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                            } else {
                                if (finalArgs.size() != 2) {
                                    throw new FailureException(plugin.getLanguage(),
                                            plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                                }
                                if (!sender.hasPermission("MCSF.modify")) {
                                    throw new NoPermissionException(plugin.getLanguage());
                                } else if (sender.hasPermission("MCSF.bypass")) {
                                    targetid = null;
                                    for (final String key : manager.getUsers(false)) {
                                        User user = new User(manager, UUID.fromString(key));
                                        if (user.exists()) {
                                            if (user.playerName().equalsIgnoreCase(finalArgs.get(1))) {
                                                targetid = UUID.fromString(key);
                                            } else if (user.getId().toString().equalsIgnoreCase(finalArgs.get(1))) {
                                                targetid = UUID.fromString(key);
                                            }
                                        }
                                    }
                                    if (targetid == null) {
                                        throw new FailureException(plugin.getLanguage(),
                                                plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));

                                    } else {
                                        User user = new User(manager, targetid);
                                        value = user.toggle();
                                        if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                            manager.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle")
                                                    .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                        manager.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                    }
                                    break;
                                }
                                if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                    throw new CommandDisabledException(plugin.getLanguage());
                                }
                                targetid = null;
                                for (final String key : manager.getUsers(false)) {
                                    User user = new User(manager, UUID.fromString(key));
                                    if (user.exists()) {
                                        if (user.playerName().equalsIgnoreCase(finalArgs.get(1))) {
                                            targetid = UUID.fromString(key);
                                        } else if (user.getId().toString().equalsIgnoreCase(finalArgs.get(1))) {
                                            targetid = UUID.fromString(key);
                                        }
                                    }
                                }
                                if (targetid == null) {
                                    throw new FailureException(plugin.getLanguage(),
                                            plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));

                                } else {
                                    User user = new User(manager, targetid);
                                    user.toggle();
                                    if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                        manager.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{value}|(?i)%value%", user.status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                    manager.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)).replaceAll("(?i)\\{value}|(?i)%value%", (new User(manager, targetid).status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                }
                            }
                            break;
                        case "status":
                            if (finalArgs.size() == 1 && (sender instanceof Player)) {
                                User user = new User(manager, ((Player) sender).getUniqueId());
                                if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                    value = true;
                                } else {
                                    value = user.status();
                                }
                                manager.send(sender, plugin.getLanguage().getString("variables.status").replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                break;
                            } else {
                                if (finalArgs.size() != 2) {
                                    throw new FailureException(plugin.getLanguage(),
                                            plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));

                                }
                                if (!sender.hasPermission("MCSF.status")) {
                                    throw new NoPermissionException(plugin.getLanguage());

                                }
                                targetid = null;
                                for (final String key : manager.getUsers(false)) {
                                    User user = new User(manager, UUID.fromString(key));
                                    if (user.exists()) {
                                        if (user.playerName().equalsIgnoreCase(finalArgs.get(1))) {
                                            targetid = UUID.fromString(key);
                                        } else if (user.getId().toString().equalsIgnoreCase(finalArgs.get(1))) {
                                            targetid = UUID.fromString(key);
                                        }
                                    }
                                }
                                if (targetid == null) {
                                    throw new FailureException(plugin.getLanguage(),
                                            plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));

                                } else {
                                    User user = new User(manager, targetid);
                                    if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                        value = true;
                                    } else {
                                        value = user.status();
                                    }
                                    manager.send(sender, plugin.getLanguage().getString("variables.status").replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                }
                            }
                            break;
                        case "reset":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());

                            }
                            if (!manager.supported("mysql")) {
                                throw new CommandDisabledException(plugin.getLanguage());

                            }
                            if (finalArgs.size() != 2) {
                                manager.send(sender, plugin.getLanguage().getString("variables.reset"));

                            } else {
                                if (finalArgs.get(1).equalsIgnoreCase("confirm")) {
                                    try {
                                        manager.createTable(true);
                                    } catch (SQLException throwables) {
                                        throwables.printStackTrace();
                                    }
                                } else {
                                    manager.send(sender, plugin.getLanguage().getString("variables.reset"));
                                    break;
                                }
                            }
                            manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.reset")));
                            break;
                        case "version":
                            if (sender.hasPermission("MCSF.version")) {
                                StringBuilder version = new StringBuilder();
                                int length = plugin.getLanguage().getStringList("variables.version").size();
                                for (String str : plugin.getLanguage().getStringList("variables.version")) {
                                    length = length - 1;
                                    if (length <= 0) { // length is the end
                                        version.append(str);
                                    } else { // length is not the end
                                        version.append(str).append("\n");
                                    }
                                }
                                manager.send(sender, version.toString());
                            } else {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            break;
                        case "add":
                        case "remove":
                            if (sender.hasPermission("MCSF.modify")) {
                                if (finalArgs.size() != 2) {
                                    throw new IllegalArgumentException(plugin.getLanguage());
                                }
                                if (manager.supported("mysql")) {
                                    manager.setTable("swears");
                                    manager.setTable("whitelist");
                                    manager.setTable("global");
                                }
                                String word = finalArgs.get(1).toLowerCase();
                                if (word.startsWith("regex:")) {
                                    try {
                                        Pattern.compile(word.replaceAll("regex:", ""));
                                    } catch (Exception e) {
                                        manager.send(sender, new FailureException(plugin.getLanguage(), "The provided regex is invalid.").getMessage());
                                        break;
                                    }
                                }
                                local = Manager.FileManager.getFile(plugin, "data/swears");
                                List<String> swears = local.getStringList("swears");
                                switch (finalArgs.get(0)) {
                                    case "add":
                                        if (manager.supported("mysql")) {
                                            if (!connector.isWorking())
                                                plugin.getHikariCP().reload();
                                            boolean exists = false;
                                            try {
                                                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.SWEARS.exists);
                                                ps.setString(1, word);
                                                if (ps.executeQuery().next()) {
                                                    exists = true;
                                                }
                                                ps.close();
                                            } catch (SQLException throwables) {
                                                throwables.printStackTrace();
                                                exists = false;
                                            }
                                            if (exists) {
                                                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.alreadyexists"));

                                            }
                                            if (!swears.contains(word))
                                                swears.add(word);

                                            try {

                                                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.SWEARS.insert);
                                                ps.setString(1, word);
                                                ps.execute();
                                                ps.close();
                                            } catch (SQLException throwables) {
                                                throwables.printStackTrace();
                                            }
                                            manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));

                                        } else {
                                            if (!swears.contains(word)) {
                                                swears.add(word);
                                                manager.debug(sender.getName() + " has added `" + word + "` to the config");
                                                manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                            } else {
                                                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.alreadyexists"));
                                            }
                                        }
                                        break;
                                    case "remove":
                                        if (manager.supported("mysql")) {
                                            if (!connector.isWorking())
                                                plugin.getHikariCP().reload();
                                            boolean exists = false;
                                            try {

                                                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.SWEARS.exists);
                                                ps.setString(1, word);
                                                if (ps.executeQuery().next()) {
                                                    exists = true;
                                                }
                                                ps.close();
                                            } catch (SQLException throwables) {
                                                throwables.printStackTrace();
                                                exists = false;
                                            }
                                            if (!exists) {
                                                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.doesntexist"));

                                            }
                                            swears.remove(word);
                                            try {

                                                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.SWEARS.delete);
                                                ps.setString(1, word);
                                                ps.execute();
                                                ps.close();
                                            } catch (SQLException throwables) {
                                                throwables.printStackTrace();
                                            }
                                            manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                        } else {
                                            boolean modified = swears.remove(word);
                                            if (modified) {
                                                manager.debug(sender.getName() + " has removed `" + word + "` from config");
                                                manager.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                            } else {
                                                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.doesntexist"));
                                            }
                                        }
                                        break;
                                    default:
                                        throw new IllegalArgumentException(plugin.getLanguage());
                                }
                                local = Manager.FileManager.getFile(plugin, "data/swears");
                                local.set("swears", swears);
                                Manager.FileManager.saveFile(plugin, local, "data/swears");
                            } else {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            break;
                    }
                } else {
                    manager.showHelp(sender);
                }
            } catch (CommandDisabledException | NoPermissionException | IllegalArgumentException | FailureException ex) {
                manager.send(sender, ex.getMessage());
            }
        });
    }
}
