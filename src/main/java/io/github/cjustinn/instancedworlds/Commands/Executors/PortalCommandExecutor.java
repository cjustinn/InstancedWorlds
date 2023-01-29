package io.github.cjustinn.instancedworlds.Commands.Executors;

import io.github.cjustinn.instancedworlds.Commands.Listeners.PortalCreationListener;
import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PortalCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You cannot use this command from the console!");
            return false;
        }

        /*
            ARGUMENT EXPECTATIONS
            ======================
            args[0]     -      The sub-command.
                valid options: [ create, origin, delete ]

            args[1]     -      Target for the 'create' and 'delete' commands.
                valid options: [ template name (create), portal uuid (delete) ]
        */
        if (args[0].equalsIgnoreCase("create")) {
            if (!sender.hasPermission("instancedworlds.portals.create")) {
                sender.sendMessage(ChatColor.RED + "You do not have the necessary permissions to create an instance portal.");
                return false;
            } else if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "You must specify a template world name and a portal name.");
                return false;
            } else if (!InstancedWorldsManager.templateExistsById(args[1])) {
                sender.sendMessage(ChatColor.RED + "There are no template worlds with that name.");
                return false;
            }

            Bukkit.getPluginManager().registerEvents(new PortalCreationListener((Player) sender, args[1], args[2]), Bukkit.getPluginManager().getPlugin("InstancedWorlds"));
        } else if (args[0].equalsIgnoreCase("origin")) {
            if (!sender.hasPermission("instancedworlds.portals.origin")) {
                sender.sendMessage(ChatColor.RED + "You do not have the necessary permissions to modify the origin of an instance portal.");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a portal name.");
                return false;
            }

            if (InstancedWorldsManager.portalExistsWithName(args[1])) {
                final int index = InstancedWorldsManager.getPortalIndexByName(args[1]);
                Location location = ((Player) sender).getLocation();

                InstancedWorldsManager.portals.get(index).setOrigin(location);
                InstancedWorldsManager.saveConfigValue(String.format("portals.%s.origin", InstancedWorldsManager.portals.get(index).getPortalId()), String.format("%s;%f;%f;%f;%f;%f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));

                sender.sendMessage(String.format("%sThe origin for portal '%s' has been updated to your position.", ChatColor.GREEN, InstancedWorldsManager.portals.get(index).getName()));

            } else {
                sender.sendMessage(ChatColor.RED + "There was a problem updating the portal origin! Please try again.");
                return false;
            }

        } else if (args[0].equalsIgnoreCase("delete")) {
            if (!sender.hasPermission("instancedworlds.portals.delete")) {
                sender.sendMessage(ChatColor.RED + "You do not have the necessary permissions to delete an instance portal.");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a portal name.");
                return false;
            }

            if (InstancedWorldsManager.portalExistsWithName(args[1])) {

                final int index = InstancedWorldsManager.getPortalIndexByName(args[1]);
                InstancePortal portal = InstancedWorldsManager.portals.get(index);

                InstancedWorldsManager.saveConfigValue(String.format("portals.%s", portal.getPortalId()), null);
                InstancedWorldsManager.portals.remove(index);

                sender.sendMessage(String.format("%sPortal [%s] has been deleted.", ChatColor.GREEN, args[1]));

            } else {
                sender.sendMessage(ChatColor.RED + "There is no portal with that name.");
                return false;
            }
        }

        return true;
    }
}
