package io.github.jochyoua.mychristianswearfilter.events;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.Utils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class DiscordEvents implements Listener {
    private final Utils utils;

    public DiscordEvents(Utils utils) {
        this.utils = utils;
        if (utils.getProvider().getConfig().getBoolean("settings.discordSRV.enabled") && utils.supported("DiscordSRV")) {
            try {
                utils.debug("Registered DiscordSRV hook successfully!");
                Plugin pl = Bukkit.getPluginManager().getPlugin("DiscordSRV");
                if (pl != null) {
                    DiscordSRV.api.subscribe(this);
                    utils.debug("DiscordSRV is installed, attempting to filter chat");
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