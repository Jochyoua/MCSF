# MCSF (MY CHRISTIAN SWEAR FILTER)

This is my first plugin I've ever made and I'm going to try to keep it updated for as long as possible. Currently this plugin has been tested with Minecraft Java edition versions 1.8.8 through 1.15.2.

Download latest version from [SpigotMC.org](https://www.spigotmc.org/resources/54115/)

MCSF is a swear filter that enables your server to let users personally toggle their filter status. That means users who feel as if they don't need to have a swear filter could toggle it on/off whenever they please.
# FEATURES:
* **Per player toggling**: Giving players the option to disable or enable the swear filter whenever they wish
* **DiscordSRV support**: This allows you to filter discord messages along with server chat messages, optionally you may surround blacklisted words with spoilers.
* **Sign filtering**: This allows you to filter Sign text based on the user's view distance, though this could become laggy if there are enough signs.
* **Punishments**: This allows you to punish players if they swear by executing commands.
* **Whitelists**: This is optional but allows you to remove words from being filtered, this is still a work in progress but so far it's working quite well
* **Custom regex support**: This allows you to add custom regex, by default, there is an IP and a domain regex that would remove any matches, enable it through the config!
* **UTF-8 support**: This enables you to use special characters in your blacklist so that languages like Chinese could also be filtered!
* **MySQL support**: MySQL if enabled would save both the swears and whitelist values into the database of choice along with user information.
* **Customizability**: This is probably one of the most important but every message seen by the player may be modified!
* **Multiple languages files**: MCSF supports multiple languages! Language files are located in MCSF/locales/, you may choose which one you'd like to use by changing settings.language to the file name of the language that you'd like to use :)
* **RGB support**: Any message sent by MCSF may have RGB using this format: &#RRGGBB
* **Cooldowns**: By default all MCSF messages have a time delay by 5 seconds, meaning players may only use an MCSF message every 5 seconds if they don't have the permission MCSF.bypass
* **Global blocking**: This allows you to block certain words for everyone

# REQUIRMENTS:

    (Optional: for global filtering) ProtocolLib
    Java 8+

# COMMANDS:

* **/MCSF toggle** *(Permission: MCSF.toggle & MCSF.modify is targetting other users)* This command toggles a users's swear filter.
* **/MCSF reload** *(Permission: MCSF.reload)* This command reloads the configuration files
* **/MCSF status** *(Permission: MCSF.modify if targetting other users)* This command views the status of a user's swear filter status
* **/MCSF add/remove [word]** *(Permission:MCSF.modify)* Modifies the blacklist
* **/MCSF unset [all/player/uuid]** *(Permission:MCSF.modify)* Removes remove specific or all users from the config/database
* **/MCSF whitelist [word]** *(Permission:MCSF.modify)* Adds/Removes word from the whitelist (Requires settings.whitelist to be true)
* **/MCSF global [word]** *(Permission:MCSF.modify)* Adds/Removes word from the global list (Requires settings.global blacklist to be true)
* **/MCSF reload [confirm]** *(Permission:MCSF.modify)* Reloads the configuration files and the database (can be dangerous)
# PLACEHOLDERS:

* {prefix}: gets the prefix set in config
* {command}: gets the command set in config
* {player}: gets the user's name %message%: gets the failure/success value if applicable
# NOTES:

    If you have this plugin on your server, be sure to let me know so I can add it here.

# Installation instructions:

Download this plugin into your server's plugin folder

**(OPTIONAL):** If you wish all messages to be filtered, you must install [ProtocolLib](https://www.spigotmc.org/resources/1997/) as-well!

**(OPTIONAL):** If you plan on using [DiscordSRV](https://www.spigotmc.org/resources/18494/) you must first download the plugin and then add them to your server and then restart the server.
