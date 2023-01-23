package io.github.cjustinn.instancedworlds.Commands;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import org.apache.logging.log4j.CloseableThreadContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;

public class PortalCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You cannot create a portal from the console!");
            return false;
        }

        /*
            ARGUMENT EXPECTATIONS
            ======================
            args[0]     -      The sub-command.
                valid options: [ create, delete ]

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
            } else if (!InstancedWorldsManager.templateExistsByName(args[1])) {
                sender.sendMessage(ChatColor.RED + "There are no template worlds with that name.");
                return false;
            }

            Bukkit.getPluginManager().registerEvents(new PortalCreationListener((Player) sender, args[1], args[2]), Bukkit.getPluginManager().getPlugin("InstancedWorlds"));
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (!sender.hasPermission("instancedworlds.portals.create")) {
                sender.sendMessage(ChatColor.RED + "You do not have the necessary permissions to create an instance portal.");
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
