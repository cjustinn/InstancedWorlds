package io.github.cjustinn.instancedworlds.CustomItems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomItem {
    private final String itemId;
    private final String name;
    private final int customModelData;
    private final List<Component> lore;
    private final Material itemType;
    private final Map<String, Integer> enchantments;

    public CustomItem(String id, String name, int cmd, List<String> lore, String item, Map<String, Integer> enchantments) {
        this.itemId = id;
        this.name = name;
        this.customModelData = cmd;
        this.lore = new ArrayList<>();

        // Iterate through the lore and create TextComponents out of the lines.
        for (String line : lore) {
            this.lore.add(Component.text(line));
        }

        this.itemType = Material.matchMaterial(item);
        this.enchantments = enchantments;
    }

    // Getter function.
    public String getItemId() { return this.itemId; }

    // Uses all the custom item data to create and return an ItemStack populated with all the settings.
    public ItemStack getItem() {
        // Create the item.
        ItemStack item = new ItemStack(this.itemType);
        ItemMeta itemMeta = item.getItemMeta();

        // Update it's display data.
        itemMeta.displayName(Component.text(this.name));
        itemMeta.lore(this.lore);
        itemMeta.setCustomModelData(this.customModelData);

        // Assign the item meta to the item.
        item.setItemMeta(itemMeta);

        // Add all enchantments.
        for (String enchantmentName : this.enchantments.keySet()) {
            Enchantment currentEnchantment = Enchantment.getByName(enchantmentName.toUpperCase());
            if (currentEnchantment != null) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + currentEnchantment.toString() + " / " + this.enchantments.get(enchantmentName));
                item.addEnchantment(currentEnchantment, this.enchantments.get(enchantmentName));
            }
        }

        // Return the item.
        return item;
    }

}
