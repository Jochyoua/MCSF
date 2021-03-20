package io.github.jochyoua.mychristianswearfilter.shared.hooks;

import github.scarsz.discordsrv.api.Subscribe;
import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class DiscordSRV {
    private final MCSF plugin;
    private Manager manager;
    @Getter
    @Setter
    private boolean enabled = true;

    public DiscordSRV(MCSF plugin) {
        this.plugin = plugin;
    }

    public void register() {
        manager = plugin.getManager();
        if (manager.supported("DiscordSRV")) {
            try {
                manager.debug("Registered DiscordSRV hook successfully!");
                github.scarsz.discordsrv.DiscordSRV.api.subscribe(this);
            } catch (Exception e) {
                String message = e.getMessage();
                setEnabled(false);
                manager.debug("Registered DiscordSRV hook unsuccessfully: " + message);
            }
        }
    }

    @Subscribe
    public void DiscordGameMessage(
            final github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent event) {
        event.setProcessedMessage(manager.clean(event.getProcessedMessage(), true, false, manager.reloadPattern(Types.Filters.BOTH), Types.Filters.DISCORD));
    }
}
