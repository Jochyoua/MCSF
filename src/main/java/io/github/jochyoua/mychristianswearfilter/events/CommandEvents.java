package io.github.jochyoua.mychristianswearfilter.events;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.HikariCP.DatabaseConnector;
import io.github.jochyoua.mychristianswearfilter.shared.HikariCP.HikariCP;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.Utils;
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


    public CommandEvents(MCSF plugin, DatabaseConnector connector, Utils utils) {
        this.plugin = plugin;
        this.connector = connector;
        if (plugin.getConfig().getBoolean("mysql.enabled"))
        try {
            this.connection = connector.getConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
                                List<String> users = utils.getUsers();
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
            if (!sender.hasPermission("MCSF.use")) {
                utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                return;
            }
            if (!finalArgs.isEmpty()) {
                if (plugin.getConfig().isSet("settings.command args enabled." + finalArgs.get(0))) {
                    if (!plugin.getConfig().getBoolean("settings.command args enabled." + finalArgs.get(0))) {
                        utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                        return;
                    }
                }
                switch (finalArgs.get(0).toLowerCase()) {
                    case "help":
                    default:
                        if (!plugin.getConfig().getBoolean("settings.command args enabled.help")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                            break;
                        }
                        utils.showHelp(sender);
                        break;
                    case "reload":
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
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
                            plugin.reloadConfig();
                            sender.sendMessage("Successfully reloaded configuration information!");
                        } catch (Exception e) {
                            sender.sendMessage("Failed to reload configuration data: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "global":
                        utils.setTable("global");
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
                        }
                        if (finalArgs.size() != 2) {
                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.incorrectargs")));
                            break;
                        }
                        String glo = finalArgs.get(1).toLowerCase();
                        FileConfiguration local = plugin.getFile("global");
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
                        local = plugin.getFile("global");
                        local.set("global", global);
                        plugin.saveFile(local, "global");
                        break;
                    case "parse":
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
                        }
                        if (args.size() == 1 || args.size() == 2) {
                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.incorrectargs")));
                            return;
                        }
                        boolean state;
                        try {
                            switch(args.get(1)){
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
                                    throw new IllegalArgumentException(plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.invalidtype").replaceAll("(?i)\\{arg}|(?i)%arg%", args.get(1)).replaceAll("(?i)\\{type}|(?i)%type%", "boolean")));
                            }
                        } catch (IllegalArgumentException e) {
                            utils.send(sender, e.getMessage());
                            return;
                        }
                        StringBuilder message = new StringBuilder();
                        for (int i = 2; i < args.size(); i++) {
                            String arg = args.get(i) + " ";
                            message.append(arg);
                        }
                        utils.reloadPattern();
                        if (state)
                            utils.send(sender, utils.clean(message.toString(), false, false, utils.getGlobalRegex(), Types.Filters.DEBUG));
                        else {
                            utils.send(sender, utils.clean(message.toString(), false, false, utils.getBoth(), Types.Filters.DEBUG));
                        }
                        break;
                    case "whitelist":
                        if (!plugin.getConfig().getBoolean("settings.filtering.whitelist words")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                            break;
                        }
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
                        }
                        if (finalArgs.size() != 2) {
                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.incorrectargs")));
                            break;
                        }
                        String white = finalArgs.get(1).toLowerCase();
                        local = plugin.getFile("whitelist");
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
                        plugin.saveFile(local, "whitelist");
                        break;
                    case "unset":
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
                        }
                        if (finalArgs.size() == 1) {
                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.error.incorrectargs"))));
                            break;
                        }
                        if (finalArgs.get(1).equalsIgnoreCase("all")) {
                            plugin.getConfig().set("users", null);
                            plugin.getConfig().set("users.069a79f4-44e9-4726-a5be-fca90e38aaf5.enabled", true);
                            plugin.getConfig().set("users.069a79f4-44e9-4726-a5be-fca90e38aaf5.playername", "Notch");
                            plugin.saveConfig();
                            try (
                                    PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.reset)) {
                                ps.execute();
                                ps.close();
                                utils.createTable(false);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            utils.send(sender,
                                    Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).
                                            replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed_players"))));
                        } else {
                            UUID targetid = null;
                            for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                                if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(finalArgs.get(1))) {
                                    targetid = UUID.fromString(key);
                                } else if (key.equalsIgnoreCase(finalArgs.get(1))) {
                                    targetid = UUID.fromString(key);
                                }
                            }
                            if (targetid == null) {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%",
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1))));
                                break;
                            } else {
                                plugin.getConfig().set("users." + targetid, null);
                                plugin.saveConfig();
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
                            if (!plugin.getConfig().isSet("users." + targetid)) {
                                utils.send(sender,
                                        Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).
                                                replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed_player"))).
                                                replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                            } else {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%",
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1))));
                            }
                        }
                        break;
                    case "toggle":
                        boolean value;
                        if (finalArgs.size() == 1 && (sender instanceof Player)) {
                            if (!sender.hasPermission("MCSF.toggle")) {
                                utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                                break;
                            }
                            if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                                break;
                            }
                            if (sender.hasPermission("MCSF.bypass")) {
                                value = utils.toggle(((Player) sender).getUniqueId());
                                if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                    utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").
                                            replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));
                                utils.send(sender, plugin.getLanguage().getString("variables.toggle")
                                        .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));

                                break;
                            }
                            value = utils.toggle(((Player) sender).getUniqueId());
                            utils.send(sender, plugin.getLanguage().getString("variables.toggle")
                                    .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));
                            if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle")
                                        .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));
                        } else {
                            if (finalArgs.size() != 2) {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%",
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", sender.getName())));
                                break;
                            }
                            if (!sender.hasPermission("MCSF.modify")) {
                                utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                                break;
                            } else if (sender.hasPermission("MCSF.bypass")) {
                                UUID targetid = null;
                                for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                                    if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(finalArgs.get(1))) {
                                        targetid = UUID.fromString(key);
                                    } else if (key.equalsIgnoreCase(finalArgs.get(1))) {
                                        targetid = UUID.fromString(key);
                                    }
                                }
                                if (targetid == null) {
                                    utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%",
                                            plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1))));
                                    break;
                                } else {
                                    value = utils.toggle(targetid);
                                    if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                        utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle")
                                                .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                                    utils.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                }
                                break;
                            }
                            if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                                break;
                            }
                            UUID targetid = null;
                            for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                                if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(finalArgs.get(1))) {
                                    targetid = UUID.fromString(key);
                                }
                            }
                            if (targetid == null) {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%",
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1))));
                                break;
                            } else {
                                utils.toggle(targetid);
                                if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                                    utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{value}|(?i)%value%", utils.status(targetid) ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                                utils.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)).replaceAll("(?i)\\{value}|(?i)%value%", (utils.status(targetid) ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                            }
                        }
                        break;
                    case "status":
                        if (finalArgs.size() == 1 && (sender instanceof Player)) {
                            if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                value = true;
                            } else {
                                value = utils.status(((Player) sender).getUniqueId());
                            }
                            utils.send(sender, plugin.getLanguage().getString("variables.status").replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                            break;
                        } else {
                            if (finalArgs.size() != 2) {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%",
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", sender.getName())));
                                break;
                            }
                            if (!sender.hasPermission("MCSF.status")) {
                                utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                                break;
                            }
                            UUID targetid = null;
                            for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                                if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(finalArgs.get(1))) {
                                    targetid = UUID.fromString(key);
                                } else if (key.equalsIgnoreCase(finalArgs.get(1))) {
                                    targetid = UUID.fromString(key);
                                }
                            }
                            if (targetid == null) {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%",
                                        plugin.getLanguage().getString("variables.error.invalidtarget").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1))));
                                break;
                            } else {
                                if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                                    value = true;
                                } else {
                                    value = utils.status(targetid);
                                }
                                utils.send(sender, plugin.getLanguage().getString("variables.status").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                            }
                        }
                        break;
                    case "reset":
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
                        }
                        if (!utils.supported("mysql")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                            break;
                        }
                        if (finalArgs.size() != 2) {
                            utils.send(sender, plugin.getLanguage().getString("variables.reset"));
                            break;
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
                            for (String str : plugin.getLanguage().getStringList("variables.version")) {
                                utils.send(sender, str);
                            }
                        } else {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                        }
                        break;
                    case "add":
                    case "remove":
                        if (sender.hasPermission("MCSF.modify")) {
                            if (finalArgs.size() != 2) {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.incorrectargs")));
                                break;
                            }
                            if (utils.supported("mysql")) {
                                utils.setTable("swears");
                                utils.setTable("whitelist");
                                utils.setTable("global");
                            }
                            String word = finalArgs.get(1).toLowerCase();
                            local = plugin.getFile("swears");
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
                                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.alreadyexists")));
                                            break;
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
                                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.alreadyexists")));
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
                                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.doesntexist")));
                                            break;
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
                                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.doesntexist")));
                                        }
                                    }
                                    break;
                                default:
                                    utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%value%", plugin.getLanguage().getString("variables.error.incorrectargs")));
                                    break;
                            }
                            local = plugin.getFile("swears");
                            local.set("swears", swears);
                            plugin.saveFile(local, "swears");
                        } else {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                        }
                        break;
                }
            } else {
                utils.showHelp(sender);
            }
        });
    }
}
