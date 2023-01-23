package io.github.cjustinn.instancedworlds.Instances;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class SpawnMythicMobAction implements InstanceAction, Listener {
    // Data Members
    private final String mobName;
    private final int amount, id, radius;
    private final Location location;

    // Flags
    private boolean isChecking = false;
    private boolean hasCompleted = false;

    // Constructor
    public SpawnMythicMobAction(String name, int amount, int id, int radius, Location location) {
        this.mobName = name;
        this.amount = amount;
        this.id = id;
        this.radius = radius;
        this.location = location;

        if (radius < 0)
            this.performAction();
    }

    // Getters
    public boolean hasCompleted() { return this.hasCompleted; }

    // InstanceAction Overrides
    @Override
    public void performAction() {
        // Create the entity.
        for (int i = 0; i < amount; i++) {
            // Get the mob type.
            MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob(this.mobName).orElse(null);
            if (mob != null) {

                // Spawn the mob
                ActiveMob spawnedMob = mob.spawn(BukkitAdapter.adapt(this.location), 1);
                Entity vanillaEntity = spawnedMob.getEntity().getBukkitEntity();

                // If the id value has been set, assign the id to the mob in its metadata.
                if (this.id >= 0) {
                    vanillaEntity.setMetadata("mobId", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("InstancedWorlds"), this.id));
                }

                // Set the mob to persistent so that it won't despawn.
                vanillaEntity.setPersistent(true);

            }
        }

        // Disable the listener.
        disableListener();

        // Update the hasCompleted variable.
        this.hasCompleted = true;
    }

    @Override
    public void disableListener() {
        HandlerList.unregisterAll(this);
    }

    // Event Handlers
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo().getWorld().equals(this.location.getWorld())) {

            if (!isChecking) {

                this.isChecking = true;
                if (event.getTo().distance(this.location) <= this.radius) {
                    performAction();
                } else this.isChecking = false;

            }

        }
    }
}
