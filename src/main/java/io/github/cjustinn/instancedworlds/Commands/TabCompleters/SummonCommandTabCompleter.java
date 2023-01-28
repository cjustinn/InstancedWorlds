package io.github.cjustinn.instancedworlds.Commands.TabCompleters;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SummonCommandTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        List<String> completions = new ArrayList<>();

        if (strings.length == 1) {
            completions.addAll(new ArrayList<String>() {{ add("send"); add("accept"); }}.stream().filter(o -> o.toLowerCase().contains(strings[0].toLowerCase())).collect(Collectors.toList()));
        } else if (strings.length == 2) {
            if (strings[0].equalsIgnoreCase("send")) {
                for (Player player : Bukkit.getOnlinePlayers().stream().filter(p -> ((TextComponent) p.displayName()).content().toLowerCase().contains(strings[1].toLowerCase())).collect(Collectors.toList())) {
                    completions.add(((TextComponent) player.displayName()).content());
                }
            }
        }

        return completions;
    }
}
