package io.github.zowja.generator;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.EventExecutor;

public class WorldInitEventExecutor implements EventExecutor {

    private final CustomStructuresBlockPopulator populator = new CustomStructuresBlockPopulator();

    @Override
    public void execute(final Listener listener, final Event event) {
        final World world = ((WorldInitEvent) event).getWorld();
        world.getPopulators().add(this.populator);
    }

}
