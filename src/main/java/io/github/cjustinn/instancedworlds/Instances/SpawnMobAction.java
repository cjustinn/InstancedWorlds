package io.github.cjustinn.instancedworlds.Instances;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class SpawnMobAction implements InstanceAction, Listener {
    public final EntityType mobType;
    public final int amount, radius, id;
    public final Location location;

    private boolean isChecking = false;
    private boolean hasCompleted = false;

    public SpawnMobAction(EntityType type, int amount, int radius, int id, Location location) {
        this.mobType = type;
        this.amount = amount;
        this.radius = radius;
        this.id = id;
        this.location = location;

        if (radius < 0)
            performAction();
    }

    // Getter Functions
    public boolean hasCompleted() { return this.hasCompleted; }

    // InstanceAction Override(s)
    @Override
    public void performAction() {
        // Create the entities.
        for (int i = 0; i < amount; i++) {
            LivingEntity mob = (LivingEntity) this.location.getWorld().spawnEntity(this.location, this.mobType);

            if (this.id >= 0)
                mob.setMetadata("mobId", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("InstancedWorlds"), this.id));

            mob.setPersistent(true);
        }

        disableListener();

        this.hasCompleted = true;
    }

    // Function to disable the listener. This should be done before the event is removed.
    @Override
    public void disableListener() {
        HandlerList.unregisterAll(this);
    }

    /*
        Event handlers used by this action to track the location of players in order to spawn the mobs
        when they enter the radius around the defined location.
    */
    @EventHandler
    public void onPlayerMovement(PlayerMoveEvent event) {
        if (event.getPlayer().getWorld().equals(this.location.getWorld())) {
            if (!isChecking) {
                isChecking = true;

                if (event.getTo().distance(this.location) <= this.radius) {
                    performAction();
                } else isChecking = false;
            }
        }
    }

}
