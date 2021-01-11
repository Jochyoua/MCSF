package io.github.jochyoua.mychristianswearfilter.dependencies.configapi.utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

public class Logger {

    /**
     * This method should not be used outside this API
     * Print the given message to an output for debugging purposes ONLY
     * If you want to log info to the console, please use Logger#sendToConsole(String message, Plugin)
     *
     * @param message the message to be logged
     */
    public static void log(String message) {
        System.out.println("[ConfigAPI] " + message);
    }


    /**
     * Print the given message to the console
     * The name of the provided plugin will be used as prefix
     *
     * @param message the message to be sent
     * @param plugin  the plugin sending the message
     */
    public static void sendToConsole(String message, Plugin plugin) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        console.sendMessage("[" + plugin.getName() + "] " + message);
    }


    /**
     * Print the given message to the console in the provided color
     * The name of the provided plugin will be used as prefix (the prefix will not be colored)
     *
     * @param message the message to be sent
     * @param plugin  the plugin sending the message
     * @param color   the color in which the text should be displayed (or use the default color by providing null)
     */
    public static void sendToConsole(String message, Plugin plugin, ChatColor color) {
        if (color == null) {
            sendToConsole(message, plugin);
            return;
        }

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        console.sendMessage("[" + plugin.getName() + "] " + color + message);
    }

}
