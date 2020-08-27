package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.shared.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.slf4j.event.Level;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentEvents implements Listener {
    MCSF plugin;
    Utils utils;


    public PunishmentEvents(MCSF plugin, Utils utils) {
        this.plugin = plugin;
        this.utils = utils;
        if (!plugin.getConfig().getBoolean("settings.filtering.punishments.punish players")) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void signEdit(SignChangeEvent event) {
        if (event.getPlayer().hasPermission("MCSF.bypass") || utils.isclean(String.join("", event.getLines())) || !plugin.getConfig().getBoolean("settings.filtering.punishments.punish check.signs")) {
            return;
        }
        punishPlayers(event.getPlayer());
    }

    public void punishPlayers(Player player) {
        if(plugin.getConfig().isSet("users." + player.getUniqueId() + ".flags"))
            plugin.getConfig().set("users." + player.getUniqueId() + ".flags", plugin.getConfig().getInt("users." + player.getUniqueId() + ".flags")+1);
        else
            plugin.getConfig().set("users." + player.getUniqueId() + ".flags", 1);
        plugin.saveConfig();
        for (String str : plugin.getConfig().getConfigurationSection("settings.filtering.punishments.commands").getKeys(false)) {
            int player_flags = plugin.getConfig().getInt("users." + player.getUniqueId() + ".flags");
            try {
                if (Integer.parseInt(str) == player_flags || Integer.parseInt(str) == 0) {
                    String path = "settings.filtering.punishments.commands." + str;
                    String executor = plugin.getConfig().getString(path + ".executor", "CONSOLE");
                    for (String command : plugin.getConfig().getStringList(path + ".commands")) {
                        command = utils.prepare(player, command).replaceAll("(?i)\\{amount}|(?i)%amount%", Integer.toString(player_flags));
                        String finalCommand = command;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (executor.equalsIgnoreCase("CONSOLE")) {
                                utils.debug((Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand) ? "successfully executed " : "failed to execute ") + " command `" + finalCommand + "`");
                            } else {
                                utils.debug((Bukkit.dispatchCommand(player, finalCommand) ? "successfully executed " : "failed to execute ") + " command `" + finalCommand + "`");
                            }
                        });
                    }
                }
            } catch (NumberFormatException e) {
                utils.send(Bukkit.getConsoleSender(), plugin.getLanguage().getString("variables.failure").replaceAll("(?i)\\{message}|(?i)%message%", "Failed to parse integer (" + str + ") under path (settings.filtering.punishments.commands." + str + ")"));
                e.printStackTrace();
            }
            if (plugin.getConfig().getInt("settings.filtering.punishments.flags.reset every interval at") != 0) {
                if (player_flags >= plugin.getConfig().getInt("settings.filtering.punishments.flags.reset every interval at")) {
                    player_flags = 1;
                    plugin.getConfig().set("users." + player.getUniqueId() + ".flags", player_flags);
                    plugin.saveConfig();
                }
            }
        }
    }

    @EventHandler
    public void playerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        if (!player.hasPermission("MCSF.bypass") && !utils.isclean(message) && plugin.getConfig().getBoolean("settings.filtering.punishments.punish check.chat")) {
            punishPlayers(event.getPlayer());
        }
    }

    @EventHandler
    public void bookEdit(PlayerEditBookEvent event) {
        if (event.getPlayer().hasPermission("MCSF.bypass") || utils.isclean(String.join("", event.getNewBookMeta().getPages())) || !plugin.getConfig().getBoolean("settings.filtering.punishments.punish check.books")) {
            return;
        }
        punishPlayers(event.getPlayer());
    }
}
