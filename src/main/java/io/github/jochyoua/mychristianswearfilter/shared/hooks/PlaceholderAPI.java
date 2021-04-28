package io.github.jochyoua.mychristianswearfilter.shared.hooks;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPI extends PlaceholderExpansion {
    private final MCSF plugin;

    public PlaceholderAPI(MCSF plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return plugin.getConfig().getBoolean("settings.enable placeholder api");
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getIdentifier() {
        return "mcsf";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        User user = new User(plugin.getManager(), player.getUniqueId());
        switch (identifier.toLowerCase()) {
            case "player_name":
                return user.playerName();
            case "player_flags":
                return String.valueOf(user.getFlags());
            case "player_status":
                return String.valueOf(user.status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"));
            case "version":
                return plugin.getDescription().getVersion();
        }
        return null;
    }
}
