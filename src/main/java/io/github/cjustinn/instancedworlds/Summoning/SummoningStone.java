package io.github.cjustinn.instancedworlds.Summoning;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Parties.Party;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SummoningStone {

    // Data Members
    private final UUID stoneId;
    private final String readableId;
    private final Location origin;
    private final Location summoningPoint;
    private final String name;

    // Constructor
    public SummoningStone(Location o, Location sP, String rId, String n) {
        this.stoneId = UUID.randomUUID();
        this.origin = o;
        this.summoningPoint = sP;
        this.readableId = rId;
        this.name = n;
    }

    public SummoningStone(String id, Location o, Location sP, String rId, String n) {
        this.stoneId = UUID.fromString(id);
        this.origin = o;
        this.summoningPoint = sP;
        this.readableId = rId;
        this.name = n;
    }

    // Getters
    public String getName() { return this.name; }
    public String getReadableId() { return this.readableId; }
    public UUID getUUID() { return this.stoneId; }

    public Location getSummoningPoint() { return this.summoningPoint; }

    // Functions
    public double getDistance(Location playerLocation) {
        return this.origin.distance(playerLocation);
    }

    public boolean summonPlayer(Player sender, Player target) {
        boolean success = false;

        if (InstancedWorldsManager.playerIsInParty(sender.getUniqueId())) {
            final int index = InstancedWorldsManager.getPlayerPartyIndex(sender.getUniqueId());
            if (index >= 0) {

                Party party = InstancedWorldsManager.parties.get(index);
                if (party.playerIsInParty(target.getUniqueId())) {

                    // Create and store a new summoning invite.
                    InstancedWorldsManager.registerSummoningInvite(new SummoningInvite(this.summoningPoint, this.name, sender, target));

                    // Set the success value to true.
                    success = true;

                }

            }
        }

        return success;
    }

}
