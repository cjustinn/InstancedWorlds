package io.github.cjustinn.instancedworlds.Commands.Executors;

import io.github.cjustinn.instancedworlds.Commands.Listeners.SummoningStoneCreationListener;
import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Summoning.SummoningStone;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
                valid options: [ create, delete ]

            args[1]     -      Readable id for the 'create' and 'delete' commands.
                valid options: [ readable id (create / delete) ]

            args[2]     -      Summoning Stone location name for the create command.
                valid options: [ location name (create) ]
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

                // Remove the object from the list.
                InstancedWorldsManager.summoningStones.remove(index);
            }

        }

        return true;
    }
}
