package com.guichaguri.pvptime.bukkit;

import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.common.WorldOptions;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * @author Guilherme Chaguri
 */
public class PvPTimeBukkit extends JavaPlugin implements Listener, Runnable {

    private EngineBukkit engine;

    private int task = -1;

    @Override
    public void onEnable() {
        engine = new EngineBukkit();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("pvptime").setExecutor(new PvPTimeCommand(this));

        loadConfig();
    }

    @Override
    public void onDisable() {
        // Saves the config file
        saveConfig();
    }

    protected EngineBukkit getEngine() {
        return engine;
    }

    protected void loadConfig() {
        Configuration config = getConfig();

        engine.setAtLeastTwoPlayers(config.getBoolean("general.atLeastTwoPlayers", false));

        WorldOptions defaultOptions = new WorldOptions();
        loadWorld(config, "default", defaultOptions);

        for(World world : Bukkit.getWorlds()) {
            boolean isSurface = world.getEnvironment() == Environment.NORMAL;

            WorldOptions def = new WorldOptions(defaultOptions);
            def.setEnabled(isSurface || def.isEnabled());

            loadWorld(config, "world." + world.getName(), def);

            engine.setWorldOptions(world.getName(), def);
        }

        updateTimer(engine.update());
    }

    private void loadWorld(Configuration config, String cat, WorldOptions o) {
        o.setEnabled(config.getBoolean(cat + ".enabled", o.isEnabled()));
        o.setEngineMode(config.getInt(cat + ".engineMode", o.getEngineMode()));
        o.setTotalDayTime(config.getInt(cat + ".totalDayTime", o.getTotalDayTime()));
        o.setPvPTimeStart(config.getInt(cat + ".startTime", o.getPvPTimeStart()));
        o.setPvPTimeEnd(config.getInt(cat + ".endTime", o.getPvPTimeEnd()));
        o.setStartMessage(config.getString(cat + ".startMessage", o.getStartMessage()));
        o.setEndMessage(config.getString(cat + ".endMessage", o.getEndMessage()));
        o.setStartCmds(getStringList(config, cat + ".startCmds", o.getStartCmds()));
        o.setEndCmds(getStringList(config, cat + ".endCmds", o.getEndCmds()));
    }

    private String[] getStringList(Configuration config, String path, String[] def) {
        if(!config.contains(path)) return def;
        List<String> list = config.getStringList(path);
        return list.toArray(new String[list.size()]);
    }

    private void updateTimer(long ticksLeft) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        if(task != -1) scheduler.cancelTask(task);
        task = scheduler.scheduleSyncDelayedTask(this, this, ticksLeft);
    }

    @Override
    public void run() {
        // Updates the engine
        updateTimer(engine.update());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        // Force an update when a command is triggered
        // This prevents time commands from messing up the ticks count
        updateTimer(2);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity attacker = event.getDamager();
        Entity victim = event.getEntity();

        // The victim is not a player
        if(!(victim instanceof Player)) return;

        Player attackerPlayer = null;

        if(attacker instanceof Player) {
            attackerPlayer = (Player)attacker;
        } else if(attacker instanceof Projectile) {
            ProjectileSource source = ((Projectile)attacker).getShooter();
            if(source instanceof Player) attackerPlayer = (Player)source;
        }

        // The attacker is not a player
        if(attackerPlayer == null) return;

        if(victim.hasPermission("pvptime.nopvp")) {
            // The victim has the permission to disable pvp even in night time
            event.setCancelled(true);
            return;
        } else if(attackerPlayer.hasPermission("pvptime.override")) {
            // The attacker has the permission to enable pvp even in day time
            return;
        }

        Boolean isPvPTime = engine.isPvPTime(victim.getWorld().getName());

        // Cancel the event when it's not pvp time
        if(isPvPTime != null && !isPvPTime) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();

        ProjectileSource source = potion.getShooter();
        if(!(source instanceof Player)) return;

        Player attacker = (Player)source;

        for(LivingEntity entity : event.getAffectedEntities()) {
            // Ignore entities that are not players
            if(!(entity instanceof Player)) continue;

            if(entity.hasPermission("pvptime.nopvp")) {
                // The victim has the permission to disable pvp even in night time
                event.setCancelled(true);
                continue;
            } else if(attacker.hasPermission("pvptime.override")) {
                // The attacker has the permission to enable pvp even in day time
                continue;
            }

            Boolean isPvPTime = engine.isPvPTime(entity.getWorld().getName());

            // Cancel the event when it's not pvp time
            if(isPvPTime != null && !isPvPTime) {
                event.setIntensity(entity, 0);
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldInitEvent event) {
        IWorldOptions options = engine.getWorldOptions(event.getWorld().getName());

        if(options == null) {
            loadConfig(); // Lets reload the config
        }
    }
}
