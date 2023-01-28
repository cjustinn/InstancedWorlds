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
            completions.add("send");
            completions.add("accept");
        } else if (strings.length == 2) {
            if (strings[0].equalsIgnoreCase("send")) {
                for (Player player : Bukkit.getOnlinePlayers().stream().filter(p -> ((TextComponent) p.displayName()).content().contains(strings[1])).collect(Collectors.toList())) {
                    completions.add(((TextComponent) player.displayName()).content());
                }
            }
        }

        return completions;
    }
}
