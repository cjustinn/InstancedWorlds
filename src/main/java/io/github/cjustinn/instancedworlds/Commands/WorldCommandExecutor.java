package io.github.cjustinn.instancedworlds.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use the toworld command.");
            return false;
        } else if (!sender.hasPermission("instancedworlds.toworld")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
            return false;
        } else if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You must provide a world name!");
            return false;
        }

        World target = Bukkit.getWorld(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "You must provide a valid world name.");
            return false;
        }

        sender.sendMessage(ChatColor.GREEN + "Teleporting you to " + target.getName() + "'s world spawn.");
        ((Player) sender).teleport(target.getSpawnLocation());

        return true;
    }
}
