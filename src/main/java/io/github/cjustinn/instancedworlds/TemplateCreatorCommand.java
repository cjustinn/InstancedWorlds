package io.github.cjustinn.instancedworlds;

import net.kyori.adventure.util.TriState;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TemplateCreatorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "You must specify a template name!");
            return false;
        }

        sender.sendMessage(ChatColor.GREEN + "Creating the template world...");

        WorldCreator creator = new WorldCreator("template_" + args[0]);

        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generatorSettings("{ \"layers\": [{ \"block\": \"air\", \"height\": 1 }], \"biome\": \"plains\" }");

        World template = creator.createWorld();

        template.setSpawnLocation(new Location(template, 0.5, 62, 0.5));
        template.getSpawnLocation().getBlock().setType(Material.BEDROCK);

        InstancedWorldsManager.saveTemplateWorld(template);

        sender.sendMessage(ChatColor.GREEN + "The template world has been created [template_" + args[0] + "]!");

        if (sender instanceof Player) {
            ((Player) sender).setGameMode(GameMode.CREATIVE);
            ((Player) sender).teleport(template.getSpawnLocation());
        }

        return true;
    }
}
