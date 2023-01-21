package io.github.cjustinn.instancedworlds;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class PartyInvite {

    private Instant sent;

    private UUID partyId;
    private String leaderName;
    private UUID recipient;

    public PartyInvite(Party party, UUID ply) {
        this.partyId = party.getPartyId();
        this.leaderName = party.getLeader().getName();
        this.recipient = ply;

        this.sent = Instant.now();
    }

    public UUID getParty() { return this.partyId; }

    public boolean partyIsMatch(UUID plyId, String leader) {
        boolean match = false;

        if (plyId.equals(recipient) && leaderName.toLowerCase().equals(leader.toLowerCase()))
            match = true;

        return match;
    }

    public boolean inviteIsExpired() {
        boolean expired = false;

        if (InstancedWorldsManager.partyInviteTimeout > 0) {
            Duration difference = Duration.between(sent, Instant.now());
            if (difference.getSeconds() >= InstancedWorldsManager.partyInviteTimeout)
                expired = true;
        }

        return expired;
    }

}
