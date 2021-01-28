package io.github.jochyoua.mychristianswearfilter.events;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.HikariCP.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.HikariCP.HikariCP;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.Utils;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.IllegalArgumentException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandEvents {
    MCSF plugin;
    DatabaseConnector connector;
    Connection connection;
    Utils utils;


    public CommandEvents(Utils utils) {
        this.plugin = utils.getProvider();
        if (utils.supported("mysql")) {
            this.connector = utils.getConnector();
            this.connection = utils.getConnection();
        }
        this.utils = utils;
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
                                List<String> swears = utils.getSwears();
                                StringUtil.copyPartialMatches(args[1], swears, completions);
                                break;
                            case "reset":
                                StringUtil.copyPartialMatches(args[1], Collections.singletonList("confirm"), completions);
                                break;
                            case "whitelist":
                                List<String> white = utils.getWhitelist();
                                StringUtil.copyPartialMatches(args[1], white, completions);
                                break;
                            case "global":
                                List<String> global = utils.getGlobal();
                                StringUtil.copyPartialMatches(args[1], global, completions);
                                break;
                            case "status":
                            case "toggle":
                            case "unset":
                                List<String> users = utils.getUsers(true);
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
                            if (!utils.getCooldowns().containsKey(player.getUniqueId())) {
                                utils.addUser(player.getUniqueId());
                            } else {
                                long duration = utils.getCooldowns().getExpectedExpiration(player.getUniqueId());
                                duration = TimeUnit.MILLISECONDS.toSeconds(duration);
                                if (duration != 0) {
                                    utils.send(player, plugin.getLanguage().getString("variables.cooldown").replaceAll("(?i)\\{duration}|(?i)%duration%", String.valueOf(duration)));
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
            utils.debug("Command failed to execute: " + e.getMessage());
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
                            utils.showHelp(sender);
                            break;
                        case "reload":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            sender.sendMessage("Reload succeeded!");
                            if (utils.supported("mysql")) {
                                sender.sendMessage("Reloading databases:");
                                try {
                                    if (!plugin.reloadSQL())
                                        throw new Exception("Database was reloaded incorrectly");
                                    sender.sendMessage("Successfully reloaded database information!");
                                } catch (Exception e) {
                                    sender.sendMessage("Failed to reload database: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            sender.sendMessage("Resetting Swears, Global swears and whitelist:");
                            try {
                                utils.reloadPattern();
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
                            utils.setTable("global");
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            if (finalArgs.size() != 2) {
                                throw new IllegalArgumentException(plugin.getLanguage());
                            }
                            String glo = finalArgs.get(1).toLowerCase();
                            FileConfiguration local = plugin.getFile("data/global");
                            List<String> global = local.getStringList("global");
                            if (utils.supported("mysql")) {
                                if (!connector.isWorking())
                                    plugin.reloadSQL();
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
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
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
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            } else {
                                if (global.contains(glo)) {
                                    global.remove(glo);
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                } else {
                                    if (!global.contains(glo))
                                        global.add(glo);
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            }
                            local = plugin.getFile("data/global");
                            local.set("global", global);
                            plugin.saveFile(local, "data/global");
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
                            utils.reloadPattern();
                            utils.send(sender, plugin.getLanguage().getString("variables.parse").replaceAll("(?i)\\{message}|(?i)%message%", utils.clean(message.toString(), false, false, state ? utils.getBoth() : utils.getGlobalRegex(), Types.Filters.DEBUG)));
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
                            local = plugin.getFile("data/whitelist");
                            List<String> whitelist = local.getStringList("whitelist");
                            if (utils.supported("mysql")) {
                                if (!connector.isWorking())
                                    plugin.reloadSQL();
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
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
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
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            } else {
                                if (whitelist.contains(white)) {
                                    whitelist.remove(white);
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                } else {
                                    if (!whitelist.contains(white))
                                        whitelist.add(white);
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            }
                            local.set("whitelist", whitelist);
                            plugin.saveFile(local, "data/whitelist");
                            break;
                        case "unset":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            if (finalArgs.size() == 1) {
                                throw new IllegalArgumentException(plugin.getLanguage());
                            }
                            UUID targetid = null;
                            if (utils.supported("mysql"))
                                for (final String key : utils.getUsers(false)) {
                                    User user = new User(utils, UUID.fromString(key));
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
                                        PreparedStatement ps = utils.getUserConnection().prepareStatement(HikariCP.Query.USERS.delete)) {
                                    ps.setString(1, targetid.toString());
                                    ps.execute();
                                    ps.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                if (utils.supported("mysql"))
                                    try (
                                            PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.delete)) {
                                        ps.setString(1, targetid.toString());
                                        ps.execute();
                                        ps.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                            }
                            if (!new User(utils, targetid).exists()) {
                                utils.send(sender,
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
                                User user = new User(utils, ((Player) sender).getUniqueId());
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
                                        utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").
                                                replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                    utils.send(sender, plugin.getLanguage().getString("variables.toggle")
                                            .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));

                                    break;
                                }
                                value = user.toggle();
                                utils.send(sender, plugin.getLanguage().getString("variables.toggle")
                                        .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                    utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle")
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
                                    for (final String key : utils.getUsers(false)) {
                                        User user = new User(utils, UUID.fromString(key));
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
                                        User user = new User(utils, targetid);
                                        value = user.toggle();
                                        if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                            utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle")
                                                    .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                        utils.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                    }
                                    break;
                                }
                                if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                    throw new CommandDisabledException(plugin.getLanguage());
                                }
                                targetid = null;
                                for (final String key : utils.getUsers(false)) {
                                    User user = new User(utils, UUID.fromString(key));
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
                                    User user = new User(utils, targetid);
                                    user.toggle();
                                    if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                        utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{value}|(?i)%value%", user.status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                                    utils.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)).replaceAll("(?i)\\{value}|(?i)%value%", (new User(utils, targetid).status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                }
                            }
                            break;
                        case "status":
                            if (finalArgs.size() == 1 && (sender instanceof Player)) {
                                User user = new User(utils, ((Player) sender).getUniqueId());
                                if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                    value = true;
                                } else {
                                    value = user.status();
                                }
                                utils.send(sender, plugin.getLanguage().getString("variables.status").replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
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
                                for (final String key : utils.getUsers(false)) {
                                    User user = new User(utils, UUID.fromString(key));
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
                                    User user = new User(utils, targetid);
                                    if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                        value = true;
                                    } else {
                                        value = user.status();
                                    }
                                    utils.send(sender, plugin.getLanguage().getString("variables.status").replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                }
                            }
                            break;
                        case "reset":
                            if (!sender.hasPermission("MCSF.modify")) {
                                throw new NoPermissionException(plugin.getLanguage());

                            }
                            if (!utils.supported("mysql")) {
                                throw new CommandDisabledException(plugin.getLanguage());

                            }
                            if (finalArgs.size() != 2) {
                                utils.send(sender, plugin.getLanguage().getString("variables.reset"));

                            } else {
                                if (finalArgs.get(1).equalsIgnoreCase("confirm")) {
                                    try {
                                        utils.createTable(true);
                                    } catch (SQLException throwables) {
                                        throwables.printStackTrace();
                                    }
                                } else {
                                    utils.send(sender, plugin.getLanguage().getString("variables.reset"));
                                    break;
                                }
                            }
                            utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.reset")));
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
                                utils.send(sender, version.toString());
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
                                if (utils.supported("mysql")) {
                                    utils.setTable("swears");
                                    utils.setTable("whitelist");
                                    utils.setTable("global");
                                }
                                String word = finalArgs.get(1).toLowerCase();
                                local = plugin.getFile("data/swears");
                                List<String> swears = local.getStringList("swears");
                                switch (finalArgs.get(0)) {
                                    case "add":
                                        if (utils.supported("mysql")) {
                                            if (!connector.isWorking())
                                                plugin.reloadSQL();
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
                                            utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));

                                        } else {
                                            if (!swears.contains(word)) {
                                                swears.add(word);
                                                utils.debug(sender.getName() + " has added `" + word + "` to the config");
                                                utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                            } else {
                                                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.alreadyexists"));
                                            }
                                        }
                                        break;
                                    case "remove":
                                        if (utils.supported("mysql")) {
                                            if (!connector.isWorking())
                                                plugin.reloadSQL();
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
                                            utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                        } else {
                                            boolean modified = swears.remove(word);
                                            if (modified) {
                                                utils.debug(sender.getName() + " has removed `" + word + "` from config");
                                                utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                            } else {
                                                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.doesntexist"));
                                            }
                                        }
                                        break;
                                    default:
                                        throw new IllegalArgumentException(plugin.getLanguage());
                                }
                                local = plugin.getFile("data/swears");
                                local.set("swears", swears);
                                plugin.saveFile(local, "data/swears");
                            } else {
                                throw new NoPermissionException(plugin.getLanguage());
                            }
                            break;
                    }
                } else {
                    utils.showHelp(sender);
                }
            } catch (CommandDisabledException | NoPermissionException | IllegalArgumentException | FailureException ex) {
                utils.send(sender, ex.getMessage());
            }
        });
    }
}
