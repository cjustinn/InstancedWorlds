package io.github.cjustinn.instancedworlds;

import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class InstantiatedWorld {

    // Data Members
    private String instanceId;
    private UUID owner;
    private Location origin;
    private String template;

    // Constructors
    public InstantiatedWorld(String id, UUID owner, String originString, String template) {
        this.instanceId = id;
        this.owner = owner;

        /*
            ORIGIN EXPECTED FORMAT:
            =======================
            origin[0]       -       World Name
            origin[1]       -       X-Coordinate
            origin[2]       -       Y-Coordinate
            origin[3]       -       Z-Coordinate
        */
        String[] origin = originString.split(";");
        this.origin = new Location(Bukkit.getWorld(origin[0]), Double.parseDouble(origin[1]), Double.parseDouble(origin[2]), Double.parseDouble(origin[3]));

        this.template = template;
    }

    public InstantiatedWorld(String id, String owner, String originString, String template) {
        this.instanceId = id;
        this.owner = UUID.fromString(owner);

        /*
            ORIGIN EXPECTED FORMAT:
            =======================
            origin[0]       -       World Name
            origin[1]       -       X-Coordinate
            origin[2]       -       Y-Coordinate
            origin[3]       -       Z-Coordinate
        */
        String[] origin = originString.split(";");
        this.origin = new Location(Bukkit.getWorld(origin[0]), Double.parseDouble(origin[1]), Double.parseDouble(origin[2]), Double.parseDouble(origin[3]));

        this.template = template;
    }

    public InstantiatedWorld(String id, UUID owner, Location origin, String template) {
        this.instanceId = id;
        this.owner = owner;
        this.origin = origin;
        this.template = template;
    }

    public InstantiatedWorld(String id, String owner, Location origin, String template) {
        this.instanceId = id;
        this.owner = UUID.fromString(owner);
        this.origin = origin;
        this.template = template;
    }

    // Getters
    public UUID getOwner() { return this.owner; }
    public String getTemplateName() { return this.template; }
    public String getInstanceId() { return this.instanceId; }
    public Location getOrigin() { return this.origin; }

    // Functions
    public World getWorld() {
        return Bukkit.createWorld(new WorldCreator(instanceId));
    }

    public boolean isOwner(UUID player) {
        return this.owner.equals(player);
    }

    public void sendPlayerToInstance(Player player) {
        World world = this.getWorld();
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        }
    }

    public void sendPlayersToInstance(List<Player> players) {
        World world = this.getWorld();
        if (world != null) {
            for (Player _p : players) {
                _p.teleport(world.getSpawnLocation());
            }
        }
    }

    public void destroyInstance() {
        // Teleport out any players still in the instance.
        for (Player player : this.getWorld().getPlayers()) {
            player.teleport(this.origin);
        }

        // Unload the world.
        if (Bukkit.unloadWorld(this.getWorld(), false)) {
            // Remove the instance from the list.
            InstancedWorldsManager.instances.remove(InstancedWorldsManager.getPlayerInstanceIndex(this.instanceId));
        }
    }

}
