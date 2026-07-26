package com.guichaguri.pvptime.bukkit;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public abstract class PvPTimeScheduler {

    public static PvPTimeScheduler create(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            return new PvPTimeFoliaScheduler(plugin, runnable);
        }

        return new PvPTimeBukkitScheduler(plugin, runnable);
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    Plugin plugin;
    Runnable runnable;

    PvPTimeScheduler(Plugin plugin, Runnable runnable) {
        this.plugin = plugin;
        this.runnable = runnable;
    }

    public abstract void reschedule(long ticksToWait);

    static final class PvPTimeBukkitScheduler extends PvPTimeScheduler {
        private int task = -1;

        PvPTimeBukkitScheduler(Plugin plugin, Runnable runnable) {
            super(plugin, runnable);
        }

        @Override
        public void reschedule(long ticksToWait) {
            BukkitScheduler scheduler = Bukkit.getScheduler();

            if (task != -1) {
                scheduler.cancelTask(task);
            }

            task = scheduler.scheduleSyncDelayedTask(plugin, runnable, ticksToWait);
        }
    }

    static final class PvPTimeFoliaScheduler extends PvPTimeScheduler {
        private ScheduledTask task;

        PvPTimeFoliaScheduler(Plugin plugin, Runnable runnable) {
            super(plugin, runnable);
        }

        @Override
        public void reschedule(long ticksToWait) {
            if (task != null) {
                task.cancel();
            }

            task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, this::run, ticksToWait);
        }

        private void run(ScheduledTask scheduledTask) {
            runnable.run();
            task = null;
        }
    }
}
