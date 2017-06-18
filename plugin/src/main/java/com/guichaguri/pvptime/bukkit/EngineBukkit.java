package com.guichaguri.pvptime.bukkit;

import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.common.PvPTime;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 * @author Guilherme Chaguri
 */
public class EngineBukkit extends PvPTime<String> {

    private Plugin worldguard, towny;

    public EngineBukkit() {
        super(new HashMap<>(), new HashMap<>());
        prepareDependencies();
    }

    private void prepareDependencies() {
        PluginManager manager = Bukkit.getServer().getPluginManager();
        worldguard = manager.getPlugin("WorldGuard");
        towny = manager.getPlugin("Towny");
    }

    private boolean isPvPForced(Location loc) {
        if(worldguard != null) {
            WorldGuardPlugin wg = (WorldGuardPlugin)worldguard;
            ApplicableRegionSet set = wg.getRegionManager(loc.getWorld()).getApplicableRegions(loc);

            for(ProtectedRegion region : set) {
                // region.getFlag(DefaultFlag.PVP) leads to runtime errors for whatever reason
                if(region.getFlags().get(DefaultFlag.PVP) == State.ALLOW) return true;
            }
        }

        return false;
    }

    protected boolean isPvPForced(Location attacker, Location victim) {
        if(isPvPForced(attacker) && isPvPForced(victim)) return true;

        if(towny != null) {
            if(TownyUniverse.isWarTime()) return true;
        }

        return false;
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
