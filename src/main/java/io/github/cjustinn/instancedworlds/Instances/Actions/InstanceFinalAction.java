package io.github.cjustinn.instancedworlds.Instances.Actions;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Instances.InstantiatedWorld;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Observable;

public class InstanceFinalAction extends Action {

    // Data Members
    private final InstantiatedWorld instance;
    private final int mobId;

    // Constructor
    public InstanceFinalAction(InstantiatedWorld instance, int id) {
        this.instance = instance;
        this.mobId = id;
    }

    // Action Overrides
    @Override
    public void performAction() {
        // Update the instance 'isCompleted' value.
        this.instance.setCompleted(true);

        // Inform all players within the instance that the instance has been completed.
        for (Player player : this.instance.getWorld().getPlayers())
            player.sendMessage(String.format("%sYou have completed this %s instance!", ChatColor.GREEN, InstancedWorldsManager.getTemplateInstanceNameById(this.instance.getTemplateName())));

        // Disable the listener.
        this.disableListener();

        // Mark this action as complete.
        this.hasCompleted = true;
    }

    // Event Handlers
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getWorld().equals(this.instance.getWorld())) {
            Entity entity = event.getEntity();
            if (entity.hasMetadata("mobId")) {
                int id = entity.getMetadata("mobId").get(0).asInt();
                if (id == this.mobId)
                    performAction();
            }
        }
    }
}
