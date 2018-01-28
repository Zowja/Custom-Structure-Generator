package plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;

public class Plugin extends JavaPlugin {
	
	public static HashSet<Structure> structures;
	private final StructureLoader loader = new StructureLoader();

	@Override
	public void onDisable() { }

	@Override
	public void onEnable() {
		structures = new HashSet<Structure>();
		this.loadStructures();
		this.getServer().getPluginManager().registerEvent(Event.Type.CHUNK_POPULATED, new GenerateStructures(), Event.Priority.High, this);
		this.getServer().getLogger().info("Loaded Custom Structures.");
		this.getCommand("customstructures").setExecutor(new CustomStructuresCommand(this));
	}
	
	public void loadStructures() {
		final File structDir = this.getDataFolder();
		structDir.mkdirs();
		final File[] structureFiles = structDir.listFiles((file, name) -> name.toLowerCase().endsWith(".zip"));
		if (structureFiles == null || structureFiles.length == 0) return;
		for (final File file : structureFiles) {
			try {
				final Collection<Structure> loadedStructures = this.loader.loadFromFile(file);
				if (loadedStructures != null)
					structures.addAll(loadedStructures);
			} catch (final IOException ignored) { }
		}
	}

}
