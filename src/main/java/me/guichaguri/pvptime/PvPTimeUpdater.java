package me.guichaguri.pvptime;

import java.util.HashMap;
import java.util.List;
import me.guichaguri.pvptime.api.PvPTimeEvent.PvPTimeUpdateEvent;
import net.minecraft.command.ICommandManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PvPTimeUpdater {
    private static long ticksLeft = 100;

    private static PvPTimeTickUpdater tick = null;
    private static PvPTimeEventListener event = null;
    public static void register() {
        unregister();
        tick = new PvPTimeTickUpdater();
        event = new PvPTimeEventListener();
        FMLCommonHandler.instance().bus().register(tick);
        MinecraftForge.EVENT_BUS.register(event);
        update();
        HashMap<Integer, Boolean> hs = PvPTimeRegistry.getPvPTimeMap();
        for(int id : DimensionManager.getIDs()) {
            hs.put(id, PvPTimeRegistry.isRawPvPTime(id));
        }
    }
    public static void unregister() {
        if(tick != null) FMLCommonHandler.instance().bus().unregister(tick);
        if(event != null) MinecraftForge.EVENT_BUS.unregister(event);
        tick = null;
        event = null;
    }

    public static class PvPTimeTickUpdater {
        @SubscribeEvent
        public void ServerTickEvent(TickEvent.ServerTickEvent event) {
            ticksLeft--;
            if (ticksLeft < 0) {
                update();
            }
        }
    }

    public static class PvPTimeEventListener {
        @SubscribeEvent
        public void CommandEvent(CommandEvent event) {
            ticksLeft = 2;
        }

        @SubscribeEvent
        public void AttackEntityEvent(AttackEntityEvent event) {
            if(event.isCanceled()) return;
            if(event.target.getEntityId() == event.entityPlayer.getEntityId()) return;
            if(!(event.target instanceof EntityPlayer)) return;
            World w = event.entityPlayer.getEntityWorld();
            if(!PvPTimeRegistry.isPvPTime(w.provider.getDimensionId())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void LivingAttackEvent(LivingAttackEvent event) {
            if(event.isCanceled()) return;
            if(event.source == null) return;
            Entity damager = event.source.getEntity();
            Entity defender = event.entity;
            if(damager == null) return;
            if(defender == null) return;
            if(defender.getEntityId() == damager.getEntityId()) return;
            if(!(defender instanceof EntityPlayer)) return;
            if(damager instanceof EntityPlayer) {
                World w = damager.worldObj;
                if(!PvPTimeRegistry.isPvPTime(w.provider.getDimensionId())) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public void WorldLoad(WorldEvent.Load event) {
            int id = event.world.provider.getDimensionId();
            WorldOptions options = PvPTimeRegistry.getWorldOptions(id);
            if(options == null) {
                PvPTime.INSTANCE.loadConfig(); // Lets reload the config
            }
        }
    }

    private static void update() {
        long i = 10000;
        HashMap<Integer, WorldOptions> o = PvPTimeRegistry.getWorldOptionMap();
        HashMap<Integer, Boolean> pt = PvPTimeRegistry.getPvPTimeMap();
        for(int id : DimensionManager.getIDs()) {
            if(!o.containsKey(id)) continue;
            WorldOptions options = o.get(id);
            if(!options.isEnabled()) continue;
            World w = DimensionManager.getWorld(id);
            long time = PvPTimeRegistry.getDayTime(options, w);
            long start = options.getPvPTimeStart();
            long end = options.getPvPTimeEnd();

            Boolean was = pt.containsKey(id) ? pt.get(id) : null;
            Boolean is = PvPTimeRegistry.isRawPvPTime(id);
            if(was != null) {
                if(is != was) {
                    PvPTimeUpdateEvent update = new PvPTimeUpdateEvent(w, is);
                    MinecraftForge.EVENT_BUS.post(update);

                    if(update.tryToAnnounce) {
                        announce(w, is ? options.getStartMessage() : options.getEndMessage(),
                                is ? options.getStartCmds() : options.getEndCmds());
                    }
                    pt.put(id, is);
                }
            } else {
                pt.put(id, is);
            }

            /* Calc ticks left to check time again */

            long timeLeft = 10000;

            if(is) {
                if (time > end) {
                    timeLeft = w.getTotalWorldTime() - time + end;
                } else {
                    timeLeft = end - time;
                }
            } else {
                if(time > start) {
                    timeLeft = w.getTotalWorldTime() - time + start;
                } else {
                    timeLeft = start - time;
                }
            }

            if(timeLeft < i) i = timeLeft;
        }
        ticksLeft = i;
    }

    private static void announce(World w, String msg, String[] cmds) {
        if((cmds != null) && (cmds.length > 0)) {
            MinecraftServer server = MinecraftServer.getServer();
            if(server == null) return;
            ICommandManager manager = server.getCommandManager();
            for(String cmd : cmds) {
                manager.executeCommand(server, cmd);
            }
        }
        if((msg == null) || (msg.isEmpty())) return;
        if(PvPTime.INSTANCE.atLeastTwoPlayers) {
            MinecraftServer server = MinecraftServer.getServer();
            if(server == null) return; // Its not even a server
            if(server.getConfigurationManager().playerEntityList.size() < 2) return;
        } else if(PvPTime.INSTANCE.onlyMultiplayer) {
            MinecraftServer server = MinecraftServer.getServer();
            if(server == null) return;
            if(server.isSinglePlayer()) {
                IntegratedServer intServer = (IntegratedServer)server;
                if(!intServer.getPublic()) return;
            }
        }
        ChatComponentText c = new ChatComponentText(msg.replaceAll("&([0-9a-fk-or])", "\u00a7$1"));
        for(EntityPlayer p : (List<EntityPlayer>)w.playerEntities) {
            p.addChatMessage(c);
        }
    }
}
