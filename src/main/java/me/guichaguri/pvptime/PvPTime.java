package me.guichaguri.pvptime;

import me.guichaguri.pvptime.api.PvPTimeEvent.PvPTimeWorldSetupEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import java.io.File;

@Mod(modid="PvPTime", name="PvPTime", version="1.0.9", acceptableRemoteVersions = "*")
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

        loadConfig(config, "dimension_" + id, id, w != null ? w.provider.getDimensionType().getName() : null, id == 0);
    }

    protected void loadConfig(Configuration config, String cat, int id, String dimName, boolean isOverworld) {
        PvPTimeWorldSetupEvent setup = new PvPTimeWorldSetupEvent(id, isOverworld);
        MinecraftForge.EVENT_BUS.post(setup);

        config.setCategoryComment(cat, "Options for dimension " + id +
                (dimName != null ? (" - " + dimName) : ""));

        WorldOptions o = setup.defaultOptions;
        if(o == null) {
            o = new WorldOptions(isOverworld);
            o.setEngineMode(isOverworld ? 1 : 2);
        }

        o.setEnabled(config.get(cat, "enabled", o.isEnabled(), "If PvPTime will be disabled on this dimension").getBoolean());
        o.setEngineMode(config.get(cat, "engineMode", o.getEngineMode(), "1: Configurable Time - 2: Automatic").getInt());
        o.setTotalDayTime(config.get(cat, "totalDayTime", o.getTotalDayTime(), "The total time that a Minecraft day has").getInt());
        o.setPvPTimeStart(config.get(cat, "startTime", o.getPvPTimeStart(), "Time in ticks that the PvP will be enabled").getInt());
        o.setPvPTimeEnd(config.get(cat, "endTime", o.getPvPTimeEnd(), "Time in ticks that the PvP will be disabled").getInt());
        o.setStartMessage(config.get(cat, "startMessage", o.getStartMessage(), "Message to be broadcasted when the PvP Time starts").getString());
        o.setEndMessage(config.get(cat, "endMessage", o.getEndMessage(), "Message to be broadcasted when the PvP Time ends").getString());
        o.setStartCmds(config.get(cat, "startCmds", o.getStartCmds(), "Commands to be executed when the PvPTime starts").getStringList());
        o.setEndCmds(config.get(cat, "endCmds", o.getEndCmds(), "Commands to be executed when the PvPTime ends").getStringList());

        PvPTimeRegistry.setWorldOptions(id, o);
    }

}
