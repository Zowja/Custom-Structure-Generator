package plugin.generator;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.EventExecutor;
import plugin.CustomStructuresPlugin;

import java.util.ArrayList;
import java.util.List;

public class WorldInitEventExecutor implements EventExecutor {

    private final CustomStructuresBlockPopulator populator;
    private final CustomStructuresPlugin plugin;

    public WorldInitEventExecutor(final CustomStructuresPlugin plugin) {
        this.populator = new CustomStructuresBlockPopulator();
        this.plugin = plugin;
    }

    @Override
    public void execute(final Listener listener, final Event event) {
        final World world = ((WorldInitEvent) event).getWorld();
        final List<String> worlds = this.plugin.getConfiguration().getStringList("enableInWorlds",new ArrayList<>());
        if (worlds.contains(world.getName()))
            world.getPopulators().add(this.populator);
    }

}
