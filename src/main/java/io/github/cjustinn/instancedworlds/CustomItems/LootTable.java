package io.github.cjustinn.instancedworlds.CustomItems;

import io.github.cjustinn.instancedworlds.InstancedWorlds;
import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootTable {

    private String tableId;
    private List<String> items;

    public LootTable(String id, List<String> items) {
        this.tableId = id;
        this.items = items;
    }

    // Getter functions
    public String getTableId() { return this.tableId; }

    // Helper function to generate a random number from the provided range, inclusively.
    private int generateNumberInRange(int min, int max) {
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    // Helper function to generate a random number and return it.
    private double generateChanceRoll() {
        Random random = new Random();
        return random.nextDouble();
    }

    /*
        This function iterates through all the items in the items list, and
        creates a chest-ready list of items which is then returned.
    */
    public List<ItemStack> generateItemsFromLootTable() {
        InstancedWorlds plugin = (InstancedWorlds) Bukkit.getPluginManager().getPlugin("InstancedWorlds");
        List<ItemStack> generatedItems = new ArrayList<>();

        for (String itemName : this.items) {
            ItemStack item = null;
            double chance = 0.0;
            int min = 0, max = 0, toAdd = 0;

            // Get the necessary data about the loot table.
            chance = plugin.getConfigurationFile().getDouble(String.format("loottables.%s.items.%s.chance", this.tableId, itemName));
            min = plugin.getConfigurationFile().getInt(String.format("loottables.%s.items.%s.minAmount", this.tableId, itemName));
            max = plugin.getConfigurationFile().getInt(String.format("loottables.%s.items.%s.maxAmount", this.tableId, itemName));
            toAdd = generateNumberInRange(min, max);

            if (itemName.toLowerCase().startsWith("ci:")) {
                // Custom Item.
                item = InstancedWorldsManager.getCustomItemById(itemName.replace("ci:", "")).getItem();
            } else {
                // Default Minecraft Item.
                Material itemType = Material.matchMaterial(itemName);
                if (itemType != null) {
                    // Create the item.
                    item = new ItemStack(itemType);
                }
            }

            if (item != null) {
                if (item.getMaxStackSize() > 1 && toAdd > 1)
                    item.setAmount(toAdd);

                double chanceRoll = generateChanceRoll();

                if (chanceRoll >= (1.0 - chance)) {
                    if (item.getMaxStackSize() == 1 && toAdd > 1) {
                        for(int i = 0; i < toAdd; i++) {
                            generatedItems.add(item);
                        }
                    } else generatedItems.add(item);
                }
            }
        }

        return generatedItems;
    }

}
