package io.github.cjustinn.instancedworlds;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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
                List<String> templateNames = getConfigurationFile().getStringList("templates");
                for (String name : templateNames) {
                    World template = getServer().createWorld(new WorldCreator(name));
                    if (template == null) getServer().getConsoleSender().sendMessage(ChatColor.RED + "World [" + name + "] is null!");
                    InstancedWorldsManager.addTemplate(template);
                }

                getServer().getConsoleSender().sendMessage("[InstancedWorlds] Loaded " + ChatColor.GREEN + InstancedWorldsManager.templates.size() + ChatColor.RESET + " template worlds.");

                // Initialize the portals.
                ConfigurationSection portals = getConfigurationFile().getConfigurationSection("portals");
                if (portals != null) {
                    for (String key : portals.getKeys(false)) {
                        // Extract all the config settings for the current portal.
                        World templateWorld = getServer().getWorld(getConfigurationFile().getString("portals." + key + ".template"));
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
                        InstancedWorldsManager.savePortal(new InstancePortal(templateWorld, region, origin, portalName));
                    }
                }

                getServer().getConsoleSender().sendMessage("[InstancedWorlds] Loaded " + ChatColor.GREEN + InstancedWorldsManager.portals.size() + ChatColor.RESET + " instance portals.");
            }
        });

        // Register event listeners.
        getServer().getPluginManager().registerEvents(new InstancedWorldsListener(), this);

        // Register commands.
        getCommand("createtemplate").setExecutor(new TemplateCreatorCommand());
        getCommand("party").setExecutor(new PartyCommandExecutor());
        getCommand("portal").setExecutor(new PortalCommandExecutor());
        getCommand("toworld").setExecutor(new WorldCommandExecutor());
        getCommand("instance").setExecutor(new InstanceCommandExecutor());

        // Register command tab completion.
        getCommand("party").setTabCompleter(new PartyCommandTabCompleter());
        getCommand("portal").setTabCompleter(new PortalCreationTabCompleter());
        getCommand("toworld").setTabCompleter(new WorldCommandTabCompleter());
        getCommand("instance").setTabCompleter(new InstanceCommandTabCompleter());
    }

    @Override
    public void onDisable() {
        // Destroy all active instances.
        for (InstantiatedWorld instance : InstancedWorldsManager.instances) {
            instance.destroyInstance();
        }
    }

    public FileConfiguration getConfigurationFile() { return this.configFile; }

}
