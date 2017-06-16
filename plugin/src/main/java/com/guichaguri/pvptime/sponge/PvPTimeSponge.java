package com.guichaguri.pvptime.sponge;

import com.google.inject.Inject;
import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.common.PvPTime;
import com.guichaguri.pvptime.common.WorldOptions;
import java.io.IOException;
import java.util.List;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
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

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> config;

    private CommentedConfigurationNode configRoot;

    private EngineSponge engine;

    private Task task;

    @Listener
    public void onStart(GameStartingServerEvent event) {
        engine = new EngineSponge();

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

    protected EngineSponge getEngine() {
        return engine;
    }

    protected void reloadConfig() {
        try {
            configRoot = config.load();
        } catch(IOException ex) {
            configRoot = config.createEmptyNode();
        }
    }

    protected void loadConfig() {
        if(configRoot == null) reloadConfig();

        engine.setAtLeastTwoPlayers(configRoot.getNode("general", "atLeastTwoPlayers").getBoolean(false));

        WorldOptions defaultOptions = new WorldOptions();
        loadWorld(configRoot.getNode("default"), defaultOptions);

        for(World world : Sponge.getServer().getWorlds()) {
            boolean isSurface = world.getDimension().getType() == DimensionTypes.OVERWORLD;

            WorldOptions def = new WorldOptions(defaultOptions);
            def.setEnabled(isSurface || def.isEnabled());

            loadWorld(configRoot.getNode("world", world.getName()), def);

            engine.setWorldOptions(world.getName(), def);
        }

        updateTimer(engine.update());
    }

    private void loadWorld(CommentedConfigurationNode root, WorldOptions o) {
        o.setEnabled(root.getNode("enabled").getBoolean(o.isEnabled()));
        o.setEngineMode(root.getNode("engineMode").getInt(o.getEngineMode()));
        o.setTotalDayTime(root.getNode("totalDayTime").getInt(o.getTotalDayTime()));
        o.setPvPTimeStart(root.getNode("startTime").getInt(o.getPvPTimeStart()));
        o.setPvPTimeEnd(root.getNode("endTime").getInt(o.getPvPTimeEnd()));
        o.setStartMessage(root.getNode("startMessage").getString(o.getStartMessage()));
        o.setEndMessage(root.getNode("endMessage").getString(o.getEndMessage()));
        o.setStartCmds(getList(root.getNode("startCmds"), o.getStartCmds()));
        o.setEndCmds(getList(root.getNode("endCmds"), o.getEndCmds()));
    }

    private String[] getList(CommentedConfigurationNode node, String[] def) {
        if(node.isVirtual()) return def;
        List<String> list = node.getList(Object::toString);
        return list.toArray(new String[list.size()]);
    }

    private void updateTimer(long timeLeft) {
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
    public void onDamage(DamageEntityEvent event, EntityDamageSource source) {
        Entity victim = event.getTargetEntity();
        if(!(victim instanceof Player)) return;

        Entity attacker = source.getSource();
        if(!(attacker instanceof Player)) return;

        if(((Player)victim).hasPermission("pvptime.nopvp")) {
            // The victim has the permission to disable pvp even in night time
            event.setCancelled(true);
            return;
        } else if(((Player)attacker).hasPermission("pvptime.override")) {
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
        IWorldOptions options = engine.getWorldOptions(event.getTargetWorld().getName());

        if(options == null) {
            loadConfig(); // Lets reload the config
        }
    }

}
