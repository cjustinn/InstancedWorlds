package io.github.cjustinn.instancedworlds.Commands.TabCompleters;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PortalCreationTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(new ArrayList<String>() {{ add("create"); add("origin"); add("delete"); }}.stream().filter(o -> o.toLowerCase().contains(args[0].toLowerCase())).collect(Collectors.toList()));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                completions.addAll(InstancedWorldsManager.getTemplateNames().stream().filter(t -> t.toLowerCase().contains(args[1].toLowerCase())).collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("origin")) {
                completions.addAll(InstancedWorldsManager.portals.stream().filter(p -> p.getName().toLowerCase().contains(args[1].toLowerCase())).map(InstancePortal::getName).collect(Collectors.toList()));
            }
        }

        return completions;

    }
}
