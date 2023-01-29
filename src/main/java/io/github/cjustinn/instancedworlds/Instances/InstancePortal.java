package io.github.cjustinn.instancedworlds.Instances;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstancePortal {

    private UUID portalId;
    private String name;
    private InstanceTemplate instanceTemplate;
    private Region region;
    private Location origin;

    public InstancePortal(InstanceTemplate _template, Region _region, Location origin, String name) {
        this.portalId = UUID.randomUUID();
        this.instanceTemplate = _template;
        this.region = _region;
        this.origin = origin;
        this.name = name;
    }

    public InstancePortal(String id, InstanceTemplate _template, Region _region, Location origin, String name) {
        this.portalId = UUID.fromString(id);
        this.instanceTemplate = _template;
        this.region = _region;
        this.origin = origin;
        this.name = name;
    }

    public InstancePortal(InstanceTemplate _template, Location _c1, Location _c2, Location origin, String name) {
        this.portalId = UUID.randomUUID();
        this.instanceTemplate = _template;
        this.region = new Region(_c1, _c2);
        this.origin = origin;
        this.name = name;
    }

    public InstancePortal(String id, InstanceTemplate _template, Location _c1, Location _c2, Location origin, String name) {
        this.portalId = UUID.fromString(id);
        this.portalId = UUID.randomUUID();
        this.instanceTemplate = _template;
        this.region = new Region(_c1, _c2);
        this.origin = origin;
        this.name = name;
    }

    // Getters
    public InstanceTemplate getInstanceTemplate() { return this.instanceTemplate; }
    public Region getRegion() { return this.region; }
    public UUID getPortalId() { return this.portalId; }
    public String getName() { return this.name; }

    // Setters
    public void setOrigin(Location location) { this.origin = location; }

    public void openInstance(Player owner) {
        // Get the config values.
        UUID instanceId = UUID.randomUUID();
        UUID instanceOwner = owner.getUniqueId();

        // Unload the template.
        Bukkit.unloadWorld(this.instanceTemplate.getTemplateWorld(), true);

        // Create the InstantiatedWorld & World objects.
        InstantiatedWorld instance = null;
        World instanceWorld = null;
        AtomicBoolean success = new AtomicBoolean(false);

        // Start a separate thread to handle
        Thread thread = new Thread(() -> {

            // Get the file directories for the template world, and the new instance.
            File templateDir = new File(Bukkit.getServer().getWorldContainer(), this.instanceTemplate.getId());
            File instanceDir = new File(Bukkit.getServer().getWorldContainer(), String.format("instance_%s", instanceId));

            try {

                // Find and handle the session lock.
                File sessionLock = new File(templateDir, "session.lock");
                if (sessionLock.exists())
                    FileUtils.delete(sessionLock);

                // Copy the template directory.
                FileUtils.copyDirectory(templateDir, instanceDir);

                // Remove both 'uid.dat' files.
                File templateUid = new File(templateDir, "uid.dat");
                File instanceUid = new File(instanceDir, "uid.dat");

                if (templateUid.exists())
                    FileUtils.delete(templateUid);

                if (instanceUid.exists())
                    FileUtils.delete(instanceUid);

                success.set(true);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(success.get()) {
            // Create the world.
            WorldCreator creator = new WorldCreator(String.format("instance_%s", instanceId));
            instanceWorld = Bukkit.getServer().createWorld(creator);

            if (instanceWorld != null) {

                // Create the instance object.
                instance = new InstantiatedWorld(String.format("instance_%s", instanceId), instanceOwner, this.origin, this.instanceTemplate.getTemplateWorld().getName());

                // Save the instance object.
                InstancedWorldsManager.saveInstance(instance);

                // Send the player to the instance.
                instance.sendPlayerToInstance(owner);

            }
        }
    }

}
