package io.github.cjustinn.instancedworlds.Commands.Executors;

import io.github.cjustinn.instancedworlds.Commands.Listeners.SummoningStoneCreationListener;
import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Summoning.SummoningStone;
import io.r2dbc.spi.Parameter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SummoningStoneCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("[InstancedWorlds]" + ChatColor.RED + " You cannot run that command from the console!");
            return false;
        } else if (!sender.hasPermission("instancedworlds.summoningstones")) {
            sender.sendMessage(ChatColor.RED + "You do not have the required permissions to run that command.");
            return false;
        }

        /*
            ARGUMENT EXPECTATIONS
            ======================
            args[0]     -      The sub-command.
                valid options: [ create, set, delete ]

            args[1]     -      Readable id for the 'create', 'set', and 'delete' commands.
                valid options: [ readable id (create / set / delete) ]

            args[2]     -      Summoning Stone location name for the 'create' command, or option name for 'set'.
                valid options: [ location name (create), [ name, summoningPoint ] (set) ]

            args[3]     -      The new name for the summoning stone for the 'name' command.
                valid options: [ location name (set name) ]
        */
        if (args[0].equalsIgnoreCase("create")) {

            // Check if any part of the command invalidates it.
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "You must provide both an id (ex: 'instance1Stone'), and a location name ('Instance One Entrance').");
                return false;
            } else if (InstancedWorldsManager.summoningStoneExistsWithReadableId(args[1])) {
                sender.sendMessage(ChatColor.RED + "A summoning stone with that id already exists.");
                return false;
            }

            Bukkit.getServer().getPluginManager().registerEvents(new SummoningStoneCreationListener(((Player) sender), args[1], args[2].replace("_", " ")), Bukkit.getPluginManager().getPlugin("InstancedWorlds"));

        } else if (args[0].equalsIgnoreCase("set")) {

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must provide a summoning stone id.");
                return false;
            } else if (!InstancedWorldsManager.summoningStoneExistsWithReadableId(args[1])) {
                sender.sendMessage(ChatColor.RED + "There is no existing summoning stone with that id.");
                return false;
            }

            final int summoningStoneIndex = InstancedWorldsManager.getSummoningStoneIndexByReadableId(args[1]);
            if (summoningStoneIndex >= 0) {

                SummoningStone stone = InstancedWorldsManager.summoningStones.get(summoningStoneIndex);
                if (stone != null) {

                    if (args[2].equalsIgnoreCase("name")) {

                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.RED + "You must provide a new summoning stone location name.");
                            return false;
                        }

                        // Update the name in the stone obj and then update it in the list.
                        stone.setName(args[3].replace("_", " "));
                        InstancedWorldsManager.summoningStones.set(summoningStoneIndex, stone);

                        // Update the config file.
                        InstancedWorldsManager.saveConfigValue(String.format("summoningstones.%s.name", stone.getUUID().toString()), args[3].replace("_", " "));

                        // Notify the sender that the command was successful.
                        sender.sendMessage(String.format("%sSummoning stone '%s' has been updated!", ChatColor.GREEN, args[3].replace("_", " ")));

                    } else if (args[2].equalsIgnoreCase("summoningPoint")) {

                        if (!((Player) sender).getLocation().getWorld().equals(stone.getOrigin().getWorld())) {
                            sender.sendMessage(ChatColor.RED + "The summoning point must be in the same world as the summoning stone's origin!");
                            return false;
                        }

                        // Update the stone obj and update its list object.
                        stone.setSummoningPoint(((Player) sender).getLocation());
                        InstancedWorldsManager.summoningStones.set(summoningStoneIndex, stone);

                        // Update the config file.
                        Location loc = ((Player) sender).getLocation();
                        InstancedWorldsManager.saveConfigValue(String.format("summoningstones.%s.summoningPoint", stone.getUUID().toString()), String.format("%s;%f;%f;%f;%f;%f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));

                        // Notify the sender that the command was successful.
                        sender.sendMessage(String.format("%sSummoning stone '%s' has been updated!", ChatColor.GREEN, stone.getName()));

                    }

                }

            }

        } else if (args[0].equalsIgnoreCase("delete")) {

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must provide a summoning stone id.");
                return false;
            } else if (!InstancedWorldsManager.summoningStoneExistsWithReadableId(args[1])) {
                sender.sendMessage(ChatColor.RED + "There is no existing summoning stone with that id.");
                return false;
            }

            // Get the summoning stone index.
            final int index = InstancedWorldsManager.getSummoningStoneIndexByReadableId(args[1]);
            if (index >= 0) {
                // Set the configuration values to null.
                SummoningStone stone = InstancedWorldsManager.summoningStones.get(index);

                InstancedWorldsManager.saveConfigValue(String.format("summoningstones.%s", stone.getUUID().toString()), null);

                // Notify the sender that the command was successful.
                sender.sendMessage(String.format("%sThe '%s' summoning stone has been deleted.", ChatColor.GREEN, stone.getReadableId()));

                // Remove the object from the list.
                InstancedWorldsManager.summoningStones.remove(index);
            }

        }

        return true;
    }
}
