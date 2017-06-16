package com.guichaguri.pvptime.sponge;

import com.guichaguri.pvptime.common.PvPTime;
import com.guichaguri.pvptime.api.IWorldOptions;
import java.util.HashMap;
import java.util.Optional;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

/**
 * @author Guilherme Chaguri
 */
public class EngineSponge extends PvPTime<String> {
    public EngineSponge() {
        super(new HashMap<>(), new HashMap<>());
    }

    @Override
    protected Boolean isRawPvPTime(String dimension, IWorldOptions options) {
        Optional<World> world = Sponge.getGame().getServer().getWorld(dimension);
        if(!world.isPresent()) return null;

        switch(options.getEngineMode()) {
            case -2:
                return true; // PvP always enabled on engine mode -2
            case -1:
                return false; // PvP always disabled on engine mode -1
            case 1:
            case 2:
                return checkPvPTime(options, world.get().getProperties().getWorldTime());
            default:
                return null;
        }
    }

    @Override
    protected long getTimeLeft(String dimension, IWorldOptions options, boolean isPvPTime) {
        switch(options.getEngineMode()) {
            case 1:
                Optional<World> w = Sponge.getGame().getServer().getWorld(dimension);
                if(!w.isPresent()) break;
                return calculateTimeLeft(options, w.get().getProperties().getWorldTime(), isPvPTime);
            case 2:
                return 20;
        }
        return Long.MAX_VALUE;
    }

    @Override
    protected void announce(String dimension, IWorldOptions options, boolean isPvPTime) {
        Server server = Sponge.getGame().getServer();
        Optional<World> w = server.getWorld(dimension);
        if(!w.isPresent()) return;

        String[] cmds = isPvPTime ? options.getStartCmds() : options.getEndCmds();

        // Runs the commands if any
        if(cmds != null && cmds.length > 0) {
            CommandManager manager = Sponge.getCommandManager();
            CommandSource source = server.getConsole();
            for(String cmd : cmds) manager.process(source, cmd);
        }

        String msg = isPvPTime ? options.getStartMessage() : options.getEndMessage();

        // Announces the message if it's not empty
        if(msg == null || msg.isEmpty()) return;

        if(atLeastTwoPlayers) {
            // Only announces when there are at least two players online
            if(server.getOnlinePlayers().size() < 2) return;
        }

        // Creates the text component, converting all color codes
        Text c = Text.of(msg.replaceAll("&([0-9a-fk-or])", "\u00a7$1"));

        // Sends the message for all players in the dimension
        for(Player p : w.get().getPlayers()) p.sendMessage(c);
    }

    @Override
    public String getDimension(Object dimension) {
        if(dimension instanceof String) {
            return (String)dimension;
        } else if(dimension instanceof World) {
            return ((World)dimension).getName();
        }
        return null;
    }
}
