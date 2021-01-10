package io.github.jochyoua.mychristianswearfilter.events;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.Utils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class DiscordEvents implements Listener {
    private final Utils utils;

    public DiscordEvents(MCSF plugin, Utils utils) {
        this.utils = utils;
        if (plugin.getConfig().getBoolean("settings.discordSRV.enabled") && utils.supported("DiscordSRV")) {
            try {
                DiscordSRV.api.subscribe(this);
                utils.debug("Registered DiscordSRV hook successfully!");
                Plugin pl = Bukkit.getPluginManager().getPlugin("DiscordSRV");
                if (pl != null) {
                    utils.debug("DiscordSRV is installed, attempting to override chat event");
                    Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                        public void asyncPlayerChatEvent(AsyncPlayerChatEvent e) {utils.reloadPattern();
                            utils.reloadPattern();
                            DiscordSRV.getPlugin().processChatMessage(e.getPlayer(), utils.clean(e.getMessage(), true, false, utils.getBoth(), Types.Filters.DISCORD), DiscordSRV.getPlugin().getChannels().size() == 1 ? null : "global", !plugin.getConfig().getBoolean("settings.discordSRV.ignore cancelled") || e.isCancelled());
                        }
                    }, plugin);
                }
            } catch (Exception e) {
                String message = e.getMessage();
                utils.debug("Registered DiscordSRV hook unsuccessfully: " + message);
            }
        }
    }

    @Subscribe
    public void DiscordGameMessage(
            final github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent event) {
        utils.reloadPattern();
        event.setProcessedMessage(utils.clean(event.getProcessedMessage(), true, false, utils.getBoth(), Types.Filters.DISCORD));
    }
}