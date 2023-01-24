package io.github.cjustinn.instancedworlds.Instances.Actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TeleportAction implements InstanceAction, Listener {
    // Data members
    private final Location target;
    private final Location teleportFrom;
    private final int radius, mobId;

    // Constants
    private final int cooldownSeconds = 5;

    // Lists
    private Map<Player, Instant> teleportLog = new HashMap<>();

    // Flags
    private boolean hasCompleted = false;
    private boolean targetMobIsDead = false;

    // Constructor
    public TeleportAction(Location target, Location trigger, int radius, int mobId) {
        this.target = target;
        this.teleportFrom = trigger;
        this.radius = radius;
        this.mobId = mobId;
    }

    // Getter
    public boolean hasCompleted() { return this.hasCompleted; }

    // Helper Functions
    private boolean playerIsOffCooldown(Player player) {
        boolean offCooldown = true;

        if (this.teleportLog.containsKey(player)) {
            Duration timeSinceLastTeleport = Duration.between(this.teleportLog.get(player), Instant.now());

            if (timeSinceLastTeleport.getSeconds() < this.cooldownSeconds) {
                offCooldown = false;
            }
        }

        return offCooldown;
    }

    // InstantAction Overrides
    @Override
    public void performAction() {}

    public void performAction(Player player) {
        // Check if the player is on teleport cooldown.
        if (playerIsOffCooldown(player)) {
            // If the player is NOT on cooldown.
            // Update the last time the player was teleported by this teleport action in the log.
            if (this.teleportLog.containsKey(player))
                this.teleportLog.replace(player, Instant.now());
            else
                this.teleportLog.put(player, Instant.now());

            // Teleport the player.
            player.teleport(this.target);
        }
    }

    @Override
    public void disableListener() {
        HandlerList.unregisterAll(this);
    }

    // Event Handlers
    // Listen for player movement to check when it's in radius of the teleport sign.
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo().getWorld().equals(this.target.getWorld())) {
            if (event.getTo().distance(this.teleportFrom) <= this.radius) {
                if (this.targetMobIsDead)
                    performAction(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getWorld().equals(this.target.getWorld())) {
            Entity entity = event.getEntity();
            if (entity.hasMetadata("mobId")) {
                int id = entity.getMetadata("mobId").get(0).asInt();
                if (this.mobId == id)
                    this.targetMobIsDead = true;
            }
        }
    }
}
