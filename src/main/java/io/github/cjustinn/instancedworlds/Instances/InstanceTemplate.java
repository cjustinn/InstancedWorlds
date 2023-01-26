package io.github.cjustinn.instancedworlds.Instances;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public class InstanceTemplate {

    // Data Members
    private final String id;
    private final String name;

    // Constructor
    public InstanceTemplate(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters
    public String getId() { return this.id; }
    public String getName() { return this.name; };
    public World getTemplateWorld() {
        World template = Bukkit.getServer().createWorld(new WorldCreator(this.id));
        return template;
    }
}
