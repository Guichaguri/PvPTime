package com.guichaguri.pvptime.api;

/**
 * Holds an API instance
 * @author Guilherme Chaguri
 */
public class PvPTimeAPI {

    private static IPvPTimeAPI api;

    public static IPvPTimeAPI getAPI() {
        return api;
    }

    public static void setAPI(IPvPTimeAPI instance) {
        api = instance;
    }

}
