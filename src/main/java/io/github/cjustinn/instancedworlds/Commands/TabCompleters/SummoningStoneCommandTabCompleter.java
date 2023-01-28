package io.github.cjustinn.instancedworlds.Commands.TabCompleters;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Summoning.SummoningStone;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SummoningStoneCommandTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        List<String> completions = new ArrayList<>();

        if (strings.length == 1) {
            completions.add("create");
            completions.add("delete");
        } else if (strings.length == 2) {
            if (strings[0].equalsIgnoreCase("delete")) {
                completions.addAll(InstancedWorldsManager.summoningStones.stream().filter(stone -> stone.getReadableId().contains(strings[1])).map(SummoningStone::getReadableId).collect(Collectors.toList()));
            }
        }

        return completions;

    }
}
