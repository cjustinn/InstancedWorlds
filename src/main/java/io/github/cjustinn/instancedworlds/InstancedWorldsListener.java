package io.github.cjustinn.instancedworlds;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InstancedWorldsListener implements Listener {

    private Map<UUID, Instant> playerPortalStatuses = new HashMap<>();
    private boolean playerCanUsePortal(UUID player) {
        boolean canUsePortal = false;

        if (!playerPortalStatuses.containsKey(player))
            canUsePortal = true;
        else {
            Duration timeSinceLastPortalUse = Duration.between(playerPortalStatuses.get(player), Instant.now());
            if (timeSinceLastPortalUse.getSeconds() >= InstancedWorldsManager.instancePortalCooldown)
                canUsePortal = true;
        }

        return canUsePortal;
    }

    @EventHandler
    public void onPlayerMoved(PlayerMoveEvent event) {
        // Check if the player has moved into any portal region.
        InstancePortal touchedPortal = InstancedWorldsManager.playerTouchedPortal(event.getTo());
        if (touchedPortal != null) {
            if (playerCanUsePortal(event.getPlayer().getUniqueId())) {
                // Update the map to store the new current time that the portal was used, or store it in the first place if the key doesn't exist.
                if (playerPortalStatuses.containsKey(event.getPlayer().getUniqueId()))
                    playerPortalStatuses.replace(event.getPlayer().getUniqueId(), Instant.now());
                else
                    playerPortalStatuses.put(event.getPlayer().getUniqueId(), Instant.now());

                // Check if the player has the permission they need to be able to use instances.
                if (!event.getPlayer().hasPermission("instancedworlds.portals.use")) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You do not have the required permission to use instance portals!");
                    return;
                }

                event.getPlayer().sendMessage(ChatColor.GREEN + "Loading instance...");

                // Store the player's UUID to make life easier.
                final UUID playerUuid = event.getPlayer().getUniqueId();

                // Variables to reduce code-repetition.
                InstantiatedWorld instance = null;
                String joinMessage = "";
                boolean instanceWasCreated = false;

                // Check if the player is in a party.
                if (InstancedWorldsManager.playerIsInParty(playerUuid)) {
                    // Player IS in a party.
                    // Check if the party leader has an already existing instance of the intended template.
                    final int partyIdx = InstancedWorldsManager.getPlayerPartyIndex(playerUuid);
                    if (partyIdx > -1) {
                        final Party playerParty = InstancedWorldsManager.parties.get(partyIdx);

                        if (InstancedWorldsManager.playerHasExistingInstance(playerParty.getLeader().getUniqueId(), touchedPortal.getInstanceTemplate().getName())) {
                            // Party leader DOES have an existing instance.
                            // Teleport the player into the instance.
                            final int instanceIdx = InstancedWorldsManager.getPlayerInstanceIndex(playerParty.getLeader().getUniqueId(), touchedPortal.getInstanceTemplate().getName());
                            if (instanceIdx > -1) {
                                instance = InstancedWorldsManager.instances.get(instanceIdx);
                            } else {
                                // For whatever reason the instance index couldn't be found.
                                event.getPlayer().sendMessage(ChatColor.RED + "There was a problem joining the instance! Please try again.");
                            }
                        } else {
                            // Party leader does NOT have an existing instance.
                            // Create an instance for the party leader.
                            instance = touchedPortal.openInstance(playerParty.getLeader());
                            instanceWasCreated = true;
                        }

                        joinMessage = InstancedWorldsManager.playerIsLeadingParty(playerUuid) ? "You have joined your own instance." : String.format("You have joined %s's instance.", playerParty.getLeader().getName());
                    } else {
                        // For whatever reason, the party couldn't be found.
                        event.getPlayer().sendMessage(ChatColor.RED + "There was a problem joining the instance! Please try again.");
                    }
                } else {
                    // Player is NOT in a party.
                    // Check if the player has an already-existing instance of the intended template.
                    if (InstancedWorldsManager.playerHasExistingInstance(playerUuid, touchedPortal.getInstanceTemplate().getName())) {
                        // Player DOES have an instance.
                        final int instanceIdx = InstancedWorldsManager.getPlayerInstanceIndex(playerUuid, touchedPortal.getInstanceTemplate().getName());
                        if (instanceIdx > -1) {
                            instance = InstancedWorldsManager.instances.get(instanceIdx);
                        } else event.getPlayer().sendMessage(ChatColor.RED + "There was a problem joining the instance! Please try again.");
                    } else {
                        // Player does NOT have an instance.
                        instance = touchedPortal.openInstance(event.getPlayer());
                        instanceWasCreated = true;
                    }

                    joinMessage = "You have joined your own instance.";
                }

                // Save the instance to the list.
                if (instanceWasCreated)
                    InstancedWorldsManager.saveInstance(instance);

                // Move the player into the instance.
                instance.sendPlayerToInstance(event.getPlayer());
                event.getPlayer().sendMessage(String.format("%s%s", ChatColor.GOLD, joinMessage));
            }
        }
    }

    @EventHandler
    public void onPlayerWorldSwitch(PlayerChangedWorldEvent event) {
        if (event.getFrom().getName().startsWith("instance_")) {
            // The world was an instance.
            // Check the size of the world's player list, if empty, destroy the instance.
            if (event.getFrom().getPlayerCount() == 0) {
                InstantiatedWorld instance = InstancedWorldsManager.instances.get(InstancedWorldsManager.getPlayerInstanceIndex(event.getFrom().getName()));
                instance.destroyInstance();
            }
        }
    }

    @EventHandler
    public void onWorldUnloaded(WorldUnloadEvent event) {
        if (event.getWorld().getName().startsWith("instance_")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("InstancedWorlds"), () -> {
                try {
                    FileUtils.deleteDirectory(event.getWorld().getWorldFolder());
                } catch(IOException e) {
                    Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] %sThe instance [%s] could not be unloaded.", ChatColor.RED, event.getWorld().getName()));
                    Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] %s%s", ChatColor.RED, e.getMessage()));
                }
            }, 1L);
        }
    }

}
