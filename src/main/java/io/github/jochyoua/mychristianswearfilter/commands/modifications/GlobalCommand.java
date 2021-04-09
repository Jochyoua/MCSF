package io.github.jochyoua.mychristianswearfilter.commands.modifications;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.IllegalArgumentException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.HikariCP;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GlobalCommand {
    MCSF plugin;
    Manager manager;

    public GlobalCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * This method adds or removes a word from the current global database or configuration file
     *
     * @param sender CommandSender
     * @param args   Command args
     * @throws IllegalArgumentException if the arguments are too short
     * @throws FailureException         if the word already exists in database or configuration files or invalid regex
     * @throws NoPermissionException    if the CommandSender lacks the `MCSF.modify.global' permission
     */
    public void execute(CommandSender sender, String[] args) throws NoPermissionException, IllegalArgumentException, FailureException {
        if (!sender.hasPermission("MCSF.modify.global")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (args.length != 2) {
            throw new IllegalArgumentException(plugin.getLanguage());
        }
        String glo = args[1].toLowerCase();
        if (glo.startsWith("regex:")) {
            try {
                Pattern.compile(glo.replaceAll("regex:", ""));
            } catch (PatternSyntaxException e) {
                manager.send(sender, new FailureException(plugin.getLanguage(), "The provided regex is invalid.").getMessage());
                return;
            }
        }
        FileConfiguration local = Manager.FileManager.getFile(plugin, "data/global");
        List<String> global = local.getStringList("global");
        if (manager.supported("mysql")) {
            if (!manager.getConnector().isWorking())
                plugin.getHikariCP().reload();
            boolean sqlexists = false;
            try {
                PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.GLOBAL.exists);
                ps.setString(1, glo);
                if (ps.executeQuery().next()) {
                    sqlexists = true;
                }
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                sqlexists = false;
            }
            if (sqlexists && global.contains(glo)) {
                global.remove(glo);
                try (
                        PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.GLOBAL.delete)) {
                    ps.setString(1, glo);
                    ps.execute();
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Manager.debug(sender.getName() + " has removed `" + glo + "` from the global database", plugin.getDebug(), Level.INFO);
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed"))));
            } else {
                if (!global.contains(glo))
                    global.add(glo);
                if (!sqlexists) {
                    try (
                            PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.GLOBAL.insert)) {
                        ps.setString(1, glo);
                        ps.execute();
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.added"))));
                Manager.debug(sender.getName() + " has added `" + glo + "` to the global database", plugin.getDebug(), Level.INFO);
            }
        } else {
            if (global.contains(glo)) {
                global.remove(glo);
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed"))));
                Manager.debug(sender.getName() + " has removed `" + glo + "` from the global config", plugin.getDebug(), Level.INFO);
            } else {
                if (!global.contains(glo))
                    global.add(glo);
                Manager.debug(sender.getName() + " has added `" + glo + "` to the global config", plugin.getDebug(), Level.INFO);
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.added"))));
            }
        }
        local.set("global", global);
        Manager.FileManager.saveFile(plugin, local, "data/global");
    }
}
