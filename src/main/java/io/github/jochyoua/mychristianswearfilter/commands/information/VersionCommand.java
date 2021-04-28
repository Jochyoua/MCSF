package io.github.jochyoua.mychristianswearfilter.commands.information;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.command.CommandSender;

public class VersionCommand {
    MCSF plugin;
    Manager manager;

    public VersionCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * Shows version information of MCSF
     *
     * @param sender CommandSender
     * @throws NoPermissionException if the CommandSender lacks the `MCSF.use.version' permission
     */
    public void execute(CommandSender sender) throws NoPermissionException {
        if (sender.hasPermission("MCSF.use.version")) {
            StringBuilder version = new StringBuilder();
            int length = plugin.getLanguage().getStringList("variables.version").size();
            for (String str : plugin.getLanguage().getStringList("variables.version")) {
                length -= 1;
                if (length <= 0) {
                    version.append(str);
                } else {
                    version.append(str).append("\n");
                }
            }
            manager.send(sender, version.toString());
        } else {
            throw new NoPermissionException(plugin.getLanguage());
        }
    }
}
