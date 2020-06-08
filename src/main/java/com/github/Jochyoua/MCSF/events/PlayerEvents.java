package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import github.scarsz.discordsrv.DiscordSRV;
import me.vagdedes.mysql.database.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import xyz.upperlevel.spigot.book.BookUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PlayerEvents implements Listener {
    Main plugin;
    MySQL MySQL;
    Utils utils;


    public PlayerEvents(Main plugin, Utils utils, MySQL MySQL) {
        this.plugin = plugin;
        this.MySQL = MySQL;
        this.utils = utils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void PlayerChatEvent(PlayerChatEvent e) {
        if (plugin.getConfig().getBoolean("settings.only_filter_players")) {
            e.setCancelled(true);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (utils.status(player.getUniqueId())) {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), utils.clean(e.getMessage(), false, true)));
                } else {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), e.getMessage()));
                }
            }
        }
    }

    @EventHandler
    public void Join(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        plugin.getConfig().set("users." + player.getUniqueId() + ".playername", player.getName());
        plugin.saveConfig();
        if (!plugin.getConfig().isSet("users." + player.getUniqueId() + ".enabled"))
            utils.toggle(player.getUniqueId());
        if (plugin.getConfig().getBoolean("settings.mysql")) {
            MySQL.update("UPDATE users SET name='" + player.getName() + "' WHERE uuid='" + player.getUniqueId() + "';");
            ResultSet rs = MySQL.query("SELECT status FROM users WHERE uuid='" + player.getUniqueId() + "'");
            boolean result = false;
            try {
                while (rs.next()) {
                    result = Boolean.parseBoolean(rs.getString("status"));
                }
            } catch (SQLException ignored) {
                result = plugin.getConfig().getBoolean("settings.default");
            }
            plugin.getConfig().set("users." + player.getUniqueId() + ".enabled", result);
            plugin.saveConfig();
        }
        utils.debug("Player " + player.getName() + "'s swear filter is " + (utils.status(player.getUniqueId()) ? "enabled" : "disabled"));

        if (plugin.getConfig().getBoolean("settings.update_notification_ingame") && player.hasPermission("MCSF.update") && plugin.getConfig().getBoolean("settings.check_for_updates") && !utils.isUpToDate()) {
            utils.send(player, plugin.getConfig().getString("variables.updatecheck.update_available"));
            utils.send(player, plugin.getConfig().getString("variables.updatecheck.update_link"));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void openBook(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (plugin.getConfig().getBoolean("settings.bookcheck") && utils.status(player.getUniqueId())) {
            if (player.getItemInHand().getType() == Material.WRITTEN_BOOK && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
                if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    BlockState bs = Objects.requireNonNull(e.getClickedBlock()).getState();
                    if (bs instanceof InventoryHolder) {
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
                    newmeta.addPage(utils.isclean(page) ? page : utils.clean(page, true, false));
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
