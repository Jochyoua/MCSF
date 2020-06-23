package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import me.vagdedes.mysql.database.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.*;
import java.util.stream.Collectors;

public class CommandEvents implements Listener {
    Main plugin;
    MySQL MySQL;
    Utils utils;

    public CommandEvents(Main plugin, MySQL mysql, Utils utils) {
        this.plugin = plugin;
        this.MySQL = mysql;
        this.utils = utils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void serverCommand(ServerCommandEvent e) {
        String command = e.getCommand().split(" ")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (command.equalsIgnoreCase(plugin.getConfig().getString("variables.command")) || command.equalsIgnoreCase("mcsf")) {
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
        if (command.equalsIgnoreCase(plugin.getConfig().getString("variables.command")) || command.equalsIgnoreCase("mcsf")) {
            e.setCancelled(true);
            Player sender = e.getPlayer();
            List<String> args = new LinkedList<>(Arrays.asList(e.getMessage().split(" ")));
            args.remove(0);
            if (!(plugin.getConfig().getInt("settings.cooldown") <= 0) && !sender.hasPermission("MCSF.bypass")) {
                if (!utils.getAll().containsKey(sender.getUniqueId())) {
                    utils.setUser(sender.getUniqueId(), plugin.getConfig().getInt("settings.cooldown"));
                } else if (!(utils.getAll().get(sender.getUniqueId()) <= 0)){
                    utils.send(sender, plugin.getConfig().getString("variables.cooldown").replace("%duration%", String.valueOf(utils.getAll().get(sender.getUniqueId()))));
                    return;
                }
            }
            registerCommand(sender, args);
        }
    }

    private void registerCommand(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("MCSF.use")) {
            utils.send(sender, plugin.getConfig().getString("variables.noperm"));
            return;
        }
        args = args.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        plugin.reloadConfig();
        if (!args.isEmpty()) {
            if (plugin.getConfig().isSet("settings.command_args_enabled." + args.get(0))) {
                if (!plugin.getConfig().getBoolean("settings.command_args_enabled." + args.get(0))) {
                    utils.send(sender, plugin.getConfig().getString("variables.disabled"));
                    return;
                }
            }
            switch (args.get(0).toLowerCase()) {
                case "help":
                default:
                    if (!plugin.getConfig().getBoolean("settings.command_args_enabled.help")) {
                        utils.send(sender, plugin.getConfig().getString("variables.disabled"));
                        break;
                    }
                    utils.showHelp(sender);
                    break;
                case "unset":
                    if (!sender.hasPermission("MCSF.modify")) {
                        utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                        break;
                    }
                    if (args.size() == 1) {
                        utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", Objects.requireNonNull(plugin.getConfig().getString("variables.error.incorrectargs"))));
                        break;
                    }
                    if (args.get(1).equalsIgnoreCase("all")) {
                        plugin.getConfig().set("users", null);
                        plugin.getConfig().set("users.069a79f4-44e9-4726-a5be-fca90e38aaf5.enabled", true);
                        plugin.getConfig().set("users.069a79f4-44e9-4726-a5be-fca90e38aaf5.playername", "Notch");
                        plugin.saveConfig();
                        MySQL.update("DROP TABLE users;");
                        utils.send(Bukkit.getConsoleSender(),
                                Objects.requireNonNull(plugin.getConfig().getString("variables.success")).
                                        replace("%message%", Objects.requireNonNull(plugin.getConfig().getString("variables.successful.removed_players"))));
                    } else {
                        UUID targetid = null;
                        for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                            if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(args.get(1))) {
                                targetid = UUID.fromString(key);
                            } else if (key.equalsIgnoreCase(args.get(1))) {
                                targetid = UUID.fromString(key);
                            }
                        }
                        if (targetid == null) {
                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%",
                                    plugin.getConfig().getString("variables.error.invalidtarget").replace("%target%", args.get(1))));
                            break;
                        } else {
                            plugin.getConfig().set("users." + targetid, null);
                            plugin.saveConfig();
                            if (utils.supported("mysql"))
                                MySQL.stateRemovePlayer(targetid);
                        }
                        if (!plugin.getConfig().isSet("users." + targetid)) {
                            utils.send(Bukkit.getConsoleSender(),
                                    Objects.requireNonNull(plugin.getConfig().getString("variables.success")).
                                            replace("%message%", Objects.requireNonNull(plugin.getConfig().getString("variables.successful.removed_player"))).
                                            replace("%target%", args.get(1)));
                        } else {
                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%",
                                    plugin.getConfig().getString("variables.error.invalidtarget").replace("%target%", args.get(1))));
                        }
                    }
                    break;
                case "toggle":
                    boolean value;
                    if (args.size() == 1 && (sender instanceof Player)) {
                        if (!sender.hasPermission("MCSF.toggle")) {
                            utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                            break;
                        }
                        if (plugin.getConfig().getBoolean("settings.force")) {
                            utils.send(sender, plugin.getConfig().getString("variables.disabled"));
                            break;
                        }
                        if (sender.hasPermission("MCSF.bypass")) {
                            value = utils.toggle(((Player) sender).getUniqueId());
                            if (plugin.getConfig().getBoolean("settings.log"))
                                utils.send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.targetToggle").
                                        replaceAll("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));
                            utils.send(sender, plugin.getConfig().getString("variables.toggle")
                                    .replace("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));

                            break;
                        }
                        value = utils.toggle(((Player) sender).getUniqueId());
                        utils.send(sender, plugin.getConfig().getString("variables.toggle")
                                .replace("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));
                        if (plugin.getConfig().getBoolean("settings.log"))
                            utils.send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.targetToggle")
                                    .replace("%value%", value ? "enabled" : "disabled").replaceAll("%target%", sender.getName()));
                    } else {
                        if (args.size() != 2) {
                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%",
                                    plugin.getConfig().getString("variables.error.invalidtarget").replace("%target%", sender.getName())));
                            break;
                        }
                        if (!sender.hasPermission("MCSF.modify")) {
                            utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                            break;
                        } else if (sender.hasPermission("MCSF.bypass")) {
                            UUID targetid = null;
                            for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                                if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(args.get(1))) {
                                    targetid = UUID.fromString(key);
                                }
                            }
                            if (targetid == null) {
                                utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%",
                                        plugin.getConfig().getString("variables.error.invalidtarget").replace("%target%", args.get(1))));
                                break;
                            } else {
                                value = utils.toggle(targetid);
                                if (plugin.getConfig().getBoolean("settings.log"))
                                    utils.send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.targetToggle")
                                            .replace("%value%", value ? "enabled" : "disabled").replaceAll("%target%", args.get(1)));
                                utils.send(sender, plugin.getConfig().getString("variables.targetToggle").replace("%target%", args.get(1)).replace("%value%", (value ? "enabled" : "disabled")));
                            }
                            break;
                        }
                        if (plugin.getConfig().getBoolean("settings.force")) {
                            utils.send(sender, plugin.getConfig().getString("variables.disabled"));
                            break;
                        }
                        UUID targetid = null;
                        for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                            if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(args.get(1))) {
                                targetid = UUID.fromString(key);
                            }
                        }
                        if (targetid == null) {
                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%",
                                    plugin.getConfig().getString("variables.error.invalidtarget").replace("%target%", args.get(1))));
                            break;
                        } else {
                            utils.toggle(targetid);
                            if (plugin.getConfig().getBoolean("settings.log"))
                                utils.send(Bukkit.getConsoleSender(), plugin.getConfig().getString("variables.targetToggle").replace("%value%", utils.status(targetid) ? "enabled" : "disabled").replaceAll("%target%", args.get(1)));
                            utils.send(sender, plugin.getConfig().getString("variables.targetToggle").replace("%target", args.get(1)).replace("%value%", (utils.status(targetid) ? "enabled" : "disabled")));
                        }
                    }
                    break;
                case "reload":
                    if (!sender.hasPermission("MCSF.modify")) {
                        utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                        break;
                    }
                    utils.reload();
                    utils.send(sender, plugin.getConfig().getString("variables.success").replace("%message%", plugin.getConfig().getString("variables.successful.reloaded")));
                    break;
                case "status":
                    if (args.size() == 1 && (sender instanceof Player)) {
                        if (plugin.getConfig().getBoolean("settings.force")) {
                            value = true;
                        } else {
                            value = utils.status(((Player) sender).getUniqueId());
                        }
                        utils.send(sender, plugin.getConfig().getString("variables.status").replace("%target%", sender.getName()).replace("%value%", (value ? "enabled" : "disabled")));
                        break;
                    } else {
                        if (args.size() != 2) {
                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%",
                                    plugin.getConfig().getString("variables.error.invalidtarget").replace("%target%", sender.getName())));
                            break;
                        }
                        if (!sender.hasPermission("MCSF.status")) {
                            utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                            break;
                        }
                        UUID targetid = null;
                        for (final String key : plugin.getConfig().getConfigurationSection("users").getKeys(false)) {
                            if (plugin.getConfig().getString("users." + key + ".playername").equalsIgnoreCase(args.get(1))) {
                                targetid = UUID.fromString(key);
                            }
                        }
                        if (targetid == null) {
                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%",
                                    plugin.getConfig().getString("variables.error.invalidtarget").replace("%target%", args.get(1))));
                            break;
                        } else {
                            if (plugin.getConfig().getBoolean("settings.force")) {
                                value = true;
                            } else {
                                value = utils.status(targetid);
                            }
                            utils.send(sender, plugin.getConfig().getString("variables.status").replace("%target%", args.get(1)).replace("%value%", (value ? "enabled" : "disabled")));
                        }
                    }
                    break;
                case "reset":
                    if (!sender.hasPermission("MCSF.modify")) {
                        utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                        break;
                    }
                    if (!utils.supported("mysql")) {
                        utils.send(sender, plugin.getConfig().getString("variables.disabled"));
                        break;
                    }
                    if (args.size() != 2) {
                        utils.send(sender, plugin.getConfig().getString("variables.reset"));
                        break;
                    } else {
                        if (args.get(1).equalsIgnoreCase("confirm")) {
                            utils.createTable(true);
                        } else {
                            utils.send(sender, plugin.getConfig().getString("variables.reset"));
                            break;
                        }
                    }
                    utils.send(sender, plugin.getConfig().getString("variables.success").replace("%message%", plugin.getConfig().getString("variables.successful.reset")));
                    break;
                case "version":
                    if (sender.hasPermission("MCSF.version")) {
                        for (String str : plugin.getConfig().getStringList("variables.version")) {
                            utils.send(sender, str);
                        }
                    } else {
                        utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                    }
                    break;
                case "add":
                case "remove":
                    if (sender.hasPermission("MCSF.modify")) {
                        if (args.size() != 2) {
                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.incorrectargs")));
                            break;
                        }
                        String word = args.get(1).toLowerCase();
                        List<String> swears = plugin.getConfig().getStringList("swears");
                        switch (args.get(0)) {
                            case "add":
                                if (utils.supported("mysql")) {
                                    if (!MySQL.isConnected())
                                        MySQL.connect();
                                    if (MySQL.isConnected()) {
                                        if (MySQL.exists("word", word, "swears")) {
                                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.alreadyexists")));
                                            break;
                                        }
                                        if (!swears.contains(word))
                                            swears.add(word);
                                        MySQL.stateInsert(word);
                                        utils.send(sender, plugin.getConfig().getString("variables.success").replace("%message%", plugin.getConfig().getString("variables.successful.added")));
                                    } else {
                                        utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.failedtoconnect")));
                                    }
                                } else {
                                    boolean modified = false;
                                    if (!swears.contains(word))
                                        modified = true;
                                    if (modified) {
                                        swears.add(word);
                                        utils.debug(sender.getName() + " has added `" + word + "` to the config");
                                        utils.send(sender, plugin.getConfig().getString("variables.success").replace("%message%", plugin.getConfig().getString("variables.successful.added")));
                                    } else {
                                        utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.alreadyexists")));
                                    }
                                }
                                break;
                            case "remove":
                                if (utils.supported("mysql")) {
                                    if (!MySQL.isConnected())
                                        MySQL.connect();
                                    if (MySQL.isConnected()) {
                                        if (!MySQL.exists("word", word, "swears")) {
                                            utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.doesntexist")));
                                            break;
                                        }
                                        swears.remove(word);
                                        MySQL.stateRemove(word);
                                        utils.send(sender, plugin.getConfig().getString("variables.success").replace("%message%", plugin.getConfig().getString("variables.successful.removed")));
                                    } else {
                                        utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.failedtoconnect")));
                                    }
                                } else {
                                    boolean modified = swears.remove(word);
                                    if (modified) {
                                        utils.debug(sender.getName() + " has removed `" + word + "` from config");
                                        utils.send(sender, plugin.getConfig().getString("variables.success").replace("%message%", plugin.getConfig().getString("variables.successful.removed")));
                                    } else {
                                        utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.doesntexist")));
                                    }
                                }
                                break;
                            default:
                                utils.send(sender, plugin.getConfig().getString("variables.failure").replace("%message%", plugin.getConfig().getString("variables.error.incorrectargs")));
                                break;
                        }
                        plugin.getConfig().set("swears", swears);
                        plugin.saveConfig();
                    } else {
                        utils.send(sender, plugin.getConfig().getString("variables.noperm"));
                    }
                    break;
            }
        } else {
            utils.showHelp(sender);
        }
    }
}
