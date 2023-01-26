package io.github.cjustinn.instancedworlds.Instances.Actions;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class Action implements InstanceAction, Listener {
    // Data Members
    protected boolean hasCompleted = false;

    // InstanceAction Overrides
    @Override
    public void performAction() {
        // This function will be overridden in child action classes to perform their associated actions.
    }

    @Override
    public void disableListener() {
        HandlerList.unregisterAll(this);
    }

    // Getters
    public boolean hasCompleted() { return this.hasCompleted; }
}
