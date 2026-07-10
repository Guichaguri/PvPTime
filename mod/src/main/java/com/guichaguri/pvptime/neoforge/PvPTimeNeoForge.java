package com.guichaguri.pvptime.neoforge;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.api.PvPTimeAPI;
import com.guichaguri.pvptime.common.WorldOptions;

import java.util.Optional;
import java.util.function.Function;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.timeline.Timeline;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * @author Guilherme Chaguri
 */
@Mod("pvptime")
public class PvPTimeNeoForge {
    private final PvPTimeConfig config = new PvPTimeConfig();
    private EngineNeoForge engine;
    private WorldOptions defaultOptions;

    private MinecraftServer server;

    private long ticksLeft = 0;

    public PvPTimeNeoForge(IEventBus eventBus) {
        NeoForge.EVENT_BUS.register(this);
        eventBus.addListener(this::imc);
    }

    @SubscribeEvent
    public void onStart(ServerStartingEvent event) {
        server = event.getServer();

        config.load();

        engine = new EngineNeoForge(server);
        PvPTimeAPI.setAPI(engine);
    }

    @SubscribeEvent
    public void onPostStart(ServerStartedEvent event) {
        loadConfig(event.getServer());
    }

    @SubscribeEvent
    public void onStop(ServerStoppingEvent event) {
        config.save();

        engine = null;
        PvPTimeAPI.setAPI(null);

        server = null;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PvPTimeCommand command = new PvPTimeCommand(this);

        command.register(event.getDispatcher());
    }

    public void imc(InterModProcessEvent event) {
        for(InterModComms.IMCMessage msg : event.getIMCStream().toList()) {
            String key = msg.method().toLowerCase();
            if(!key.equals("pvptime") && !key.equals("api")) continue;

            Function<IPvPTimeAPI, Void> func = (Function<IPvPTimeAPI, Void>)msg.messageSupplier().get();

            func.apply(engine);
        }
    }

    public IPvPTimeAPI<ResourceKey<Level>> getAPI() {
        return engine;
    }

    protected void reloadConfig() {
        config.load();
        engine.resetWorldOptions();
    }

    protected void loadConfig(MinecraftServer server) {
        config.loadEngine(engine);

        defaultOptions = new WorldOptions();
        config.loadDimension("defaultOptions", defaultOptions, "Default Options. The options below are copied to newly created dimensions");

        for(ResourceKey<Level> key : server.levelKeys()) {
            ServerLevel level = server.getLevel(key);
            loadDimension(defaultOptions, key, level);
        }

        config.save();

        ticksLeft = engine.update();
    }

    private void loadDimension(WorldOptions defaultOptions, ResourceKey<Level> key, Level level) {
        boolean isSurface = level != null ? !level.dimensionType().hasFixedTime() : key == Level.OVERWORLD;
        Timeline timeline = getLevelTimeline(level);

        WorldOptions def = new WorldOptions(defaultOptions);

        def.setEnabled(isSurface || def.isEnabled());

        if (timeline != null) {
            // Calculate defaults based on the dimension total time
            int totalDayTime = timeline.periodTicks().orElse(def.getTotalDayTime());
            def.setPvPTimeStart(totalDayTime * def.getPvPTimeStart() / def.getTotalDayTime());
            def.setPvPTimeEnd(totalDayTime * def.getPvPTimeEnd() / def.getTotalDayTime());
            def.setTotalDayTime(totalDayTime);
        } else if (def.isEnabled()) {
            // If the timeline isn't available for this dimension, we'll use the automatic engine mode by default
            def.setEngineMode(2);
        }

        config.loadDimension(key.identifier().toString(), def, "Configuration for " + key.identifier());

        engine.setWorldOptions(key, def);
    }

    private Timeline getLevelTimeline(Level level) {
        if (level == null) return null;

        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElse(null);
        if (clock == null) return null;

        Optional<Registry<Timeline>> registry = level.registryAccess().lookup(Registries.TIMELINE);
        if (registry.isEmpty()) return null;

        return registry.get()
                .listElements()
                .filter(tl -> tl.value().clock().equals(clock))
                .map(Holder.Reference::value)
                .findFirst()
                .orElse(null);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if(ticksLeft-- <= 0) {
            ticksLeft = engine.update();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommand(CommandEvent event) {
        // Force an update when a command is triggered
        // This prevents time commands from messing up the ticks count
        ticksLeft = 2;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttackEntity(AttackEntityEvent event) {
        Entity victim = event.getTarget();
        Player attacker = event.getEntity();

        // Ignore if a player hit himself (Arrow?)
        if(victim.getId() == attacker.getId()) return;

        // Ignore if the victim is not a player
        if(!(victim instanceof Player)) return;

        Boolean isPvPTime = engine.isPvPTime(victim.level().dimension());

        // Cancel the event when it's not pvp time
        if(isPvPTime != null && !isPvPTime) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(EntityInvulnerabilityCheckEvent event) {
        if (event.isInvulnerable()) return;

        DamageSource source = event.getSource();

        Entity attacker = source.getEntity();
        if(attacker == null) return;

        Entity victim = event.getEntity();

        // Ignore if a player hit himself (Arrow?)
        if(victim.getId() == attacker.getId()) return;

        // Ignore if the victim is not a player
        if(!(victim instanceof Player)) return;

        // Ignore if the attacker is not a player
        if(!(attacker instanceof Player)) return;

        Boolean isPvPTime = engine.isPvPTime(victim.level().dimension());

        // Cancel the event when it's not pvp time
        if(isPvPTime != null && !isPvPTime) {
            event.setInvulnerable(true);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (engine == null) return;

        LevelAccessor levelAccessor = event.getLevel();

        if (!(levelAccessor instanceof ServerLevel level)) return;

        IWorldOptions options = engine.getWorldOptions(level.dimension());
        if(options == null) {
            if(defaultOptions == null) defaultOptions = new WorldOptions();
            loadDimension(defaultOptions, level.dimension(), level);
        }

        ticksLeft = 2;
    }

}
