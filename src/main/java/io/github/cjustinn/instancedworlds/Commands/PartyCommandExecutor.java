package io.github.cjustinn.instancedworlds.Commands;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Parties.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PartyCommandExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You must provide more command arguments!");
            return false;
        }

        /*
            ARGUMENT EXPECTATIONS
            ======================
            args[0]     -      The sub-command.
                valid options: [ create, invite, join, leave, view ]

            args[1]     -      Target for the 'invite' and 'join' commands.
                valid options: [ playerName (invite), playerName (invite) ]
        */
        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You cannot create a party from the console.");
                return false;
            } else if (!sender.hasPermission("instancedworlds.party.create")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to run that command!");
                return false;
            }

            if (InstancedWorldsManager.createParty(((Player) sender))) {
                sender.sendMessage(ChatColor.GREEN + "You have created a party! Use the '/party invite <player>' command to invite people.");
            } else {
                sender.sendMessage(ChatColor.RED + "You are already in a party!");
            }
        } else if (args[0].equalsIgnoreCase("leave")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You cannot leave a party from the console.");
                return false;
            }

            if (InstancedWorldsManager.leaveParty((Player) sender)) {
                sender.sendMessage(ChatColor.GREEN + "You have left the party!");
            } else {
                sender.sendMessage(ChatColor.RED + "You are not in a party!");
                return false;
            }
        } else if (args[0].equalsIgnoreCase("invite")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You cannot invite a player to a party from the console.");
                return false;
            } else if (!InstancedWorldsManager.playerIsLeadingParty(((Player) sender).getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "You are not the leader of a party!");
                return false;
            } else if (InstancedWorldsManager.parties.get(InstancedWorldsManager.getPlayerPartyIndex(((Player) sender).getUniqueId())).partyIsFull()) {
                sender.sendMessage(ChatColor.RED + "Your party is currently full!");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a player to invite!");
                return false;
            } else if (args[1].equalsIgnoreCase(((Player) sender).getName())) {
                sender.sendMessage(ChatColor.RED + "You cannot invite yourself to a party!");
                return false;
            } else if (InstancedWorldsManager.playerIsInParty(Bukkit.getPlayerUniqueId(args[1]))) {
                sender.sendMessage(String.format("%s%s is already in a party!", ChatColor.RED, Bukkit.getPlayer(args[1]).getName()));
                return false;
            }

            Player recipient = Bukkit.getPlayer(args[1]);
            if (recipient != null) {

                InstancedWorldsManager.invitePlayerToParty(recipient, (Player) sender);

            } else {
                sender.sendMessage(ChatColor.RED + "There are no online players with that name!");
                return false;
            }
        } else if (args[0].equalsIgnoreCase("join")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You cannot join a party from the console.");
                return false;
            } else if (InstancedWorldsManager.playerIsInParty(((Player) sender).getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "You are already in a party!");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify the name of the party leader that you want to join!");
                return false;
            }

            InstancedWorldsManager.joinParty((Player) sender, args[1].toLowerCase());
        } else if (args[0].equalsIgnoreCase("view")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You cannot view a party from the console.");
                return false;
            } else if (!InstancedWorldsManager.playerIsInParty(((Player) sender).getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "You are not in a party!");
                return false;
            }

            Party party = InstancedWorldsManager.parties.get(InstancedWorldsManager.getPlayerPartyIndex(((Player) sender).getUniqueId()));
            if (party != null) {
                String report = String.format("%s%s[Your Party (%d/%d)]:%s", ChatColor.GOLD, ChatColor.BOLD, party.getMemberCount(), InstancedWorldsManager.maxPartySize, ChatColor.RESET);

                for (int i = 0; i < party.getMembers().size(); i++) {

                    report = String.format("%s%s%s (%s%d%s/%s%d%s)", report, i == 0 ? " " : ", ", party.getMembers().get(i).getName(), ChatColor.RED, ((int) party.getMembers().get(i).getHealth()), ChatColor.RESET, ChatColor.RED, 20, ChatColor.RESET);

                }

                sender.sendMessage(report);
            }
        }

        return true;
    }

}
