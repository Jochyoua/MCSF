package io.github.jochyoua.mychristianswearfilter.listeners;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;

public class ChatListener implements Listener {
    private final MCSF plugin;
    private final Manager manager;

    /**
     * This listener listens for whenever the Player chats and filters messages if needed
     *
     * @param plugin MCSF JavaPlugin instance
     */
    public ChatListener(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
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
        } catch (NoClassDefFoundError | NullPointerException ignored) {
            use = true;
            // ProtocolLib wasn't found!!
        }
        if (plugin.getConfig().getBoolean("settings.only filter players.remove message on swear"))
            e.setCancelled(true);
        if (plugin.getConfig().getBoolean("settings.only filter players.only remove global swears"))
            if (!manager.isclean(old_message, manager.reloadPattern(Data.Filters.GLOBAL)))
                e.setCancelled(true);
        if (e.isCancelled())
            return;
        if (use || !manager.supported("ProtocolLib")) {
            if (!manager.getGlobalSwears().isEmpty()) {
                if (plugin.getConfig().getBoolean("settings.filtering.global blacklist.enabled")) {
                    new_message = manager.clean(old_message, false, manager.reloadPattern(Data.Filters.GLOBAL), Data.Filters.PLAYERS);
                    e.setMessage(new_message);
                }
            }
            Set<Player> remove = new HashSet<>();
            for (Player player : e.getRecipients()) {
                if (new User(manager, player.getUniqueId()).status() || plugin.getConfig().getBoolean("settings.filtering.force")) {
                    player.sendMessage(String.format(e.getFormat(), e.getPlayer().getDisplayName(), manager.clean(new_message, false, manager.reloadPattern(Data.Filters.BOTH), Data.Filters.PLAYERS)));
                    remove.add(player);
                }
            }
            e.getRecipients().removeAll(remove);
        }
    }
}
