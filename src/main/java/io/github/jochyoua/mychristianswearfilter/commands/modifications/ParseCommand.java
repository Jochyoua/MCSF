package io.github.jochyoua.mychristianswearfilter.commands.modifications;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.IllegalArgumentException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.command.CommandSender;

import java.util.Objects;

public class ParseCommand {
    MCSF plugin;
    Manager manager;

    public ParseCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * This method parses the arguments as a singular string and runs it against the filter
     *
     * @param sender CommandSender
     * @param args   Command args
     * @throws IllegalArgumentException if the arguments are too short
     * @throws FailureException         if the status of the parse is invalid
     * @throws NoPermissionException    if the CommandSender lacks the `MCSF.modify.parse' permission
     */
    public void execute(CommandSender sender, String[] args) throws NoPermissionException, FailureException, IllegalArgumentException {
        if (!sender.hasPermission("MCSF.modify.parse")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (args.length == 1 || args.length == 2) {
            throw new IllegalArgumentException(plugin.getLanguage());
        }
        boolean state;
        switch (args[1].toLowerCase()) {
            case "true":
            case "enable":
            case "enabled":
            case "1":
                state = true;
                break;
            case "false":
            case "disabled":
            case "disable":
            case "0":
                state = false;
                break;
            default:
                throw new FailureException(plugin.getLanguage(), Objects.requireNonNull(plugin.getLanguage().getString("variables.error.invalidtype")).replaceAll("(?i)\\{arg}|(?i)%arg%", args[1]).replaceAll("(?i)\\{type}|(?i)%type%", "boolean"));
        }
        StringBuilder message = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            String arg = args[i] + " ";
            message.append(arg);
        }
        manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.parse")).replaceAll("(?i)\\{message}|(?i)%message%", manager.clean(message.toString(), false, state ? manager.reloadPattern(Data.Filters.BOTH) : manager.reloadPattern(Data.Filters.GLOBAL), Data.Filters.DEBUG)));
    }
}
