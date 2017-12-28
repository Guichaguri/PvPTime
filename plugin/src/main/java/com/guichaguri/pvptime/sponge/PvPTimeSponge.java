package com.guichaguri.pvptime.sponge;

import com.google.inject.Inject;
import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.api.PvPTimeAPI;
import com.guichaguri.pvptime.common.PvPTime;
import com.guichaguri.pvptime.common.WorldOptions;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.World;

/**
 * @author Guilherme Chaguri
 */
@Plugin(
        id = "pvptime",
        name = "PvPTime",
        version = PvPTime.VERSION,
        description = "Lets you choose what in-game hours you want PvP enabled.",
        url = "http://guichaguri.com",
        authors = "Guichaguri"
)
public class PvPTimeSponge implements Runnable {

    private EngineSponge engine;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> config;
    private CommentedConfigurationNode configRoot;

    private WorldOptions defaultOptions;

    private Task task;

    @Listener
    public void onInit(GamePreInitializationEvent event) {
        engine = new EngineSponge();
        PvPTimeAPI.setAPI(engine);

        PvPTimeCommand executor = new PvPTimeCommand(this);

        CommandSpec infoCommand = CommandSpec.builder()
                .permission("pvptime.info")
                .executor(executor::info)
                .build();
        CommandSpec reloadCommand = CommandSpec.builder()
                .permission("pvptime.reload")
                .executor(executor::reload)
                .build();

        CommandSpec command = CommandSpec.builder()
                .permission("pvptime.info")
                .executor(executor)
                .child(infoCommand, "info")
                .child(reloadCommand, "reload")
                .build();
        Sponge.getCommandManager().register(this, command, "pvptime");

        Sponge.getServiceManager().setProvider(this, IPvPTimeAPI.class, engine);
    }

    @Listener
    public void onStarted(GameStartedServerEvent event) {
        loadConfig();
    }

    @Listener
    public void onStop(GameStoppingEvent event) {
        try {
            // Saves the config file
            config.save(configRoot);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    public IPvPTimeAPI<String> getAPI() {
        return engine;
    }

    protected void reloadConfig() {
        try {
            configRoot = config.load();
        } catch(IOException ex) {
            configRoot = config.createEmptyNode();
        }
        engine.resetWorldOptions();
    }

    protected void loadConfig() {
        if(configRoot == null) reloadConfig();

        engine.setAtLeastTwoPlayers(getConfigElement(configRoot.getNode("general", "atLeastTwoPlayers"), false));

        defaultOptions = new WorldOptions();
        loadWorld(configRoot.getNode("default"), defaultOptions);

        for(World world : Sponge.getServer().getWorlds()) {
            loadWorld(defaultOptions, world);
        }

        updateTimer(engine.update());
    }

    private void loadWorld(WorldOptions defaultOptions, World world) {
        boolean isSurface = world.getDimension().getType() == DimensionTypes.OVERWORLD;

        WorldOptions def = new WorldOptions(defaultOptions);
        def.setEnabled(isSurface || def.isEnabled());

        loadWorld(configRoot.getNode("world", world.getName()), def);

        engine.setWorldOptions(world.getName(), def);
    }

    private void loadWorld(CommentedConfigurationNode root, WorldOptions o) {
        o.setEnabled(getConfigElement(root.getNode("enabled"), o.isEnabled()));
        o.setEngineMode(getConfigElement(root.getNode("engineMode"), o.getEngineMode()));
        o.setTotalDayTime(getConfigElement(root.getNode("totalDayTime"), o.getTotalDayTime()));
        o.setPvPTimeStart(getConfigElement(root.getNode("startTime"), o.getPvPTimeStart()));
        o.setPvPTimeEnd(getConfigElement(root.getNode("endTime"), o.getPvPTimeEnd()));
        o.setStartMessage(getConfigElement(root.getNode("startMessage"), o.getStartMessage()));
        o.setEndMessage(getConfigElement(root.getNode("endMessage"), o.getEndMessage()));
        o.setStartCmds(getStringList(root.getNode("startCmds"), o.getStartCmds()));
        o.setEndCmds(getStringList(root.getNode("endCmds"), o.getEndCmds()));
    }

    private <T> T getConfigElement(ConfigurationNode node, T def) {
        if(!node.isVirtual()) {
            return (T)node.getValue(def);
        }
        node.setValue(def);
        return def;
    }

    private String[] getStringList(CommentedConfigurationNode node, String[] def) {
        if(!node.isVirtual()) {
            List<String> list = node.getList(Object::toString);
            return list.toArray(new String[list.size()]);
        }
        node.setValue(Arrays.asList(def));
        return def;
    }

    private void updateTimer(long timeLeft) {
        if(timeLeft <= 0) timeLeft = 1; // Prevents the server from freezing if something goes wrong

        if(task != null) task.cancel();
        task = Sponge.getScheduler().createTaskBuilder().delayTicks(timeLeft).execute(this).submit(this);
    }

    @Override
    public void run() {
        // Updates the engine
        updateTimer(engine.update());
    }

    @Listener(order = Order.BEFORE_POST)
    public void onCommand(SendCommandEvent event) {
        // Force an update when a command is triggered
        // This prevents time commands from messing up the ticks count
        updateTimer(2);
    }

    @Listener(order = Order.LAST)
    public void onDamage(DamageEntityEvent event, @First EntityDamageSource source) {
        Entity victim = event.getTargetEntity();
        if(!(victim instanceof Player)) return;

        Entity attacker = source.getSource();
        Player player = null;

        if(attacker instanceof Player) {
            player = (Player)attacker;
        } else if(attacker instanceof Projectile) {
            ProjectileSource shooter = ((Projectile)attacker).getShooter();
            if(shooter instanceof Player) player = (Player)shooter;
        }

        if(player == null) return;

        // Player shot himself?
        if(player.getUniqueId().equals(victim.getUniqueId())) return;

        if(((Player)victim).hasPermission("pvptime.nopvp")) {
            // The victim has the permission to disable pvp even in night time
            event.setCancelled(true);
            return;
        } else if(player.hasPermission("pvptime.override")) {
            // The attacker has the permission to enable pvp even in day time
            return;
        }

        Boolean isPvPTime = engine.isPvPTime(victim.getWorld().getName());

        // Cancel the event when it's not pvp time
        if(isPvPTime != null && !isPvPTime) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.BEFORE_POST)
    public void onWorldLoad(LoadWorldEvent event) {
        World world = event.getTargetWorld();
        IWorldOptions options = engine.getWorldOptions(world.getName());

        if(options == null) {
            if(defaultOptions == null) loadConfig();
            loadWorld(defaultOptions, world);
        }
    }

}
