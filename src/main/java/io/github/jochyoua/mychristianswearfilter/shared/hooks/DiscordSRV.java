package io.github.jochyoua.mychristianswearfilter.shared.hooks;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent;
import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import lombok.Getter;
import lombok.Setter;

import java.util.logging.Level;

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
                Manager.debug("Registered DiscordSRV hook successfully!", plugin.getDebug(), Level.INFO);
                github.scarsz.discordsrv.DiscordSRV.api.subscribe(this);
            } catch (Exception e) {
                String message = e.getMessage();
                setEnabled(false);
                Manager.debug("Registered DiscordSRV hook unsuccessfully: " + message, true, Level.WARNING);
            }
        }
    }

    @Subscribe
    public void DiscordGameMessage(
            final GameChatMessagePostProcessEvent event) {
        // Filters messages from MC to Discord
        event.setProcessedMessage(manager.clean(event.getProcessedMessage(), true, manager.reloadPattern(Data.Filters.BOTH), Data.Filters.DISCORD));
    }
}
