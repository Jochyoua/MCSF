package io.github.jochyoua.mychristianswearfilter.commands.information;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class StatusCommand {
    MCSF plugin;
    Manager manager;

    public StatusCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * Shows the status of self or other users to the CommandSender
     *
     * @param sender CommandSender
     * @param args   command arguments
     * @throws NoPermissionException if the CommandSender lacks the `MCSF.use.status' or 'MCSF.modify.others' permission
     * @throws FailureException      if the target doesn't exist
     */
    public void execute(CommandSender sender, String[] args) throws NoPermissionException, FailureException {
        boolean value;
        UUID targetid;
        if (!sender.hasPermission("MCSF.use.status")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (args.length == 1 && (sender instanceof Player)) {
            User user = new User(manager, ((Player) sender).getUniqueId());
            if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                value = true;
            } else {
                value = user.status();
            }
            manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.status")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")))));
        } else {
            if (args.length != 2) {
                throw new FailureException(plugin.getLanguage(),
                        Objects.requireNonNull(plugin.getLanguage().getString("variables.error.invalidtarget")).replaceAll("(?i)\\{target}|(?i)%target%", sender.getName()));
            }
            if (!sender.hasPermission("MCSF.modify.others")) {
                throw new NoPermissionException(plugin.getLanguage());
            }
            targetid = null;
            for (final String key : manager.getUsers(false)) {
                User user = new User(manager, UUID.fromString(key));
                if (user.exists()) {
                    if (user.playerName().equalsIgnoreCase(args[1])) {
                        targetid = UUID.fromString(key);
                    } else if (user.getId().toString().equalsIgnoreCase(args[1])) {
                        targetid = UUID.fromString(key);
                    }
                }
            }
            if (targetid == null) {
                throw new FailureException(plugin.getLanguage(),
                        Objects.requireNonNull(plugin.getLanguage().getString("variables.error.invalidtarget")).replaceAll("(?i)\\{target}|(?i)%target%", args[1]));
            } else {
                User user = new User(manager, targetid);
                if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                    value = true;
                } else {
                    value = user.status();
                }
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.status")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")))));
            }
        }
    }
}
