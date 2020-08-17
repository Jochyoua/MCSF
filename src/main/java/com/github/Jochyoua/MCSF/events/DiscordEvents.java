package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.MCSF;
import com.github.Jochyoua.MCSF.shared.Types;
import com.github.Jochyoua.MCSF.shared.Utils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import org.bukkit.event.Listener;

public class DiscordEvents implements Listener {
    private final Utils utils;
    public DiscordEvents(MCSF plugin, Utils utils){
        this.utils = utils;
        if (plugin.getConfig().getBoolean("settings.discordSRV.enabled") && utils.supported("DiscordSRV")) {
            try {
                DiscordSRV.api.subscribe(this);
            } catch (Exception e) {
                String message = e.getMessage();
                utils.debug("Registered DiscordSRV event unsuccessfully: " + message);
            }
        }
    }

    @Subscribe
    public void DiscordGameMessage(
            final github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent event) {
        event.setProcessedMessage(utils.clean(event.getProcessedMessage(), true, false, Types.Filters.DISCORD));
    }
}