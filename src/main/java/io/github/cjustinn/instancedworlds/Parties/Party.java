package io.github.cjustinn.instancedworlds.Parties;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;
import java.util.UUID;

public class Party {

    // Data Members
    private UUID partyId;
    private List<Player> members;
    private Player leader;

    private Scoreboard scoreboard;
    private Objective objective;

    private PartyStatsListener listener;

    // Constructor(s)
    public Party(List<Player> members, Player leader) {
        this.partyId = UUID.randomUUID();

        this.members = members;
        this.leader = leader;

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = this.scoreboard.registerNewObjective("health", "dummy", ChatColor.BOLD + "Your Party");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setRenderType(RenderType.HEARTS);

        this.listener = new PartyStatsListener(this);
        Bukkit.getPluginManager().registerEvents(this.listener, Bukkit.getPluginManager().getPlugin("InstancedWorlds"));

        for (Player member : this.members) {

            this.addPlayerToScoreboard(member);

        }

    }

    // Getters
    public UUID getPartyId() { return this.partyId; }
    public Player getLeader() { return this.leader; }
    public boolean partyIsFull() { return this.members.size() >= InstancedWorldsManager.maxPartySize; }
    public int getMemberCount() { return this.members.size(); }
    public List<Player> getMembers() { return this.members; }

    // Functions
    public void disableListener() {
        this.listener.shutdown();
    }

    public void broadcastMessageToMembers(String message) {
        for (Player member : this.members) {
            member.sendMessage(message);
        }
    }

    private void changePartyLeader(Player replacement) {
        if (replacement != null) {
            Team team = this.scoreboard.getTeam(replacement.getName());
            if (team != null) {
                team.color(NamedTextColor.GOLD);
            }
        }

        this.leader = replacement;
    }

    public void updatePlayerScore(Player player, int score) {
        objective.getScore(player.getName()).setScore(score);
    }

    private void addPlayerToScoreboard(Player player) {
        //Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] Adding player [%s%s%s] to the scoreboard for %s's group.", ChatColor.GOLD, player.getName(), ChatColor.RESET, this.leader.getName()));
        Team team = this.scoreboard.registerNewTeam(player.getName());

        team.addPlayer(player);
        team.color(this.leader.getUniqueId().equals(player.getUniqueId()) ? NamedTextColor.GOLD : NamedTextColor.WHITE);

        updatePlayerScore(player, (int) player.getHealth());

        player.setScoreboard(this.scoreboard);
    }

    private void removePlayerFromScoreboard(Player player) {
        Team team = this.scoreboard.getTeam(player.getName());
        if (team != null) {
            team.removePlayer(player);
            team.unregister();
            updatePlayerScore(player, 0);

            scoreboard.resetScores(player);
        }

        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private int getMemberIndex(UUID uuid) {
        int index = -1;
        boolean found = false;

        for (int i = 0; i < this.members.size() && !found; i++) {
            if (this.members.get(i).getUniqueId().equals(uuid)) {
                found = true;
                index = i;
            }
        }

        return index;
    }

    public boolean playerIsInParty(UUID ply) {
        boolean found = false, inParty = false;

        for (int i = 0; i < this.members.size() && !found; i++) {
            if (this.members.get(i).getUniqueId().equals(ply)) {
                inParty = true;
                found = true;
            }
        }

        return inParty;
    }

    public boolean removePlayerFromParty(Player ply) {
        if (!playerIsInParty(ply.getUniqueId())) return false;

        // Remove the player from the members list.
        final int index = this.getMemberIndex(ply.getUniqueId());
        if (index < 0) return false;

        this.members.remove(index);

        this.removePlayerFromScoreboard(ply);

        // If the player was the leader, choose a new leader.
        if (this.leader.getUniqueId().equals(ply.getUniqueId())) {
            this.changePartyLeader(this.members.size() > 0 ? this.members.get(0) : null);

            if (this.leader != null)
                this.leader.sendMessage(ChatColor.GOLD + "You are now the party leader!");
        }

        // Send a message to all other players in the group.
        this.broadcastMessageToMembers(ChatColor.GOLD + ply.getName() + " has left the party!");

        return true;
    }

    public boolean addPlayerToParty(Player ply) {
        boolean success = true;

        if (this.partyIsFull())
            success = false;
        else {

            this.members.add(ply);
            this.addPlayerToScoreboard(ply);
        }

        return success;
    }

}
