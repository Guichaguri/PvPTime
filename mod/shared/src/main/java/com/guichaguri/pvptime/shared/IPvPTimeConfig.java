package com.guichaguri.pvptime.shared;

import com.guichaguri.pvptime.common.WorldOptions;

public interface IPvPTimeConfig {
    void load();

    void save();

    void loadEngine(EngineMinecraft engine);

    void loadDimension(String cat, WorldOptions options, String comment);
}
