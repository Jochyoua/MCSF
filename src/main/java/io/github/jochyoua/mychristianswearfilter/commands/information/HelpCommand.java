package io.github.jochyoua.mychristianswearfilter.commands.information;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.command.CommandSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpCommand {
    MCSF plugin;
    Manager manager;

    public HelpCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * Shows the help command to the CommandSender
     *
     * @param sender CommandSender
     * @throws NoPermissionException if the CommandSender lacks the `MCSF.use.help` permission
     */
    public void execute(CommandSender sender) throws NoPermissionException {
        if (!sender.hasPermission("MCSF.use.help")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        StringBuilder message = new StringBuilder();
        int length = plugin.getLanguage().getStringList("variables.help").size();
        for (String str : plugin.getLanguage().getStringList("variables.help")) {
            length -= 1;
            Matcher match = Pattern.compile("(?i)\\{PERMISSION=(.*?)}|(?i)<%PERMISSION=(.*?)%>", Pattern.DOTALL).matcher(str);
            String permission = null;
            while (match.find()) {
                permission = match.group(1);
            }
            if (!(permission == null)) {
                if (!sender.hasPermission(permission)) {
                    continue;
                } else {
                    str = str.replaceAll("(?i)\\{PERMISSION=(.*?)}|(?i)<%PERMISSION=(.*?)%>", "");
                }
            }
            if (length <= 0) { // length is the end
                message.append(str);
            } else { // length is not the end
                message.append(str).append("\n");
            }
        }
        manager.send(sender, message.toString());
    }
}
