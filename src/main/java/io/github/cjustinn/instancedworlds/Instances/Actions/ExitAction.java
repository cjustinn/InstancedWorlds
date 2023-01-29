package io.github.cjustinn.instancedworlds.Instances.Actions;

import io.github.cjustinn.instancedworlds.Instances.Region;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.function.Function;

public class ExitAction extends Action {

    // Data Members
    private final Region portalRegion;
    private final Function<Player, Void> removePly;

    // Constructor
    public ExitAction(Location corner1, Location corner2, Function<Player, Void> remove) {
        this.portalRegion = new Region(corner1, corner2);
        this.removePly = remove;
    }

    // Event Handlers
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo().getWorld().equals(this.portalRegion.getCornerOne().getWorld())) {

            if (this.portalRegion.contains(event.getTo())) {
                this.removePly.apply(event.getPlayer());
            }

        }
    }

}
