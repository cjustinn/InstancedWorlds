package io.github.cjustinn.instancedworlds.Commands.Executors;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Parties.Party;
import io.github.cjustinn.instancedworlds.Summoning.SummoningInvite;
import io.github.cjustinn.instancedworlds.Summoning.SummoningStone;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SummonCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You cannot summon a player from the console!");
            return false;
        } else if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You must provide a sub-command!");
            return false;
        }

        /*
            ARGUMENT EXPECTATIONS
            ======================
            args[0]     -      The sub-command.
                valid options: [ send, accept ]

            args[1]     -      The target player for the 'send' command.
                valid options: [ player name (send) ]
        */
        if (args[0].equalsIgnoreCase("send")) {
            if (!sender.hasPermission("instancedworlds.summoning.send")) {
                sender.sendMessage(ChatColor.RED + "You do not have the required permissions to run that command!");
                return false;
            } else if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a summon target!");
                return false;
            } else if (!InstancedWorldsManager.playerIsInParty(((Player) sender).getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "You must be in a party to summon another player!");
                return false;
            }

            // Check if the player is in range of any stones.
            final int stoneIndex = InstancedWorldsManager.getSummoningStoneIndexInRange(((Player) sender).getLocation());
            if (stoneIndex >= 0) {
                final int senderPartyIndex = InstancedWorldsManager.getPlayerPartyIndex(((Player) sender).getUniqueId());
                if (senderPartyIndex >= 0) {
                    Party senderParty = InstancedWorldsManager.parties.get(senderPartyIndex);
                    if (senderParty != null) {

                        Player target = Bukkit.getPlayer(args[1]);
                        if (target != null) {

                            // Check if the player is in your party.
                            if (senderParty.playerIsInParty(target.getUniqueId())) {

                                // Check if the player has already been summoned.
                                if (!InstancedWorldsManager.playerHasSummoningInvite(target.getUniqueId())) {
                                    SummoningStone stone = InstancedWorldsManager.summoningStones.get(stoneIndex);

                                    // Send the player a summon invite.
                                    InstancedWorldsManager.registerSummoningInvite(new SummoningInvite(stone.getSummoningPoint(), stone.getName(), (Player) sender, target));

                                    // Notify the sender that the invite was sent.
                                    sender.sendMessage(String.format("%sYou have sent %s a summon invite.", ChatColor.GREEN, ((TextComponent) target.displayName()).content()));
                                } else {
                                    sender.sendMessage(ChatColor.RED + "The target player already has a pending summon invite!");
                                    return false;
                                }

                            } else {
                                sender.sendMessage(ChatColor.RED + "The target player is not in your party!");
                                return false;
                            }

                        }

                    }
                }
            } else {
                sender.sendMessage(String.format("%sYou are not within %d blocks of any summoning stones!", ChatColor.RED, InstancedWorldsManager.summonStoneUseRange));
                return false;
            }


        } else if (args[0].equalsIgnoreCase("accept")) {

            if (!sender.hasPermission("instancedworlds.summoning.accept")) {
                sender.sendMessage(ChatColor.RED + "You do not have the required permissions to run that command!");
                return false;
            }

            // Check if the player has any active summoning invites.
            if (InstancedWorldsManager.playerHasSummoningInvite(((Player) sender).getUniqueId())) {

                // Get the invite index.
                final int inviteIndex = InstancedWorldsManager.getPlayerSummoningInviteIndex(((Player) sender).getUniqueId());
                if (inviteIndex >= 0) {

                    // Get the invite object from the list.
                    SummoningInvite invite = InstancedWorldsManager.summoningInvites.get(inviteIndex);
                    if (invite != null) {

                        // Accept the invite.
                        invite.acceptInvite();

                        // Remove the invite from the manager list.
                        InstancedWorldsManager.summoningInvites.remove(inviteIndex);

                        // Notify the recipient that they have been summoned.
                        sender.sendMessage(String.format("%sYou have been summoned to %s!", ChatColor.GREEN, invite.getName()));

                    }

                }

            } else {
                sender.sendMessage(ChatColor.RED + "You do not have any pending summoning invites!");
                return false;
            }

        }

        return true;
    }
}
