package io.github.cjustinn.instancedworlds.Commands.Listeners;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Summoning.SummoningStone;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class SummoningStoneCreationListener implements Listener {

    // Data Members
    private final String readableId;
    private final String stoneName;
    private final Player creator;

    private Location origin = null;
    private Location summoningPoint = null;

    // Flags
    private int location = 1;

    // Constructor
    public SummoningStoneCreationListener(Player target, String id, String name) {
        this.readableId = id;
        this.stoneName = name;
        this.creator = target;

        target.sendMessage(ChatColor.GREEN + "Punch the block you wish to use as the summoning stone origin (for distance checking).");
    }

    // Event Handlers
    @EventHandler
    public void onPlayerBreakBlock(PlayerInteractEvent event) {
        if (this.origin == null || this.summoningPoint == null) {

            event.setCancelled(true);

            if (this.location == 1) {
                this.origin = event.getClickedBlock().getLocation();
                this.creator.sendMessage(ChatColor.GREEN + "Punch the block that you want the stone to teleport summoned players to.");
            } else {
                this.summoningPoint = event.getClickedBlock().getLocation();
            }

            this.location++;

            if (this.origin != null && this.summoningPoint != null) {

                // Create the new SummoningStone object.
                SummoningStone stone = new SummoningStone(this.origin, this.summoningPoint, this.readableId, this.stoneName);

                // Save all of the values into the config file.
                InstancedWorldsManager.saveConfigValue(String.format("summoningstones.%s.id", stone.getUUID().toString()), this.readableId);
                InstancedWorldsManager.saveConfigValue(String.format("summoningstones.%s.origin", stone.getUUID().toString()), String.format("%s;%f;%f;%f", this.origin.getWorld().getName(), this.origin.getX(), this.origin.getY(), this.origin.getZ()));
                InstancedWorldsManager.saveConfigValue(String.format("summoningstones.%s.summoningPoint", stone.getUUID().toString()), String.format("%s;%f;%f;%f", this.summoningPoint.getWorld().getName(), this.summoningPoint.getX(), this.summoningPoint.getY(), this.summoningPoint.getZ()));
                InstancedWorldsManager.saveConfigValue(String.format("summoningstones.%s.name", stone.getUUID().toString()), this.stoneName);

                // Register the stone with the manager.
                InstancedWorldsManager.registerSummoningStone(stone);

                // Notify the creator that it is done.
                this.creator.sendMessage(String.format("%sSummoning stone '%s' has been created!", ChatColor.GREEN, this.readableId));

                // Deregister this handler.
                HandlerList.unregisterAll(this);

            }

        }
    }

}
