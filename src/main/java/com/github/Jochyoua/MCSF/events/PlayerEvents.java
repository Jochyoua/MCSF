package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import github.scarsz.discordsrv.DiscordSRV;
import me.vagdedes.mysql.database.MySQL;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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


    public PlayerEvents(Main plugin, MySQL MySQL, Utils utils) {
        this.plugin = plugin;
        this.MySQL = MySQL;
        this.utils = utils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void PlayerChatEvent(AsyncPlayerChatEvent e) {
        //Respect cancelling of AsyncPlayerChatEvent from other plugins:
        if(e.isCancelled())
            return;
        if (plugin.getConfig().getBoolean("settings.only_filter_players") || !utils.supported("ProtocolLib")) {
            e.setCancelled(true);
            if (utils.supported("DiscordSRV") && e.isCancelled())
                DiscordSRV.getPlugin().processChatMessage(e.getPlayer(), e.getMessage(), DiscordSRV.getPlugin().getChannels().size() == 1 ? null : "global", false);
            // The above code registers the process chat message even though asyncplayerchat event is cancelled so original messages are still being sent to the discord, and then filtered elsewhere (DiscordEvents.java)
            if (plugin.getConfig().getBoolean("settings.remove_message_on_swear") && !utils.isclean(e.getMessage()) && e.getPlayer().hasPermission("MCSF.bypass"))
                return;
            String message = e.getMessage();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (utils.status(player.getUniqueId()) || plugin.getConfig().getBoolean("settings.force")) {
                    player.spigot().sendMessage(new TextComponent(String.format(e.getFormat(), e.getPlayer().getDisplayName(), utils.clean(message, false, true))));
                } else {

                    player.spigot().sendMessage(new TextComponent(String.format(e.getFormat(), e.getPlayer().getDisplayName(), message)));
                }
            }
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        if (!(plugin.getConfig().getInt("settings.cooldown") <= 0)) {
            if (utils.getAll().containsKey(e.getPlayer().getUniqueId())) {
                plugin.getConfig().set("users." + e.getPlayer().getUniqueId() + ".cooldown", utils.getAll().get(e.getPlayer().getUniqueId()));
                plugin.getConfig().set("users." + e.getPlayer().getUniqueId() + ".playername", e.getPlayer().getName().toLowerCase());
                plugin.saveConfig();
                utils.removeUser(e.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void Join(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!(plugin.getConfig().getInt("settings.cooldown") <= 0)) {
            if (plugin.getConfig().isSet("users." + e.getPlayer().getUniqueId() + ".cooldown")) {
                utils.setUser(e.getPlayer().getUniqueId(), plugin.getConfig().getInt("users." + e.getPlayer().getUniqueId() + ".cooldown"));
            }
        }
        plugin.getConfig().set("users." + player.getUniqueId() + ".playername", player.getName().toLowerCase());
        plugin.saveConfig();
        if (!plugin.getConfig().isSet("users." + player.getUniqueId() + ".playername")) {
            utils.debug("There was an issue saving " + player.getName() + "'s name to the config.");
        } else {
            utils.debug("Successfully added " + player.getName() + "'s name to the config.");
        }
        if (!plugin.getConfig().getBoolean("settings.force")) {
            if (!plugin.getConfig().isSet("users." + player.getUniqueId() + ".enabled"))
                utils.toggle(player.getUniqueId());
            if (utils.supported("mysql")) {
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
        }

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
