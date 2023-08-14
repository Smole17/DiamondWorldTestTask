# DiamondWorldTestTask
Here is project about test task to join on DiamondWorld project.

Was written on 1.16.5 version using Spigot API & PlaceholderAPI & ProtocolLib.

>API module (main):
> 
> * There is lightweight Bosses API.
> * Using MySQL to save fight result with them.
> * Using ORMLite to connect to MySQL.
> * Using ProtocolLib for packet armor stands to set timer on bosses.
> 
> API implementation in "bukkit-plugin" module:
> * There is Robber and Summoner in an impl package.
> * plugin.yml was released in build.gradle module file.
> * Using config.yml to configure bosses.