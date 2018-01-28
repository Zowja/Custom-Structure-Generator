package plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;
import plugin.structure.Structure;

public class CustomStructuresPlugin extends JavaPlugin {
	
	public static final Set<Structure> structures = new HashSet<>();
	private final StructureLoader loader = new StructureLoader();

	@Override
	public void onDisable() { }

	@Override
	public void onEnable() {
		this.loadStructures();
		this.getServer().getPluginManager().registerEvent(Event.Type.CHUNK_POPULATED, new GenerateStructures(), Event.Priority.High, this);
		this.getServer().getLogger().info("[CS] Loaded " + structures.size() + " Custom Structures.");
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
