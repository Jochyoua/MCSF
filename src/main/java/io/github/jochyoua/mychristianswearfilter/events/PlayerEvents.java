package io.github.jochyoua.mychristianswearfilter.events;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.Connector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
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
import xyz.upperlevel.spigot.book.BookUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PlayerEvents implements Listener {
    MCSF plugin;
    Connector connector;
    Manager manager;
    Connection connection;

    public PlayerEvents(Manager manager) {
        this.plugin = manager.getProvider();
        this.connector = manager.getConnector();
        if (plugin.getHikariCP().isEnabled())
            try {
                this.connection = connector.getConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        this.manager = manager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void PlayerChatEvent(AsyncPlayerChatEvent e) {
        boolean use = plugin.getConfig().getBoolean("settings.only filter players.enabled");
        String old_message = e.getMessage();
        String new_message = e.getMessage();
        // Message saving / global filtering
        manager.setTable("global");
        try {
            if (!use && !plugin.getProtocolLib().isEnabled())
                use = true;
        } catch (NoClassDefFoundError ignored) {
            use = true;
            // ProtocolLib wasn't found!!
        }
        if (e.isCancelled())
            return;
        if (use || !manager.supported("ProtocolLib")) {
            if (!manager.getGlobal().isEmpty()) {
                if (plugin.getConfig().getBoolean("settings.filtering.global blacklist.enabled")) {
                    new_message = manager.clean(old_message, false, true, manager.reloadPattern(Types.Filters.GLOBAL), Types.Filters.PLAYERS);
                    e.setMessage(new_message);
                }
            }
            Set<Player> remove = new HashSet<>();
            for (Player player : e.getRecipients()) {
                if (new User(manager, player.getUniqueId()).status() || plugin.getConfig().getBoolean("settings.filtering.force")) {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), manager.clean(new_message, false, true, manager.reloadPattern(Types.Filters.BOTH), Types.Filters.PLAYERS)));
                    remove.add(player);
                }
            }
            e.getRecipients().removeAll(remove);
        }
    }

    @EventHandler
    public void Join(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        User user = new User(manager, player.getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (user.exists()) {
                user.playerName(player.getName());
            } else {
                if (!user.exists())
                    user.create(player.getName(), plugin.getConfig().getBoolean("settings.filtering.default"));
            }
            if (!user.playerName().equalsIgnoreCase(player.getName())) {
                manager.debug("There was an issue saving " + player.getName() + "'s name to the config.");
            } else {
                manager.debug("Successfully added " + player.getName() + "'s name to the config.");
            }
            if (!plugin.getConfig().getBoolean("settings.filtering.force")) {
                if (!user.exists())
                    user.create(player.getName(), plugin.getConfig().getBoolean("settings.filtering.default"));
                if (manager.supported("mysql")) {
                    manager.setTable("users");
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
                    user.set(result);
                }
                manager.debug("Player " + player.getName() + "'s swear filter is " + (new User(manager, player.getUniqueId()).status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")));
            }

            if (plugin.getConfig().getBoolean("settings.updating.update notification ingame") && player.hasPermission("MCSF.update") && plugin.getConfig().getBoolean("settings.updating.check for updates") && Manager.needsUpdate(plugin.getDescription().getVersion())) {
                manager.send(player, plugin.getLanguage().getString("variables.updatecheck.update_available"));
                manager.send(player, plugin.getLanguage().getString("variables.updatecheck.update_link"));
            }
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            User user = new User(manager, e.getPlayer().getUniqueId());
            user.setFlags(0);
        });
    }

    @Deprecated
    @EventHandler
    public void openBook(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (plugin.getConfig().getBoolean("settings.filtering.filter checks.bookcheck")) {
            ItemStack hand;
            try {
                hand = player.getInventory().getItemInMainHand();
            } catch (Exception ex) {
                hand = player.getInventory().getItemInHand();
            }
            if (hand.getType() == Material.WRITTEN_BOOK && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
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
                    if (new User(manager, player.getUniqueId()).status())
                        newmeta.addPage(manager.isclean(page, manager.reloadPattern(Types.Filters.BOTH)) ? page : manager.clean(page, true, false, manager.reloadPattern(Types.Filters.BOTH), Types.Filters.BOOKS));
                    else
                        newmeta.addPage(manager.isclean(page, manager.reloadPattern(Types.Filters.GLOBAL)) ? page : manager.clean(page, true, false, manager.reloadPattern(Types.Filters.GLOBAL), Types.Filters.BOOKS));
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
