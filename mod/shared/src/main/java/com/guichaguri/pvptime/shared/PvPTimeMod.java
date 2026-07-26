package com.guichaguri.pvptime.shared;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.api.PvPTimeAPI;
import com.guichaguri.pvptime.common.WorldOptions;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.timeline.Timeline;

import java.util.Optional;

public abstract class PvPTimeMod {
    protected final IPvPTimeConfig config;
    protected EngineMinecraft engine;
    protected WorldOptions defaultOptions;

    protected long ticksLeft = 0;

    public PvPTimeMod(IPvPTimeConfig config) {
        this.config = config;
    }

    public IPvPTimeAPI<ResourceKey<Level>> getAPI() {
        return engine;
    }

    public void reloadConfig(MinecraftServer server) {
        config.load();
        engine.resetWorldOptions();
        loadConfig(server);
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

    protected void loadDimension(WorldOptions defaultOptions, ResourceKey<Level> key, Level level) {
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

    protected Timeline getLevelTimeline(Level level) {
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

    protected void handleRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        PvPTimeCommand command = new PvPTimeCommand(this);

        command.register(dispatcher);
    }

    protected void handleServerStarting(MinecraftServer server) {
        config.load();

        engine = new EngineMinecraft(server);
        PvPTimeAPI.setAPI(engine);
    }

    protected void handleServerStarted(MinecraftServer server) {
        loadConfig(server);
    }

    protected void handleServerStopping(MinecraftServer server) {
        config.save();

        engine = null;
        PvPTimeAPI.setAPI(null);
    }

    protected void handleServerTick(MinecraftServer server) {
        if (ticksLeft-- <= 0) {
            ticksLeft = engine.update();
        }
    }

    protected void handleCommandPerformed() {
        // Force an update when a command is triggered
        // This prevents time commands from messing up the ticks count
        if (ticksLeft > 2) {
            ticksLeft = 2;
        }
    }

    protected boolean handleAllowEntityAttack(Player attacker, Entity victim) {
        // Ignore if a player hit himself (Arrow?)
        if (victim.getId() == attacker.getId()) return true;

        // Ignore if the victim is not a player
        if (!(victim instanceof Player)) return true;

        Boolean isPvPTime = engine.isPvPTime(victim.level().dimension());

        // Cancel the event when it's not pvp time
        if (isPvPTime != null && !isPvPTime) {
            return false;
        }

        return true;
    }

    protected void handleWorldLoad(ServerLevel level) {
        if (engine == null) return;

        IWorldOptions options = engine.getWorldOptions(level.dimension());
        if (options == null) {
            if (defaultOptions == null) defaultOptions = new WorldOptions();
            loadDimension(defaultOptions, level.dimension(), level);
        }

        ticksLeft = 2;
    }

}
