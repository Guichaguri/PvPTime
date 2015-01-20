package me.guichaguri.pvptime.api;

import me.guichaguri.pvptime.PvPTimeRegistry;
import me.guichaguri.pvptime.WorldOptions;

public class PvPTimeAPI {

    public static void setWorldOptions(int dimension, WorldOptions options) {
        PvPTimeRegistry.setWorldOptions(dimension, options);
    }

    public static WorldOptions getWorldOptions(int dimension) {
        return PvPTimeRegistry.getWorldOptions(dimension);
    }

    public static Boolean isPvPTime(int dimension) {
        return PvPTimeRegistry.isPvPTime(dimension);
    }

}
