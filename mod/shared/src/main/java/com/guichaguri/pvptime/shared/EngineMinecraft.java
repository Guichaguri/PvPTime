package com.guichaguri.pvptime.shared;

import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.common.PvPTime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.List;

/**
 * @author Guilherme Chaguri
 */
public class EngineMinecraft extends PvPTime<ResourceKey<Level>> {

    private final MinecraftServer server;
    private boolean onlyMultiplayer = true;

    public EngineMinecraft(MinecraftServer server) {
        this.server = server;
    }

    public Boolean isRawPvPTime(Level world, IWorldOptions options) {
        return switch (options.getEngineMode()) {
            case -2 -> true; // PvP always enabled on engine mode -2
            case -1 -> false; // PvP always disabled on engine mode -1
            case 1 -> checkPvPTime(options, world.getDefaultClockTime());
            case 2 -> world.isDarkOutside();
            default -> null;
        };
    }

    @Override
    protected Boolean isRawPvPTime(ResourceKey<Level> dimension, IWorldOptions options) {
        Level world = server.getLevel(dimension);
        if(world == null) return null;

        return isRawPvPTime(world, options);
    }

    @Override
    protected long getTimeLeft(ResourceKey<Level> dimension, IWorldOptions options, boolean isPvPTime) {
        switch(options.getEngineMode()) {
            case 1:
                Level w = server.getLevel(dimension);
                if(w == null) break;
                return calculateTimeLeft(options, w.getDefaultClockTime(), isPvPTime);
            case 2:
                // Check whether it's daytime every second
                return 20;
        }
        return Long.MAX_VALUE;
    }

    @Override
    protected void announce(ResourceKey<Level> dimension, IWorldOptions options, boolean isPvPTime) {
        Level w = server.getLevel(dimension);
        if(w == null) return;

        List<String> cmds = isPvPTime ? options.getStartCommands() : options.getEndCommands();

        // Runs the commands if any
        if(cmds != null && !cmds.isEmpty()) {
            Commands manager = server.getCommands();
            CommandSourceStack commandSource = server.createCommandSourceStack();
            for(String cmd : cmds) {
                manager.performPrefixedCommand(commandSource, cmd);
            }
        }

        String msg = isPvPTime ? options.getStartMessage() : options.getEndMessage();

        // Announces the message if it's not empty
        if(msg == null || msg.isEmpty()) return;

        if(atLeastTwoPlayers) {
            // Only announces when there are at least two players online

            if(server.getPlayerList().getPlayerCount() < 2) return;

        } else if(onlyMultiplayer) {
            // Only announces when it's a server

            // It's single player, but we have to check if it's open to LAN
            if(server.isSingleplayer() && !server.isPublished()) return;
        }

        // Creates the text component, converting all color codes
        Component c = Component.literal(msg.replaceAll("&([0-9a-fk-or])", "\u00a7$1"));

        // Sends the message for all players in the dimension
        for(Player p : w.players()) p.sendSystemMessage(c);
    }

    @Override
    public ResourceKey<Level> getDimension(Object dimension) {
        if(dimension instanceof ResourceKey<?> &&
                ((ResourceKey<?>) dimension).registry().equals(Registries.DIMENSION.identifier())) {

            //noinspection unchecked
            return (ResourceKey<Level>) dimension;

        } else if(dimension instanceof String name) {

            for(ServerLevel w : server.getAllLevels()) {
                if(name.equals(w.dimension().identifier().toString())) {
                    return w.dimension();
                }
            }

        } else if(dimension instanceof Level) {

            return ((Level) dimension).dimension();

        } else if(dimension instanceof DimensionType type) {

            for(ServerLevel w : server.getAllLevels()) {
                if(type.equals(w.dimensionType())) {
                    return w.dimension();
                }
            }

        }
        return null;
    }

    /**
     * Whether it will only announce PvP updates when its a LAN or a dedicated server.
     * @param onlyMultiplayer - {@code true} if will only announce in multiplayer
     */
    public void setOnlyMultiplayer(boolean onlyMultiplayer) {
        this.onlyMultiplayer = onlyMultiplayer;
    }

}
