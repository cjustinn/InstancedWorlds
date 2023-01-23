package io.github.cjustinn.instancedworlds.Instances;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InstantiatedWorld implements Listener {

    // Data Members
    private String instanceId;
    private UUID owner;
    private Location origin;
    private String template;
    private List<InstanceAction> actions;

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
        for (InstanceAction action : this.actions) {
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

    // Listen for events involving players inside the current instance.
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().getWorld().equals(this.getWorld())) {

            // Iterate through all the registered instance actions and run them.

        }
    }

    @EventHandler
    public void onChunkLoaded(ChunkLoadEvent event) {
        if (event.getWorld().equals(this.getWorld())) {
            Chunk chunk = event.getChunk();
            for (BlockState s : chunk.getTileEntities()) {
                if (s instanceof Sign) {
                    Sign sign = (Sign) s;

                    // Check if the sign is an instance action sign by checking if it's first line matches the '[Instance]' tag.
                    if (((TextComponent) sign.line(0)).content().equalsIgnoreCase("[instance]")) {

                        // Check what the action type should be, based on the second line.
                        String action = ((TextComponent) sign.line(1)).content().toLowerCase();
                        InstanceActionType type = InstanceActionType.None;

                        switch (action) {
                            case "spawnmob":
                                type = InstanceActionType.SpawnMob;
                                break;
                            case "spawnloot":
                                type = InstanceActionType.SpawnLoot;
                                break;
                            default:
                                type = InstanceActionType.None;
                                break;
                        }

                        /*
                            Eventually this will need to be updated, once I add optional integration for MythicMobs,
                            so that instances are capable of spawning custom mobs created via that plugin as well as
                            vanilla Minecraft mobs. They will have to be differentiated with a prefix when the intended
                            mob is a MythicMob type.
                        */

                        if (type == InstanceActionType.SpawnMob) {
                            boolean isMythicMob = false;

                            // Check the third line for the type of entity.
                            String entityName = ((TextComponent) sign.line(2)).content();
                            isMythicMob = entityName.toLowerCase().startsWith("mm:");

                            int amount = 0, mobId = -1, radius = -1;

                            // Use the final line to extract the necessary data to handle spawn conditions, amount, and ids.
                            String[] splitValues = ((TextComponent) sign.line(3)).content().split(";");

                            /*
                                Different things should happen, depending on the values provided by the user.

                                1 Value     -       The value should be considered the AMOUNT of the mob to spawn.
                                2 Values    -       The values should be considered the AMOUNT of the mob to spawn, and the
                                                        RADIUS the player has to be in, from the spawn location, in order
                                                        for the mob(s) to be spawned.
                                3 Values    -       The values should be considered the AMOUNT of the mob to spawn, the
                                                        ID that should be assigned to the mob(s), and the RADIUS the player
                                                        has to be in, from the spawn location, in order for the mob(s) to be
                                                        spawned.
                            */

                            switch (splitValues.length) {
                                case 1:
                                    amount = Integer.parseInt(splitValues[0]);
                                    break;
                                case 2:
                                    amount = Integer.parseInt(splitValues[0]);
                                    radius = Integer.parseInt(splitValues[1]);
                                    break;
                                case 3:
                                    amount = Integer.parseInt(splitValues[0]);
                                    mobId = Integer.parseInt(splitValues[1]);
                                    radius = Integer.parseInt(splitValues[2]);
                                    break;
                                default:
                                    break;
                            }

                            /*
                                If the sign indicates that it should be a MythicMob that is spawned, create a SpawnMythicMobAction provided that the
                                MythicMobs plugin is enabled on the server. If it isn't marked with the "mm:" tag, then consider
                                it to be a vanilla minecraft mob and create a SpawnMobAction.
                            */
                            if (!isMythicMob) {
                                EntityType mobType = EntityType.fromName(entityName.toLowerCase());
                                if (mobType != null) {
                                    SpawnMobAction parsedAction = new SpawnMobAction(mobType, amount, radius, mobId, sign.getLocation());
                                    this.actions.add(parsedAction);

                                    Bukkit.getServer().getPluginManager().registerEvents(parsedAction, Bukkit.getPluginManager().getPlugin("InstancedWorlds"));
                                }
                            } else {
                                if (InstancedWorldsManager.isPluginEnabled("MythicMobs")) {
                                    SpawnMythicMobAction parsedAction = new SpawnMythicMobAction(entityName.replace("mm:", ""), amount, mobId, radius, sign.getLocation());
                                    this.actions.add(parsedAction);

                                    Bukkit.getServer().getPluginManager().registerEvents(parsedAction, Bukkit.getPluginManager().getPlugin("InstancedWorlds"));
                                }
                            }
                        } else if (type == InstanceActionType.SpawnLoot) {
                            // Get the loot table id from the third line.
                            String targetLootTable = ((TextComponent) sign.line(2)).content();
                            if (InstancedWorldsManager.lootTableExists(targetLootTable)) {

                                // Get the mob id number to watch for, from the final line of the string.
                                int targetMobId;
                                try {
                                    targetMobId = Integer.parseInt(((TextComponent) sign.line(3)).content());
                                } catch (NumberFormatException e) {
                                    targetMobId = -1;
                                }

                                // Create the action and add it to the actions list.
                                SpawnLootAction newAction = new SpawnLootAction(targetMobId, targetLootTable, sign.getLocation());
                                this.actions.add(newAction);

                                // Register the new action as an Event Listener with the plugin.
                                Bukkit.getServer().getPluginManager().registerEvents(newAction, Bukkit.getPluginManager().getPlugin("InstancedWorlds"));
                            }
                        }

                        // Destroy the sign. Instance signs should only be visible in template worlds, not their instances.
                        sign.getBlock().setType(Material.AIR);
                    }

                }
            }
        }
    }
}