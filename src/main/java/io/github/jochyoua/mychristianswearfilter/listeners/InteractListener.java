package io.github.jochyoua.mychristianswearfilter.listeners;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import xyz.upperlevel.spigot.book.BookUtil;

import java.util.Objects;

public class InteractListener implements Listener {
    private final MCSF plugin;
    private final Manager manager;

    /**
     * This Listener listens for when the player interacts with a book
     * if the book contains blacklisted strings, the book is filtered against them
     *
     * @param plugin MCSF JavaPlugin instance
     */
    public InteractListener(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Deprecated
    @EventHandler
    public void openBook(PlayerInteractEvent event) {
        if (plugin.getConfig().getBoolean("settings.filtering.filter checks.bookcheck")) {
            Player player = event.getPlayer();
            ItemStack hand;
            try {
                hand = player.getInventory().getItemInMainHand();
            } catch (Exception ex) {
                hand = player.getInventory().getItemInHand();
            }
            if (hand.getType() == Material.WRITTEN_BOOK && (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
                if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    BlockState bs = Objects.requireNonNull(event.getClickedBlock()).getState();
                    if (bs instanceof InventoryHolder) {
                        return;
                    }
                    if (bs instanceof Door || bs instanceof TrapDoor) {
                        return;
                    }
                }
                event.setCancelled(true);
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
                        newmeta.addPage(manager.isclean(page, manager.reloadPattern(Data.Filters.BOTH)) ? page : manager.clean(page, true, manager.reloadPattern(Data.Filters.BOTH), Data.Filters.BOOKS));
                    else
                        newmeta.addPage(manager.isclean(page, manager.reloadPattern(Data.Filters.GLOBAL)) ? page : manager.clean(page, true, manager.reloadPattern(Data.Filters.GLOBAL), Data.Filters.BOOKS));
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
