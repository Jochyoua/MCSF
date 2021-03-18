package io.github.jochyoua.mychristianswearfilter.events;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiscordEvents implements Listener {
    private final Manager manager;

    public DiscordEvents(Manager manager) {
        this.manager = manager;
        if (manager.getProvider().getConfig().getBoolean("settings.discordSRV.enabled") && manager.supported("DiscordSRV")) {
            try {
                manager.debug("Registered DiscordSRV hook successfully!");
                Plugin pl = Bukkit.getPluginManager().getPlugin("DiscordSRV");
                if (pl != null) {
                    DiscordSRV.api.subscribe(this);
                    manager.debug("DiscordSRV is installed, attempting to filter chat");
                }
            } catch (Exception e) {
                String message = e.getMessage();
                manager.debug("Registered DiscordSRV hook unsuccessfully: " + message);
            }
        }
    }

    @Subscribe
    public void DiscordGameMessage(
            final github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent event) {
        manager.reloadPattern();
        event.setProcessedMessage(manager.clean(event.getProcessedMessage(), true, false, Stream.of(manager.getRegex(), manager.getGlobalRegex()).collect(Collectors.toList()).get(0), Types.Filters.DISCORD));
    }
}