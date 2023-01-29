package io.github.cjustinn.instancedworlds.Summoning;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class SummoningInvite {

    // Data Members
    private final Instant sent;
    private final Location targetLocation;
    private final String targetName;
    private final Player sender;
    private final Player recipient;

    // Constructor
    public SummoningInvite(Location targetLoc, String targetName, Player sender, Player target) {
        this.sent = Instant.now();

        this.targetLocation = targetLoc;
        this.targetName = targetName;
        this.sender = sender;
        this.recipient = target;

        // Notify the target player that a summoning invite has been sent.
        this.recipient.sendMessage(String.format("%s%s is attempting to summon you to %s. Type '/summon accept' to accept the summon and teleport.", ChatColor.GOLD, sender.getName(), this.targetName));
    }

    // Getters
    public String getName() { return this.targetName; }

    // Functions
    public boolean isTimedout() {
        Duration duration = Duration.between(this.sent, Instant.now());
        return duration.getSeconds() >= InstancedWorldsManager.summonInviteTimeout;
    }

    public boolean acceptInvite() {
        boolean success = false;

        if (!this.isTimedout()) {
            // Notify the sender that the target has accepted their invite.
            this.sender.sendMessage(String.format("%s%s has accepted your summon.", ChatColor.GOLD, ((TextComponent) this.recipient.displayName()).content()));

            // Teleport the player.
            this.recipient.teleport(this.targetLocation);

            // Set the success value to true.
            success = true;
        } else {
            this.recipient.sendMessage(String.format("%sThe summoning invite has timed out!", ChatColor.RED));
        }

        return success;
    }

    public boolean playerIsRecipient(UUID player) {
        return this.recipient.getUniqueId().equals(player);
    }

}
