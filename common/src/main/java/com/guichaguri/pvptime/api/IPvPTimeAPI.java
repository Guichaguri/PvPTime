package com.guichaguri.pvptime.api;

/**
 * @author Guilherme Chaguri
 */
public interface IPvPTimeAPI<D> {

    D getDimension(Object dimension);

    Boolean isPvPTime(D dimension);

    IWorldOptions getWorldOptions(D dimension);

    void setWorldOptions(D dimension, IWorldOptions options);

    IWorldOptions createWorldOptions(IWorldOptions base);

    IWorldOptions createWorldOptions();

}
