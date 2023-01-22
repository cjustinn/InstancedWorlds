package io.github.cjustinn.instancedworlds.Commands;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Instances.InstantiatedWorld;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InstanceCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You cannot leave an instance from the console.");
            return false;
        } else if (!((Player) sender).getWorld().getName().startsWith("instance_")) {
            sender.sendMessage(ChatColor.RED + "You must be in an instance to run that command!");
            return false;
        } else if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You must provide command arguments!");
            return false;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            // Get the instance details.
            String instanceId = ((Player) sender).getWorld().getName();
            InstantiatedWorld instance = InstancedWorldsManager.instances.get(InstancedWorldsManager.getPlayerInstanceIndex(instanceId));

            // Teleport the player out of the instance and let them know that they successfully left.
            ((Player) sender).teleport(instance.getOrigin());
            sender.sendMessage(ChatColor.GOLD + "You have left the instance.");
        }

        return true;
    }
}
