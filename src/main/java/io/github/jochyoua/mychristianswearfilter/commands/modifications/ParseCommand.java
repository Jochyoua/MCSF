package io.github.jochyoua.mychristianswearfilter.commands.modifications;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.Types;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
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

    public void execute(CommandSender sender, String[] args) throws NoPermissionException, CommandDisabledException, FailureException, IllegalArgumentException {
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
        manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.parse")).replaceAll("(?i)\\{message}|(?i)%message%", manager.clean(message.toString(), false, false, state ? manager.reloadPattern(Types.Filters.BOTH) : manager.reloadPattern(Types.Filters.GLOBAL), Types.Filters.DEBUG)));
    }
}
