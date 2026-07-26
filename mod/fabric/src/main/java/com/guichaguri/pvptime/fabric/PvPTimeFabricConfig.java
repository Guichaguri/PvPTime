package com.guichaguri.pvptime.fabric;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.guichaguri.pvptime.common.WorldOptions;
import com.guichaguri.pvptime.shared.EngineMinecraft;
import com.guichaguri.pvptime.shared.IPvPTimeConfig;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class PvPTimeFabricConfig implements IPvPTimeConfig {
    public static final Logger LOGGER = LogUtils.getLogger();

    private final Path configFile = FabricLoader.getInstance().getConfigDir().resolve("pvptime.json");
    private JsonObject config = new JsonObject();

    @Override
    public void load() {
        if (!Files.exists(configFile)) return;

        try (var input = Files.newInputStream(configFile)) {
            JsonElement data = Jankson.builder().build().loadElement(input);
            config = data instanceof JsonObject ? (JsonObject) data : new JsonObject();
        } catch (Exception ex) {
            LOGGER.error("Failed to load PvPTime config", ex);
        }
    }

    @Override
    public void save() {
        try (var output = Files.newBufferedWriter(configFile)) {
            output.write(config.toJson(true, true));
        } catch (Exception ex) {
            LOGGER.error("Failed to load PvPTime config", ex);
        }
    }

    private <T> T get(String cat, String key, T def, String comment) {
        return config.putDefault(cat + "." + key, def, comment);
    }

    @Override
    public void loadEngine(EngineMinecraft engine) {
        engine.setOnlyMultiplayer(get("general", "onlyMultiplayer", true, "Whether messages will broadcast when is a server or lan"));
        engine.setAtLeastTwoPlayers(get("general", "atLeastTwoPlayers", false, "Whether messages will broadcast if there's at least two players online"));
        config.setComment("general", "General Configuration");
    }

    @Override
    public void loadDimension(String cat, WorldOptions o, String comment) {
        o.setEnabled(get(cat, "enabled", o.isEnabled(), "Whether PvPTime will be disabled on this dimension"));
        o.setEngineMode(get(cat, "engineMode", o.getEngineMode(), "1: Configurable Time | 2: Automatic | -1: PvP always disabled | -2: PvP always enabled"));
        o.setTotalDayTime(get(cat, "totalDayTime", o.getTotalDayTime(), "The total time that a Minecraft day has"));
        o.setPvPTimeStart(get(cat, "startTime", o.getPvPTimeStart(), "Time in ticks that the PvP will be enabled"));
        o.setPvPTimeEnd(get(cat, "endTime", o.getPvPTimeEnd(), "Time in ticks that the PvP will be disabled"));
        o.setStartMessage(get(cat, "startMessage", o.getStartMessage(), "Message to be broadcasted when the PvP Time starts"));
        o.setEndMessage(get(cat, "endMessage", o.getEndMessage(), "Message to be broadcasted when the PvP Time ends"));
        o.setStartCommands(get(cat, "startCmds", o.getStartCommands(), "Commands to be executed when the PvPTime starts"));
        o.setEndCommands(get(cat, "endCmds", o.getEndCommands(), "Commands to be executed when the PvPTime ends"));
        config.setComment(cat, comment);
    }
}
