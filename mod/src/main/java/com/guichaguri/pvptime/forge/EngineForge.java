package com.guichaguri.pvptime.forge;

import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.common.PvPTime;
import gnu.trove.map.hash.THashMap;
import net.minecraft.command.ICommandManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

/**
 * @author Guilherme Chaguri
 */
public class EngineForge extends PvPTime<Integer> {

    private boolean onlyMultiplayer = true;

    public EngineForge() {
        super(new THashMap<>(), new THashMap<>());
    }

    public Boolean isRawPvPTime(World world, IWorldOptions options) {
        switch(options.getEngineMode()) {
            case -2:
                return true; // PvP always enabled on engine mode -2
            case -1:
                return false; // PvP always disabled on engine mode -1
            case 1:
                return checkPvPTime(options, world.getWorldTime());
            case 2:
                return world.isDaytime();
            default:
                return null;
        }
    }

    @Override
    protected Boolean isRawPvPTime(Integer dimension, IWorldOptions options) {
        World world = DimensionManager.getWorld(dimension);
        if(world == null) return null;

        return isRawPvPTime(world, options);
    }

    @Override
    protected long getTimeLeft(Integer dimension, IWorldOptions options, boolean isPvPTime) {
        switch(options.getEngineMode()) {
            case 1:
                World w = DimensionManager.getWorld(dimension);
                if(w == null) break;
                return calculateTimeLeft(options, w.getWorldTime(), isPvPTime);
            case 2:
                return 20;
        }
        return Long.MAX_VALUE;
    }

    @Override
    protected void announce(Integer dimension, IWorldOptions options, boolean isPvPTime) {
        World w = DimensionManager.getWorld(dimension);
        if(w == null) return;

        MinecraftServer server = w.getMinecraftServer();

        String[] cmds = isPvPTime ? options.getStartCmds() : options.getEndCmds();

        // Runs the commands if any
        if(cmds != null && cmds.length > 0 && server != null) {
            ICommandManager manager = server.getCommandManager();
            for(String cmd : cmds) manager.executeCommand(server, cmd);
        }

        String msg = isPvPTime ? options.getStartMessage() : options.getEndMessage();

        // Announces the message if it's not empty
        if(msg == null || msg.isEmpty()) return;

        if(atLeastTwoPlayers) {
            // Only announces when there are at least two players online

            if(server == null) return; // It's not even a server
            if(server.getPlayerList().getCurrentPlayerCount() < 2) return;

        } else if(onlyMultiplayer) {
            // Only announces when it's a server

            if(server == null) return; // It's not a server

            // It's single player, but we have to check if it's open to LAN
            if(server.isSinglePlayer() && !((IntegratedServer)server).getPublic()) return;
        }

        // Creates the text component, converting all color codes
        ITextComponent c = new TextComponentString(msg.replaceAll("&([0-9a-fk-or])", "\u00a7$1"));

        // Sends the message for all players in the dimension
        for(EntityPlayer p : w.playerEntities) p.sendMessage(c);
    }

    @Override
    public Integer getDimension(Object dimension) {
        if(dimension instanceof Integer) {
            return (Integer)dimension;
        } else if(dimension instanceof World) {
            return ((World)dimension).provider.getDimension();
        }
        return null;
    }

    public void setOnlyMultiplayer(boolean onlyMultiplayer) {
        this.onlyMultiplayer = onlyMultiplayer;
    }
}
