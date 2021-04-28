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

public class RemoveCommand {
    MCSF plugin;
    Manager manager;

    public RemoveCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * This method removes a word from the current database or configuration file
     *
     * @param sender CommandSender
     * @param args   Command args
     * @throws IllegalArgumentException if the arguments are too short
     * @throws FailureException         if the word already exists in database or configuration files or invalid regex
     * @throws NoPermissionException    if the CommandSender lacks the `MCSF.modify.remove' permission
     */
    public void execute(CommandSender sender, String[] args) throws NoPermissionException, IllegalArgumentException, FailureException {
        if (!sender.hasPermission("MCSF.modify.remove")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (args.length != 2) {
            throw new IllegalArgumentException(plugin.getLanguage());
        }
        String word = args[1].toLowerCase();
        if (word.startsWith("regex:")) {
            try {
                Pattern.compile(word.replaceAll("regex:", ""));
            } catch (Exception e) {
                throw new FailureException(plugin.getLanguage(), "The provided regex is invalid.");
            }
        }
        FileConfiguration local = Manager.FileManager.getFile(plugin, "data/swears");
        List<String> swears = local.getStringList("swears");
        if (manager.supported("mysql")) {
            if (!manager.getConnector().isWorking())
                plugin.getHikariCP().reload();
            boolean exists = false;
            try {
                PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.SWEARS.exists);
                ps.setString(1, word);
                if (ps.executeQuery().next()) {
                    exists = true;
                }
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                exists = false;
            }
            if (!exists) {
                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.doesntexist"));
            }
            swears.remove(word);
            try {
                PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.SWEARS.delete);
                ps.setString(1, word);
                ps.execute();
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            Manager.debug(sender.getName() + " has removed `" + word + "` from database", plugin.getDebug(), Level.INFO);
            manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed"))));
        } else {
            boolean modified = swears.remove(word);
            if (modified) {
                Manager.debug(sender.getName() + " has removed `" + word + "` from config", plugin.getDebug(), Level.INFO);
                manager.send(sender, Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed"))));
            } else {
                throw new FailureException(plugin.getLanguage(), plugin.getLanguage().getString("variables.error.doesntexist"));
            }
        }
        local.set("swears", swears);
        Manager.FileManager.saveFile(plugin, local, "data/swears");
    }
}
