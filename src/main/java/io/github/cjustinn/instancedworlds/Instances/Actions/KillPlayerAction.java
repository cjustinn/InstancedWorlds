package io.github.cjustinn.instancedworlds.Instances.Actions;

import io.github.cjustinn.instancedworlds.Instances.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class KillPlayerAction extends Action {
    // Data Members
    private Region region;

    // Constructor
    public KillPlayerAction(Location corner1, Location corner2) {
        this.region = new Region(corner1, corner2);
    }

    // Overrides
    public void performAction(Player player) {
        player.damage(player.getHealth());
    }

    // Event Handlers
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo().getWorld().equals(region.getCornerOne().getWorld())) {
            if (this.region.contains(event.getTo())) {
                this.performAction(event.getPlayer());
            }
        }
    }
}
