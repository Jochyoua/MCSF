package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentEvents implements Listener {
    Main plugin;
    Utils utils;

    public PunishmentEvents(Main plugin, Utils utils) {
        this.plugin = plugin;
        this.utils = utils;
        if (!plugin.getConfig().getBoolean("settings.force") || !plugin.getConfig().getBoolean("settings.punish_players")) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void signEdit(SignChangeEvent event) {
        if (event.getPlayer().hasPermission("MCSF.bypass") || utils.isclean(String.join("", event.getLines())) || !plugin.getConfig().getBoolean("settings.punish_check.signs")) {
            return;
        }
        for (String str : plugin.getConfig().getStringList("variables.punishment_commands")) {
            String command = utils.prepare(event.getPlayer(), str);
            Matcher match = Pattern.compile("(?i)<%EXECUTE_AS=(.*?)%>", Pattern.DOTALL).matcher(command);
            String executor = "CONSOLE";
            while (match.find()) {
                executor = match.group(1);
            }
            command = command.replaceAll("(?i)<%EXECUTE_AS=(.*?)%>", "").trim();
            if (executor.equalsIgnoreCase("CONSOLE")) {
                utils.debug((Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command) ? "successfully executed " : "failed to execute ") + " command `" + command + "`");
            } else {
                utils.debug((Bukkit.dispatchCommand(event.getPlayer(), command) ? "successfully executed " : "failed to execute ") + " command `" + command + "`");
            }
        }
    }

    @EventHandler
    public void playerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        if(!player.hasPermission("MCSF.bypass") && !utils.isclean(message) && plugin.getConfig().getBoolean("settings.punish_check.chat")) {
            for (String str : plugin.getConfig().getStringList("variables.punishment_commands")) {
                final String[] command = {utils.prepare(player, str)};
                Matcher match = Pattern.compile("(?i)<%EXECUTE_AS=(.*?)%>", Pattern.DOTALL).matcher(command[0]);
                while (match.find()) {
                    String executor = match.group(1);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        command[0] = command[0].replaceAll("(?i)<%EXECUTE_AS=(.*?)%>", "").trim();
                        if (executor.equalsIgnoreCase("CONSOLE")) {
                            utils.debug((Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command[0].replaceAll("(?i)<%EXECUTE_AS=(.*?)%>", "").trim()) ? "successfully executed " : "failed to execute ") + " command `" + command[0] + "`");
                        } else {
                            utils.debug((Bukkit.dispatchCommand(player, command[0]) ? "successfully executed " : "failed to execute ") + " command `" + command[0] + "`");
                        }
                    });
                }
            }
        }
    }

    @EventHandler
    public void bookEdit(PlayerEditBookEvent event) {
        if (event.getPlayer().hasPermission("MCSF.bypass") || utils.isclean(String.join("", event.getNewBookMeta().getPages())) || !plugin.getConfig().getBoolean("settings.punish_check.books")) {
            return;
        }
        for (String str : plugin.getConfig().getStringList("variables.punishment_commands")) {
            String command = utils.prepare(event.getPlayer(), str);
            Matcher match = Pattern.compile("(?i)<%EXECUTE_AS=(.*?)%>", Pattern.DOTALL).matcher(command);
            String executor = "CONSOLE";
            while (match.find()) {
                executor = match.group(1);
            }
            command = command.replaceAll("(?i)<%EXECUTE_AS=(.*?)%>", "").trim();
            if (executor.equalsIgnoreCase("CONSOLE")) {
                utils.debug((Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command) ? "successfully executed " : "failed to execute ") + " command `" + command + "`");
            } else {
                utils.debug((Bukkit.dispatchCommand(event.getPlayer(), command) ? "successfully executed " : "failed to execute ") + " command `" + command + "`");
            }
        }
    }
}
