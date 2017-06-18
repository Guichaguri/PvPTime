package com.guichaguri.pvptime.bukkit;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.api.PvPTimeAPI;
import com.guichaguri.pvptime.common.WorldOptions;
import java.util.Arrays;
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

    private WorldOptions defaultOptions;

    private int task = -1;

    @Override
    public void onEnable() {
        engine = new EngineBukkit();
        PvPTimeAPI.setAPI(engine);

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("pvptime").setExecutor(new PvPTimeCommand(this));

        loadConfig();
    }

    @Override
    public void onDisable() {
        // Saves the config file
        saveConfig();
    }

    public IPvPTimeAPI<String> getAPI() {
        return engine;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        engine.resetWorldOptions();
    }

    protected void loadConfig() {
        engine.setAtLeastTwoPlayers(getConfigElement("general.atLeastTwoPlayers", false));

        defaultOptions = new WorldOptions();
        loadWorld("default", defaultOptions);

        for(World world : Bukkit.getWorlds()) {
            loadWorld(defaultOptions, world);
        }

        updateTimer(engine.update());
    }

    private void loadWorld(WorldOptions defaultOptions, World world) {
        boolean isSurface = world.getEnvironment() == Environment.NORMAL;

        WorldOptions def = new WorldOptions(defaultOptions);
        def.setEnabled(isSurface || def.isEnabled());

        loadWorld("world." + world.getName(), def);

        engine.setWorldOptions(world.getName(), def);
    }

    private void loadWorld(String cat, WorldOptions o) {
        o.setEnabled(getConfigElement(cat + ".enabled", o.isEnabled()));
        o.setEngineMode(getConfigElement(cat + ".engineMode", o.getEngineMode()));
        o.setTotalDayTime(getConfigElement(cat + ".totalDayTime", o.getTotalDayTime()));
        o.setPvPTimeStart(getConfigElement(cat + ".startTime", o.getPvPTimeStart()));
        o.setPvPTimeEnd(getConfigElement(cat + ".endTime", o.getPvPTimeEnd()));
        o.setStartMessage(getConfigElement(cat + ".startMessage", o.getStartMessage()));
        o.setEndMessage(getConfigElement(cat + ".endMessage", o.getEndMessage()));
        o.setStartCmds(getStringList(cat + ".startCmds", o.getStartCmds()));
        o.setEndCmds(getStringList(cat + ".endCmds", o.getEndCmds()));
    }

    private <T> T getConfigElement(String path, T def) {
        Configuration config = getConfig();

        if(config.contains(path)) {
            return (T)config.get(path, def);
        }
        config.set(path, def);
        return def;
    }

    private String[] getStringList(String path, String[] def) {
        Configuration config = getConfig();

        if(config.contains(path)) {
            List<String> list = config.getStringList(path);
            return list.toArray(new String[list.size()]);
        }
        config.set(path, Arrays.asList(def));
        return def;
    }

    private void updateTimer(long ticksLeft) {
        if(ticksLeft <= 0) ticksLeft = 1; // Prevents the server from freezing if something goes wrong

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

        // Player shot itself?
        if(attackerPlayer.getUniqueId().equals(victim.getUniqueId())) return;

        if(engine.isPvPForced(attackerPlayer.getLocation(), victim.getLocation())) return;

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

            if(engine.isPvPForced(attacker.getLocation(), entity.getLocation())) continue;

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
        World world = event.getWorld();
        IWorldOptions options = engine.getWorldOptions(world.getName());

        if(options == null) {
            if(defaultOptions == null) loadConfig();
            loadWorld(defaultOptions, world);
        }
    }
}
