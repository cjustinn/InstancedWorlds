package io.github.cjustinn.instancedworlds;

import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import io.github.cjustinn.instancedworlds.Instances.InstantiatedWorld;
import io.github.cjustinn.instancedworlds.Parties.Party;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.File;
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
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        if (InstancedWorldsManager.playerIsInParty(event.getPlayer().getUniqueId())) {

            final int partyIndex = InstancedWorldsManager.getPlayerPartyIndex(event.getPlayer().getUniqueId());
            if (partyIndex >= 0) {

                Party party = InstancedWorldsManager.parties.get(partyIndex);
                if (party != null) {
                    party.removePlayerFromParty(event.getPlayer());
                }

            }

        }

        if (event.getPlayer().getWorld().getName().startsWith("instance_")) {
            InstantiatedWorld instance = InstancedWorldsManager.getInstanceByName(event.getPlayer().getWorld().getName());
            if (instance != null) {
                instance.removePlayerFromInstance(event.getPlayer());
            }
        }
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
                                InstantiatedWorld instance = InstancedWorldsManager.instances.get(instanceIdx);
                                instance.sendPlayerToInstance(event.getPlayer());
                            } else {
                                // For whatever reason the instance index couldn't be found.
                                event.getPlayer().sendMessage(ChatColor.RED + "There was a problem joining the instance! Please try again.");
                            }
                        } else {
                            // Party leader does NOT have an existing instance.
                            // Create an instance for the party leader.
                            touchedPortal.openInstance(playerParty.getLeader());
                        }
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
                            InstantiatedWorld instance = InstancedWorldsManager.instances.get(instanceIdx);
                            instance.sendPlayerToInstance(event.getPlayer());
                        } else event.getPlayer().sendMessage(ChatColor.RED + "There was a problem joining the instance! Please try again.");
                    } else {
                        // Player does NOT have an instance.
                        touchedPortal.openInstance(event.getPlayer());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerWorldSwitch(PlayerChangedWorldEvent event) {
        // Check if the world was an instance.
        if (event.getFrom().getName().startsWith("instance_")) {
            // Check the size of the world's player list, if empty, destroy the instance.
            if (event.getFrom().getPlayerCount() == 0) {
                InstancedWorldsManager.instances.get(InstancedWorldsManager.getPlayerInstanceIndex(event.getFrom().getName())).destroyInstance();
            }
        }
    }

    @EventHandler
    public void onWorldUnloaded(WorldUnloadEvent event) {
        if (event.getWorld().getName().startsWith("instance_")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("InstancedWorlds"), () -> {
                new Thread(() -> {
                    try {
                        FileUtils.deleteDirectory(event.getWorld().getWorldFolder());
                    } catch (IOException e) {
                        Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] %sThe instance [%s] could not be unloaded.", ChatColor.RED, event.getWorld().getName()));
                        Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] %s%s", ChatColor.RED, e.getMessage()));
                        e.printStackTrace();
                    }
                }).start();
            }, 1L);
        }
    }

}
