package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.shared.MySQL;
import com.github.Jochyoua.MCSF.shared.Types;
import com.github.Jochyoua.MCSF.shared.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandEvents {
    MCSF plugin;
    MySQL MySQL;
    Utils utils;

    public CommandEvents(MCSF plugin, MySQL mysql, Utils utils) {
        this.plugin = plugin;
        this.MySQL = mysql;
        this.utils = utils;
        //plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
                            if (!utils.getAll().containsKey(player.getUniqueId())) {
                                utils.setUser(player.getUniqueId(), plugin.getConfig().getInt("settings.cooldown"));
                            } else if (!(utils.getAll().get(player.getUniqueId()) <= 0)) {
                                utils.send(player, plugin.getLanguage().getString("variables.cooldown").replaceAll("(?i)\\{duration}|(?i)%duration%", String.valueOf(utils.getAll().get(player.getUniqueId()))));
                                return true;
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
            plugin.reloadConfig();
            if (!finalArgs.isEmpty()) {
                if (plugin.getConfig().isSet("settings.command_args_enabled." + finalArgs.get(0))) {
                    if (!plugin.getConfig().getBoolean("settings.command_args_enabled." + finalArgs.get(0))) {
                        utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                        return;
                    }
                }
                switch (finalArgs.get(0).toLowerCase()) {
                    case "help":
                    default:
                        if (!plugin.getConfig().getBoolean("settings.command_args_enabled.help")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                            break;
                        }
                        utils.showHelp(sender);
                        break;
                    case "whitelist":
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
                        }
                        if (finalArgs.size() != 2) {
                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.incorrectargs")));
                            break;
                        }
                        String white = finalArgs.get(1).toLowerCase();
                        List<String> whitelist = plugin.getConfig().getStringList("whitelist");
                        if (utils.supported("mysql")) {
                            if (!MySQL.isConnected())
                                MySQL.connect();
                            if (MySQL.isConnected()) {
                                boolean sqlexists = MySQL.exists("word", white, "whitelist");
                                if (sqlexists && whitelist.contains(white)) {
                                    whitelist.remove(white);
                                    MySQL.stateRemove(white);
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                } else {
                                    if (!whitelist.contains(white))
                                        whitelist.add(white);
                                    if (!sqlexists)
                                        MySQL.whiteInsert(white);
                                    utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                }
                            } else {
                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.failedtoconnect")));
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
                        if (!whitelist.isEmpty()) {
                            plugin.getConfig().set("whitelist", whitelist);
                            plugin.saveConfig();
                        }
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
                            MySQL.update("DROP TABLE users;");
                            utils.createTable(false);
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
                                    MySQL.stateRemovePlayer(targetid);
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
                            if (plugin.getConfig().getBoolean("settings.force")) {
                                utils.send(sender, plugin.getLanguage().getString("variables.disabled"));
                                break;
                            }
                            if (sender.hasPermission("MCSF.bypass")) {
                                value = utils.toggle(((Player) sender).getUniqueId());
                                if (plugin.getConfig().getBoolean("settings.log"))
                                    utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").
                                            replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));
                                utils.send(sender, plugin.getLanguage().getString("variables.toggle")
                                        .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));

                                break;
                            }
                            value = utils.toggle(((Player) sender).getUniqueId());
                            utils.send(sender, plugin.getLanguage().getString("variables.toggle")
                                    .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));
                            if (plugin.getConfig().getBoolean("settings.log"))
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
                                    if (plugin.getConfig().getBoolean("settings.log"))
                                        utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle")
                                                .replaceAll("(?i)\\{value}|(?i)%value%", value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                                    utils.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)).replaceAll("(?i)\\{value}|(?i)%value%", (value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                                }
                                break;
                            }
                            if (plugin.getConfig().getBoolean("settings.force")) {
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
                                if (plugin.getConfig().getBoolean("settings.log"))
                                    utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{value}|(?i)%value%", utils.status(targetid) ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")).replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)));
                                utils.send(sender, plugin.getLanguage().getString("variables.targetToggle").replaceAll("(?i)\\{target}|(?i)%target%", finalArgs.get(1)).replaceAll("(?i)\\{value}|(?i)%value%", (utils.status(targetid) ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))));
                            }
                        }
                        break;
                    case "reload":
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getLanguage().getString("variables.noperm"));
                            break;
                        }
                        utils.reload();
                        plugin.reloadLanguage();
                        utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.reloaded")));
                        break;
                    case "status":
                        if (finalArgs.size() == 1 && (sender instanceof Player)) {
                            if (plugin.getConfig().getBoolean("settings.force")) {
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
                                if (plugin.getConfig().getBoolean("settings.force")) {
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
                                utils.createTable(true);
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
                            String word = finalArgs.get(1).toLowerCase();
                            List<String> swears = plugin.getConfig().getStringList("swears");
                            switch (finalArgs.get(0)) {
                                case "add":
                                    if (utils.supported("mysql")) {
                                        if (!MySQL.isConnected())
                                            MySQL.connect();
                                        if (MySQL.isConnected()) {
                                            if (MySQL.exists("word", word, "swears")) {
                                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.alreadyexists")));
                                                break;
                                            }
                                            if (!swears.contains(word))
                                                swears.add(word);
                                            MySQL.stateInsert(word);
                                            utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.added")));
                                        } else {
                                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.failedtoconnect")));
                                        }
                                    } else {
                                        boolean modified = false;
                                        if (!swears.contains(word))
                                            modified = true;
                                        if (modified) {
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
                                        if (!MySQL.isConnected())
                                            MySQL.connect();
                                        if (MySQL.isConnected()) {
                                            if (!MySQL.exists("word", word, "swears")) {
                                                utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.doesntexist")));
                                                break;
                                            }
                                            swears.remove(word);
                                            MySQL.stateRemove(word);
                                            utils.send(sender, plugin.getLanguage().getString("variables.success").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.successful.removed")));
                                        } else {
                                            utils.send(sender, plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", plugin.getLanguage().getString("variables.error.failedtoconnect")));
                                        }
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
                            plugin.getConfig().set("swears", swears);
                            plugin.saveConfig();
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
