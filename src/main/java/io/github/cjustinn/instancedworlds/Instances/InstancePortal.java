package io.github.cjustinn.instancedworlds.Instances;

import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class InstancePortal {

    private UUID portalId;
    private String name;
    private World instanceTemplate;
    private Region region;
    private Location origin;

    public InstancePortal(World _template, Region _region, Location origin, String name) {
        this.portalId = UUID.randomUUID();
        this.instanceTemplate = _template;
        this.region = _region;
        this.origin = origin;
        this.name = name;
    }

    public InstancePortal(String id, World _template, Region _region, Location origin, String name) {
        this.portalId = UUID.fromString(id);
        this.instanceTemplate = _template;
        this.region = _region;
        this.origin = origin;
        this.name = name;
    }

    public InstancePortal(World _template, Location _c1, Location _c2, Location origin, String name) {
        this.portalId = UUID.randomUUID();
        this.instanceTemplate = _template;
        this.region = new Region(_c1, _c2);
        this.origin = origin;
        this.name = name;
    }

    public InstancePortal(String id, World _template, Location _c1, Location _c2, Location origin, String name) {
        this.portalId = UUID.fromString(id);
        this.portalId = UUID.randomUUID();
        this.instanceTemplate = _template;
        this.region = new Region(_c1, _c2);
        this.origin = origin;
        this.name = name;
    }

    // Getters
    public World getInstanceTemplate() { return this.instanceTemplate; }
    public Region getRegion() { return this.region; }
    public UUID getPortalId() { return this.portalId; }
    public String getName() { return this.name; }

    // Setters
    public void setOrigin(Location location) { this.origin = location; }

    public InstantiatedWorld openInstance(Player owner) {
        // Gather the values needed to save in the config file.
        UUID instanceId = UUID.randomUUID();
        UUID instanceOwner = owner.getUniqueId();

        // Unload the template world.
        Bukkit.unloadWorld(this.instanceTemplate.getName(), true);

        // Create a copy of the template world directory.
        File templateDirectory = this.instanceTemplate.getWorldFolder();
        File instanceDirectory = new File(Bukkit.getWorldContainer(), "instance_" + instanceId);

        try {
            FileUtils.copyDirectory(templateDirectory, instanceDirectory);
            Bukkit.getConsoleSender().sendMessage(String.format("[InstancedWorlds] %sA new instance of template [%s] has been created (%s).", ChatColor.GREEN, this.instanceTemplate.getName(), "instance_" + instanceId));
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "The template could not be instantiated!");
            return null;
        }

        // Delete the uid.dat files from both worlds.
        File templateUid = new File(this.instanceTemplate.getWorldFolder(), "uid.dat");
        templateUid.delete();

        File instanceUid = new File(Bukkit.getWorldContainer() + "\\instance_" + instanceId, "uid.dat");
        instanceUid.delete();

        // Load the new instance.
        WorldCreator creator = new WorldCreator("instance_" + instanceId);
        Bukkit.createWorld(creator);

        // Reload the template.
        Bukkit.createWorld(new WorldCreator(this.instanceTemplate.getName()));

        return new InstantiatedWorld("instance_" + instanceId, instanceOwner, this.origin, this.instanceTemplate.getName());
    }

}
