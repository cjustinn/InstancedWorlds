package io.github.cjustinn.instancedworlds.Commands.TabCompleters;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateCreatorTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(new ArrayList<String>() {{ add("create"); add("set"); add("delete"); }}.stream().filter(o -> o.toLowerCase().contains(args[0].toLowerCase())).collect(Collectors.toList()));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete")) {
                completions.addAll(InstancedWorldsManager.getTemplateNames().stream().filter(t -> t.toLowerCase().contains(args[1].toLowerCase())).collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("set")) {
                completions.addAll(new ArrayList<String>() {{ add("gamerule"); add("spawn"); }}.stream().filter(o -> o.toLowerCase().contains(args[1].toLowerCase())).collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("gamerule")) {
                completions.addAll(Arrays.stream(GameRule.values()).filter(gr -> gr.getName().toLowerCase().contains(args[2].toLowerCase())).map(GameRule::getName).collect(Collectors.toList()));
            }
        }

        return completions;
    }
}
