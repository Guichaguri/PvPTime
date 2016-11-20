package me.guichaguri.pvptime;

import java.util.HashMap;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class PvPTimeRegistry {

    private static HashMap<Integer, WorldOptions> worlds = new HashMap<Integer, WorldOptions>();
    private static HashMap<Integer, Boolean> pvpTime = new HashMap<Integer, Boolean>();

    public static void setWorldOptions(int dimension, WorldOptions options) {
        worlds.put(dimension, options);
    }

    public static WorldOptions getWorldOptions(int dimension) {
        if(worlds.containsKey(dimension)) {
            return worlds.get(dimension);
        }
        return null;
    }

    public static Boolean isPvPTime(int dimension) {
        if(pvpTime.containsKey(dimension)) {
            return pvpTime.get(dimension);
        } else if(worlds.containsKey(dimension)) {
            Boolean pt = isRawPvPTime(dimension);
            pvpTime.put(dimension, pt);
            return pt;
        }
        return null;
    }

    public static Boolean isRawPvPTime(int dimension) {
        if(!worlds.containsKey(dimension)) return null;
        WorldOptions options = worlds.get(dimension);
        if(!options.isEnabled()) return null;
        World w = DimensionManager.getWorld(dimension);
        if(w == null) return null;
        switch(options.getEngineMode()) {
            case -2:
                return true; // PvP always enabled on engine mode -2
            case -1:
                return false; // PvP always disabled on engine mode -1
            case 1:
                return isRawPvPTime1(options, w);
            case 2:
                return isRawPvPTime2(options, w);
            default:
                return null;
        }
    }

    /**
     * Engine Mode 2
     */
    public static Boolean isRawPvPTime2(WorldOptions options, World w) {
        return !w.isDaytime();
    }

    /**
     * Engine Mode 1
     */
    public static Boolean isRawPvPTime1(WorldOptions options, World w) {
        long currentTime = getDayTime(options, w);
        long startTime = options.getPvPTimeStart();
        long endTime = options.getPvPTimeEnd();
        boolean pt;
        if(startTime < endTime) {
            pt = (currentTime > startTime && currentTime < endTime);
        } else {
            pt = (currentTime > startTime || currentTime < endTime);
        }
        return pt;
    }

    public static HashMap<Integer, WorldOptions> getWorldOptionMap() {
        return worlds;
    }

    public static HashMap<Integer, Boolean> getPvPTimeMap() {
        return pvpTime;
    }

    public static long getDayTime(WorldOptions options, World w) {
        return (w.getWorldTime() % options.getTotalDayTime());
    }

}
