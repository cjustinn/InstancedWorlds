package io.github.cjustinn.instancedworlds.Commands.Listeners;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import io.github.cjustinn.instancedworlds.Instances.InstancePortal;
import io.github.cjustinn.instancedworlds.Instances.InstanceTemplate;
import io.github.cjustinn.instancedworlds.Instances.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class PortalCreationListener implements Listener {

    private final Player player;
    private final int locationCount = 3;

    private final World template;
    private final String name;

    private List<Location> locations = new ArrayList<>();

    public PortalCreationListener(Player target, String template, String name) {
        this.player = target;

        // If the template value doesn't start with "template_", add it.
        if (!template.startsWith("template_"))
            template = String.format("template_%s", template);

        // Get the instance template.
        this.template = InstancedWorldsManager.getTemplateWorldById(template);
        this.name = name;

        target.sendMessage(String.format("%s%s", ChatColor.GOLD, "Please left-click the first corner of the instance portal."));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() == this.player && this.locations.size() < this.locationCount) {
            event.setCancelled(true);
            this.locations.add(event.getClickedBlock().getLocation());

            if (this.locations.size() < this.locationCount) {
                // Send a prompt message to the player.
                String prompt = "";

                switch(this.locations.size()) {
                    case 0:
                        prompt = "Please left-click the first corner of the instance portal.";
                        break;
                    case 1:
                        prompt = "Please left-click the second corner of the instance portal.";
                        break;
                    case 2:
                        prompt = "Please left-click the location you want players to be teleported to when the instance is left, or deleted.";
                        break;
                    default:
                        prompt = "Uh-oh, you shouldn't be seeing this message.";
                        break;
                }

                player.sendMessage(String.format("%s%s", ChatColor.GOLD, prompt));
            }

            if (this.locations.size() == this.locationCount) {
                // Save the portal into the manager list.
                Region portalRegion = new Region(this.locations.get(0), this.locations.get(1));

                final int templateIndex = InstancedWorldsManager.getTemplateIndexById(this.template.getName());
                if (templateIndex >= 0) {
                    InstanceTemplate _template = InstancedWorldsManager.templates.get(templateIndex);

                    InstancePortal portal = new InstancePortal(_template, portalRegion, this.locations.get(2), name);
                    InstancedWorldsManager.savePortal(portal);

                    // Create the portal in the config
                    String cornerOne = String.format("%s;%f;%f;%f",
                            portalRegion.getCornerOne().getWorld().getName(),
                            portalRegion.getCornerOne().getX(),
                            portalRegion.getCornerOne().getY(),
                            portalRegion.getCornerOne().getZ()
                    );

                    String cornerTwo = String.format("%s;%f;%f;%f",
                            portalRegion.getCornerTwo().getWorld().getName(),
                            portalRegion.getCornerTwo().getX(),
                            portalRegion.getCornerTwo().getY(),
                            portalRegion.getCornerTwo().getZ()
                    );

                    String originString = String.format("%s;%f;%f;%f",
                            this.locations.get(2).getWorld().getName(),
                            this.locations.get(2).getX(),
                            this.locations.get(2).getY(),
                            this.locations.get(2).getZ()
                    );

                    InstancedWorldsManager.saveConfigValue("portals." + portal.getPortalId() + ".template", this.template.getName());
                    InstancedWorldsManager.saveConfigValue("portals." + portal.getPortalId() + ".region.cornerOne", cornerOne);
                    InstancedWorldsManager.saveConfigValue("portals." + portal.getPortalId() + ".region.cornerTwo", cornerTwo);
                    InstancedWorldsManager.saveConfigValue("portals." + portal.getPortalId() + ".origin", originString);
                    InstancedWorldsManager.saveConfigValue("portals." + portal.getPortalId() + ".name", this.name);

                    player.sendMessage(ChatColor.GREEN + "The instance portal has been created!");
                }

                // Deregister this listener.
                HandlerList.unregisterAll(this);
            } else if (this.locations.size() == 2 && !this.locations.get(0).getWorld().getName().equalsIgnoreCase(this.locations.get(1).getWorld().getName())) {
                player.sendMessage(ChatColor.RED + "The portal region cannot go across worlds!");
                HandlerList.unregisterAll(this);
            }
        }
    }

}
