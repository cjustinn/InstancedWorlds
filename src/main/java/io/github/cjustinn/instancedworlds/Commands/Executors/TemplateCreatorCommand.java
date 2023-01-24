package io.github.cjustinn.instancedworlds.Commands.Executors;

import com.sun.org.apache.xpath.internal.operations.Bool;
import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import net.kyori.adventure.util.TriState;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateCreatorCommand implements CommandExecutor {

    // Function to take a class type and string value, and return the string value converted into the passed class type.
    private <T> @Nullable T conformValueToExpectedType(Class<T> type, String value) {
        T conformedValue;

        if (type == Integer.class) conformedValue = (T) Integer.valueOf(value);
        else if (type == Double.class) conformedValue = (T) Double.valueOf(value);
        else if (type == Boolean.class) conformedValue = (T) Boolean.valueOf(value);
        else if (type == String.class) conformedValue = (T) value;
        else conformedValue = null;

        return conformedValue;
    }

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

            args[3]     -      The value for the 'set gamerule' command.
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

            // Check if the command as-sent is invalid.
            if (!(sender instanceof Player)) {
                // The sender isn't a player.
                sender.sendMessage(ChatColor.RED + "This command cannot be run from the console!");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must define what setting you want to update!");
                return false;
            } else if (!args[1].equalsIgnoreCase("gamerule") && !args[1].equalsIgnoreCase("spawn")) {
                sender.sendMessage(ChatColor.RED + "You can only set 'gamemode' or 'spawn'!");
                return false;
            }

            World world = ((Player) sender).getWorld();
            if (args[1].equalsIgnoreCase("gamerule")) {
                // Check if the command as-sent is invalid.
                if (!sender.hasPermission("instancedworlds.template.set.gamemode")) {
                    // Player doesn't have permission.
                    sender.sendMessage(ChatColor.RED + "You do not have permission to run this command!");
                    return false;
                } else if (args.length < 3) {
                    // No gamerule name was provided.
                    sender.sendMessage(ChatColor.RED + "You must provide a gamerule name!");
                    return false;
                }

                // Check if the gamerule passed is a valid name.
                String target = "";
                boolean found = false;
                for (String gameruleName : Arrays.stream(GameRule.values()).map(GameRule::getName).collect(Collectors.toList())) {
                    // If the gamerule is found, update the flag variable to true and also assign the "target" value to the gamerule name from the list; this is just to be safe about case sensitivity.
                    if (gameruleName.equalsIgnoreCase(args[2])) {
                        found = true;
                        target = gameruleName;
                    }
                }

                // The gamerule that was provided was valid.
                if (found) {
                    if (args.length < 4) {
                        // No gamerule value was provided.
                        sender.sendMessage(ChatColor.RED + "You must provide a new value for the gamerule!");
                        return false;
                    } else if (!world.getName().startsWith("template_")) {
                        // The world that the player is in is NOT a template world.
                        sender.sendMessage(ChatColor.RED + "That command can only be used within template worlds!");
                        return false;
                    }

                    // Update the value of the gamerule.
                    GameRule targetGamerule = GameRule.getByName(target);
                    world.setGameRule(targetGamerule, conformValueToExpectedType(targetGamerule.getType(), args[3]));

                    // Inform the player.
                    sender.sendMessage(String.format("%sThe %s gamerule has been updated to %s for world '%s'.", ChatColor.GREEN, target, args[3], world.getName()));
                }

            } else if (args[1].equalsIgnoreCase("spawn")) {
                // Check if the command as-sent is invalid.
                if (!world.getName().startsWith("template_")) {
                    // The sender isn't a player.
                    sender.sendMessage(ChatColor.RED + "You can only run this command inside of a template world!");
                    return false;
                }

                // Update the world spawn location.
                world.setSpawnLocation(((Player) sender).getLocation());

                // Notify the player.
                sender.sendMessage(String.format("%sYou have updated the world spawn to your position for world '%s'.", ChatColor.GREEN, world.getName()));
            }

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
