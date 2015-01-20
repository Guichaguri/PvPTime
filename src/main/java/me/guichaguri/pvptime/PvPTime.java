package me.guichaguri.pvptime;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.*;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

@Mod(modid="PvPTime", name="PvPTime", version="1.0.0")
public class PvPTime {

    @Mod.Instance(value = "PvPTime")
    public static PvPTime instance;

    private File configFile;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
    }

    @Mod.EventHandler
    public void start(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandPvPTime());

        PvPTimeUpdater.register();
    }

    @Mod.EventHandler
    public void postStart(FMLServerStartedEvent event) {
        Log.info("Loading PvPTime config...");
        loadConfig();
    }

    @Mod.EventHandler
    public void start(FMLServerStoppingEvent event) {
        PvPTimeUpdater.unregister();
    }


    public void loadConfig() {
        Configuration config = new Configuration(configFile);
        config.load();

        for(int id : DimensionManager.getIDs()) {
            String cat = "dimension_" + id;
            config.setCategoryComment(cat, "Options for dimension " + id);
            boolean enabled = config.get(cat, "enabled", id == 0).getBoolean();
            long start = config.get(cat, "startTime", 13000).getInt();
            long end = config.get(cat, "endTime", 500).getInt();
            String startMsg = config.get(cat, "startMessage", "&cIt's night and PvP is turned on").getString();
            String endMsg = config.get(cat, "endMessage", "&aIt's daytime and PvP is turned off").getString();
            WorldOptions options = new WorldOptions(enabled, start, end, startMsg, endMsg);
            PvPTimeRegistry.setWorldOptions(id, options);
        }

        config.save();
    }
}
