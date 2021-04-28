package io.github.jochyoua.mychristianswearfilter.commands.modifications;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.User;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.IllegalArgumentException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import io.github.jochyoua.mychristianswearfilter.shared.hikaricp.HikariCP;
import org.bukkit.command.CommandSender;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class UnsetCommand {
    MCSF plugin;
    Manager manager;

    public UnsetCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    /**
     * This method removes a user from the database
     *
     * @param sender CommandSender
     * @param args   Command args
     * @throws IllegalArgumentException if the arguments are too short
     * @throws FailureException         if the user doesn't exist
     * @throws NoPermissionException    if the CommandSender lacks the `MCSF.modify.unset' permission
     */
    public void execute(CommandSender sender, String[] args) throws NoPermissionException, FailureException, IllegalArgumentException {
        if (!sender.hasPermission("MCSF.modify.unset")) {
            throw new NoPermissionException(plugin.getLanguage());
        }
        if (args.length == 1) {
            throw new IllegalArgumentException(plugin.getLanguage());
        }
        UUID targetid = null;
        if (manager.supported("mysql"))
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
            try (
                    PreparedStatement ps = manager.getUserConnection().prepareStatement(HikariCP.Query.USERS.delete)) {
                ps.setString(1, targetid.toString());
                ps.execute();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (manager.supported("mysql"))
                try (
                        PreparedStatement ps = manager.getConnection().prepareStatement(HikariCP.Query.USERS.delete)) {
                    ps.setString(1, targetid.toString());
                    ps.execute();
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
        if (!new User(manager, targetid).exists()) {
            manager.send(sender,
                    Objects.requireNonNull(plugin.getLanguage().getString("variables.success")).
                            replaceAll("(?i)\\{message}|(?i)%message%", Objects.requireNonNull(plugin.getLanguage().getString("variables.successful.removed_player"))).
                            replaceAll("(?i)\\{target}|(?i)%target%", args[1]));
        } else {
            throw new FailureException(plugin.getLanguage(),
                    Objects.requireNonNull(plugin.getLanguage().getString("variables.error.invalidtarget")).replaceAll("(?i)\\{target}|(?i)%target%", args[1]));
        }
    }
}
