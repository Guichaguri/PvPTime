package me.guichaguri.pvptime;

import net.minecraftforge.common.DimensionManager;

import java.util.HashMap;

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
        WorldOptions options = worlds.get(dimension);
        if(!options.isEnabled()) return null;
        long currentTime = DimensionManager.getWorld(dimension).getWorldTime();
        long startTime = options.getPvPTimeStart();
        long endTime = options.getPvPTimeEnd();
        boolean pt;
        if(startTime < endTime) {
            pt = ( currentTime > startTime && currentTime < endTime);
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

}
