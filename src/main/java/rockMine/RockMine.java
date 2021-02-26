package rockMine;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.AsyncTask;

public class RockMine extends PluginBase implements Listener {
    private ConcurrentHashMap<Location, Long> resetQueue = new ConcurrentHashMap<>();
    private Level caveLevel;
    private Block stoneBlock;

    @Override
    public void onEnable() {
        if (getServer().loadLevel("cave")) {
            this.caveLevel = this.getServer().getLevelByName("cave");
        } else {
            getLogger().info("Could not find level 'cave'.");
            getServer().getPluginManager().disablePlugin(this);
        }

        this.stoneBlock = Block.get(Block.STONE);
        this.getServer().getScheduler().scheduleRepeatingTask(this, new AsyncTask() {

            @Override
            public void onRun() {
                resetMine(false);
            }

        }, 30, true);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        resetMine(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent ev) {
        Player player = ev.getPlayer();
        Level level = player.getLevel();
        Block block = ev.getBlock();

        if (level == this.caveLevel && block.getId() == BlockID.STONE) {
            ev.setCancelled(false);
            this.resetQueue.put(block.getLocation(), System.currentTimeMillis() / 1000);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent ev) {
        if (!(ev.getEntity() instanceof Player)) return;
        if (ev.getEntity().getLevel() != this.caveLevel) return;
        if (ev.getCause().equals(DamageCause.SUFFOCATION)) ev.setCancelled(true);
    }

	public void resetMine(boolean resetAll) {
		long current = System.currentTimeMillis() / 1000;

		if (resetAll) {
			for (Location location : resetQueue.keySet()) {
				this.caveLevel.setBlock(location, this.stoneBlock, false, false);
			}

			this.resetQueue.clear();
		} else {
			Iterator<Location> it = this.resetQueue.keySet().iterator();

			while (it.hasNext()) {
				Location location = it.next();
				if ((current - this.resetQueue.get(location)) < 30) {
					continue;
				}

				this.caveLevel.setBlock(location, this.stoneBlock, false, false);
				it.remove();
			}
		}
	}
}
