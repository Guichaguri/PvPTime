package com.guichaguri.pvptime.bukkit;

import com.guichaguri.pvptime.common.PvPTime;
import com.guichaguri.pvptime.api.IWorldOptions;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Guilherme Chaguri
 */
public class EngineBukkit extends PvPTime<String> {
    public EngineBukkit() {
        super(new HashMap<>(), new HashMap<>());
    }

    @Override
    protected Boolean isRawPvPTime(String dimension, IWorldOptions options) {
        World world = Bukkit.getWorld(dimension);
        if(world == null) return null;

        switch(options.getEngineMode()) {
            case -2:
                return true; // PvP always enabled on engine mode -2
            case -1:
                return false; // PvP always disabled on engine mode -1
            case 1:
            case 2:
                return checkPvPTime(options, world.getFullTime());
            default:
                return null;
        }
    }

    @Override
    protected long getTimeLeft(String dimension, IWorldOptions options, boolean isPvPTime) {
        switch(options.getEngineMode()) {
            case 1:
                World w = Bukkit.getWorld(dimension);
                if(w == null) break;
                return calculateTimeLeft(options, w.getFullTime(), isPvPTime);
            case 2:
                return 20;
        }
        return Long.MAX_VALUE;
    }

    @Override
    protected void announce(String dimension, IWorldOptions options, boolean isPvPTime) {
        World w = Bukkit.getWorld(dimension);
        if(w == null) return;

        String[] cmds = isPvPTime ? options.getStartCmds() : options.getEndCmds();

        // Runs the commands if any
        if(cmds != null && cmds.length > 0) {
            CommandSender sender = Bukkit.getConsoleSender();
            for(String cmd : cmds) Bukkit.dispatchCommand(sender, cmd);
        }

        String msg = isPvPTime ? options.getStartMessage() : options.getEndMessage();

        // Announces the message if it's not empty
        if(msg == null || msg.isEmpty()) return;

        if(atLeastTwoPlayers) {
            // Only announces when there are at least two players online
            if(Bukkit.getOnlinePlayers().size() < 2) return;
        }

        // Converts all color codes
        String c = msg.replaceAll("&([0-9a-fk-or])", "\u00a7$1");

        // Sends the message for all players in the dimension
        for(Player p : w.getPlayers()) p.sendMessage(c);
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
