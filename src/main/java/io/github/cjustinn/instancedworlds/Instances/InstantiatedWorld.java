package io.github.cjustinn.instancedworlds.Instances;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Instances.Actions.*;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.*;

public class InstantiatedWorld implements Listener {

    // Data Members
    private String instanceId;
    private UUID owner;
    private Location origin;
    private String template;
    private List<Action> actions;

    // Flags
    private boolean isCompleted = false;

    // Constructors
    public InstantiatedWorld(String id, UUID owner, Location origin, String template) {
        this.instanceId = id;
        this.owner = owner;
        this.origin = origin;
        this.template = template;
        this.actions = new ArrayList<>();

        Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("InstancedWorlds"));
    }

    // Getters
    public UUID getOwner() {
        return this.owner;
    }

    public String getTemplateName() {
        return this.template;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public Location getOrigin() {
        return this.origin;
    }
    public boolean isCompleted() { return this.isCompleted; }

    // Setters
    public void setCompleted(boolean complete) { this.isCompleted = complete; }

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

        // Deregister all the action listeners.
        for (Action action : this.actions) {
            action.disableListener();
        }

        // Deregister this instance as a listener.
        HandlerList.unregisterAll(this);

        // Unload the world.
        if (Bukkit.unloadWorld(this.getWorld(), false)) {
            // Remove the instance from the list.
            InstancedWorldsManager.instances.remove(InstancedWorldsManager.getPlayerInstanceIndex(this.instanceId));
        }
    }

    @EventHandler
    public void onChunkLoaded(ChunkLoadEvent event) {
        if (event.getWorld().equals(this.getWorld())) {
            Chunk chunk = event.getChunk();
            chunk.setForceLoaded(true);
            for (BlockState s : chunk.getTileEntities()) {
                if (s instanceof Sign) {
                    Sign sign = (Sign) s;

                    // Check if the sign is an instance action sign by checking if it's first line matches the '[Instance]' tag.
                    if (((TextComponent) sign.line(0)).content().equalsIgnoreCase("[Instance]")) {

                        // Check what the action type should be, based on the second line.
                        String action = ((TextComponent) sign.line(1)).content().toLowerCase();
                        Action parsedAction = null;

                        // Check for a matching action mapping from the "actionMaps" list in the Manager class.
                        if (InstancedWorldsManager.actionMaps.containsKey(action.toLowerCase())) {
                            parsedAction = InstancedWorldsManager.actionMaps.get(action.toLowerCase()).apply(sign);

                            if (parsedAction != null) {
                                // Store the new action in the instance's actions list.
                                this.actions.add(parsedAction);

                                // Register the new action as an event handler for the plugin.
                                Bukkit.getPluginManager().registerEvents(parsedAction, Bukkit.getPluginManager().getPlugin("InstancedWorlds"));
                            }
                        }

                        // Destroy the sign. Instance signs should only be visible in template worlds, not their instances.
                        sign.getBlock().setType(Material.AIR);
                    }

                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerRespawnEvent event) {
        if (event.getPlayer().getLastDeathLocation().getWorld().equals(this.getWorld())) {
            event.setRespawnLocation(this.getWorld().getSpawnLocation());
        }
    }
}