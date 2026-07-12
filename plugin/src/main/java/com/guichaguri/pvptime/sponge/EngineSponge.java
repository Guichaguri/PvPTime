package com.guichaguri.pvptime.sponge;

import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.common.PvPTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;
import sawfowl.regionguard.api.Flags;
import sawfowl.regionguard.api.RegionAPI;
import sawfowl.regionguard.api.data.Region;
import sawfowl.regionguard.api.data.WorldRegions;

/**
 * @author Guilherme Chaguri
 */
public class EngineSponge extends PvPTime<ResourceKey> {
    private final Logger logger;
    private PluginContainer regionGuard;
    private boolean enableRegionGuard = false;

    public EngineSponge(Logger logger) {
        super();
        this.logger = logger;

        prepareDependencies();
    }

    private void prepareDependencies() {
        PluginManager manager = Sponge.game().pluginManager();
        regionGuard = manager.plugin("regionguard").orElse(null);
    }

    private boolean isRegionGuardPvPForced(ServerLocation location) {
        RegionAPI api = RegionAPI.getInstance();

        WorldRegions regions = api.getRegions(location.world());
        Region region = regions.findRegion(location.blockPosition());

        return region.getFlagResult(Flags.PVP, null, null) == Tristate.TRUE;
    }

    private boolean isPvPForced(ServerLocation location) {
        if (regionGuard != null && enableRegionGuard) {
            try {
                if (isRegionGuardPvPForced(location)) return true;
            } catch(Exception ex) {
                // May happen when the RegionGuard API changes
                regionGuard = null;
                logger.warn("Couldn't check whether the PvP is forced on RegionGuard.");
                logger.warn("The integration has been disabled for now.");
                logger.warn(ex);
            }
        }

        return false;
    }

    protected boolean isPvPForced(ServerLocation attacker, ServerLocation victim) {
        return isPvPForced(attacker) && isPvPForced(victim);
    }

    @Override
    protected Boolean isRawPvPTime(ResourceKey dimension, IWorldOptions options) {
        Optional<ServerWorld> world = Sponge.server().worldManager().world(dimension);
        if(world.isEmpty()) return null;

        return switch (options.getEngineMode()) {
            case -2 -> true; // PvP always enabled on engine mode -2
            case -1 -> false; // PvP always disabled on engine mode -1
            case 1, 2 -> checkPvPTime(options, world.get().properties().dayTime().asTicks().ticks());
            default -> null;
        };
    }

    @Override
    protected long getTimeLeft(ResourceKey dimension, IWorldOptions options, boolean isPvPTime) {
        switch(options.getEngineMode()) {
            case 1:
                Optional<ServerWorld> w = Sponge.server().worldManager().world(dimension);
                if(w.isEmpty()) break;
                return calculateTimeLeft(options, w.get().properties().dayTime().asTicks().ticks(), isPvPTime);
            case 2:
                // Doesn't calculate the time left automatically
                // instead, check for it every second
                return 20;
        }
        return Long.MAX_VALUE;
    }

    @Override
    protected void announce(ResourceKey dimension, IWorldOptions options, boolean isPvPTime) {
        Server server = Sponge.server();
        Optional<ServerWorld> w = server.worldManager().world(dimension);
        if(w.isEmpty()) return;

        List<String> cmds = isPvPTime ? options.getStartCommands() : options.getEndCommands();

        // Runs the commands if any
        if(cmds != null && !cmds.isEmpty()) {
            CommandManager manager = server.commandManager();
            for(String cmd : cmds) {
                try {
                    manager.process(cmd);
                } catch (CommandException ex) {
                    logger.error("Failed to run the command", ex);
                }
            }
        }

        String msg = isPvPTime ? options.getStartMessage() : options.getEndMessage();

        // Announces the message if it's not empty
        if(msg == null || msg.isEmpty()) return;

        if(atLeastTwoPlayers) {
            // Only announces when there are at least two players online
            if(server.onlinePlayers().size() < 2) return;
        }

        // Creates the text component, converting all color codes
        Component c;
        try {
            c = MiniMessage.miniMessage().deserialize(msg);
        } catch (Exception ex) {
            logger.warn("Failed to deserialize MiniMessage", ex);
            c = Component.text(msg);
        }

        // Sends the message for all players in the dimension
        for(Player p : w.get().players()) p.sendMessage(c);
    }

    @Override
    public ResourceKey getDimension(Object dimension) {
        if(dimension instanceof ResourceKey) {

            return (ResourceKey) dimension;

        } else if(dimension instanceof String) {

            return ResourceKey.resolve((String) dimension);

        } else if(dimension instanceof UUID) {

            return Sponge.server().worldManager().worldKey((UUID) dimension).orElse(null);

        } else if(dimension instanceof ServerWorld) {

            return ((ServerWorld) dimension).key();

        }
        return null;
    }

    /**
     * Whether the RegionGuard integration will be enabled
     * @param enableRegionGuard The flag
     */
    public void setEnableRegionGuard(boolean enableRegionGuard) {
        this.enableRegionGuard = enableRegionGuard;
    }
}
