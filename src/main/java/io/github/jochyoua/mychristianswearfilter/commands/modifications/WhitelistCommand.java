package io.github.jochyoua.mychristianswearfilter.commands.modifications;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
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

public class WhitelistCommand {
    MCSF plugin;
    Manager manager;

    public WhitelistCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    public void execute(CommandSender sender, String[] args) throws NoPermissionException, IllegalArgumentException, FailureException, CommandDisabledException {
        if (!sender.hasPermission("MCSF.modify.whitelist")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (args.length != 2) {
            throw new IllegalArgumentException(plugin.getLanguage());
        }
        String white = args[1].toLowerCase();
        FileConfiguration local = Manager.FileManager.getFile(plugin, "data/whitelist");
        List<String> whitelist = local.getStringList("whitelist");
        if (manager.supported("mysql")) {
            if (!manager.getConnector().isWorking())
                plugin.getHikariCP().reload();
            boolean sqlexists = false;
            try {

                PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.WHITELIST.exists);
                ps.setString(1, white);
                if (ps.executeQuery().next()) {
                    sqlexists = true;
                }
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                sqlexists = false;
            }
            if (sqlexists && whitelist.contains(white)) {
                whitelist.remove(white);
                try (
                        PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.WHITELIST.delete)) {
                    ps.setString(1, white);
                    ps.execute();
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Manager.debug(sender.getName() + " has removed `" + white + "` from the whitelist database", plugin.getDebug(), Level.INFO);
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed"))));
            } else {
                if (!whitelist.contains(white))
                    whitelist.add(white);
                if (!sqlexists) {
                    try (
                            PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.WHITELIST.insert)) {
                        ps.setString(1, white);
                        ps.execute();
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.added"))));
                Manager.debug(sender.getName() + " has added `" + white + "` to the whitelist database", plugin.getDebug(), Level.INFO);
            }
        } else {
            if (whitelist.contains(white)) {
                whitelist.remove(white);
                Manager.debug(sender.getName() + " has removed `" + white + "` from the whitelist config", plugin.getDebug(), Level.INFO);
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed"))));
            } else {
                if (!whitelist.contains(white))
                    whitelist.add(white);
                Manager.debug(sender.getName() + " has added `" + white + "` to the whitelist config", plugin.getDebug(), Level.INFO);
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.added"))));
            }
        }
        local.set("whitelist", whitelist);
        Manager.FileManager.saveFile(plugin, local, "data/whitelist");
    }
}
