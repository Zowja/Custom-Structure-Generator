package io.github.zowja;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
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
	private final StructureLoader loader = new StructureLoader(this.getServer().getLogger());

	@Override
	public void onDisable() { }

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		this.getConfiguration().load();

		this.loadStructures();
		this.getServer().getLogger().info("[CS] Loaded " + structures.size() + " Custom Structures.");

		this.getServer().getPluginManager().registerEvent(Event.Type.WORLD_INIT, new WorldListener(), new WorldInitEventExecutor(this), Event.Priority.High, this);

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

	private void saveDefaultConfig() {
		final File configFile = new File(this.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			try {
				final InputStream in = this.getResource("config.yml");
				if (in == null) return;
				Files.copy(in, configFile.toPath());
				in.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}


	private InputStream getResource(final String filename) {
		try {
			final URL url = getClassLoader().getResource(filename);
			if (url == null) return null;
			final URLConnection connection = url.openConnection();
			connection.setUseCaches(false);
			return connection.getInputStream();
		} catch (final IOException e) {
			return null;
		}
	}

}
