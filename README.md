# MCSF

This repo is a plugin for Minecraft, and I'm going to keep it updated for as long as possible. Currently, this plugin has been tested with Minecraft Java edition versions 1.8.8 through 1.16.4.

Download the latest version from [SpigotMC.org](https://www.spigotmc.org/resources/54115/)

MCSF is a swear filter that enables your server to let users personally toggle their filter status. That means users who feel like they don't need to have a swear filter could toggle it on/off whenever they please.
# FEATURES:
* **Per player toggling**:
  * Giving players the option to disable or enable the swear filter whenever they wish
* **DiscordSRV support**: 
  * This allows you to filter discord messages along with server chat messages. 
  * Optionally you may surround blacklisted words with spoilers.
* **Sign filtering**: 
  * This will enable you to filter Sign text based on the user's view distance.
  * Though this could become laggy if there are enough signs.
* **Punishments**: 
  * This allows you to punish players if they swear by executing commands.
* **Whitelists**: 
  * This is optional but will enable you to remove words from being filtered.
* **Custom regex support**: 
  * This allows you to add custom regex. 
  * By default, an IP and a domain regex would remove any matches and enable it through the config!
* **UTF-8 support**: 
  * This allows you to use special characters in your blacklist so that the filter could also filter languages like Chinese!
* **MySQL support**: 
  * MySQL, if enabled, would save both the swears and whitelist values into the database of choice along with user information.
* **Customizability**: 
  * This is probably one of the most important, but you may modify every message seen by the player!
* **Multiple languages files**: 
  * MCSF supports multiple languages! 
  * You can find language files in MCSF/locales/; you may choose which one you'd like to use by changing settings.language to the file name of the language that you'd like to use :)
* **RGB support**: 
  * Any message sent by MCSF may have RGB using this format: &#RRGGBB
* **Cooldowns**: 
  * By default, all MCSF messages have a time delay of 5 seconds.
  * A 5 second delay would mean players can only use MCSF commands every 5 seconds
  * You can disable this with the permission MCSF.bypass or set the cooldown to 0.
* **Global blocking**: 
  * This allows you to block specific words for everyone even if the player's filter is disabled.

# REQUIRMENTS:

    (Optional: for global filtering) ProtocolLib
    Java 8+
# COMMANDS:

* **/MCSF toggle [player]** *(Permission: MCSF.toggle & MCSF.modify if toggling other users)* 
  * This command toggles a users's swear filter.
* **/MCSF reload** *(Permission: MCSF.reload)* 
  * This command reloads the configuration files
* **/MCSF status [player]** *(Permission: MCSF.modify if viewing the status of other users)* 
  * This command views the status of a user's swear filter status
* **/MCSF add/remove [word]** *(Permission:MCSF.modify)* 
  * Modifies the blacklist
* **/MCSF unset [all/player/uuid]** *(Permission:MCSF.modify)* 
  * Removes remove specific or all users from the config/database
* **/MCSF whitelist [word]** *(Permission:MCSF.modify)* 
  * Adds/Removes word from the whitelist (Requires settings.whitelist to be true)
* **/MCSF global [word]** *(Permission:MCSF.modify)* 
  * Adds/Removes word from the global list (Requires settings.global blacklist to be true)
* **/MCSF reload [confirm]** *(Permission:MCSF.modify)* 
  * Reloads the configuration files, and the database (can be dangerous)

# PLACEHOLDERS:

* {prefix}: gets the prefix set in config
* {command}: gets the current command (MCSF)
* {player}: gets the user's name 
* {message}: gets the failure/success value if applicable
* {current}: gets the current version of the plugin
* {version}: gets the latest plugin verison
* {serverversion}: gets the version of the server
* {swearcount}: gets the amount of times a word was blacklisted
* {wordcount}: gets the amount of swear words in the config

# NOTES:

    If you have this plugin on your server, please let me know so I can add it here.
    This plugin is compiled with the latest Paper API and works with versions 1.8 - latest.

# Installation instructions:

Download this plugin into your server's plugin folder

**(OPTIONAL):** If you wish all messages to be filtered, you must install [ProtocolLib](https://www.spigotmc.org/resources/1997/) as-well!

**(OPTIONAL):** If you plan on using [DiscordSRV](https://www.spigotmc.org/resources/18494/), you must first download the plugin and then add them to your server and then restart the server.
