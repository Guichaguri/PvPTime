package com.guichaguri.pvptime.api;

/**
 * PvPTime api
 * @author Guilherme Chaguri
 */
public interface IPvPTimeAPI<D> {

    /**
     * Gets the dimension identifier
     * Forge: The dimension id (int) is the identifier, this also accepts a World object
     * Bukkit: The dimension name (string) is the identifier, this also accepts a World object
     * Sponge: The dimension name (string) is the identifier, this also accepts a World object
     *
     * @param dimension The dimension object
     * @return The dimension identifier
     */
    D getDimension(Object dimension);

    /**
     * Check if it's PvP time in a dimension
     * @param dimension The dimension identifier
     * @return Whether it's pvp time or {@code null} if the PvP time is disabled for this dimension
     */
    Boolean isPvPTime(D dimension);

    /**
     * Gets the options for a dimension
     * @param dimension The dimension identifier
     * @return The options
     */
    IWorldOptions getWorldOptions(D dimension);

    /**
     * Sets the options for a dimension
     * @param dimension The dimension identifier
     * @param options The options
     */
    void setWorldOptions(D dimension, IWorldOptions options);

    /**
     * Creates a options object based on another one
     * @param base The base for the options
     * @return The new options object
     */
    IWorldOptions createWorldOptions(IWorldOptions base);

    /**
     * Creates a options object
     * @return The new options object
     */
    IWorldOptions createWorldOptions();

}
