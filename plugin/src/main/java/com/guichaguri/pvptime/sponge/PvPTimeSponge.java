package com.guichaguri.pvptime.sponge;

import com.google.inject.Inject;
import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.api.PvPTimeAPI;
import com.guichaguri.pvptime.common.WorldOptions;
import java.io.IOException;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

/**
 * @author Guilherme Chaguri
 */
@Plugin("pvptime")
public class PvPTimeSponge implements Runnable {

    @Inject
    private PluginContainer container;

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<@NotNull CommentedConfigurationNode> config;
    private CommentedConfigurationNode configRoot;

    private EngineSponge engine;

    private WorldOptions defaultOptions;

    private ScheduledTask task;

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Parameterized> event) {
        PvPTimeCommand executor = new PvPTimeCommand(this);

        Command.Parameterized infoCommand = Command.builder()
                .permission("pvptime.info")
                .executor(executor::info)
                .build();
        Command.Parameterized reloadCommand = Command.builder()
                .permission("pvptime.reload")
                .executor(executor::reload)
                .build();
        Command.Parameterized helpCommand = Command.builder()
                .permission("pvptime.info")
                .executor(executor::help)
                .build();

        Command.Parameterized command = Command.builder()
                .permission("pvptime.info")
                .executor(executor)
                .addChild(infoCommand, "info")
                .addChild(reloadCommand, "reload")
                .addChild(helpCommand, "help")
                .build();

        event.register(this.container, command, "pvptime");
    }

    @Listener
    public void providePvPTimeEngine(ProvideServiceEvent.EngineScoped<IPvPTimeAPI<?>, Server> event) {
        event.suggest(() -> engine);
    }

    @Listener
    public void onStarted(StartingEngineEvent<Server> event) {
        engine = new EngineSponge(logger);
        PvPTimeAPI.setAPI(engine);

        try {
            loadConfig();
        } catch(Exception ex) {
            logger.error("Failed to load configuration file", ex);
        }

        PermissionService perms = event.engine().serviceProvider().permissionService();

        perms.newDescriptionBuilder(container)
                .id("pvptime.nopvp")
                .description(Component.text("Disable PvP for a player even in nighttime."))
                .defaultValue(Tristate.FALSE)
                .register();

        perms.newDescriptionBuilder(container)
                .id("pvptime.override")
                .description(Component.text("Let the player deal damage even when it's not pvp time."))
                .defaultValue(Tristate.FALSE)
                .register();

        perms.newDescriptionBuilder(container)
                .id("pvptime.reload")
                .description(Component.text("Reload the configuration file."))
                .defaultValue(Tristate.FALSE)
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();

        perms.newDescriptionBuilder(container)
                .id("pvptime.info.current")
                .description(Component.text("Get info for the current world."))
                .defaultValue(Tristate.TRUE)
                .register();

        perms.newDescriptionBuilder(container)
                .id("pvptime.info.all")
                .description(Component.text("Get info for all worlds."))
                .defaultValue(Tristate.TRUE)
                .register();
    }

    @Listener
    public void onStop(StoppingEngineEvent<Server> event) {
        try {
            // Saves the config file
            config.save(configRoot);
        } catch(IOException ex) {
            logger.error("Error saving the config", ex);
        }

        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public IPvPTimeAPI<ResourceKey> getAPI() {
        return engine;
    }

    protected void reloadConfig() {
        try {
            configRoot = config.load();
        } catch(IOException ex) {
            configRoot = config.createNode();
        }
        engine.resetWorldOptions();
    }

    protected void loadConfig() {
        if(configRoot == null) reloadConfig();

        engine.setAtLeastTwoPlayers(getConfigElement(configRoot.node("general", "atLeastTwoPlayers"), false, "Messages will broadcast if there's at least two players online"));
        engine.setEnableRegionGuard(getConfigElement(configRoot.node("general", "enableRegionGuardIntegration"), false, "Whether the RegionGuard integration will be enabled.\nThis will enable PvP at day in a region with 'pvp' flag set to true."));
        configRoot.node("general").comment("General Configuration");

        defaultOptions = new WorldOptions();
        defaultOptions.setStartMessage("<red>It's night and PvP is turned on</red>"); // use minimessage instead of color codes
        defaultOptions.setEndMessage("<green>It's daytime and PvP is turned off</green>"); // use minimessage instead of color codes
        loadWorld(configRoot.node("default"), defaultOptions, "Default Options. The options below are copied to newly created dimensions");

        for(ServerWorld world : Sponge.server().worldManager().worlds()) {
            loadWorld(defaultOptions, world);
        }

        updateTimer(engine.update());
    }

    private void loadWorld(WorldOptions defaultOptions, ServerWorld world) {
        boolean isSurface = world.worldType().hasSkylight();

        WorldOptions def = new WorldOptions(defaultOptions);
        def.setEnabled(isSurface || def.isEnabled());

        loadWorld(configRoot.node("world", world.key().asString()), def, "Configuration for " + world.key().asString());

        engine.setWorldOptions(world.key(), def);
    }

    private void loadWorld(CommentedConfigurationNode root, WorldOptions o, String comment) {
        o.setEnabled(getConfigElement(root.node("enabled"), o.isEnabled(), "Whether PvPTime will be disabled on this dimension"));
        o.setEngineMode(getConfigElement(root.node("engineMode"), o.getEngineMode(), "1: Configurable Time | -1: PvP always disabled | -2: PvP always enabled"));
        o.setTotalDayTime(getConfigElement(root.node("totalDayTime"), o.getTotalDayTime(), "The total time that a Minecraft day has"));
        o.setPvPTimeStart(getConfigElement(root.node("startTime"), o.getPvPTimeStart(), "Time in ticks that the PvP will be enabled"));
        o.setPvPTimeEnd(getConfigElement(root.node("endTime"), o.getPvPTimeEnd(), "Time in ticks that the PvP will be disabled"));
        o.setStartMessage(getConfigElement(root.node("startMessage"), o.getStartMessage(), "Message to be broadcasted when the PvP Time starts"));
        o.setEndMessage(getConfigElement(root.node("endMessage"), o.getEndMessage(), "Message to be broadcasted when the PvP Time ends"));
        o.setStartCommands(getStringList(root.node("startCmds"), o.getStartCommands(), "Commands to be executed when the PvPTime starts"));
        o.setEndCommands(getStringList(root.node("endCmds"), o.getEndCommands(), "Commands to be executed when the PvPTime ends"));
        root.comment(comment);
    }

    private <T> T getConfigElement(CommentedConfigurationNode node, T def, String comment) {
        node.comment(comment);

        if(!node.virtual()) {
            try {
                return (T) node.get(def.getClass(), def);
            } catch (Exception ex) {
                logger.warn("Invalid config value", ex);
            }
        }

        try {
            node.set(def.getClass(), def);
        } catch (Exception ex) {
            logger.warn("Failed to save config value", ex);
        }
        return def;
    }

    private List<String> getStringList(CommentedConfigurationNode node, List<String> def, String comment) {
        node.comment(comment);

        if(!node.virtual()) {
            try {
                return node.getList(String.class);
            } catch (Exception ex) {
                logger.warn("Invalid config value", ex);
            }
        }

        try {
            node.setList(String.class, def);
        } catch (Exception ex) {
            logger.warn("Failed to save config value", ex);
        }
        return def;
    }

    private void updateTimer(long timeLeft) {
        if(timeLeft <= 0) timeLeft = 1; // Prevents the server from freezing if something goes wrong

        if(task != null) task.cancel();

        task = Sponge.asyncScheduler().submit(Task.builder()
                .execute(this)
                .plugin(container)
                .delay(Ticks.of(timeLeft))
                .build()
        );
    }

    @Override
    public void run() {
        // Updates the engine
        updateTimer(engine.update());
    }

    @Listener(order = Order.BEFORE_POST)
    public void onCommand(ExecuteCommandEvent event) {
        // Force an update when a command is triggered
        // This prevents time commands from messing up the ticks count
        updateTimer(2);
    }

    @Listener(order = Order.LAST)
    public void onDamage(DamageEntityEvent event, @First DamageSource source) {
        Entity victim = event.entity();
        if(!(victim instanceof ServerPlayer)) return;

        Entity attacker = source.indirectSource().or(source::source).orElse(null);
        ServerPlayer player = null;

        if (attacker == null) return;

        if(attacker instanceof ServerPlayer) {
            player = (ServerPlayer)attacker;
        } else if(attacker instanceof Projectile) {
            Value.Mutable<ProjectileSource> shooter = ((Projectile)attacker).shooter().orElse(null);
            if(shooter != null && shooter.get() instanceof ServerPlayer) player = (ServerPlayer)shooter;
        }

        if(player == null) return;

        // Player shot himself?
        if(player.uniqueId().equals(victim.uniqueId())) return;

        if(engine.isPvPForced(attacker.serverLocation(), victim.serverLocation())) return;

        if(((ServerPlayer)victim).hasPermission("pvptime.nopvp", event.cause())) {
            // The victim has the permission to disable pvp even in night time
            event.setCancelled(true);
            return;
        } else if(player.hasPermission("pvptime.override", event.cause())) {
            // The attacker has the permission to enable pvp even in day time
            return;
        }

        Boolean isPvPTime = engine.isPvPTime(player.world().key());

        // Cancel the event when it's not pvp time
        if(isPvPTime != null && !isPvPTime) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.BEFORE_POST)
    public void onWorldLoad(LoadWorldEvent event) {
        ServerWorld world = event.world();
        IWorldOptions options = engine.getWorldOptions(world.key());

        if(options == null) {
            if(defaultOptions == null) loadConfig();
            loadWorld(defaultOptions, world);
        }

        updateTimer(2);
    }

}
