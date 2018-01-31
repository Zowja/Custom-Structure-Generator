package io.github.zowja;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.github.zowja.generator.WorldInitEventExecutor;
import org.bukkit.event.world.WorldListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;
import io.github.zowja.command.CustomStructuresCommand;
import io.github.zowja.structure.Structure;

public class CustomStructuresPlugin extends JavaPlugin {

    public static final Set<Structure> structures = new HashSet<>();
    private StructureLoader loader;

    @Override
    public void onDisable() { }

    @Override
    public void onEnable() {
        this.loader = new StructureLoader(this.getServer().getLogger());

        this.loadStructures();
        this.getServer().getLogger().info("[CS] Loaded " + structures.size() + " Custom Structure" + (structures.size() == 1 ? "." : "s."));

        this.getServer().getPluginManager().registerEvent(Event.Type.WORLD_INIT, new WorldListener(), new WorldInitEventExecutor(), Event.Priority.High, this);

        this.getCommand("customstructures").setExecutor(new CustomStructuresCommand(this));
    }

    public void loadStructures() {
        final File structDir = this.getDataFolder();
        structDir.mkdirs();
        final File[] structureFiles = structDir.listFiles((file, name) -> name.toLowerCase().endsWith(".zip"));
        if (structureFiles == null || structureFiles.length == 0) return;
        for (final File file : structureFiles) {
            final Collection<Structure> loadedStructures = this.loader.loadFromFile(file);
            if (loadedStructures != null)
                structures.addAll(loadedStructures);
        }
    }

}
