package io.github.cjustinn.instancedworlds.Instances.Actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

public class ExperienceAction extends Action {
    // Data Members
    private final Location origin;
    private final int radius, amount, mobId;
    private final boolean isLevels;

    // Constructor
    public ExperienceAction(Location origin, int radius, int amount, int mobId, boolean isLevels) {
        this.origin = origin;
        this.radius = radius;
        this.amount = amount;
        this.mobId = mobId;
        this.isLevels = isLevels;
    }

    // Overrides
    @Override
    public void performAction() {

        // Give all eligible players the experience.
        for (Player player : this.origin.getWorld().getPlayers()) {
            // If the radius value is greater than zero, give it to only players who are within the radius distance of the origin.
            if (this.radius > 0) {
                if (player.getLocation().distance(this.origin) <= this.radius) {
                    if (this.isLevels)
                        player.giveExpLevels(this.amount);
                    else
                        player.giveExp(this.amount);
                }
            }
            // If the radius is <= zero, give the experience to all players in the instance world.
            else {
                if (this.isLevels)
                    player.giveExpLevels(this.amount);
                else
                    player.giveExp(this.amount);
            }
        }

        // Deregister this action as an event listener.
        this.disableListener();

        // Mark the action as having completed.
        this.hasCompleted = true;

    }

    // Event Handlers
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getWorld().equals(this.origin.getWorld())) {
            Entity entity = event.getEntity();
            if (entity.hasMetadata("mobId")) {

                int id = entity.getMetadata("mobId").get(0).asInt();
                if (id == this.mobId)
                    performAction();

            }
        }
    }
}
