package com.guichaguri.pvptime.forge;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.api.IWorldOptions;
import com.guichaguri.pvptime.api.PvPTimeAPI;
import com.guichaguri.pvptime.common.PvPTime;
import com.guichaguri.pvptime.common.WorldOptions;
import java.io.File;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCMessage;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

/**
 * @author Guilherme Chaguri
 */
@Mod(
        modid = "pvptime",
        name = "PvPTime",
        version = PvPTime.VERSION,
        acceptableRemoteVersions = "*"
)
public class PvPTimeForge {

    private EngineForge engine;

    private File configFile;
    private Configuration config;

    private WorldOptions defaultOptions;

    private long ticksLeft = 0;

    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        engine = new EngineForge();
        PvPTimeAPI.setAPI(engine);

        configFile = event.getSuggestedConfigurationFile();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void onStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new PvPTimeCommand(this));
    }

    @EventHandler
    public void onPostStart(FMLServerStartedEvent event) {
        loadConfig();
    }

    @EventHandler
    public void onStop(FMLServerStoppingEvent event) {
        // Saves the config file
        config.save();
    }

    @EventHandler
    public void imc(IMCEvent event) {
        for(IMCMessage msg : event.getMessages()) {
            if(!msg.key.equalsIgnoreCase("pvptime") || !msg.key.equalsIgnoreCase("api")) continue;

            Optional<Function<IPvPTimeAPI, Void>> func = msg.getFunctionValue(IPvPTimeAPI.class, Void.class);
            if(func.isPresent()) func.get().apply(engine);
        }
    }

    public IPvPTimeAPI<Integer> getAPI() {
        return engine;
    }

    protected void reloadConfig() {
        config = new Configuration(configFile);
        config.load();
        engine.resetWorldOptions();
    }

    protected void loadConfig() {
        if(config == null) reloadConfig();

        engine.setOnlyMultiplayer(config.get("general", "onlyMultiplayer", true, "Messages will broadcast when is a server or lan").getBoolean());
        engine.setAtLeastTwoPlayers(config.get("general", "atLeastTwoPlayers", false, "Messages will broadcast if there's at least two players online").getBoolean());
        config.setCategoryComment("general", "General Configuration");

        defaultOptions = new WorldOptions();
        config.setCategoryComment("default", "Default Options. The options below are copied to newly created dimensions");
        loadDimension(config, "default", defaultOptions);

        for(int id : DimensionManager.getIDs()) {
            loadDimension(defaultOptions, id);
        }

        ticksLeft = engine.update();
    }

    private void loadDimension(WorldOptions defaultOptions, int id) {
        World w = DimensionManager.getWorld(id);
        boolean isSurface = w != null ? w.provider.isSurfaceWorld() : id == 0;

        WorldOptions def = new WorldOptions(defaultOptions);
        def.setEnabled(isSurface || def.isEnabled());

        String cat = "dimension_" + id;
        String name = id + (w != null ? " (" + w.provider.getDimensionType().getName() : ")");
        config.setCategoryComment(cat, "Options for dimension " + name);

        loadDimension(config, cat, def);

        engine.setWorldOptions(id, def);
    }

    private void loadDimension(Configuration config, String cat, WorldOptions o) {
        o.setEnabled(config.get(cat, "enabled", o.isEnabled(), "Whether PvPTime will be disabled on this dimension").getBoolean());
        o.setEngineMode(config.get(cat, "engineMode", o.getEngineMode(), "1: Configurable Time | 2: Automatic | -1: PvP always disabled | 1: PvP always enabled").getInt());
        o.setTotalDayTime(config.get(cat, "totalDayTime", o.getTotalDayTime(), "The total time that a Minecraft day has").getInt());
        o.setPvPTimeStart(config.get(cat, "startTime", o.getPvPTimeStart(), "Time in ticks that the PvP will be enabled").getInt());
        o.setPvPTimeEnd(config.get(cat, "endTime", o.getPvPTimeEnd(), "Time in ticks that the PvP will be disabled").getInt());
        o.setStartMessage(config.get(cat, "startMessage", o.getStartMessage(), "Message to be broadcasted when the PvP Time starts").getString());
        o.setEndMessage(config.get(cat, "endMessage", o.getEndMessage(), "Message to be broadcasted when the PvP Time ends").getString());
        o.setStartCmds(config.get(cat, "startCmds", o.getStartCmds(), "Commands to be executed when the PvPTime starts").getStringList());
        o.setEndCmds(config.get(cat, "endCmds", o.getEndCmds(), "Commands to be executed when the PvPTime ends").getStringList());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if(ticksLeft-- <= 0) {
            ticksLeft = engine.update();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommand(CommandEvent event) {
        // Force an update when a command is triggered
        // This prevents time commands from messing up the ticks count
        ticksLeft = 2;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttackEntity(AttackEntityEvent event) {
        Entity victim = event.getTarget();
        EntityPlayer attacker = event.getEntityPlayer();

        // Ignore if a player hit himself (Arrow?)
        if(victim.getEntityId() == attacker.getEntityId()) return;

        // Ignore if the victim is not a player
        if(!(victim instanceof EntityPlayer)) return;

        Boolean isPvPTime = engine.isPvPTime(victim.getEntityWorld().provider.getDimension());

        // Cancel the event when it's not pvp time
        if(isPvPTime != null && !isPvPTime) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if(source == null) return;

        Entity attacker = source.getEntity();
        if(attacker == null) return;

        Entity victim = event.getEntity();
        if(victim == null) return;

        // Ignore if a player hit himself (Arrow?)
        if(victim.getEntityId() == attacker.getEntityId()) return;

        // Ignore if the victim is not a player
        if(!(victim instanceof EntityPlayer)) return;

        // Ignore if the attacker is not a player
        if(!(attacker instanceof EntityPlayer)) return;

        Boolean isPvPTime = engine.isPvPTime(victim.getEntityWorld().provider.getDimension());

        // Cancel the event when it's not pvp time
        if(isPvPTime != null && !isPvPTime) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(Load event) {
        int id = event.getWorld().provider.getDimension();

        IWorldOptions options = engine.getWorldOptions(id);
        if(options == null) {
            if(defaultOptions == null) loadConfig();
            loadDimension(defaultOptions, id);
        }
    }

}
