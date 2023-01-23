package io.github.cjustinn.instancedworlds.Commands;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import net.kyori.adventure.util.TriState;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class TemplateCreatorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length < 1) {
            sender.sendMessage(String.format("%sYou must provide a sub-command and arguments.", ChatColor.RED));
            return false;
        }

        /*
            ARGUMENT EXPECTATIONS
            ======================
            args[0]     -      The sub-command.
                valid options: [ create, set, delete ]

            args[1]     -      Template name for the 'create' and 'delete' commands, or the sub-sub-command for 'set'.
                valid options: [ worldName (create / delete), gamerule (set), spawn (set)

            args[2]     -      The gamerule name for the 'set gamerule' command.
                valid options: [ any ]

            args[4]     -      The value for the 'set gamerule' command.
                valid options: [ any ]
        */
        if (args[0].equalsIgnoreCase("create")) {

            // Check if the command cannot be run or is invalid for any reason.
            if (!sender.hasPermission("instancedworlds.template.create")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a template name!");
                return false;
            }

            // Notify the player that the template is being created.
            sender.sendMessage(String.format("%sCreating the template world. Please wait...", ChatColor.GREEN));

            // Create the world creator and modify its settings so that the generated world is an empty void world.
            WorldCreator creator = new WorldCreator(String.format("template_%s", args[1]));

            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generatorSettings("{ \"layers\": [{ \"block\": \"air\", \"height\": 1 }], \"biome\": \"plains\" }");

            World template = creator.createWorld();

            // Update the template's spawn position and add a bedrock block.
            Location location = new Location(template, 0.5, 62.0, 0.5);

            template.setSpawnLocation(location);
            location.getBlock().setType(Material.BEDROCK);

            // Add the template to the list.
            InstancedWorldsManager.saveTemplateWorld(template);

            // Inform the user that the template has been saved and created.
            sender.sendMessage(String.format("%sThe template has been created [%s].", ChatColor.GREEN, template.getName()));

            // If the sender was a player instead of the console, teleport them to the new world.
            if (!(sender instanceof Player)) {
                ((Player) sender).setGameMode(GameMode.CREATIVE);
                ((Player) sender).teleport(location);
            }

        } else if (args[0].equalsIgnoreCase("set")) {

        } else if (args[0].equalsIgnoreCase("delete")) {
            if (!sender.hasPermission("instancedworlds.template.delete")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must provide a template name!");
                return false;
            } else if (!InstancedWorldsManager.templateExistsByName(args[1])) {
                sender.sendMessage(ChatColor.RED + "There is no template with that name!");
                return false;
            }

            boolean success;

            // Unload the world.
            World world = InstancedWorldsManager.findTemplateByName(args[1]);
            if (Bukkit.unloadWorld(world, false)) {

                File templateDirectory = world.getWorldFolder();
                try {
                    FileUtils.deleteDirectory(templateDirectory);
                    success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    success = false;
                }

                if (success)
                    sender.sendMessage(String.format("%sThe template has been removed.", ChatColor.GREEN));
                else
                    sender.sendMessage(String.format("%sCould not remove %s.", ChatColor.RED, args[1]));

            } else {
                sender.sendMessage(String.format("%sCould not remove %s.", ChatColor.RED, args[1]));
                return false;
            }
        }

        return true;
    }
}
