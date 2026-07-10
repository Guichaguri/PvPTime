package com.guichaguri.pvptime.api;

/**
 * Holds an API instance
 * @author Guilherme Chaguri
 */
public class PvPTimeAPI {

    private static IPvPTimeAPI<?> api;

    public static <T> IPvPTimeAPI<T> getAPI() {
        //noinspection unchecked
        return (IPvPTimeAPI<T>) api;
    }

    public static void setAPI(IPvPTimeAPI<?> instance) {
        api = instance;
    }

}
