package me.guichaguri.pvptime;

import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import java.io.File;

@Mod(modid="PvPTime", name="PvPTime", version="1.0.3", acceptableRemoteVersions = "*")
public class PvPTime {

    @Mod.Instance(value = "PvPTime")
    public static PvPTime INSTANCE;

    private File configFile;

    public boolean onlyMultiplayer = true;
    public boolean atLeastTwoPlayers = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
    }

    @Mod.EventHandler
    public void start(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandPvPTime());
    }

    @Mod.EventHandler
    public void postStart(FMLServerStartedEvent event) {
        Log.info("Loading PvPTime config...");
        loadConfig();

        PvPTimeUpdater.register();
    }

    @Mod.EventHandler
    public void stop(FMLServerStoppingEvent event) {
        PvPTimeUpdater.unregister();
    }


    public void loadConfig() {
        Configuration config = new Configuration(configFile);
        config.load();

        onlyMultiplayer = config.get("General", "onlyMultiplayer", true, "Messages will broadcast when is a server or lan").getBoolean();
        atLeastTwoPlayers = config.get("General", "atLeastTwoPlayers", false,
                                "Messages will broadcast if there's at least two players online").getBoolean();

        boolean bukkit = CompatibilityManager.isBukkitServer();
        boolean sponge = CompatibilityManager.isSpongeServer();

        for(int id : DimensionManager.getIDs()) {
            loadDimensionFromConfig(config, id, bukkit, sponge);
        }

        config.save();
    }

    private void loadDimensionFromConfig(Configuration config, int id, boolean bukkit, boolean sponge) {
        World w = DimensionManager.getWorld(id);
        if(sponge) {
            if(CompatibilityManager.createSpongeCompatibility(config, w, w)) return;
        }
        if(bukkit) {
            Object bukkitWorld = CompatibilityManager.getBukkitWorld(w);
            if(bukkitWorld != null) {
                if(CompatibilityManager.createBukkitCompatibility(config, bukkitWorld, w)) return;
            }
        }

        String cat = "dimension_" + id;
        config.setCategoryComment(cat, "Options for dimension " + id +
                w == null ? "" : (" - " + w.provider.getDimensionName()));
        boolean enabled = config.get(cat, "enabled", id == 0).getBoolean();
        long start = config.get(cat, "startTime", 13000, "Time in ticks that the PvP will be enabled").getInt();
        long end = config.get(cat, "endTime", 500, "Time in ticks that the PvP will be disabled").getInt();
        String startMsg = config.get(cat, "startMessage", "&cIt's night and PvP is turned on").getString();
        String endMsg = config.get(cat, "endMessage", "&aIt's daytime and PvP is turned off").getString();
        WorldOptions options = new WorldOptions(enabled, start, end, startMsg, endMsg);
        PvPTimeRegistry.setWorldOptions(id, options);
    }
}
