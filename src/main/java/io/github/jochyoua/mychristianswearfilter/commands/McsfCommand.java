package io.github.jochyoua.mychristianswearfilter.commands;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.commands.information.HelpCommand;
import io.github.jochyoua.mychristianswearfilter.commands.information.StatusCommand;
import io.github.jochyoua.mychristianswearfilter.commands.information.ToggleCommand;
import io.github.jochyoua.mychristianswearfilter.commands.information.VersionCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.AddCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.GlobalCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.ParseCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.ReloadCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.RemoveCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.ResetCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.UnsetCommand;
import io.github.jochyoua.mychristianswearfilter.commands.modifications.WhitelistCommand;
import io.github.jochyoua.mychristianswearfilter.shared.Data;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.CommandDisabledException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.FailureException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.IllegalArgumentException;
import io.github.jochyoua.mychristianswearfilter.shared.exceptions.NoPermissionException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class McsfCommand implements CommandExecutor {
    MCSF plugin;
    Manager manager;

    private AddCommand addCommand;
    private ParseCommand parseCommand;
    private ReloadCommand reloadCommand;
    private RemoveCommand removeCommand;
    private ResetCommand resetCommand;
    private UnsetCommand unsetCommand;
    private WhitelistCommand whitelistCommand;
    private GlobalCommand globalCommand;

    private HelpCommand helpCommand;
    private StatusCommand statusCommand;
    private ToggleCommand toggleCommand;
    private VersionCommand versionCommand;

    public McsfCommand(MCSF plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();

        setSubCommands();

        PluginCommand cmd = plugin.getCommand("mcsf");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter((sender, command, s, args) -> {
                final List<String> completions = new ArrayList<>();
                if (args.length == 1) {
                    List<Data.Arguments> arguments = new ArrayList<>();
                    for (Data.Arguments a : Data.Arguments.values()) {
                        if (sender.hasPermission(a.getPermission())) {
                            arguments.add(a);
                        }
                    }
                    StringUtil.copyPartialMatches(args[0], Stream.of(arguments.toArray(new Data.Arguments[0]))
                            .map(Enum::name)
                            .collect(Collectors.toList()), completions);
                }
                if (args.length >= 2) {
                    if (!sender.hasPermission("MCSF.modify"))
                        return null;
                    switch (args[0]) {
                        case "remove":
                            List<String> swears = manager.getLocalSwears();
                            StringUtil.copyPartialMatches(args[1], swears, completions);
                            break;
                        case "reset":
                            StringUtil.copyPartialMatches(args[1], Collections.singletonList("confirm"), completions);
                            break;
                        case "whitelist":
                            List<String> white = manager.getLocalWhitelist();
                            StringUtil.copyPartialMatches(args[1], white, completions);
                            break;
                        case "global":
                            List<String> global = manager.getGlobalSwears();
                            StringUtil.copyPartialMatches(args[1], global, completions);
                            break;
                        case "status":
                        case "toggle":
                        case "unset":
                            List<String> users = manager.getUsers(true);
                            StringUtil.copyPartialMatches(args[1], users, completions);
                            break;
                    }
                }
                Collections.sort(completions);
                return completions;
            });
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!(plugin.getConfig().getInt("settings.cooldown") <= 0) && !sender.hasPermission("MCSF.bypass")) {
                if (!manager.getCooldowns().containsKey(player.getUniqueId())) {
                    manager.addUser(player.getUniqueId());
                } else {
                    long duration = manager.getCooldowns().getExpectedExpiration(player.getUniqueId());
                    duration = TimeUnit.MILLISECONDS.toSeconds(duration);
                    if (duration != 0) {
                        manager.send(player, Objects.requireNonNull(plugin.getLanguage().getString("variables.cooldown")).replaceAll("(?i)\\{duration}|(?i)%duration%", String.valueOf(duration)));
                        return true;
                    }
                }
            }
        }
        CompletableFuture.runAsync(() -> {
            try {
                String arg = (args.length == 0 ? "mcsf" : args[0]).toLowerCase();
                if (plugin.getConfig().isSet("settings.command args enabled." + arg)) {
                    if (!plugin.getConfig().getBoolean("settings.command args enabled." + arg)) {
                        throw new CommandDisabledException(plugin.getLanguage());
                    }
                }
                try {
                    if (!sender.hasPermission(Data.Arguments.valueOf(arg).getPermission()))
                        throw new NoPermissionException(plugin.getLanguage());
                } catch (java.lang.IllegalArgumentException | NullPointerException ignored) {
                }
                switch (arg) {
                    case "help":
                    default:
                        helpCommand.execute(sender);
                        break;
                    case "toggle":
                        toggleCommand.execute(sender, args);
                        break;
                    case "status":
                        statusCommand.execute(sender, args);
                        break;
                    case "version":
                        versionCommand.execute(sender);
                        break;
                    case "unset":
                        unsetCommand.execute(sender, args);
                        break;
                    case "reset":
                        resetCommand.execute(sender, args);
                        break;
                    case "add":
                        addCommand.execute(sender, args);
                        break;
                    case "remove":
                        removeCommand.execute(sender, args);
                        break;
                    case "whitelist":
                        whitelistCommand.execute(sender, args);
                        break;
                    case "reload":
                        reloadCommand.execute(sender);
                        break;
                    case "global":
                        globalCommand.execute(sender, args);
                        break;
                    case "parse":
                        parseCommand.execute(sender, args);
                        break;
                }
            } catch (CommandDisabledException | NoPermissionException | FailureException | IllegalArgumentException ex) {
                manager.send(sender, ex.getMessage());
            }
        });
        return true;
    }

    private void setSubCommands() {
        addCommand = new AddCommand(plugin);
        parseCommand = new ParseCommand(plugin);
        reloadCommand = new ReloadCommand(plugin);
        removeCommand = new RemoveCommand(plugin);
        resetCommand = new ResetCommand(plugin);
        unsetCommand = new UnsetCommand(plugin);
        whitelistCommand = new WhitelistCommand(plugin);
        globalCommand = new GlobalCommand(plugin);

        helpCommand = new HelpCommand(plugin);
        statusCommand = new StatusCommand(plugin);
        toggleCommand = new ToggleCommand(plugin);
        versionCommand = new VersionCommand(plugin);
    }
}
