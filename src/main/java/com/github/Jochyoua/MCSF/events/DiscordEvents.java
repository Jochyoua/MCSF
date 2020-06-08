package com.github.Jochyoua.MCSF.events;

import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import org.bukkit.event.Listener;

public class DiscordEvents implements Listener {
    Main plugin;
    Utils utils;

    public DiscordEvents(Main plugin, Utils utils) {
        this.plugin = plugin;
        this.utils = utils;
        if (utils.supported("DiscordSRV")) {
            try {
                DiscordSRV.api.subscribe(new Listener() {
                    @Subscribe
                    public void DiscordGuildMessageSentEvent(
                            final github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent event) {
                        //Some escaping before actually changing the message so it doesn't contain any text altering strings
                        utils.debug("Registered DiscordSRV event successfully!");
                        event.setProcessedMessage(utils.clean(event.getProcessedMessage(), true, false)
                                .replaceAll("\\*", "\\\\*")
                                .replaceAll("_", "\\_")
                                .replaceAll("\\|", "\\\\|")
                                .replaceAll("~", "\\~")
                                .replaceAll("`", "\\\\`"));
                    }
                });
            } catch (Exception e) {
                String message = e.getMessage();
                utils.debug("Registered DiscordSRV event unsuccessfully: " + message);
            }
        }
    }


}
