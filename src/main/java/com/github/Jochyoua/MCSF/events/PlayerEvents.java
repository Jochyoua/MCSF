package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.shared.MySQL;
import com.github.Jochyoua.MCSF.shared.Types;
import com.github.Jochyoua.MCSF.shared.Utils;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import xyz.upperlevel.spigot.book.BookUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PlayerEvents implements Listener {
    MCSF plugin;
    MySQL MySQL;
    Utils utils;



    public PlayerEvents(MCSF plugin, MySQL MySQL, Utils utils) {
        this.plugin = plugin;
        this.MySQL = MySQL;
        this.utils = utils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void PlayerChatEvent(AsyncPlayerChatEvent e) {
        String message = e.getMessage();
        if (plugin.getConfig().getBoolean("settings.only filter players.enabled") || !utils.supported("ProtocolLib")) {
            e.setCancelled(true);
            if (plugin.getConfig().getBoolean("settings.only filter players.remove message on swear") && !utils.isclean(e.getMessage()) && e.getPlayer().hasPermission("MCSF.bypass"))
                return;
            if (utils.supported("DiscordSRV") && e.isCancelled())
                DiscordSRV.getPlugin().processChatMessage(e.getPlayer(), e.getMessage(), DiscordSRV.getPlugin().getChannels().size() == 1 ? null : "global", false);
            // The above code registers the process chat message even though asyncplayerchat event is cancelled so original messages are still being sent to the discord, and then filtered elsewhere (DiscordEvents.java)
            // String message = e.getMessage();
            if (plugin.getConfig().getBoolean("settings.only filter players.log chat messages")) {
                Bukkit.getConsoleSender().sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), message));
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (utils.status(player.getUniqueId()) || plugin.getConfig().getBoolean("settings.filtering.force")) {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), utils.clean(message, false, true, Types.Filters.PLAYERS)));
                } else {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), message));
                }
            }
        }
    }
    @EventHandler
    public void Join(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getConfig().set("users." + player.getUniqueId() + ".playername", player.getName().toLowerCase());
            plugin.saveConfig();
            if (!plugin.getConfig().isSet("users." + player.getUniqueId() + ".playername")) {
                utils.debug("There was an issue saving " + player.getName() + "'s name to the config.");
            } else {
                utils.debug("Successfully added " + player.getName() + "'s name to the config.");
            }
            if (!plugin.getConfig().getBoolean("settings.filtering.force")) {
                if (!plugin.getConfig().isSet("users." + player.getUniqueId() + ".enabled"))
                    utils.toggle(player.getUniqueId());
                if (utils.supported("mysql")) {
                    utils.setTable("users");
                    MySQL.update("UPDATE users SET name='" + player.getName() + "' WHERE uuid='" + player.getUniqueId() + "';");
                    ResultSet rs = MySQL.query("SELECT status FROM users WHERE uuid='" + player.getUniqueId() + "'");
                    boolean result = false;
                    try {
                        while (rs.next()) {
                            result = Boolean.parseBoolean(rs.getString("status"));
                        }
                    } catch (SQLException ignored) {
                        result = plugin.getConfig().getBoolean("settings.filtering.default");
                    }
                    plugin.getConfig().set("users." + player.getUniqueId() + ".enabled", result);
                    plugin.saveConfig();
                }
                utils.debug("Player " + player.getName() + "'s swear filter is " + (utils.status(player.getUniqueId()) ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")));

            }

            if (plugin.getConfig().getBoolean("settings.updating.update notification ingame") && player.hasPermission("MCSF.update") && plugin.getConfig().getBoolean("settings.updating.check for updates") && !utils.isUpToDate()) {
                utils.send(player, plugin.getLanguage().getString("variables.updatecheck.update_available"));
                utils.send(player, plugin.getLanguage().getString("variables.updatecheck.update_link"));
            }
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e){
        if(plugin.getConfig().getBoolean("settings.filtering.punishments.flags.reset every leave")){
            plugin.getConfig().set("users."+e.getPlayer().getUniqueId()+".flags", 0);
            plugin.saveConfig();
        }
    }
    @EventHandler
    public void openBook(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (plugin.getConfig().getBoolean("settings.filtering.filter checks.bookcheck") && utils.status(player.getUniqueId())) {
            if (player.getItemInHand().getType() == Material.WRITTEN_BOOK && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
                if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    BlockState bs = Objects.requireNonNull(e.getClickedBlock()).getState();
                    if (bs instanceof InventoryHolder) {
                        return;
                    }
                    if (bs instanceof Door || bs instanceof TrapDoor) {
                        return;
                    }
                }
                e.setCancelled(true);
                ItemStack book = player.getInventory().getItemInHand();
                BookMeta meta = (BookMeta) book.getItemMeta();
                int slot = player.getInventory().getHeldItemSlot();
                ItemStack old = player.getInventory().getItem(slot);
                ItemStack newbook = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta newmeta = (BookMeta) newbook.getItemMeta();
                if (newmeta == null || meta == null) {
                    return;
                }
                for (String page : meta.getPages()) {
                    // Colors of the replacement string are being stripped before filtering because it causes issues for pre-formatted books that have any text modifiers in them.
                    newmeta.addPage(utils.isclean(page) ? page : utils.clean(page, true, false, Types.Filters.BOOKS));
                }
                newmeta.setAuthor(meta.getAuthor());
                newmeta.setTitle(meta.getTitle());
                newbook.setItemMeta(newmeta);
                player.getInventory().setItem(slot, newbook);
                BookUtil.openPlayer(player, newbook);
                player.getInventory().setItem(slot, old);
            }


        }
    }
}
