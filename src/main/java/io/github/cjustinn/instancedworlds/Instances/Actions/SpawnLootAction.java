package io.github.cjustinn.instancedworlds.Instances.Actions;

import io.github.cjustinn.instancedworlds.CustomItems.LootTable;
import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SpawnLootAction extends Action {
    private final int targetId;
    private final String lootTableId;
    private final Location chestLocation;

    private boolean hasCompleted = false;

    // Constructor
    public SpawnLootAction(int target, String lootTable, Location chest) {
        this.targetId = target;
        this.lootTableId = lootTable;
        this.chestLocation = chest;
    }

    // Getters
    public boolean hasCompleted() { return this.hasCompleted; }

    // InstanceAction Overrides
    @Override
    public void performAction() {
        // Get the loot table.
        LootTable targetLootTable = InstancedWorldsManager.getLootTableById(this.lootTableId);

        if (targetLootTable != null) {
            // Spawn the chest.
            Block chestBlock = this.chestLocation.getBlock();
            chestBlock.setType(Material.CHEST);

            Chest lootChest = (Chest) chestBlock.getState();
            Inventory chestInventory = lootChest.getBlockInventory();

            // Generate the loot.
            List<ItemStack> lootItems = targetLootTable.generateItemsFromLootTable();

            // Add the loot to the chest.
            for (ItemStack item : lootItems)
                chestInventory.addItem(item);
        }

        // Disable the event listener.
        disableListener();

        this.hasCompleted = true;
    }

    // Event Handlers
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        if (event.getEntity().getWorld().equals(this.chestLocation.getWorld())) {

            Entity entity = event.getEntity();
            if (entity.hasMetadata("mobId")) {
                int mobId = entity.getMetadata("mobId").get(0).asInt();
                if (mobId == this.targetId) {
                    performAction();
                }
            }

        }
    }
}
