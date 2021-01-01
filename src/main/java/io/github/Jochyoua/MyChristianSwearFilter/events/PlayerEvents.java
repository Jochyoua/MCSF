package io.github.Jochyoua.MyChristianSwearFilter.events;

import io.github.Jochyoua.MyChristianSwearFilter.MCSF;
import io.github.Jochyoua.MyChristianSwearFilter.shared.HikariCP.Connector;
import io.github.Jochyoua.MyChristianSwearFilter.shared.Types;
import io.github.Jochyoua.MyChristianSwearFilter.shared.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import xyz.upperlevel.spigot.book.BookUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;

public class PlayerEvents implements Listener {
    MCSF plugin;
    Connector connector;
    Utils utils;
    Connection connection;


    public PlayerEvents(MCSF plugin, Connector connector, Utils utils) {
        this.plugin = plugin;
        this.connector = connector;

        if (plugin.getConfig().getBoolean("mysql.enabled"))
            try {
                this.connection = connector.getConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        this.utils = utils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void PlayerChatEvent(AsyncPlayerChatEvent e) {
        boolean use = plugin.getConfig().getBoolean("settings.only filter players.enabled");
        String old_message = e.getMessage();
        String new_message = e.getMessage();
        // Message saving / global filtering
        utils.setTable("global");
        try {
            if (!utils.getGlobal().isEmpty()) {
                utils.reloadPattern();
                if (plugin.getConfig().getBoolean("settings.filtering.global blacklist.enabled")) {
                    new_message = utils.clean(old_message, false, true, "only", Types.Filters.PLAYERS);
                    e.setMessage(new_message);
                }
            }

            if (!utils.isclean(old_message, "both") && plugin.getConfig().getBoolean("settings.filtering.save messages.enabled")) {
                File file = new File(plugin.getDataFolder(), "/logs/flags.txt");
                File dir = new File(plugin.getDataFolder(), "logs");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
                BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
                bw.append("[").append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date())).append("] ").append(e.getPlayer().getName()).append(" ").append(ChatColor.stripColor(old_message)).append("\n");
                bw.close();
            }
        } catch (Exception ex) {
            FileConfiguration language = plugin.getLanguage();
            plugin.getLogger().log(Level.SEVERE, "Failure: {message}"
                    .replaceAll("(?i)\\{message}|(?i)%message%",
                            Objects.requireNonNull(language.getString("variables.error.execute_failure"))
                                    .replaceAll("(?i)\\{feature}", "Message saving")), e);
            plugin.getLogger().log(Level.INFO, "Failure: {message}".replaceAll("(?i)\\{message}", Objects.requireNonNull(language.getString("variables.error.execute_failure_link"))));

        }
        try {
            if (!use && !ProtocolLib.isEnabled())
                use = true;
        } catch (NoClassDefFoundError ignored) {
            use = true;
            // ProtocolLib wasn't found!!
        }
        if (e.isCancelled())
            return;
        if (use || !utils.supported("ProtocolLib")) {
            e.setCancelled(true);
            if (plugin.getConfig().getBoolean("settings.only filter players.remove message on swear") && !utils.isclean(e.getMessage(), "both") && e.getPlayer().hasPermission("MCSF.bypass"))
                return;
            if (plugin.getConfig().getBoolean("settings.only filter players.log chat messages")) {
                Bukkit.getConsoleSender().sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), new_message));
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (utils.status(player.getUniqueId()) || plugin.getConfig().getBoolean("settings.filtering.force")) {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), utils.clean(new_message, false, true, "both", Types.Filters.PLAYERS)));
                } else {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), new_message));
                }
            }
        }
    }

    @EventHandler
    public void Join(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Plugin pl = Bukkit.getPluginManager().getPlugin("DiscordSRV");
            if (pl != null && (plugin.getConfig().getBoolean("settings.discordSRV.enabled") && utils.supported("DiscordSRV")))
                new AsyncPlayerChatEvent(true, player, "", Collections.singleton(player)).getHandlers().unregister(pl);
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
                    try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET name=? WHERE uuid=?")) {
                        ps.setString(1, player.getName());
                        ps.setString(2, String.valueOf(player.getUniqueId()));
                        ps.execute();
                        ps.close();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    boolean result = false;
                    try (PreparedStatement ps = connection.prepareStatement("SELECT status FROM users WHERE uuid=?")) {
                        ps.setString(1, String.valueOf(player.getUniqueId()));
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            result = rs.getBoolean("status");
                        }
                        ps.close();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
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
    public void onLeave(PlayerQuitEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getConfig().getBoolean("settings.filtering.punishments.flags.reset every leave")) {
                plugin.getConfig().set("users." + e.getPlayer().getUniqueId() + ".flags", 0);

                plugin.saveConfig();
            }
        });
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
                    newmeta.addPage(utils.isclean(page, "both") ? page : utils.clean(page, true, false, "both", Types.Filters.BOOKS));
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
