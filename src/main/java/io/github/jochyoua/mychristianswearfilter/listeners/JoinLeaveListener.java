package io.github.jochyoua.mychristianswearfilter.listeners;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

public class JoinLeaveListener implements Listener {
    private final MCSF plugin;
    private final Manager manager;
    Connection connection;

    public JoinLeaveListener(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();

        if (plugin.getHikariCP().isEnabled())
            try {
                this.connection = plugin.getConnector().getConnection();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
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
                manager.debug("There was an issue saving " + player.getName() + "'s name to the config.", true, Level.WARNING);
            } else {
                manager.debug("Successfully added " + player.getName() + "'s name to the config.", true, Level.INFO);
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
                        rs.close();
                        ps.close();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        result = plugin.getConfig().getBoolean("settings.filtering.default");
                    }
                    user.set(result);
                }
                manager.debug("Player " + player.getName() + "'s swear filter is " + (user.status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")), true, Level.INFO);
            }

            if (plugin.getConfig().getBoolean("settings.updating.update notification ingame") && player.hasPermission("MCSF.update") && plugin.getNeedsUpdate()) {
                manager.send(player, Objects.requireNonNull(plugin.getLanguage().getString("variables.updatecheck.update_available")));
                manager.send(player, Objects.requireNonNull(plugin.getLanguage().getString("variables.updatecheck.update_link")));
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            User user = new User(manager, e.getPlayer().getUniqueId());
            user.setFlags(0);
        });
    }
}
