package com.guichaguri.pvptime.neoforge;

import com.electronwill.nightconfig.core.concurrent.SynchronizedConfig;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.guichaguri.pvptime.common.WorldOptions;
import com.guichaguri.pvptime.shared.EngineMinecraft;
import com.guichaguri.pvptime.shared.IPvPTimeConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public class PvPTimeNeoForgeConfig implements IPvPTimeConfig {
    public static final Logger LOGGER = LogUtils.getLogger();

    private final Path configFile = FMLPaths.CONFIGDIR.get().resolve("pvptime.toml");
    private final SynchronizedConfig config = new SynchronizedConfig(TomlFormat.instance(), LinkedHashMap::new);

    @Override
    public void load() {
        if (!Files.exists(configFile)) return;

        try (var reader = Files.newBufferedReader(configFile)) {
            config.bulkCommentedUpdate(view -> {
                new TomlParser().parse(reader, view, ParsingMode.REPLACE);
            });
        } catch (IOException ex) {
            LOGGER.error("Failed to load PvPTime config", ex);
        }
    }

    @Override
    public void save() {
        new TomlWriter().write(config, configFile, WritingMode.REPLACE_ATOMIC);
    }

    private <T> T get(String cat, String key, T def, String comment) {
        String path = cat + "." + key;

        if (!config.contains(path)) {
            config.setComment(path, comment);
            config.set(path, def);
        }

        return config.getOrElse(path, def);
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
