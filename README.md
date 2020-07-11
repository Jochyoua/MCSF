# MCSF (MY CHRISTIAN SWEAR FILTER)
This is my first plugin I've ever made and I'm going to try to keep it updated for as long as possible.
Currently this plugin has been tested with Minecraft Java edition versions 1.8.8 through 1.15.2.

[Download latest version from Spigot](https://www.spigotmc.org/resources/mcsf-my-christian-swear-filter-100-customizable.54115/)

MCSF is a swear filter that enables your server to let users personally toggle their filter status. That means users who feel as if they don't need to have a swear filter could toggle it on/off whenever they please.

# FEATURES:
- 100% customizable, every message can be changed
- The ability to disable/enable the swear filter per player
- [DiscordSRV](https://www.spigotmc.org/resources/discordsrv.18494/) support
- MySQL support via [MySQL API](https://www.spigotmc.org/resources/mysql-api.23932/)

# REQUIRMENTS:
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
- Java 8+

# COMMANDS:
/MCSF toggle <player> (Permission: MCSF.toggle & MCSF.modify is targetting other users)
This command toggles a users's swear filter.
  
/MCSF reload (Permission: MCSF.reload)
This command reloads the configuration files

/MCSF status <player> (Permission: MCSF.modify if targetting other users)
This command views the status of a user's swear filter status
  
/MCSF add/remove <word> (Permission:MCSF.modify)
Modifies the blacklist
  
/MCSF unset \[all/player/uuid] (Permission:MCSF.modify)
Removes remove specific or all users from the config/database

/MCSF whitlist \[word] (Permission:MCSF.modify)
Adds/Removes word from the whitelist (Requires settings.whitelist to be true)

# PLACEHOLDERS:
%prefix%: gets the prefix set in config
%command%: gets the command set in config
%player%: gets the user's name
%message%: gets the failure/success value if applicable

# NOTES:
- If you have this plugin on your server, be sure to let me know so I can add it here.

# Installation instructions:
Download both the ProtocolLib & this plugin
Place both these plugins inside of the plugins folder

(OPTIONAL)
If you plan on using [DiscordSRV](https://www.spigotmc.org/resources/discordsrv.18494/) you must first download the plugin and then add them to your server and then restart the server.

# IMAGES:
![](https://oof.ddns.net/u/05.31.01-05.05.20.png)
![](https://oof.ddns.net/u/05.29.04-05.05.20.png)
![](https://oof.ddns.net/u/05.28.55-05.05.20.png)
