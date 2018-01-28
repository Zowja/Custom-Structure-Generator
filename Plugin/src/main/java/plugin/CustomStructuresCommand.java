package plugin;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class CustomStructuresCommand implements CommandExecutor {

	private final String PREFIX = ChatColor.YELLOW + "[cstruct] " + ChatColor.WHITE;
	private final CustomStructuresPlugin plugin;

	CustomStructuresCommand(final CustomStructuresPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (args.length >= 1 && args[0].equalsIgnoreCase("export")) {
			if (this.foundWorldEdit()) {
				return this.handleExportCommand(sender, args);
			} else {
				sender.sendMessage(PREFIX + "Can't export structure." + ChatColor.GRAY + " (WorldEdit not found)");
				return true;
			}
		}
		final PluginDescriptionFile desc = this.plugin.getDescription();
		sender.sendMessage(PREFIX + desc.getName() + " v" + desc.getVersion());
		return true;
	}

	private boolean foundWorldEdit() {
		return this.plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
	}

	private boolean handleExportCommand(final CommandSender sender, final String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(PREFIX + "Only players can use this command.");
			return true;
		}
		if (args.length < 3) {
			sender.sendMessage(PREFIX + "/cstruct export <name> <triesPerChunk>");
			sender.sendMessage(PREFIX + "eg. /cstruct export shrub 10");
			return true;
		}

		final Player player = (Player) sender;

		final String name = args[1];
		final String worldType = String.valueOf(player.getWorld().getEnvironment().getId());
		final String triesPerChunk = args[2];

		// hook into WorldEdit to get selection region
		final Region region;
		try {
			region = this.getSelection(player);
		} catch (final Exception e) {
			player.sendMessage(PREFIX + "Could not get your WorldEdit selection.");
			return true;
		}

		// get structure size
		int width = region.getWidth();
		int height = region.getHeight();
		int length = region.getLength();

		// get blocks
		final List<String> blocks = this.getBlocksLines(region, player.getWorld());

		// prepare lines
		final List<String> lines = new ArrayList<>();
		lines.add(worldType);
		lines.add("x");
		lines.add(triesPerChunk);
		lines.add("0 128");
		lines.add(width + " " + height + " " + length);
		lines.add("");
		lines.add("x");
		lines.add("");
		lines.addAll(blocks);

		// make sure 'exportedStructures' directory exists
		new File(plugin.getDataFolder().getPath(),"exportedStructures").mkdirs();

		// write lines into file located in /plugins/CustomStructures/exportedStructures
		final Path file = Paths.get(plugin.getDataFolder().getPath(), "exportedStructures", name);
		try {
			Files.write(file, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			player.sendMessage(PREFIX + "Unable to save exported structure. Check console for errors.");
			e.printStackTrace();
			return true;
		}
		player.sendMessage(PREFIX + "Structure successfully exported as '" + name + "'.");
		return true;
	}

	private Region getSelection(final Player player) throws Exception {
		final WorldEditPlugin we = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		final LocalSession session = we.getSession(player);
		return session.getSelection(session.getSelectionWorld());
	}

	private List<String> getBlocksLines(final Region region, final World world) {
		final List<String> blocks = new ArrayList<>();
		final Vector min = region.getMinimumPoint();
		final Vector max = region.getMaximumPoint();
		for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
			for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
				final StringBuilder line = new StringBuilder();
				for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
					int id = world.getBlockTypeIdAt(x,y,z);
					if (line.length() > 0) line.append(" ");
					line.append(id);
				}
				blocks.add(line.toString().trim());
			}
		}
		return blocks;
	}

}
