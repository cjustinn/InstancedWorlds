package io.github.cjustinn.instancedworlds.Instances;

import io.github.cjustinn.instancedworlds.InstancedWorldsManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public class InstanceTemplate {

    // Data Members
    private final String id;
    private final String name;
    private GameMode gamemode;

    // Constructor
    public InstanceTemplate(String id, String name, GameMode gamemode) {
        this.id = id;
        this.name = name;
        this.gamemode = gamemode;
    }

    // Getters
    public GameMode getGamemode() { return this.gamemode; }
    public String getId() { return this.id; }
    public String getName() { return this.name; }
    public World getTemplateWorld() {
        World template = Bukkit.getServer().createWorld(new WorldCreator(this.id));
        return template;
    }

    // Setters
    public void setGameMode(GameMode gamemode) {
        this.gamemode = gamemode;

        InstancedWorldsManager.saveConfigValue(String.format("templates.%s.gamemode", this.id), gamemode.name());
    }
}
