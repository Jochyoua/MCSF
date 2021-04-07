package io.github.jochyoua.mychristianswearfilter.commands.information;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class ToggleCommand {
    MCSF plugin;
    Manager manager;

    public ToggleCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    public void execute(CommandSender sender, String[] args) throws CommandDisabledException, NoPermissionException, FailureException {
        boolean value;
        UUID targetid = null;
        if (args.length == 1 && (sender instanceof Player)) {
            User user = new User(manager, ((Player) sender).getUniqueId());
            if (!user.exists()) {
                user.create(sender.getName(), plugin.getConfig().getBoolean("settings.filtering.default"));
            }
            if (!sender.hasPermission("MCSF.use.toggle")) {
                throw new NoPermissionException(plugin.getLanguage());
            }
            if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                throw new CommandDisabledException(plugin.getLanguage());
            }
            if (sender.hasPermission("MCSF.bypass")) {
                value = user.toggle();
                if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(plugin.getLanguage().getString("variables.targetToggle")).
                            replaceAll("(?i)\\{value}|(?i)%value%", Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.toggle"))
                        .replaceAll("(?i)\\{value}|(?i)%value%", Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));

                return;
            }
            value = user.toggle();
            manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.toggle"))
                    .replaceAll("(?i)\\{value}|(?i)%value%", Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
            if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(plugin.getLanguage().getString("variables.targetToggle"))
                        .replaceAll("(?i)\\{value}|(?i)%value%", Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
        } else {
            if (args.length != 2) {
                throw new FailureException(plugin.getLanguage(),
                        Objects.requireNonNull(plugin.getLanguage().getString("variables.error.invalidtarget")).replaceAll("(?i)\\{target}|(?i)%target%", args[1]));
            }
            if (!sender.hasPermission("MCSF.modify.others")) {
                throw new NoPermissionException(plugin.getLanguage());
            } else if (sender.hasPermission("MCSF.bypass")) {
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
                    value = user.toggle();
                    if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                        manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(plugin.getLanguage().getString("variables.targetToggle"))
                                .replaceAll("(?i)\\{value}|(?i)%value%", Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                    manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.targetToggle")).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()).replaceAll("(?i)\\{value}|(?i)%value%", (Objects.requireNonNull(value ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")))));
                }
                return;
            }
            if (plugin.getConfig().getBoolean("settings.filtering.force")) {
                throw new CommandDisabledException(plugin.getLanguage());
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
                user.toggle();
                if (plugin.getConfig().getBoolean("settings.filtering.log filter changes") && !(sender instanceof ConsoleCommandSender))
                    manager.send(Bukkit.getConsoleSender(), Objects.requireNonNull(plugin.getLanguage().getString("variables.targetToggle")).replaceAll("(?i)\\{value}|(?i)%value%", Objects.requireNonNull(user.status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated"))).replaceAll("(?i)\\{target}|(?i)%target%", user.playerName()));
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.targetToggle")).replaceAll("(?i)\\{target}|(?i)%target%", args[1]).replaceAll("(?i)\\{value}|(?i)%value%", (Objects.requireNonNull(new User(manager, targetid).status() ? plugin.getLanguage().getString("variables.activated") : plugin.getLanguage().getString("variables.deactivated")))));
            }
        }
    }
}
