package io.github.jochyoua.mychristianswearfilter.commands.modifications;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.Objects;

public class ResetCommand {
    MCSF plugin;
    Manager manager;

    public ResetCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * This method resets the mysql database
     *
     * @param sender CommandSender
     * @param args   Command args
     * @throws NoPermissionException    if the CommandSender lacks the `MCSF.modify.add' permission
     * @throws CommandDisabledException if MySQL isn't in use
     */
    public void execute(CommandSender sender, String[] args) throws NoPermissionException, CommandDisabledException {
        if (!sender.hasPermission("MCSF.modify.reset")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (!manager.supported("mysql")) {
            throw new CommandDisabledException(plugin.getLanguage());
        }
        if (args.length != 2) {
            manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.reset")));
            return;
        } else {
            if (args[1].equalsIgnoreCase("confirm")) {
                try {
                    manager.createTable(true);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            } else {
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.reset")));
                return;
            }
        }
        manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.reset"))));
    }
}
