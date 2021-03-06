package io.github.jochyoua.mychristianswearfilter.listeners;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;

import java.util.Objects;
import java.util.logging.Level;

public class PunishmentListener implements Listener {
    MCSF plugin;
    Manager manager;


    /**
     * This Listener listens for Sign, Book and Chat modifications and checks them for blacklisted strings.
     * if a blacklisted string is located, the player is punished based on the configurations set
     *
     * @param plugin MCSF JavaPlugin instance
     */
    public PunishmentListener(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        if (!plugin.getConfig().getBoolean("settings.filtering.punishments.punish players")) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void signEdit(SignChangeEvent event) {
        if (event.getPlayer().hasPermission("MCSF.bypass") || manager.isclean(String.join("", event.getLines()), plugin.getConfig().getBoolean("settings.filtering.punishments.only global") ? manager.reloadPattern(Data.Filters.GLOBAL) : manager.reloadPattern(Data.Filters.BOTH)) || !plugin.getConfig().getBoolean("settings.filtering.punishments.punish check.signs")) {
            return;
        }
        punishPlayers(event.getPlayer());
    }

    public void punishPlayers(Player player) {
        User user = new User(manager, player.getUniqueId());
        int flags = user.getFlags();
        if (flags != 0)
            flags += 1;
        else
            flags = 1;
        user.setFlags(flags);
        for (String str : Objects.requireNonNull(plugin.getConfig().getConfigurationSection("settings.filtering.punishments.commands")).getKeys(false)) {
            try {
                if (Integer.parseInt(str) == flags || Integer.parseInt(str) == 0) {
                    String path = "settings.filtering.punishments.commands." + str;
                    String executor = plugin.getConfig().getString(path + ".executor", "CONSOLE");
                    for (String command : plugin.getConfig().getStringList(path + ".commands")) {
                        command = manager.prepare(player, command).replaceAll("(?i)\\{amount}|(?i)%amount%", Integer.toString(flags));
                        String finalCommand = command;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (Objects.requireNonNull(executor).equalsIgnoreCase("CONSOLE")) {
                                Manager.debug((Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand) ? "successfully executed " : "failed to execute ") + " command `" + finalCommand + "`", false, Level.INFO);
                            } else {
                                Manager.debug((Bukkit.dispatchCommand(player, finalCommand) ? "successfully executed " : "failed to execute ") + " command `" + finalCommand + "`", false, Level.INFO);
                            }
                        });
                    }
                }
            } catch (NumberFormatException e) {
                manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(plugin.getLanguage().getString("variables.failure")).replaceAll("(?i)\\{message}|(?i)%message%", "Failed to parse integer (" + str + ") under path (settings.filtering.punishments.commands." + str + ")"));
                e.printStackTrace();
            }
            if (plugin.getConfig().getInt("settings.filtering.punishments.flags.reset every interval at") != 0) {
                if (flags >= plugin.getConfig().getInt("settings.filtering.punishments.flags.reset every interval at")) {
                    user.setFlags(0);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void playerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        if (!player.hasPermission("MCSF.bypass") && !manager.isclean(message, plugin.getConfig().getBoolean("settings.filtering.punishments.only global") ? manager.reloadPattern(Data.Filters.GLOBAL) : manager.reloadPattern(Data.Filters.BOTH)) && plugin.getConfig().getBoolean("settings.filtering.punishments.punish check.chat")) {
            punishPlayers(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void bookEdit(PlayerEditBookEvent event) {
        if (event.getPlayer().hasPermission("MCSF.bypass") || manager.isclean(String.join("", event.getNewBookMeta().getPages()), plugin.getConfig().getBoolean("settings.filtering.punishments.only global") ? manager.reloadPattern(Data.Filters.GLOBAL) : manager.reloadPattern(Data.Filters.BOTH)) || !plugin.getConfig().getBoolean("settings.filtering.punishments.punish check.books")) {
            return;
        }
        punishPlayers(event.getPlayer());
    }
}
