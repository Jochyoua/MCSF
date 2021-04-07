package io.github.jochyoua.mychristianswearfilter.commands.modifications;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.command.CommandSender;

public class ReloadCommand {
    MCSF plugin;
    Manager manager;

    public ReloadCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    public void execute(CommandSender sender, String[] args) throws NoPermissionException, CommandDisabledException, FailureException {
        if (!sender.hasPermission("MCSF.modify.reload")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (plugin.getHikariCP().isEnabled()) {
            sender.sendMessage("Reloading databases:");
            plugin.getHikariCP().reload();
            if (!plugin.getHikariCP().isEnabled()) {
                throw new FailureException(plugin.getLanguage(), "Database was reloaded unsuccessfully");
            }
            sender.sendMessage("Successfully reloaded database information!");
        }
        sender.sendMessage("Resetting Swears, Global swears and whitelist:");
        try {
            manager.reloadPattern(Types.Filters.OTHER);
            sender.sendMessage("Successfully reloaded swear, global swears and whitelist information!");
        } catch (Exception e) {
            sender.sendMessage("Failed to reload: " + e.getMessage());
            e.printStackTrace();
        }
        sender.sendMessage("Reloading configuration data:");
        try {
            plugin.reloadLanguage();
            plugin.reloadConfig();
            sender.sendMessage("Successfully reloaded configuration information!");
        } catch (Exception e) {
            sender.sendMessage("Failed to reload configuration data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
