package io.github.cjustinn.instancedworlds;

import io.github.cjustinn.instancedworlds.Commands.Executors.*;
import io.github.cjustinn.instancedworlds.Commands.TabCompleters.*;
import io.github.cjustinn.instancedworlds.Instances.Actions.Action;
import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import io.github.cjustinn.instancedworlds.Instances.InstanceTemplate;
import io.github.cjustinn.instancedworlds.Instances.Region;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public final class InstancedWorlds extends JavaPlugin {

    private FileConfiguration configFile;

    @Override
    public void onEnable() {
        // Initialize the configuration file.
        saveResource("config.yml", false);
        this.configFile = getConfig();

        // Schedule the additional template worlds and portals to be initialized post-startup.
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                // Initialize the templates.
                ConfigurationSection templateNames = getConfigurationFile().getConfigurationSection("templates");
                for (String key : templateNames.getKeys(false)) {
                    // Get the template instance name.
                    String instanceName = getConfigurationFile().getString(String.format("templates.%s.name", key));

                    if (instanceName != null) {
                        // Load the template world.
                        getServer().createWorld(new WorldCreator(key));

                        // Register the instance template.
                        InstancedWorldsManager.registerTemplateWorld(new InstanceTemplate(key, instanceName));
                    }
                }

                getServer().getConsoleSender().sendMessage("[InstancedWorlds] Loaded " + ChatColor.GREEN + InstancedWorldsManager.templates.size() + ChatColor.RESET + " template worlds.");

                // Initialize the portals.
                ConfigurationSection portals = getConfigurationFile().getConfigurationSection("portals");
                if (portals != null) {
                    for (String key : portals.getKeys(false)) {
                        // Extract all the config settings for the current portal.
                        World templateWorld = getServer().getWorld(getConfigurationFile().getString("portals." + key + ".template"));
                        if (templateWorld != null) {
                            String[] regionCorners = new String[2];
                            regionCorners[0] = getConfigurationFile().getString("portals." + key + ".region.cornerOne");
                            regionCorners[1] = getConfigurationFile().getString("portals." + key + ".region.cornerTwo");
                            String originString = getConfigurationFile().getString("portals." + key + ".origin");
                            String portalName = getConfigurationFile().getString("portals." + key + ".name");

                            // Parse the location strings into proper locations.
                            Location[] regionLocations = new Location[2];
                            Location origin = null;

                            for (int i = 0; i < regionCorners.length; i++) {
                                final String[] data = regionCorners[i].split(";");
                                regionLocations[i] = new Location(getServer().getWorld(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2]), Double.parseDouble(data[3]));
                            }

                            final String[] originData = originString.split(";");
                            origin = new Location(getServer().getWorld(originData[0]), Double.parseDouble(originData[1]), Double.parseDouble(originData[2]), Double.parseDouble(originData[3]));

                            // Convert the two region locations into a region object.
                            Region region = new Region(regionLocations[0], regionLocations[1]);

                            // Create a new InstancePortal object and store it in the list.
                            InstancedWorldsManager.savePortal(new InstancePortal(key, templateWorld, region, origin, portalName));
                        }
                    }
                }

                getServer().getConsoleSender().sendMessage("[InstancedWorlds] Loaded " + ChatColor.GREEN + InstancedWorldsManager.portals.size() + ChatColor.RESET + " instance portals.");
            }
        });

        // Register all custom items.
        ConfigurationSection itemSection = getConfigurationFile().getConfigurationSection("items");
        if (itemSection != null) {

            for (String itemKey : itemSection.getKeys(false)) {

                String name = getConfigurationFile().getString(String.format("items.%s.name", itemKey));
                int cmd = getConfigurationFile().getInt(String.format("items.%s.customModelData", itemKey));
                List<String> lore = getConfigurationFile().getStringList(String.format("items.%s.lore", itemKey));
                String item = getConfigurationFile().getString(String.format("items.%s.item", itemKey));
                Map<String, Integer> enchants = new HashMap<>();

                // Get Enchantments
                for (String enchantmentKey : getConfigurationFile().getConfigurationSection(String.format("items.%s.enchantments", itemKey)).getKeys(false)) {
                    Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] Enchantment: %s", enchantmentKey));
                    enchants.put(enchantmentKey, getConfigurationFile().getInt(String.format("items.%s.enchantments.%s.level", itemKey, enchantmentKey)));
                }

                InstancedWorldsManager.registerCustomItem(itemKey, name, item, cmd, lore, enchants);

            }

        }

        // Register all loot tables.
        ConfigurationSection lootTableSection = getConfigurationFile().getConfigurationSection("loottables");
        if (lootTableSection != null) {

            for (String tableKey : lootTableSection.getKeys(false)) {
                List<String> items = new ArrayList<>();
                for (String item : getConfigurationFile().getConfigurationSection(String.format("loottables.%s.items", tableKey)).getKeys(false)) {
                    items.add(item);
                }

                InstancedWorldsManager.registerLootTable(tableKey, items);
            }

        }

        // Register event listeners.
        getServer().getPluginManager().registerEvents(new InstancedWorldsListener(), this);

        // Register commands.
        getCommand("template").setExecutor(new TemplateCreatorCommand());
        getCommand("party").setExecutor(new PartyCommandExecutor());
        getCommand("portal").setExecutor(new PortalCommandExecutor());
        getCommand("toworld").setExecutor(new WorldCommandExecutor());
        getCommand("instance").setExecutor(new InstanceCommandExecutor());

        // Register command tab completion.
        getCommand("template").setTabCompleter(new TemplateCreatorTabCompleter());
        getCommand("party").setTabCompleter(new PartyCommandTabCompleter());
        getCommand("portal").setTabCompleter(new PortalCreationTabCompleter());
        getCommand("toworld").setTabCompleter(new WorldCommandTabCompleter());
        getCommand("instance").setTabCompleter(new InstanceCommandTabCompleter());
    }

    @Override
    public void onDisable() {

    }

    public FileConfiguration getConfigurationFile() { return this.configFile; }
    public void registerListenerWithPlugin(Listener target) {
        getServer().getPluginManager().registerEvents(target, this);
    }

    // Function to allow for other plugins to register new action sign types.
    public boolean registerInstanceAction(String actionName, Function<Sign, Action> func) {
        boolean success = false;

        if (!InstancedWorldsManager.actionMaps.containsKey(actionName)) {
            InstancedWorldsManager.actionMaps.put(actionName, func);
            success = true;
        }

        return success;
    }

}
